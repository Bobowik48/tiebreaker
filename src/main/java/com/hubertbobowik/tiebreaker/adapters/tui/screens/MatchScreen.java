package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;

public final class MatchScreen {

    private final LanternaView view;
    private final MatchService service;
    long lastTick = -1;

    public MatchScreen(LanternaView view, MatchService service) {
        this.view = view;
        this.service = service;
    }

    /**
     * Zwraca po prostu do menu gdy użytkownik naciśnie Q.
     */
    public void run(MatchId matchId) throws Exception {
        Match m = service.getMatch(matchId);
        view.fullClear();
        view.renderStatic(m);
        view.renderScoreLine(m.scoreLine());

        while (true) {
            view.checkResizeAndRedraw(m);

            long nowSec = System.currentTimeMillis() / 1000;
            if (nowSec != lastTick) { // co sekundę
                lastTick = nowSec;
                var d = m.elapsed();
                long h = d.toHours();
                long mm = d.toMinutesPart();
                long ss = d.toSecondsPart();

                String t = (h > 0)
                        ? String.format("Czas meczu: %d:%02d:%02d", h, mm, ss)
                        : String.format("Czas meczu: %02d:%02d", mm, ss);

                view.renderElapsed(t);
            }

            switch (view.readIntent()) {
                case POINT_A -> {
                    m = service.addPoint(matchId, 0);
                    view.renderScoreLine(m.scoreLine());
                }
                case POINT_B -> {
                    m = service.addPoint(matchId, 1);
                    view.renderScoreLine(m.scoreLine());
                }
                case UNDO -> {
                    m = service.undo(matchId);
                    view.renderScoreLine(m.scoreLine());
                }
                case REDO -> {
                    m = service.redo(matchId);
                    view.renderScoreLine(m.scoreLine());
                }
                case QUIT -> {
                    return;
                }
                case NONE -> {
                }
            }

            if (m.isFinished()) {
                view.renderWinner("Zwycięzca: " + (m.winner() == 0 ? m.playerA() : m.playerB()));
            }
        }
    }
}