package com.hubertbobowik.tiebreaker.domain;

import java.util.Objects;

public final class BracketPair {
    private String a;              // ← już nie final: musimy móc wstawić zwycięzcę do next round
    private String b;              // ← jw.
    private MatchId matchId;       // ustawiane, gdy odpalimy mecz pary
    private Integer winner;        // 0 = a, 1 = b, null = nierozstrzygnięte

    public BracketPair(String a, String b) {
        this(a, b, null, null);
    }

    public BracketPair(String a, String b, MatchId matchId, Integer winner) {
        this.a = a;
        this.b = b;
        this.matchId = matchId;
        if (winner != null && winner != 0 && winner != 1) {
            throw new IllegalArgumentException("Winner must be 0 or 1");
        }
        this.winner = winner;
    }

    public String a() { return a; }
    public String b() { return b; }
    public MatchId matchId() { return matchId; }
    public Integer winner() { return winner; }

    public boolean hasMatch() { return matchId != null; }
    public boolean isDone() { return winner != null; }

    public void setMatchId(MatchId matchId) { this.matchId = matchId; }

    public void setWinner(Integer winner) {
        if (winner != null && winner != 0 && winner != 1) {
            throw new IllegalArgumentException("Winner must be 0 or 1");
        }
        this.winner = winner;
    }

    /** Wygodny alias: wymusza 0/1. */
    public void markWinner(int idx) {
        if (idx != 0 && idx != 1) {
            throw new IllegalArgumentException("Winner must be 0 or 1");
        }
        this.winner = idx;
    }

    /** Potrzebne do natychmiastowego uzupełniania kolejnej rundy. */
    public void setA(String a) { this.a = a; }

    /** Potrzebne do natychmiastowego uzupełniania kolejnej rundy. */
    public void setB(String b) { this.b = b; }

    public String winnerName() {
        if (winner == null) return null;
        return winner == 0 ? a : b;
    }

    @Override
    public String toString() {
        String base = a + " vs " + b;
        if (winner == null) return base;
        return base + " → " + winnerName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BracketPair that)) return false;
        return Objects.equals(a, that.a) &&
                Objects.equals(b, that.b) &&
                Objects.equals(matchId, that.matchId) &&
                Objects.equals(winner, that.winner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, matchId, winner);
    }
}