package com.hubertbobowik.tiebreaker.adapters.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hubertbobowik.tiebreaker.domain.Tournament;
import com.hubertbobowik.tiebreaker.domain.TournamentId;
import com.hubertbobowik.tiebreaker.ports.TournamentRepository;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class JsonTournamentRepository implements TournamentRepository {

    private static final Path DATA_DIR = Paths.get("data");
    private static final Path DATA_FILE = DATA_DIR.resolve("tournaments.json");

    private final ObjectMapper mapper;

    public JsonTournamentRepository() {
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ensureFileExists();
    }

    @Override
    public Optional<Tournament> findById(TournamentId id) {
        TournamentStore store = readStore();
        TournamentDto dto = store.tournaments.get(id.value());
        return Optional.ofNullable(dto).map(TournamentDto::toDomain);
    }

    @Override
    public List<Tournament> findAll() {
        TournamentStore store = readStore();
        List<Tournament> out = new ArrayList<>();
        for (TournamentDto dto : store.tournaments.values()) {
            out.add(dto.toDomain());
        }
        return out;
    }

    @Override
    public void save(Tournament t) {
        TournamentStore store = readStore();
        store.tournaments.put(t.id().value(), TournamentDto.fromDomain(t));
        writeStore(store);
    }

    @Override
    public void delete(TournamentId id) {
        TournamentStore store = readStore();
        store.tournaments.remove(id.value());
        writeStore(store);
    }

    private void ensureFileExists() {
        try {
            if (Files.notExists(DATA_DIR)) Files.createDirectories(DATA_DIR);
            if (Files.notExists(DATA_FILE)) writeStore(TournamentStore.empty());
        } catch (IOException e) {
            throw new RuntimeException("Cannot prepare data file: " + DATA_FILE, e);
        }
    }

    private TournamentStore readStore() {
        try {
            return mapper.readValue(DATA_FILE.toFile(), TournamentStore.class);
        } catch (IOException e) {
            return TournamentStore.empty();
        }
    }

    private void writeStore(TournamentStore store) {
        try {
            mapper.writeValue(DATA_FILE.toFile(), store);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write data file: " + DATA_FILE, e);
        }
    }
}
