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

    // pełne snapshoty stanu meczu
    private final Map<MatchId, Deque<Match>> undoStack = new HashMap<>();
    private final Map<MatchId, Deque<Match>> redoStack = new HashMap<>();

    public MatchServiceImpl(MatchRepository repo, Rules defaultRules) {
        this.repo = repo;
        this.defaultRules = defaultRules != null ? defaultRules : Rules.defaults();
    }

    @Override
    public Match getMatch(MatchId id) {
        return repo.findById(id).orElseThrow(() ->
                new IllegalStateException("Brak aktywnego meczu o id: " + id.value()));
    }

    @Override
    public Match createMatch(String player1, String player2, Rules rules) {
        Match m = new Match(new MatchId("M-" + UUID.randomUUID()), player1, player2,
                rules != null ? rules : defaultRules);
        System.out.println("[DEBUG] createMatch bestOf=" + m.rules().bestOf());
        repo.save(m);
        return m;
    }

    @Override
    public Match createMatch(String a, String b, Rules r, MatchId id) {
        Match m = new Match(id, a, b, r);
        repo.save(m);
        return m;
    }

    @Override
    public Match addPoint(MatchId id, int playerIndex) {
        Match current = getMatch(id);

        // snapshot przed zmianą
        undoStack.computeIfAbsent(id, k -> new ArrayDeque<>()).push(current.copy());
        redoStack.computeIfAbsent(id, k -> new ArrayDeque<>()).clear();

        current.addPointFor(playerIndex);
        repo.save(current);
        return current;
    }

    @Override
    public Match undo(MatchId id) {
        Deque<Match> stack = undoStack.get(id);
        if (stack == null || stack.isEmpty())
            return getMatch(id);

        Match current = getMatch(id);
        redoStack.computeIfAbsent(id, k -> new ArrayDeque<>()).push(current.copy());

        Match prev = stack.pop();
        repo.save(prev);
        return prev;
    }

    @Override
    public Match redo(MatchId id) {
        Deque<Match> stack = redoStack.get(id);
        if (stack == null || stack.isEmpty())
            return getMatch(id);

        Match current = getMatch(id);
        undoStack.computeIfAbsent(id, k -> new ArrayDeque<>()).push(current.copy());

        Match next = stack.pop();
        repo.save(next);
        return next;
    }

    @Override
    public Match markFinished(MatchId id) {
        Match m = getMatch(id);
        m.finishNow();
        repo.save(m);
        return m;
    }

    @Override
    public void delete(MatchId id) {
        repo.delete(id);
    }

    @Override
    public List<Match> getAllMatches() {
        return repo.findAll();
    }

    @Override
    public List<Match> listFinished() {
        return repo.findAll().stream().filter(Match::isFinished).toList();
    }
}
