package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;

public final class MenuScreen {
    public enum Choice { CONTINUE, NEW_MATCH, HISTORY, EXIT }

    private final LanternaView view;
    private final MatchService service;

    public MenuScreen(LanternaView view, MatchService service) {
        this.view = view;
        this.service = service;
    }

    public Choice show(MatchId activeId) throws Exception {
        boolean canContinue = false;
        if (activeId != null) {
            try {
                Match m = service.getMatch(activeId);
                canContinue = (m != null && !m.isFinished());
            } catch (Exception ignored) {}
        }

        String[] items;
        Choice[] map;
        if (canContinue) {
            items = new String[] { "Kontynuuj mecz", "Nowy mecz", "Rozegrane mecze", "Wyjście" };
            map   = new Choice[] { Choice.CONTINUE,  Choice.NEW_MATCH, Choice.HISTORY, Choice.EXIT };
        } else {
            items = new String[] { "Nowy mecz", "Rozegrane mecze", "Wyjście" };
            map   = new Choice[] { Choice.NEW_MATCH, Choice.HISTORY, Choice.EXIT };
        }

        int idx = 0;
        while (true) {
            view.renderSimpleMenu("TIEBREAKER — MENU", items, idx);
            switch (view.readKey()) {
                case UP    -> idx = (idx + items.length - 1) % items.length;
                case DOWN  -> idx = (idx + 1) % items.length;
                case ENTER -> { return map[idx]; }
                case ESC   -> { return Choice.EXIT; }
                default    -> {}
            }
        }
    }
}
