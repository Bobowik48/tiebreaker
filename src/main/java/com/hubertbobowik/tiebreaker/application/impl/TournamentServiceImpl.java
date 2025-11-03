package com.hubertbobowik.tiebreaker.application.impl;

import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.application.TournamentService;
import com.hubertbobowik.tiebreaker.domain.*;
import com.hubertbobowik.tiebreaker.ports.TournamentRepository;

import java.util.List;
import java.util.UUID;

public final class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository repo;
    private final MatchService matchService;

    public TournamentServiceImpl(TournamentRepository repo, MatchService matchService) {
        this.repo = repo;
        this.matchService = matchService;
    }

    @Override
    @Deprecated
    public Tournament create(int size, List<String> players) {
        return create(size, players, Rules.defaults());
    }

    @Override
    public Tournament create(int size, List<String> names, Rules rules) {
        Tournament t = new Tournament(
                new TournamentId("T-" + UUID.randomUUID()),
                size,
                names,
                rules != null ? rules : Rules.defaults()
        );
        repo.save(t);
        return t;
    }

    @Override
    public Tournament get(TournamentId id) {
        return repo.findById(id).orElseThrow(() ->
                new IllegalStateException("Tournament not found: " + id.value()));
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
    public MatchId ensureCurrentMatch(TournamentId tid) {
        Tournament t = get(tid);

        if (t.isFinished()) {
            throw new IllegalStateException("Tournament already finished: " + tid.value());
        }

        // 1) Spróbuj bieżącej pary
        BracketPair pair = t.currentPairObj();

        // 2) Jeśli brak lub niegrywalna, znajdź pierwszą realnie grywalną parę w całej drabince
        if (!isPlayable(pair)) {
            pair = findFirstPlayablePair(t);
        }

        if (!isPlayable(pair)) {
            // Brak czegokolwiek do grania. Traktuj jak koniec albo czekanie na zwycięzców.
            throw new IllegalStateException("No active pair for tournament: " + tid.value());
        }

        // 3) Jeśli mecz już istnieje, zwróć jego id
        if (pair.matchId() != null) {
            return pair.matchId();
        }

        // 4) Utwórz nowy mecz na bazie zasad turnieju
        Rules rules = t.rules();
        MatchId newId = new MatchId("TM-" + tid.value() + "-" + System.currentTimeMillis());

        String a = pair.a() == null ? "TBD" : pair.a();
        String b = pair.b() == null ? "TBD" : pair.b();

        matchService.createMatch(a, b, rules, newId);

        // 5) Zwiąż mecz z parą i zapisz turniej
        pair.setMatchId(newId);
        repo.save(t);

        return newId;
    }

    @Override
    public void advanceWithWinner(TournamentId tid, int winnerIdx) {
        Tournament t = get(tid);
        t.advanceWithWinner(winnerIdx);
        repo.save(t);
    }

    // ─────────────────────────────────────────────────────

    private static boolean isPlayable(BracketPair p) {
        if (p == null) return false;
        if (p.isDone()) return false;
        // gramy tylko jeśli obie strony znane
        return p.a() != null && p.b() != null;
    }

    private static BracketPair findFirstPlayablePair(Tournament t) {
        for (int r = 0; r < t.rounds().size(); r++) {
            BracketRound round = t.rounds().get(r);
            for (int i = 0; i < round.size(); i++) {
                BracketPair cand = round.pairs().get(i);
                if (isPlayable(cand)) {
                    return cand;
                }
            }
        }
        return null;
    }
}
