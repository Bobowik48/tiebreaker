package com.hubertbobowik.tiebreaker.app;

import com.hubertbobowik.tiebreaker.adapters.json.JsonMatchRepository;
import com.hubertbobowik.tiebreaker.adapters.json.JsonTournamentRepository;
import com.hubertbobowik.tiebreaker.adapters.tui.SwingView;
import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.adapters.tui.screens.*;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.application.TournamentService;
import com.hubertbobowik.tiebreaker.application.impl.MatchServiceImpl;
import com.hubertbobowik.tiebreaker.application.impl.TournamentServiceImpl;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.domain.Rules;
import com.hubertbobowik.tiebreaker.domain.TournamentId;
import com.hubertbobowik.tiebreaker.ports.MatchRepository;
import com.hubertbobowik.tiebreaker.ports.TournamentRepository;

public final class MainGui {

    enum State {MENU, MATCH, HISTORY, TOURNAMENT_SETUP, TOURNAMENT_MENU, EXIT}

    public static void main(String[] args) {

        MatchRepository matchRepo = new JsonMatchRepository();
        TournamentRepository tournamentRepo = new JsonTournamentRepository();
        MatchService matchService = new MatchServiceImpl(matchRepo, Rules.defaults());
        TournamentService tournamentService = new TournamentServiceImpl(tournamentRepo, matchService);

        MatchId activeMatchId = null;
        TournamentId activeTournamentId = null;

        State state = State.MENU;

        // JEDYNA RÓŻNICA: SwingView zamiast LanternaView
        try (LanternaView view = new SwingView()) {
            view.open();

            var menu = new MenuScreen(view, matchService, tournamentService);
            var matchUi = new MatchScreen(view, matchService);
            var histUi = new HistoryScreen(view, matchService);
            var tourSetupUi = new TournamentSetupScreen(view, tournamentService);
            var tourMenuUi = new TournamentMenuScreen(view, tournamentService);
            var bracketUi = new BracketScreen(view, tournamentService);

            Rules lastPickedRules = Rules.defaults();

            while (state != State.EXIT) {
                switch (state) {
                    case MENU -> {
                        var sel = menu.show(activeMatchId, activeTournamentId);

                        switch (sel) {
                            case CONTINUE_MATCH -> state = State.MATCH;

                            case NEW_MATCH -> {
                                final boolean[] confirmed = {false};
                                final Rules[] picked = {lastPickedRules != null ? lastPickedRules : Rules.defaults()};

                                RulesScreen rs = new RulesScreen(view, new RulesScreen.Callback() {
                                    @Override
                                    public void onConfirm(Rules rules) {
                                        picked[0] = rules;
                                        confirmed[0] = true;
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

                                var names = new NameInputScreen(view).ask();
                                if (names == null) break;

                                boolean canceled = false;
                                int starter = 0;
                                int cur = 0;
                                serve:
                                while (true) {
                                    view.renderSimpleMenu(
                                            "USTAW SERWUJĄCEGO",
                                            new String[]{
                                                    (cur == 0 ? "► " : "") + names.a,
                                                    (cur == 1 ? "► " : "") + names.b,
                                                    "Rzut monetą [T]"
                                            },
                                            cur
                                    );
                                    var k = view.readRaw();
                                    if (k == null) continue;

                                    switch (k.getKeyType()) {
                                        case ArrowUp, ArrowDown -> cur ^= 1;
                                        case Enter -> {
                                            starter = cur;
                                            break serve;
                                        }
                                        case Character -> {
                                            if (Character.toUpperCase(k.getCharacter()) == 'T') {
                                                starter = (Math.random() < 0.5 ? 0 : 1);
                                                break serve;
                                            }
                                        }
                                        case Escape -> {
                                            canceled = true;
                                            break serve;
                                        }
                                        default -> {
                                        }
                                    }
                                }
                                if (canceled) break;

                                MatchId newId = new MatchId("LOCAL-" + System.currentTimeMillis());
                                Match created = matchService.createMatch(names.a, names.b, picked[0], newId);
                                created.setStartingServer(starter);
                                matchRepo.save(created);

                                activeMatchId = newId;
                                lastPickedRules = picked[0];
                                state = State.MATCH;
                            }

                            case CONTINUE_TOURNAMENT -> state = State.TOURNAMENT_MENU;
                            case NEW_TOURNAMENT -> state = State.TOURNAMENT_SETUP;
                            case HISTORY -> state = State.HISTORY;

                            case EXIT -> {
                                for (var m : matchService.getAllMatches()) {
                                    if (!m.isFinished()) matchRepo.delete(m.id());
                                }
                                state = State.EXIT;
                            }
                        }
                    }

                    case MATCH -> {
                        MatchScreen.Result res = (activeTournamentId != null)
                                ? matchUi.run(activeMatchId, true)
                                : matchUi.run(activeMatchId);

                        if (activeTournamentId != null) {
                            try {
                                var t = tournamentService.get(activeTournamentId);
                                var p = t.currentPairObj();
                                if (p != null && p.matchId() != null && p.matchId().equals(activeMatchId)) {
                                    var m = matchService.getMatch(activeMatchId);
                                    if (m != null && m.isFinished()) {
                                        String wn = m.winnerName();
                                        int winnerIdx = wn.equals(p.a()) ? 0 : 1;
                                        tournamentService.advanceWithWinner(activeTournamentId, winnerIdx);
                                        activeMatchId = null;
                                    }
                                }
                            } catch (Exception ignored) {
                            }

                            if (res == MatchScreen.Result.GO_TO_BRACKET) {
                                bracketUi.show(activeTournamentId);
                                state = State.TOURNAMENT_MENU;
                                break;
                            }

                            state = State.MENU;
                            break;
                        }

                        if (res == MatchScreen.Result.GO_TO_HISTORY) {
                            state = State.HISTORY;
                        } else {
                            state = State.MENU;
                        }
                    }

                    case HISTORY -> {
                        histUi.show();
                        state = State.MENU;
                    }

                    case TOURNAMENT_SETUP -> {
                        var r = tourSetupUi.show();
                        if (!r.isOk()) {
                            state = State.MENU;
                            break;
                        }
                        activeTournamentId = r.id();
                        state = State.TOURNAMENT_MENU;
                    }

                    case TOURNAMENT_MENU -> {
                        var r = tourMenuUi.show(activeTournamentId);

                        switch (r.action()) {
                            case START_MATCH -> {
                                activeMatchId = r.matchId();
                                state = State.MATCH;
                            }
                            case SHOW_BRACKET -> {
                                bracketUi.show(activeTournamentId);
                                state = State.TOURNAMENT_MENU;
                            }
                            case BACK -> state = State.MENU;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}