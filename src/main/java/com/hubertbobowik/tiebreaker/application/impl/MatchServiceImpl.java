// src/main/java/com/hubertbobowik/tiebreaker/application/impl/MatchServiceImpl.java
package com.hubertbobowik.tiebreaker.application.impl;

import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.domain.Rules;
import com.hubertbobowik.tiebreaker.ports.MatchRepository;

import java.util.*;

public class MatchServiceImpl implements MatchService {

    private final MatchRepository repo;
    private final Rules defaultRules;

    // Historia akcji w pamięci (per mecz)
    private final Map<MatchId, Deque<Integer>> undoStack = new HashMap<>();
    private final Map<MatchId, Deque<Integer>> redoStack = new HashMap<>();

    public MatchServiceImpl(MatchRepository repo, Rules defaultRules) {
        this.repo = repo;
        this.defaultRules = defaultRules != null ? defaultRules : Rules.defaults();
    }

    @Override
    public Match getMatch(MatchId id) {
        return repo.findById(id)
                .orElseGet(() -> {
                    Match m = new Match(id, "Player A", "Player B", defaultRules);
                    repo.save(m);
                    return m;
                });
    }

    @Override
    public Match createMatch(String player1, String player2) {
        Match match = new Match(new MatchId("M-" + UUID.randomUUID()), player1, player2, defaultRules);
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

    @Override
    public Match undo(MatchId id) {
        Deque<Integer> u = undoStack.getOrDefault(id, new ArrayDeque<>());
        if (u.isEmpty()) return getMatch(id);

        Integer last = u.pop();
        Match match = getMatch(id);
        match.removePointFor(last);
        repo.save(match);

        redoStack.computeIfAbsent(id, k -> new ArrayDeque<>()).push(last);
        return match;
    }

    @Override
    public Match redo(MatchId id) {
        Deque<Integer> r = redoStack.getOrDefault(id, new ArrayDeque<>());
        if (r.isEmpty()) return getMatch(id);

        Integer again = r.pop();
        Match match = getMatch(id);
        match.addPointFor(again);
        repo.save(match);

        undoStack.computeIfAbsent(id, k -> new ArrayDeque<>()).push(again);
        return match;
    }
}
