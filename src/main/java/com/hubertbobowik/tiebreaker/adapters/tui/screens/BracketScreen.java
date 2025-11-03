package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.TournamentService;
import com.hubertbobowik.tiebreaker.domain.Tournament;
import com.hubertbobowik.tiebreaker.domain.TournamentId;

public final class BracketScreen {

    private final LanternaView view;
    private final TournamentService tournamentService;

    public BracketScreen(LanternaView view, TournamentService tournamentService) {
        this.view = view;
        this.tournamentService = tournamentService;
    }

    /**
     * Graficzny podgląd drabinki (Esc – wyjście).
     */
    public void show(TournamentId tid) throws Exception {
        while (true) {
            Tournament t = tournamentService.get(tid);

            // nowy widok graficzny
            view.renderBracket(t);

            switch (view.readKey()) {
                case ESC -> { return; }
                default -> { /* odświeżaj na bieżąco, gdy stan się zmieni */ }
            }
        }
    }
}
