// src/main/java/com/hubertbobowik/tiebreaker/application/impl/MatchServiceImpl.java
package com.hubertbobowik.tiebreaker.application.impl;

import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.ports.MatchRepository;

import java.util.*;

public class MatchServiceImpl implements MatchService {

    private final MatchRepository repo;

    // Historia akcji w pamięci (per mecz)
    private final Map<MatchId, Deque<Integer>> undoStack = new HashMap<>();
    private final Map<MatchId, Deque<Integer>> redoStack = new HashMap<>();

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
        // wyczyść historię dla nowego meczu
        undoStack.remove(id);
        redoStack.remove(id);
        return match;
    }

    @Override
    public Match addPoint(MatchId id, int playerIndex) {
        Match match = getMatch(id);
        match.addPointFor(playerIndex);
        repo.save(match);

        // zapamiętaj akcję do undo i wyczyść redo
        undoStack.computeIfAbsent(id, k -> new ArrayDeque<>()).push(playerIndex);
        redoStack.remove(id);
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
