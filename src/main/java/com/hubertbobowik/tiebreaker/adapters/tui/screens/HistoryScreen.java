package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.domain.Match;

import java.util.ArrayList;
import java.util.List;

public final class HistoryScreen {

    private final LanternaView view;
    private final MatchService service;

    public HistoryScreen(LanternaView view, MatchService service) {
        this.view = view;
        this.service = service;
    }

    /**
     * Wyświetla listę i wraca do menu po Enter/Esc.
     */
    public void show() throws Exception {
        var finished = service.listFinished();
        List<String> lines = finished.stream()
                .map(m -> String.format("%-12s  %-12s vs %-12s   [FIN — zwycięzca: %s]",
                        m.id().value(),
                        m.playerA(),
                        m.playerB(),
                        (m.winner() != null && m.winner() == 0 ? m.playerA() : m.playerB())))
                .toList();

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