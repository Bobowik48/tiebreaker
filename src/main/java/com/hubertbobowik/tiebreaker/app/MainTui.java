package com.hubertbobowik.tiebreaker.app;

import com.hubertbobowik.tiebreaker.adapters.json.JsonMatchRepository;
import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView.UserIntent;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.application.impl.MatchServiceImpl;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.ports.MatchRepository;
import com.hubertbobowik.tiebreaker.domain.Rules;
import static com.hubertbobowik.tiebreaker.domain.Rules.TieBreakMode.*;

public final class MainTui {
    public static void main(String[] args) {
        MatchRepository repo = new JsonMatchRepository();
        Rules rules = new Rules(3, CLASSIC7, CLASSIC7);
        MatchService service = new MatchServiceImpl(repo, rules);
        MatchId matchId = new MatchId("LOCAL-0001");

        try (LanternaView view = new LanternaView()) {
            view.open();

            Match m = service.getMatch(matchId);
            view.renderStatic(m);
            view.renderScore(m);

            while (true) {
                view.checkResizeAndRedraw(m);

                UserIntent intent = view.readIntent();
                switch (intent) {
                    case POINT_A -> { m = service.addPoint(matchId, 0); view.renderScore(m); }
                    case POINT_B -> { m = service.addPoint(matchId, 1); view.renderScore(m); }
                    case UNDO -> { m = service.undo(matchId); view.renderScore(m); }
                    case REDO -> { m = service.redo(matchId); view.renderScore(m); }
                    case QUIT -> { return; }
                    case NONE -> { /* no-op */ }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
