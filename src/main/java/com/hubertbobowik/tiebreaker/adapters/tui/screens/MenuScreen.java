package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;

public final class MenuScreen {
    public enum Choice {CONTINUE, NEW_MATCH, HISTORY, EXIT}

    private final LanternaView view;
    private final MatchService service;

    public MenuScreen(LanternaView view, MatchService service) {
        this.view = view;
        this.service = service;
    }

    public Choice show(MatchId activeId) throws Exception {
        boolean canContinue = false;
        try {
            var current = service.getMatch(activeId);
            canContinue = (current != null && !current.isFinished());
        } catch (Exception ignored) {
        }

        String[] items = canContinue
                ? new String[]{"Kontynuuj mecz", "Nowy mecz", "Rozegrane mecze", "Wyjście"}
                : new String[]{"Nowy mecz", "Rozegrane mecze", "Wyjście"};

        int idx = 0;
        while (true) {
            view.renderSimpleMenu("TIEBREAKER — MENU", items, idx);
            switch (view.readKey()) {
                case UP -> idx = (idx + items.length - 1) % items.length;
                case DOWN -> idx = (idx + 1) % items.length;
                case ENTER -> {
                    if (canContinue) return Choice.values()[idx];
                    // mapowanie, gdy CONTINUE nie ma na liście
                    return switch (idx) {
                        case 0 -> Choice.NEW_MATCH;
                        case 1 -> Choice.HISTORY;
                        default -> Choice.EXIT;
                    };
                }
                case ESC -> {
                    return Choice.EXIT;
                }
                default -> {
                }
            }
        }
    }
}
