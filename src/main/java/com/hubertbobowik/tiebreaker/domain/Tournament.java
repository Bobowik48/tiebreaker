package com.hubertbobowik.tiebreaker.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Tournament {
    private final TournamentId id;
    private final int size;                 // 4/8/16/32...
    private final List<String> players;     // kolejność po losowaniu
    private final List<BracketRound> rounds = new ArrayList<>();

    private int currentRound = 0;           // indeks rundy
    private int currentPair = 0;            // indeks pary w rundzie
    private final Instant createdAt;
    private Instant finishedAt;
    private boolean finished = false;

    public Tournament(TournamentId id, int size, List<String> players) {
        if (size <= 1 || (size & (size - 1)) != 0) {
            throw new IllegalArgumentException("Size must be power of two (>= 2)");
        }
        if (players == null || players.size() != size) {
            throw new IllegalArgumentException("Players list must have exactly " + size + " names");
        }
        this.id = id;
        this.size = size;
        this.players = new ArrayList<>(players);
        this.createdAt = Instant.now();
        // Pierwsza runda z aktualnej kolejności
        buildInitialRound();
    }

    private void buildInitialRound() {
        rounds.clear();
        List<BracketPair> pairs = new ArrayList<>();
        for (int i = 0; i < players.size(); i += 2) {
            pairs.add(new BracketPair(players.get(i), players.get(i + 1)));
        }
        rounds.add(new BracketRound(pairs));
        currentRound = 0;
        currentPair = 0;
        finished = false;
        finishedAt = null;
    }

    public TournamentId id() {
        return id;
    }

    public int size() {
        return size;
    }

    public List<String> players() {
        return Collections.unmodifiableList(players);
    }

    public List<BracketRound> rounds() {
        return Collections.unmodifiableList(rounds);
    }

    public int currentRound() {
        return currentRound;
    }

    public int currentPair() {
        return currentPair;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public boolean isFinished() {
        return finished;
    }

    /**
     * Losuje kolejność graczy w drabince (przed zatwierdzeniem).
     */
    public void shuffle() {
        if (!rounds.isEmpty() && (currentRound != 0 || currentPair != 0)) {
            throw new IllegalStateException("Cannot shuffle after tournament started");
        }
        Collections.shuffle(players, new Random());
        buildInitialRound();
    }

    /**
     * Formalnie „zatwierdza” rozstawienie — nic nie robi, ale zostawiamy jako punkt kontrolny.
     */
    public void confirmSeeding() {
        // Miejsce na przyszłe walidacje/lock seeding
    }

    /**
     * Zwraca bieżącą parę do rozegrania, lub null jeśli turniej skończony.
     */
    public BracketPair currentPairObj() {
        if (finished) return null;
        if (currentRound >= rounds.size()) return null;
        BracketRound r = rounds.get(currentRound);
        if (currentPair >= r.size()) return null;
        return r.pairs().get(currentPair);
    }

    /**
     * Ustaw zwycięzcę bieżącej pary: 0=a, 1=b. Przepycha zwycięzcę dalej.
     */
    public void advanceWithWinner(int winnerIdx) {
        if (finished) return;
        BracketPair pair = currentPairObj();
        if (pair == null) return;
        if (winnerIdx != 0 && winnerIdx != 1) {
            throw new IllegalArgumentException("Winner must be 0 or 1");
        }
        pair.setWinner(winnerIdx);

        // Następna para w tej rundzie
        BracketRound round = rounds.get(currentRound);
        currentPair++;

        // Jeżeli zamknęliśmy rundę, zbuduj kolejną
        if (currentPair >= round.size()) {
            List<String> winners = new ArrayList<>();
            for (BracketPair p : round.pairs()) {
                if (!p.isDone()) {
                    throw new IllegalStateException("Round not fully decided");
                }
                winners.add(p.winnerName());
            }

            if (winners.size() == 1) {
                // Mamy zwycięzcę turnieju
                finished = true;
                finishedAt = Instant.now();
                return;
            }

            // Budujemy kolejną rundę
            List<BracketPair> nextPairs = new ArrayList<>();
            for (int i = 0; i < winners.size(); i += 2) {
                nextPairs.add(new BracketPair(winners.get(i), winners.get(i + 1)));
            }
            rounds.add(new BracketRound(nextPairs));
            currentRound++;
            currentPair = 0;
        }
    }

    /**
     * Ustaw MatchId dla bieżącej pary, gdy startujemy mecz.
     */
    public void setCurrentPairMatch(MatchId id) {
        BracketPair p = currentPairObj();
        if (p == null) return;
        p.setMatchId(id);
    }

    public void replaceRounds(List<BracketRound> newRounds) {
        this.rounds.clear();
        this.rounds.addAll(newRounds);
    }

    /**
     * Proste API do odtworzenia ze storage (opcjonalnie).
     */
    public Tournament restoreProgress(int currentRound, int currentPair, boolean finished, Instant finishedAt) {
        this.currentRound = currentRound;
        this.currentPair = currentPair;
        this.finished = finished;
        this.finishedAt = finishedAt;
        return this;
    }
}