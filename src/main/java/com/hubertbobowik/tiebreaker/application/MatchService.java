package com.hubertbobowik.tiebreaker.application;

import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;

public interface MatchService {
    Match getMatch(MatchId id);
    Match createMatch(String player1, String player2);
    Match addPoint(MatchId id, int playerIndex);
}
