package com.hubertbobowik.tiebreaker.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class BracketRound {
    private final List<BracketPair> pairs;

    public BracketRound(List<BracketPair> pairs) {
        this.pairs = new ArrayList<>(Objects.requireNonNull(pairs));
    }

    public List<BracketPair> pairs() {
        return Collections.unmodifiableList(pairs);
    }

    public boolean isFinished() {
        for (BracketPair p : pairs) {
            if (!p.isDone()) return false;
        }
        return true;
    }

    public int size() {
        return pairs.size();
    }

    @Override
    public String toString() {
        return "Round(" + pairs + ")";
    }
}