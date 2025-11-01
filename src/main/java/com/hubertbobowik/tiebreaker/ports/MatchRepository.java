package com.hubertbobowik.tiebreaker.ports;

import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;

import java.util.List;
import java.util.Optional;

public interface MatchRepository {
    Optional<Match> findById(MatchId id);
    List<Match> findAll();
    void save(Match match);
}
