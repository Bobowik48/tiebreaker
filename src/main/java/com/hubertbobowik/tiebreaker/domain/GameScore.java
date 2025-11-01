package com.hubertbobowik.tiebreaker.domain;

public final class GameScore {
    private final boolean tieBreak;
    private final int tbTarget;

    // zwykły gem: „kroki” 0..3 == 0,15,30,40
    private int stepsA = 0;
    private int stepsB = 0;
    private int adv = -1; // -1 brak, 0 adv A, 1 adv B

    // tie-break: surowe punkty
    private int tbA = 0;
    private int tbB = 0;

    public GameScore() {
        this(false, 0);
    }

    public GameScore(boolean tieBreak, int tbTarget) {
        this.tieBreak = tieBreak;
        this.tbTarget = tbTarget;
    }

    // konstruktor do klonowania/przywracania
    public GameScore(boolean tieBreak, int tbTarget, int stepsA, int stepsB, int adv, int tbA, int tbB) {
        this.tieBreak = tieBreak;
        this.tbTarget = tbTarget;
        this.stepsA = stepsA;
        this.stepsB = stepsB;
        this.adv = adv;
        this.tbA = tbA;
        this.tbB = tbB;
    }

    public void pointFor(int player) {
        if (isFinished()) return;

        if (tieBreak) {
            if (player == 0) tbA++;
            else tbB++;
            return;
        }

        if (stepsA >= 3 && stepsB >= 3) { // deuce/adv
            if (adv == -1) {
                adv = player;            // advantage
            } else if (adv == player) {
                if (player == 0) stepsA++;
                else stepsB++; // zamyka gema
            } else {
                adv = -1;                // powrót do deuce
            }
            return;
        }

        if (player == 0) stepsA++;
        else stepsB++;
    }

    public boolean isFinished() {
        if (tieBreak) {
            return (tbA >= tbTarget || tbB >= tbTarget) && Math.abs(tbA - tbB) >= 2;
        }
        return (stepsA >= 4 || stepsB >= 4);
    }

    /**
     * 0 → A, 1 → B, -1 gdy trwa.
     */
    public int winner() {
        if (!isFinished()) return -1;
        if (tieBreak) return (tbA > tbB) ? 0 : 1;
        return (stepsA > stepsB) ? 0 : 1;
    }

    /**
     * Tekstowa reprezentacja punktów dla UI.
     */
    public String displayA() {
        return tieBreak ? String.valueOf(tbA) : mapStep(0);
    }

    public String displayB() {
        return tieBreak ? String.valueOf(tbB) : mapStep(1);
    }

    private String mapStep(int who) {
        int sA = stepsA, sB = stepsB;
        if (sA >= 3 && sB >= 3) {
            if (adv == -1) return "40";              // deuce (dla obu)
            if (adv == 0) return who == 0 ? "Ad" : "40";
            return who == 0 ? "40" : "Ad";
        }
        int s = (who == 0) ? sA : sB;
        return switch (s) {
            case 0 -> "0";
            case 1 -> "15";
            case 2 -> "30";
            default -> "40";
        };
    }

    // gettery potrzebne do klonowania/DTO
    public boolean isTieBreak() {
        return tieBreak;
    }

    public int tbTarget() {
        return tbTarget;
    }

    public int stepsA() {
        return stepsA;
    }

    public int stepsB() {
        return stepsB;
    }

    public int adv() {
        return adv;
    }

    public int tbA() {
        return tbA;
    }

    public int tbB() {
        return tbB;
    }
}
