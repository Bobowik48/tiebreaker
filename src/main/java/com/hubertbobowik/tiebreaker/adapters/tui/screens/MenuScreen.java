package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.application.TournamentService;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.domain.TournamentId;
import com.hubertbobowik.tiebreaker.domain.Tournament;

import java.util.ArrayList;
import java.util.List;

public final class MenuScreen {

    public enum Choice {
        CONTINUE_MATCH,
        NEW_MATCH,
        CONTINUE_TOURNAMENT,
        NEW_TOURNAMENT,
        HISTORY,
        EXIT
    }

    private final LanternaView view;
    private final MatchService matchService;
    private final TournamentService tournamentService;

    public MenuScreen(LanternaView view,
                      MatchService matchService,
                      TournamentService tournamentService) {
        this.view = view;
        this.matchService = matchService;
        this.tournamentService = tournamentService;
    }

    /**
     * Pokazuje menu główne; pozycje są warunkowe (kontynuacje tylko gdy jest co kontynuować).
     */
    public Choice show(MatchId activeMatchId, TournamentId activeTournamentId) throws Exception {
        boolean canContinueMatch = false;
        if (activeMatchId != null) {
            try {
                var m = matchService.getMatch(activeMatchId);
                canContinueMatch = (m != null && !m.isFinished());
            } catch (Exception ignored) { /* brak aktywnego meczu */ }
        }

        boolean canContinueTournament = false;
        if (activeTournamentId != null) {
            try {
                Tournament t = tournamentService.get(activeTournamentId);
                canContinueTournament = (t != null && !t.isFinished());
            } catch (Exception ignored) { /* brak aktywnego turnieju */ }
        }

        // Budujemy listę pozycji + mapowanie na Choice
        record Item(String label, Choice choice) {
        }
        List<Item> items = new ArrayList<>();

        if (canContinueMatch) {
            items.add(new Item("Kontynuuj mecz", Choice.CONTINUE_MATCH));
        }
        if (canContinueTournament) {
            items.add(new Item("Kontynuuj turniej", Choice.CONTINUE_TOURNAMENT));
        }

        items.add(new Item("Nowy mecz", Choice.NEW_MATCH));
        items.add(new Item("Nowy turniej", Choice.NEW_TOURNAMENT));
        items.add(new Item("Rozegrane mecze", Choice.HISTORY));
        items.add(new Item("Wyjście", Choice.EXIT));

        int idx = 0;
        while (true) {
            // Rysujemy etykiety
            String[] labels = items.stream().map(Item::label).toArray(String[]::new);
            view.renderSimpleMenu("TIEBREAKER — MENU", labels, idx);

            switch (view.readKey()) {
                case UP -> idx = (idx + items.size() - 1) % items.size();
                case DOWN -> idx = (idx + 1) % items.size();
                case ENTER -> {
                    return items.get(idx).choice();
                }
                case ESC -> {
                    return Choice.EXIT;
                }
                default -> { /* no-op */ }
            }
        }
    }
}
