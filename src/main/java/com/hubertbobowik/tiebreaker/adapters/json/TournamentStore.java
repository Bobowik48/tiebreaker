package com.hubertbobowik.tiebreaker.adapters.json;

import java.util.HashMap;
import java.util.Map;

final class TournamentStore {
    public Map<String, TournamentDto> tournaments = new HashMap<>();

    static TournamentStore empty() {
        return new TournamentStore();
    }
}
