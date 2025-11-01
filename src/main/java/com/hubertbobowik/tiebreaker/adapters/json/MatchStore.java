package com.hubertbobowik.tiebreaker.adapters.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/** Prosty „kontener” na mapę meczów w pliku JSON. */
public final class MatchStore {
    public final Map<String, MatchDto> matches;

    @JsonCreator
    public MatchStore(@JsonProperty("matches") Map<String, MatchDto> matches) {
        this.matches = (matches == null) ? new LinkedHashMap<>() : matches;
    }

    public static MatchStore empty() {
        return new MatchStore(new LinkedHashMap<>());
    }
}
