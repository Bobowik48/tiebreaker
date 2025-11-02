package com.hubertbobowik.tiebreaker.app;

import com.hubertbobowik.tiebreaker.adapters.json.JsonMatchRepository;
import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.adapters.tui.screens.HistoryScreen;
import com.hubertbobowik.tiebreaker.adapters.tui.screens.MatchScreen;
import com.hubertbobowik.tiebreaker.adapters.tui.screens.MenuScreen;
import com.hubertbobowik.tiebreaker.adapters.tui.screens.NameInputScreen;
import com.hubertbobowik.tiebreaker.adapters.tui.screens.RulesScreen;
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

            var menu = new MenuScreen(view, service);
            var matchUi = new MatchScreen(view, service);
            var histUi = new HistoryScreen(view, service);

            while (state != State.EXIT) {
                switch (state) {
                    case MENU -> {
                        switch (menu.show(activeId)) {
                            case CONTINUE -> state = State.MATCH;

                            case NEW_MATCH -> {
                                Match cur = service.getMatch(activeId);
                                if (!cur.isFinished() && !view.confirm("Trwa mecz. Rozpocząć nowy?")) {
                                    state = State.MENU;
                                    break;
                                }

                                // 1) wybór zasad
                                final boolean[] confirmed = {false};
                                final Rules[] picked = {Rules.defaults()};
                                RulesScreen rs = new RulesScreen(view, new RulesScreen.Callback() {
                                    @Override
                                    public void onConfirm(Rules rules) {
                                        confirmed[0] = true;
                                        picked[0] = rules;
                                    }

                                    @Override
                                    public void onCancel() {
                                        confirmed[0] = false;
                                    }
                                });
                                rs.show();
                                if (!confirmed[0]) {
                                    state = State.MENU;
                                    break;
                                }

                                // 2) imiona
                                NameInputScreen nameScreen = new NameInputScreen(view);
                                var names = nameScreen.ask();
                                if (names == null) {
                                    state = State.MENU;
                                    break;
                                }

                                // 3) serwis: wybór A/B lub rzut monetą
                                boolean canceled = false;
                                int starter = 0;
                                int cursor = 0;
                                serveLoop:
                                while (true) {
                                    view.renderSimpleMenu("USTAWIENIA SERWISU", new String[]{
                                                    "Zaczyna: " + (cursor == 0 ? "► " + names.a + " (A)" : names.a + " (A)"),
                                                    "Zaczyna: " + (cursor == 1 ? "► " + names.b + " (B)" : names.b + " (B)"),
                                                    "Rzut monetą [T]"
                                            },
                                            cursor
                                    );
                                    var ks = view.readRaw();
                                    if (ks == null) continue;
                                    switch (ks.getKeyType()) {
                                        case ArrowUp, ArrowDown -> cursor ^= 1;
                                        case Enter -> {
                                            starter = cursor;
                                            break serveLoop;
                                        }
                                        case Character -> {
                                            char c = Character.toUpperCase(ks.getCharacter());
                                            if (c == 'T') {
                                                starter = (new java.util.Random().nextBoolean() ? 0 : 1);
                                                break serveLoop;
                                            }
                                        }
                                        case Escape -> {
                                            canceled = true;
                                            break serveLoop;
                                        }
                                        default -> {
                                        }
                                    }
                                }
                                if (canceled) {
                                    state = State.MENU;
                                    break;
                                }

                                // 4) utworzenie meczu
                                Match created = service.createMatch(names.a, names.b, picked[0]);
                                created.setStartingServer(starter);
                                repo.save(created);
                                activeId = created.id();
                                state = State.MATCH;
                            }

                            case HISTORY -> state = State.HISTORY;
                            case EXIT -> state = State.EXIT;
                        }
                    }

                    case MATCH -> {
                        var result = matchUi.run(activeId);
                        state = (result == MatchScreen.Result.GO_TO_HISTORY) ? State.HISTORY : State.MENU;
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