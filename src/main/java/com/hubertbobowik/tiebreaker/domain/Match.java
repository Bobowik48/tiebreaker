package com.hubertbobowik.tiebreaker.domain;


public final class Match {
    private final MatchId id;
    private final String playerA;
    private final String playerB;

    private int pointsA;
    private int pointsB;

    public Match(MatchId id, String playerA, String playerB) {
        this.id = id;
        this.playerA = playerA;
        this.playerB = playerB;
        this.pointsA = 0;
        this.pointsB = 0;
    }

    public MatchId id()     { return id; }
    public String playerA() { return playerA; }
    public String playerB() { return playerB; }

    public int pointsA() { return pointsA; }
    public int pointsB() { return pointsB; }

    public void addPointFor(int playerIndex) {
        if (playerIndex == 0) pointsA++;
        else pointsB++;
    }

    public void removePointFor(int playerIndex) {
        if (playerIndex == 0 && pointsA > 0) pointsA--;
        if (playerIndex == 1 && pointsB > 0) pointsB--;
    }
}
