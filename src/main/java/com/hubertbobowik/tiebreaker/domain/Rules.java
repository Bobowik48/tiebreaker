package com.hubertbobowik.tiebreaker.domain;

public final class Rules {
    public enum TieBreakMode {NONE, CLASSIC7, TB10}

    private final int bestOf;                 // 3 lub 5
    private final TieBreakMode tieBreakEverySet;
    private final TieBreakMode finalSetMode;

    public Rules(int bestOf, TieBreakMode tieBreakEverySet, TieBreakMode finalSetMode) {
        this.bestOf = bestOf;
        this.tieBreakEverySet = tieBreakEverySet;
        this.finalSetMode = finalSetMode;
    }

    public int bestOf() {
        return bestOf;
    }

    public TieBreakMode tieBreakEverySet() {
        return tieBreakEverySet;
    }

    public TieBreakMode finalSetMode() {
        return finalSetMode;
    }

    public static Rules defaults() {
        return new Rules(3, TieBreakMode.CLASSIC7, TieBreakMode.CLASSIC7);
    }
}
