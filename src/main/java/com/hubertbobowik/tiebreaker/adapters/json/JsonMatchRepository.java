package com.hubertbobowik.tiebreaker.adapters.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;
import com.hubertbobowik.tiebreaker.ports.MatchRepository;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JsonMatchRepository implements MatchRepository {

    private static final Path DATA_DIR = Paths.get("data");
    private static final Path DATA_FILE = DATA_DIR.resolve("matches.json");

    private final ObjectMapper mapper;

    public JsonMatchRepository() {
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        ensureFileExists();
    }

    @Override
    public Optional<Match> findById(MatchId id) {
        MatchStore store = readStore();
        MatchDto dto = store.matches.get(id.value());
        return Optional.ofNullable(dto).map(MatchDto::toDomain);
    }

    @Override
    public List<Match> findAll() {
        MatchStore store = readStore();
        List<Match> out = new ArrayList<>();
        for (MatchDto dto : store.matches.values()) {
            out.add(dto.toDomain());
        }
        return out;
    }

    @Override
    public void save(Match match) {
        MatchStore store = readStore();
        store.matches.put(match.id().value(), MatchDto.fromDomain(match));
        writeStore(store);
    }

    @Override
    public void delete(MatchId id) {
        var all = findAll();
        all.removeIf(m -> m.id().equals(id));
        writeAllMatches(all);
    }


    // ───────────────────────────────────────────────────────────────────────────

    private void writeAllMatches(java.util.List<Match> matches) {
        try {
            var dtos = matches.stream()
                    .map(MatchDto::fromDomain)
                    .toList();

            mapper.writeValue(DATA_FILE.toFile(), dtos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void ensureFileExists() {
        try {
            if (Files.notExists(DATA_DIR)) {
                Files.createDirectories(DATA_DIR);
            }
            if (Files.notExists(DATA_FILE)) {
                writeStore(MatchStore.empty());
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot prepare data file: " + DATA_FILE, e);
        }
    }

    private MatchStore readStore() {
        try {
            return mapper.readValue(DATA_FILE.toFile(), MatchStore.class);
        } catch (IOException e) {
            // w razie uszkodzenia pliku – zaczynamy od pustego
            return MatchStore.empty();
        }
    }

    private void writeStore(MatchStore store) {
        try {
            mapper.writeValue(DATA_FILE.toFile(), store);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write data file: " + DATA_FILE, e);
        }
    }
}
