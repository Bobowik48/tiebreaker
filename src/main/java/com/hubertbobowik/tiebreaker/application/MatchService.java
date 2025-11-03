package com.hubertbobowik.tiebreaker.application;

import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.domain.Rules;

import java.util.List;

public interface MatchService {
    Match getMatch(MatchId id);
    Match createMatch(String player1, String player2, Rules rules);
    Match createMatch(String a, String b, Rules r, MatchId id);
    Match addPoint(MatchId id, int playerIndex);
    Match undo(MatchId id);
    Match redo(MatchId id);
    Match markFinished(MatchId id);
    void delete(MatchId id);
    List<Match> getAllMatches();
    List<Match> listFinished();
}
