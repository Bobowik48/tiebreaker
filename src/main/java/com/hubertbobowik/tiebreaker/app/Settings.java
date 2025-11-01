package com.hubertbobowik.tiebreaker.app;

import com.hubertbobowik.tiebreaker.domain.Rules;

import static com.hubertbobowik.tiebreaker.domain.Rules.TieBreakMode.*;

public final class Settings {
    public int bestOf = 3;
    public Rules.TieBreakMode tieBreakEverySet = CLASSIC7;
    public Rules.TieBreakMode finalSetMode    = CLASSIC7;

    public Rules toRules() {
        return new Rules(bestOf, tieBreakEverySet, finalSetMode);
    }
}
