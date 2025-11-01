package com.hubertbobowik.tiebreaker.domain;
import java.util.Objects;

public final class MatchId {
    private final String value;

    public MatchId(String value) { this.value = value; }
    public String value() { return value; }

    @Override public String toString() { return value; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchId)) return false;
        MatchId matchId = (MatchId) o;
        return Objects.equals(value, matchId.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
}
