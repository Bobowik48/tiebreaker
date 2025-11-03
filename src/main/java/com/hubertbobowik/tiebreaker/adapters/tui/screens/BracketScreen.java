package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.TournamentService;
import com.hubertbobowik.tiebreaker.domain.BracketPair;
import com.hubertbobowik.tiebreaker.domain.BracketRound;
import com.hubertbobowik.tiebreaker.domain.Tournament;
import com.hubertbobowik.tiebreaker.domain.TournamentId;

import java.util.ArrayList;
import java.util.List;

public final class BracketScreen {

    private final LanternaView view;
    private final TournamentService tournamentService;

    public BracketScreen(LanternaView view, TournamentService tournamentService) {
        this.view = view;
        this.tournamentService = tournamentService;
    }

    /**
     * Tylko podgląd (Esc – wyjście).
     */
    public void show(TournamentId tid) throws Exception {
        while (true) {
            Tournament t = tournamentService.get(tid);

            List<String> lines = new ArrayList<>();
            lines.add("TURNIEJ — DRABINKA (" + t.size() + ")" + (t.isFinished() ? " [ZAKOŃCZONY]" : ""));
            lines.add("");

            for (int r = 0; r < t.rounds().size(); r++) {
                BracketRound round = t.rounds().get(r);
                lines.add("Runda " + (r + 1) + ":");
                for (int i = 0; i < round.size(); i++) {
                    BracketPair p = round.pairs().get(i);
                    String left = p.a();
                    String right = p.b();
                    String winner = p.isDone() ? ("  → wygrał: " + p.winnerName()) : "";
                    String marker = (r == t.currentRound() && i == t.currentPair() && !t.isFinished())
                            ? "► " : "  ";
                    lines.add(marker + "M" + (i + 1) + ": " + left + " vs " + right + winner);
                }
                lines.add("");
            }
            lines.add("[Esc] wstecz");

            view.renderMatchesList("PODGLĄD DRABINKI", lines, 0);
            switch (view.readKey()) {
                case ESC -> { return; }
                default -> { /* just stay */ }
            }
        }
    }
}
