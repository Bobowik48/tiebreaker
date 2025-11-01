package com.hubertbobowik.tiebreaker.app;

import com.hubertbobowik.tiebreaker.adapters.json.JsonMatchRepository;
import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView.UserIntent;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.application.impl.MatchServiceImpl;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.ports.MatchRepository;

public final class MainTui {
    public static void main(String[] args) {
        MatchRepository repo = new JsonMatchRepository();
        MatchService service = new MatchServiceImpl(repo);
        MatchId matchId = new MatchId("LOCAL-0001");

        try (LanternaView view = new LanternaView()) {
            view.open();

            Match m = service.getMatch(matchId);
            view.renderStatic(m);
            view.renderScore(m);

            while (true) {
                UserIntent intent = view.readIntent();
                switch (intent) {
                    case POINT_A -> { m = service.addPoint(matchId, 0); view.renderScore(m); }
                    case POINT_B -> { m = service.addPoint(matchId, 1); view.renderScore(m); }
                    case UNDO, REDO -> { /* wkrótce */ }
                    case QUIT -> { return; }
                    case NONE -> { /* no-op */ }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
