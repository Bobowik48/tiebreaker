package com.hubertbobowik.tiebreaker.ports;

import com.hubertbobowik.tiebreaker.domain.Tournament;
import com.hubertbobowik.tiebreaker.domain.TournamentId;

import java.util.List;
import java.util.Optional;

public interface TournamentRepository {
    Optional<Tournament> findById(TournamentId id);
    List<Tournament> findAll();
    void save(Tournament t);
    void delete(TournamentId id);
}
