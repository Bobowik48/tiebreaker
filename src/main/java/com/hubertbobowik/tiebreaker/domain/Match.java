package com.hubertbobowik.tiebreaker.domain;

public class Match {
    private final MatchId id;
    private final String playerA;
    private final String playerB;

    // nowość — na razie opcjonalne (może być null dla starych zapisów)
    private final Rules rules;

    // twoje istniejące pola punktów
    private int pointsA;
    private int pointsB;

    // STARY konstruktor – zostaje dla kompatybilności (repo JSON go używa)
    public Match(MatchId id, String playerA, String playerB) {
        this(id, playerA, playerB, null);
    }

    // NOWY – z regułami
    public Match(MatchId id, String playerA, String playerB, Rules rules) {
        this.id = id;
        this.playerA = playerA;
        this.playerB = playerB;
        this.rules = rules;
        this.pointsA = 0;
        this.pointsB = 0;
    }

    public MatchId id() { return id; }
    public String playerA() { return playerA; }
    public String playerB() { return playerB; }
    public int pointsA() { return pointsA; }
    public int pointsB() { return pointsB; }
    public void addPointFor(int idx) { if (idx==0) pointsA++; else pointsB++; }
    public void removePointFor(int idx) { if (idx==0 && pointsA>0) pointsA--; if (idx==1 && pointsB>0) pointsB--; }

    // wygodny dostęp: jeśli null → domyślne
    public Rules rules() { return rules != null ? rules : Rules.defaults(); }
}
