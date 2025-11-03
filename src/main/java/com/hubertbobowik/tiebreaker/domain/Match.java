package com.hubertbobowik.tiebreaker.domain;

import java.time.Duration;
import java.time.Instant;

public class Match {
    // ### Identity & config ###
    private final MatchId id;
    private final String playerA;
    private final String playerB;
    private final Rules rules;

    // ### Serving ###
    // 0 = A, 1 = B
    private int startingServer = 0;        // kto zaczyna mecz
    private int currentServer = 0;         // kto serwuje w bieżącym gemie
    private int startingServerOfSet = 0;   // kto zaczął bieżący set

    // ### Timestamps ###
    private final Instant createdAt;
    private Instant finishedAt; // null = trwa

    // ### Score state ###
    private int gamesA = 0, gamesB = 0;   // bieżący set
    private int setsA = 0, setsB = 0;     // całe sety
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

            // Czy po tym gemie zamkniemy set lub wejdziemy w TB?
            boolean finalSet = isFinalSetIncoming();
            boolean willPlayTB =
                    (gamesA == 6 && gamesB == 6) &&
                            (finalSet ? rules.finalSetMode() != Rules.TieBreakMode.NONE
                                    : rules.tieBreakEverySet() != Rules.TieBreakMode.NONE);

            boolean setWillCloseNow =
                    ((gamesA >= 6 || gamesB >= 6) && Math.abs(gamesA - gamesB) >= 2) ||
                            ((gamesA == 7 && gamesB == 6) || (gamesB == 7 && gamesA == 6));

            // Standard ATP: rotacja serwującego po KAŻDYM gemie,
            // chyba że właśnie zamykamy set albo wchodzimy w TB.
            if (!willPlayTB && !setWillCloseNow) {
                currentServer ^= 1;
            }

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

        if (!doTB && gamesA + gamesB == 0) {
            // pierwszy gem nowego seta zaczyna startingServerOfSet
            currentServer = startingServerOfSet;
        }
        // w TB serwujący liczony jest dynamicznie w serverInTieBreak()
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

        // nowy set
        gamesA = gamesB = 0;
        game = new GameScore(false, 0);

        // kolejny set zacznie przeciwnik względem poprzedniego startera seta
        startingServerOfSet ^= 1;
        currentServer = startingServerOfSet;

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

    /**
     * Ustaw startera meczu. Wołaj tuż po utworzeniu meczu.
     */
    public void setStartingServer(int s) {
        this.startingServer = (s == 0) ? 0 : 1;
        this.currentServer = this.startingServer;
        this.startingServerOfSet = this.startingServer;
    }

    /**
     * Kto powinien być pokazany jako serwujący aktualnie w UI.
     */
    public int currentServerForDisplay() {
        return game.isTieBreak() ? serverInTieBreak() : currentServer;
    }

    /**
     * Serwujący w TB: pierwszy punkt starter seta, dalej bloki 2-punktowe.
     */
    public int serverInTieBreak() {
        int total = game.tbA() + game.tbB();
        int s = startingServerOfSet;
        if (total == 0) return s;
        int block = (total - 1) / 2;
        return (block % 2 == 0) ? (s ^ 1) : s;
    }

    public Duration elapsed() {
        Instant end = finishedAt != null ? finishedAt : Instant.now();
        return Duration.between(createdAt, end);
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

    // Gettery serwisu BEZ prefiksu get
    public int startingServer() {
        return startingServer;
    }

    public int currentServer() {
        return currentServer;
    }

    public int startingServerOfSet() {
        return startingServerOfSet;
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

        c.startingServer = this.startingServer;
        c.currentServer = this.currentServer;
        c.startingServerOfSet = this.startingServerOfSet;

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
     * Przywrócenie stanu punktacji.
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
     * Przywrócenie pól serwisu ze storage.
     */
    public Match restoreServing(int starting, int startingOfSet, int current) {
        this.startingServer = (starting == 0) ? 0 : 1;
        this.startingServerOfSet = (startingOfSet == 0) ? 0 : 1;
        this.currentServer = (current == 0) ? 0 : 1;
        return this;
    }

    /**
     * Tylko dla DTO: ustaw finish z pliku.
     */
    public Match setFinishedAtFromStorage(Instant finishedAt) {
        this.finishedAt = finishedAt;
        this.finished = finishedAt != null;
        return this;
    }

    public void finishNow() {
        if (!finished) {
            finished = true;
            finishedAt = java.time.Instant.now();
        }
    }
}
