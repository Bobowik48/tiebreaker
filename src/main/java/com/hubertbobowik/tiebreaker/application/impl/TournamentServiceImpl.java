package com.hubertbobowik.tiebreaker.application.impl;

import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.application.TournamentService;
import com.hubertbobowik.tiebreaker.domain.*;
import com.hubertbobowik.tiebreaker.ports.TournamentRepository;

import java.util.List;

public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository repo;
    private final MatchService matchService;

    public TournamentServiceImpl(TournamentRepository repo, MatchService matchService) {
        this.repo = repo;
        this.matchService = matchService;
    }

    @Override
    public Tournament create(int size, List<String> players) {
        Tournament t = new Tournament(new TournamentId("T-" + System.currentTimeMillis()), size, players);
        repo.save(t);
        return t;
    }

    @Override
    public Tournament get(TournamentId id) {
        return repo.findById(id).orElseThrow(() -> new IllegalStateException("Brak turnieju " + id.value()));
    }

    @Override
    public void delete(TournamentId id) {
        repo.delete(id);
    }

    @Override
    public void save(Tournament t) {
        repo.save(t);
    }

    @Override
    public List<Tournament> listAll() {
        return repo.findAll();
    }

    @Override
    public MatchId ensureCurrentMatch(TournamentId tid, Rules rules) {
        Tournament t = get(tid);
        BracketPair p = t.currentPairObj();
        if (p == null) return null;

        if (p.matchId() != null) {
            return p.matchId();
        }

        MatchId mid = new MatchId("M-" + System.currentTimeMillis());
        Match m = matchService.createMatch(p.a(), p.b(), rules, mid);
        t.setCurrentPairMatch(mid);
        repo.save(t);
        return mid;
    }

    @Override
    public void advanceWithWinner(TournamentId tid, int winnerIdx) {
        Tournament t = get(tid);
        t.advanceWithWinner(winnerIdx);
        repo.save(t);
    }
}
