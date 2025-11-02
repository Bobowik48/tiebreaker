package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;

public final class MatchScreen {

    public enum Result {BACK_TO_MENU, GO_TO_HISTORY}

    private final LanternaView view;
    private final MatchService service;
    long lastTick = -1;

    public MatchScreen(LanternaView view, MatchService service) {
        this.view = view;
        this.service = service;
    }

    public Result run(MatchId matchId) throws Exception {
        Match m = service.getMatch(matchId);
        view.fullClear();
        view.renderStatic(m);
        view.renderScoreLine(m.scoreLine());
        view.renderServer("Serwuje: " + (m.currentServerForDisplay() == 0 ? m.playerA() : m.playerB()));

        boolean winnerPanelShown = false;

        while (true) {
            // Jeśli mecz skończony: pokazujemy panel i czekamy na Q/H/Enter
            if (m.isFinished()) {
                if (!winnerPanelShown) {
                    String winName = (m.winner() != null) ? (m.winner() == 0 ? m.playerA() : m.playerB())
                            : "(brak zwycięzcy)";
                    view.renderWinnerPanel(winName);
                    winnerPanelShown = true;
                }

                // Tryb końcowy – tylko klawisze nawigacyjne/znakowe.
                var ks = view.readRaw();
                if (ks == null) continue;

                switch (ks.getKeyType()) {
                    case Character -> {
                        char c = Character.toUpperCase(ks.getCharacter());
                        if (c == 'H') return Result.GO_TO_HISTORY;
                        if (c == 'Q') return Result.BACK_TO_MENU;
                        // inne znaki ignorujemy
                    }
                    case Enter -> {
                        return Result.GO_TO_HISTORY;
                    }
                    case Escape -> {
                        return Result.BACK_TO_MENU;
                    }
                    default -> { /* ignoruj */ }
                }
                continue;
            }

            // ⬇️ Tylko gdy mecz TRWA – odświeżamy zegar i serwującego
            view.checkResizeAndRedraw(m);

            long nowSec = System.currentTimeMillis() / 1000;
            if (nowSec != lastTick) {
                lastTick = nowSec;
                var d = m.elapsed();
                long h = d.toHours();
                long mm = d.toMinutesPart();
                long ss = d.toSecondsPart();
                String t = (h > 0)
                        ? String.format("Czas meczu: %d:%02d:%02d", h, mm, ss)
                        : String.format("Czas meczu: %02d:%02d", mm, ss);
                view.renderElapsed(t);
                view.renderServer("Serwuje: " + (m.currentServerForDisplay() == 0 ? m.playerA() : m.playerB()));
            }

            switch (view.readIntent()) {
                case POINT_A -> {
                    m = service.addPoint(matchId, 0);
                    view.renderScoreLine(m.scoreLine());
                    view.renderServer("Serwuje: " + (m.currentServerForDisplay() == 0 ? m.playerA() : m.playerB()));
                }
                case POINT_B -> {
                    m = service.addPoint(matchId, 1);
                    view.renderScoreLine(m.scoreLine());
                    view.renderServer("Serwuje: " + (m.currentServerForDisplay() == 0 ? m.playerA() : m.playerB()));
                }
                case UNDO -> {
                    m = service.undo(matchId);
                    view.renderScoreLine(m.scoreLine());
                    view.renderServer("Serwuje: " + (m.currentServerForDisplay() == 0 ? m.playerA() : m.playerB()));
                }
                case REDO -> {
                    m = service.redo(matchId);
                    view.renderScoreLine(m.scoreLine());
                    view.renderServer("Serwuje: " + (m.currentServerForDisplay() == 0 ? m.playerA() : m.playerB()));
                }
                case FINISH -> {
                    if (view.confirm("Zakończyć mecz i zapisać do historii?")) {
                        m = service.markFinished(matchId); // ustawia finishedAt i finished=true
                        String winName = (m.winner() != null) ? (m.winner() == 0 ? m.playerA() : m.playerB())
                                : "(brak zwycięzcy)";
                        view.renderWinnerPanel(winName);
                        // Pozwól użytkownikowi wybrać H/Q/Enter w bloku wyżej
                    } else if (view.confirm("Usunąć mecz bez zapisu?")) {
                        service.delete(matchId);
                        return Result.BACK_TO_MENU;
                    }
                }
                case QUIT -> {
                    return Result.BACK_TO_MENU;
                }
                case NONE -> { /* nic */ }
            }
        }
    }
}