package com.hubertbobowik.tiebreaker.application.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;

public class MatchServiceImpl implements MatchService {

    private final Map<MatchId, Match> storage = new HashMap<>();

    @Override
    public Match getMatch(MatchId id) {
        // jeśli nie ma – utwórz domyślny, żeby UI zawsze miało co wyświetlać
        return storage.computeIfAbsent(id, key -> new Match(key, "Player A", "Player B"));
    }

    @Override
    public Match createMatch(String player1, String player2) {
        MatchId id = new MatchId("M-" + UUID.randomUUID());
        Match match = new Match(id, player1, player2);
        storage.put(id, match);
        return match;
    }

    @Override
    public Match addPoint(MatchId id, int playerIndex) {
        Match match = getMatch(id);
        match.addPointFor(playerIndex);
        return match;
    }
}
