package com.hubertbobowik.tiebreaker.application;

import com.hubertbobowik.tiebreaker.domain.*;

import java.util.List;

public interface TournamentService {
    @Deprecated
    Tournament create(int size, List<String> players);
    Tournament create(int size, List<String> names, Rules rules);
    Tournament get(TournamentId id);
    void delete(TournamentId id);
    void save(Tournament t);
    List<Tournament> listAll();
    MatchId ensureCurrentMatch(TournamentId tid);
    void advanceWithWinner(TournamentId tid, int winnerIdx);
}
