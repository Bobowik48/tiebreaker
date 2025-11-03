package com.hubertbobowik.tiebreaker.domain;

import java.util.Objects;

public final class TournamentId {
    private final String value;

    public TournamentId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TournamentId cannot be null/blank");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TournamentId that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}