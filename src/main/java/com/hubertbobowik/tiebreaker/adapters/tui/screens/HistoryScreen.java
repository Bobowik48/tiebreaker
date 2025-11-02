package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.MatchService;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class HistoryScreen {

    private final LanternaView view;
    private final MatchService service;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public HistoryScreen(LanternaView view, MatchService service) {
        this.view = view;
        this.service = service;
    }

    /**
     * Wyświetla listę i wraca do menu po Esc.
     */
    public void show() throws Exception {
        var finished = service.listFinished();

        List<String> lines = finished.stream().map(m -> {
            String ts = TS.format(m.createdAt());
            String who = (m.winner() == null) ? "brak zwycięzcy"
                    : (m.winner() == 0 ? m.playerA() : m.playerB());
            String sets = m.setsA() + "–" + m.setsB();
            String format = "BO" + m.rules().bestOf();
            Duration d = m.elapsed();
            long h = d.toHours();
            long mm = d.toMinutesPart();
            String dur = (h > 0) ? String.format("%d:%02d", h, mm) : String.format("%02d min", mm);

            return String.format("[%s]  %s vs %s  —  Zwycięzca: %s  —  Sety: %s (%s) Czas: %s",
                    ts, m.playerA(), m.playerB(), who, sets, format, dur);
        }).toList();

        if (lines.isEmpty()) {
            lines = List.of("Brak zakończonych meczów.");
        }

        int idx = 0;
        while (true) {
            view.renderMatchesList("ROZEGRANE MECZE", lines, idx);
            switch (view.readKey()) {
                case ESC -> {
                    return;
                }
                case UP -> idx = (idx + lines.size() - 1) % Math.max(1, lines.size());
                case DOWN -> idx = (idx + 1) % Math.max(1, lines.size());
                default -> {
                }
            }
        }
    }
}