package com.hubertbobowik.tiebreaker.domain;

import java.time.Duration;
import java.time.Instant;

public class Match {
    // ### Identity & config ###
    private final MatchId id;
    private final String playerA;
    private final String playerB;
    private final Rules rules;

    // ### Timestamps ###
    private final Instant createdAt;
    private Instant finishedAt; // null = trwa

    // ### Score state ###
    private int gamesA = 0, gamesB = 0;   // bieżący set
    private int setsA = 0, setsB = 0;   // całe sety
    private GameScore game = new GameScore(false, 0);

    // ### Status ###
    private boolean finished = false;
    private Integer winner = null; // 0 = A, 1 = B

    // ### Ctors ###
    public Match(MatchId id, String playerA, String playerB) {
        this(id, playerA, playerB, Rules.defaults());
    }

    public Match(MatchId id, String playerA, String playerB, Rules rules) {
        this(id, playerA, playerB, rules, Instant.now());
    }

    /**
     * Pomocniczy konstruktor używany przy odtwarzaniu ze storage.
     */
    public Match(MatchId id, String playerA, String playerB, Rules rules, Instant createdAt) {
        this.id = id;
        this.playerA = playerA;
        this.playerB = playerB;
        this.rules = (rules != null) ? rules : Rules.defaults();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    // ### Public API ###
    public void addPointFor(int idx) {
        if (finished) return;

        game.pointFor(idx);

        if (game.isFinished()) {
            int w = game.winner();
            if (w == 0) gamesA++;
            else gamesB++;
            afterGameMaybeEndSet();
            if (!finished) startNextGameOrTieBreak();
        }
    }

    // ### Set / TB logic ###
    private void startNextGameOrTieBreak() {
        boolean finalSet = isFinalSetIncoming();
        boolean doTB = shouldPlayTieBreak(finalSet);
        int target = doTB ? (finalSet ? tbTargetForFinal() : tbTargetCommon()) : 0;
        game = new GameScore(doTB, target);
    }

    private void afterGameMaybeEndSet() {
        if (isTieBreakSetWon()) {
            awardSetTo(gamesA > gamesB ? 0 : 1);
            return;
        }
        if ((gamesA >= 6 || gamesB >= 6) && Math.abs(gamesA - gamesB) >= 2) {
            awardSetTo(gamesA > gamesB ? 0 : 1);
        }
    }

    private void awardSetTo(int who) {
        if (who == 0) setsA++;
        else setsB++;
        gamesA = gamesB = 0;       // nowy set
        game = new GameScore(false, 0);

        int need = rules.bestOf() / 2 + 1; // 2 dla BO3, 3 dla BO5
        if (setsA >= need || setsB >= need) {
            finished = true;
            winner = (setsA > setsB) ? 0 : 1;
            finishedAt = Instant.now();
        }
    }

    private boolean isFinalSetIncoming() {
        int need = rules.bestOf() / 2 + 1;
        return setsA == need - 1 || setsB == need - 1;
    }

    private boolean shouldPlayTieBreak(boolean finalSet) {
        if (gamesA == 6 && gamesB == 6) {
            if (finalSet) return rules.finalSetMode() != Rules.TieBreakMode.NONE;
            return rules.tieBreakEverySet() != Rules.TieBreakMode.NONE;
        }
        return false;
    }

    private boolean isTieBreakSetWon() {
        return (gamesA == 7 && gamesB == 6) || (gamesB == 7 && gamesA == 6);
    }

    private int tbTargetCommon() {
        return switch (rules.tieBreakEverySet()) {
            case CLASSIC7 -> 7;
            case TB10 -> 10;
            default -> 7;
        };
    }

    private int tbTargetForFinal() {
        return switch (rules.finalSetMode()) {
            case CLASSIC7 -> 7;
            case TB10 -> 10;
            default -> 7;
        };
    }

    // ### UI helpers ###
    public String scoreLine() {
        return setsA + "-" + setsB + "  |  " + gamesA + ":" + gamesB + "  |  " +
                game.displayA() + " : " + game.displayB() + (game.isTieBreak() ? " (TB)" : "");
    }

    // ### Accessors ###
    public MatchId id() {
        return id;
    }

    public String playerA() {
        return playerA;
    }

    public String playerB() {
        return playerB;
    }

    public Rules rules() {
        return rules;
    }

    public int gamesA() {
        return gamesA;
    }

    public int gamesB() {
        return gamesB;
    }

    public int setsA() {
        return setsA;
    }

    public int setsB() {
        return setsB;
    }

    public GameScore game() {
        return game;
    }

    public boolean isFinished() {
        return finished;
    }

    public Integer winner() {
        return winner;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public Duration elapsed() {
        Instant end = finishedAt != null ? finishedAt : Instant.now();
        return Duration.between(createdAt, end);
    }

    // ### Copy & restore ###

    /**
     * Głęboki klon — do undo/redo.
     */
    public Match copy() {
        Match c = new Match(this.id, this.playerA, this.playerB, this.rules, this.createdAt);
        c.gamesA = this.gamesA;
        c.gamesB = this.gamesB;
        c.setsA = this.setsA;
        c.setsB = this.setsB;
        c.finished = this.finished;
        c.winner = this.winner;
        c.finishedAt = this.finishedAt;

        GameScore g = this.game;
        c.game = new GameScore(
                g.isTieBreak(),
                g.tbTarget(),
                g.stepsA(), g.stepsB(), g.adv(),
                g.tbA(), g.tbB()
        );
        return c;
    }

    /**
     * Przywrócenie stanu punktacji (bez czasów).
     */
    public Match restore(int setsA, int setsB, int gamesA, int gamesB,
                         GameScore game, boolean finished, Integer winner) {
        this.setsA = setsA;
        this.setsB = setsB;
        this.gamesA = gamesA;
        this.gamesB = gamesB;
        this.game = game;
        this.finished = finished;
        this.winner = winner;
        return this;
    }

    /**
     * Tylko dla DTO: ustaw finiš z pliku.
     */
    public Match setFinishedAtFromStorage(Instant finishedAt) {
        this.finishedAt = finishedAt;
        this.finished = finishedAt != null;
        return this;
    }
}
