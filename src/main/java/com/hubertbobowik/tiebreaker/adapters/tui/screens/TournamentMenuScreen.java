// File: src/main/java/com/hubertbobowik/tiebreaker/adapters/tui/screens/TournamentMenuScreen.java
package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.TournamentService;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.domain.Tournament;
import com.hubertbobowik.tiebreaker.domain.TournamentId;

public final class TournamentMenuScreen {

    public enum Action {START_MATCH, SHOW_BRACKET, BACK}

    public record Result(Action action, MatchId matchId) {
        public static Result start(MatchId id) {
            return new Result(Action.START_MATCH, id);
        }

        public static Result bracket() {
            return new Result(Action.SHOW_BRACKET, null);
        }

        public static Result back() {
            return new Result(Action.BACK, null);
        }
    }

    private final LanternaView view;
    private final TournamentService tournamentService;

    public TournamentMenuScreen(LanternaView view, TournamentService tournamentService) {
        this.view = view;
        this.tournamentService = tournamentService;
    }

    public Result show(TournamentId tid) throws Exception {
        Tournament t = tournamentService.get(tid);

        int cursor = 0;
        while (true) {
            String status = t.isFinished() ? "ZAKOŃCZONY" : ("Runda " + (t.currentRound() + 1));
            String header = "TURNIEJ — " + status;

            String startLabel = t.isFinished()
                    ? "Turniej zakończony — [Enter] podgląd drabinki"
                    : "Kontynuuj: następny mecz (Enter)";

            view.renderSimpleMenu(header,
                    new String[]{
                            startLabel,
                            "Zobacz drabinkę",
                            "Wstecz"
                    },
                    cursor);

            switch (view.readKey()) {
                case UP -> cursor = (cursor + 2) % 3;
                case DOWN -> cursor = (cursor + 1) % 3;
                case ENTER -> {
                    if (cursor == 0) {
                        if (t.isFinished()) {
                            return Result.bracket();
                        }
                        MatchId mid = tournamentService.ensureCurrentMatch(tid); // zasady brane z turnieju
                        return Result.start(mid);
                    } else if (cursor == 1) {
                        return Result.bracket();
                    } else {
                        return Result.back();
                    }
                }
                case ESC -> {
                    return Result.back();
                }
                default -> { /* ignore */ }
            }
        }
    }
}
