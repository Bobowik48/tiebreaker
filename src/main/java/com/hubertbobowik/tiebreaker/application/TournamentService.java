package com.hubertbobowik.tiebreaker.application;

import com.hubertbobowik.tiebreaker.domain.Tournament;
import com.hubertbobowik.tiebreaker.domain.TournamentId;
import com.hubertbobowik.tiebreaker.domain.Rules;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.domain.Match;

import java.util.List;

public interface TournamentService {
    Tournament create(int size, List<String> players);
    Tournament get(TournamentId id);
    void delete(TournamentId id);
    void save(Tournament t);
    List<Tournament> listAll();
    MatchId ensureCurrentMatch(TournamentId tid, Rules rules);
    void advanceWithWinner(TournamentId tid, int winnerIdx);
}
