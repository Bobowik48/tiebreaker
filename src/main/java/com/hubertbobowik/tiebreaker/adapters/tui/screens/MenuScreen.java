package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;

public final class MenuScreen {
    public enum Choice { CONTINUE, NEW_MATCH, HISTORY, EXIT }

    private final LanternaView view;
    private final MatchService service;
    private final MatchId activeId;

    public MenuScreen(LanternaView view, MatchService service, MatchId activeId) {
        this.view = view;
        this.service = service;
        this.activeId = activeId;
    }

    public Choice show() throws Exception {
        String[] items = { "Kontynuuj mecz", "Nowy mecz", "Rozegrane mecze", "Wyjście" };
        int idx = 0;

        while (true) {
            view.renderSimpleMenu("TIEBREAKER — MENU", items, idx);

            switch (view.readKey()) {
                case UP    -> idx = (idx + items.length - 1) % items.length;
                case DOWN  -> idx = (idx + 1) % items.length;
                case ENTER -> { return Choice.values()[idx]; }
                case ESC   -> { return Choice.EXIT; }
                default    -> {}
            }
        }
    }
}
