package com.hubertbobowik.tiebreaker.app;

import com.hubertbobowik.tiebreaker.adapters.json.JsonMatchRepository;
import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.adapters.tui.screens.HistoryScreen;
import com.hubertbobowik.tiebreaker.adapters.tui.screens.MatchScreen;
import com.hubertbobowik.tiebreaker.adapters.tui.screens.MenuScreen;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.application.impl.MatchServiceImpl;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.domain.Rules;
import com.hubertbobowik.tiebreaker.ports.MatchRepository;

public final class MainTui {
    enum State {MENU, MATCH, HISTORY, EXIT}

    public static void main(String[] args) {
        MatchRepository repo = new JsonMatchRepository();
        MatchService service = new MatchServiceImpl(repo, Rules.defaults());

        MatchId activeId = new MatchId("LOCAL-0001");
        State state = State.MENU;

        try (LanternaView view = new LanternaView()) {
            view.open();

            var menu = new MenuScreen(view, service, activeId);
            var matchUi = new MatchScreen(view, service);
            var histUi = new HistoryScreen(view, service);

            while (state != State.EXIT) {
                switch (state) {
                    case MENU -> {
                        switch (menu.show()) {
                            case CONTINUE -> state = State.MATCH;
                            case NEW_MATCH -> {
                                Match cur = service.getMatch(activeId);
                                if (!cur.isFinished() && !view.confirm("Trwa mecz. Rozpocząć nowy?")) {
                                    state = State.MENU;
                                    break;
                                }
                                Match created = service.createMatch("Player A", "Player B");
                                activeId = created.id();          // <— KLUCZOWE
                                state = State.MATCH;
                            }
                            case HISTORY -> state = State.HISTORY;
                            case EXIT -> state = State.EXIT;
                        }
                    }
                    case MATCH -> {
                        matchUi.run(activeId);
                        state = State.MENU;
                    }
                    case HISTORY -> {
                        histUi.show();
                        state = State.MENU;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}