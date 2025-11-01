package com.hubertbobowik.tiebreaker.application.impl;

import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.ports.MatchRepository;

import java.util.UUID;

public class MatchServiceImpl implements MatchService {

    private final MatchRepository repo;

    public MatchServiceImpl(MatchRepository repo) {
        this.repo = repo;
    }

    @Override
    public Match getMatch(MatchId id) {
        return repo.findById(id)
                .orElseGet(() -> {
                    Match m = new Match(id, "Player A", "Player B");
                    repo.save(m);
                    return m;
                });
    }

    @Override
    public Match createMatch(String player1, String player2) {
        MatchId id = new MatchId("M-" + UUID.randomUUID());
        Match match = new Match(id, player1, player2);
        repo.save(match);
        return match;
    }

    @Override
    public Match addPoint(MatchId id, int playerIndex) {
        Match match = getMatch(id);
        match.addPointFor(playerIndex);
        repo.save(match);
        return match;
    }
}
