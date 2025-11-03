package com.hubertbobowik.tiebreaker.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Tournament {
    private final TournamentId id;
    private final int size;                 // 4/8/16/32...
    private final List<String> players;
    private final Rules rules;
    private final List<BracketRound> rounds = new ArrayList<>();

    private int currentRound = 0;           // indeks rundy
    private int currentPair = 0;            // indeks pary w rundzie
    private final Instant createdAt;
    private Instant finishedAt;
    private boolean finished = false;

    public Tournament(TournamentId id, int size, List<String> players, Rules rules) {
        if (size <= 1 || (size & (size - 1)) != 0) {
            throw new IllegalArgumentException("Size must be power of two (>= 2)");
        }
        if (players == null || players.size() != size) {
            throw new IllegalArgumentException("Players list must have exactly " + size + " names");
        }
        this.id = id;
        this.size = size;
        this.players = new ArrayList<>(players);
        this.rules = Rules.copy(rules);
        this.createdAt = Instant.now();

        buildAllRounds();            // ← prebuduj wszystkie rundy
        moveCursorToFirstPlayable(); // ← ustaw kursor na pierwszy grywalny mecz
    }

    @Deprecated
    public Tournament(TournamentId id, int size, List<String> players) {
        this(id, size, players, Rules.defaults());
    }

    /** Prebuduj całą drabinkę: runda 0 z graczami, kolejne rundy z pustymi slotami (null,null). */
    private void buildAllRounds() {
        rounds.clear();

        // Runda 0 – z graczami
        List<BracketPair> r0 = new ArrayList<>();
        for (int i = 0; i < players.size(); i += 2) {
            r0.add(new BracketPair(players.get(i), players.get(i + 1)));
        }
        rounds.add(new BracketRound(r0));

        // Kolejne rundy – puste sloty, które będziemy uzupełniać zwycięzcami
        int pairs = players.size() / 4; // w następnej rundzie połowa par
        while (pairs >= 1) {
            List<BracketPair> rs = new ArrayList<>(pairs);
            for (int i = 0; i < pairs; i++) {
                rs.add(new BracketPair(null, null));
            }
            rounds.add(new BracketRound(rs));
            pairs /= 2;
        }

        currentRound = 0;
        currentPair = 0;
        finished = false;
        finishedAt = null;
    }

    public TournamentId id() { return id; }
    public int size() { return size; }
    public List<String> players() { return Collections.unmodifiableList(players); }
    public Rules rules() { return rules; }
    public List<BracketRound> rounds() { return Collections.unmodifiableList(rounds); }
    public int currentRound() { return currentRound; }
    public int currentPair() { return currentPair; }
    public Instant createdAt() { return createdAt; }
    public Instant finishedAt() { return finishedAt; }
    public boolean isFinished() { return finished; }

    /** Tasowanie tylko przed startem. */
    public void shuffle() {
        if (!rounds.isEmpty() && (currentRound != 0 || currentPair != 0)) {
            throw new IllegalStateException("Cannot shuffle after tournament started");
        }
        Collections.shuffle(players, new Random());
        buildAllRounds();
        moveCursorToFirstPlayable();
    }

    public void confirmSeeding() {
        // hook na przyszłość
    }

    /** Bieżąca para, lub null gdy turniej zakończony. */
    public BracketPair currentPairObj() {
        if (finished) return null;
        if (currentRound >= rounds.size()) return null;
        BracketRound r = rounds.get(currentRound);
        if (currentPair >= r.size()) return null;
        return r.pairs().get(currentPair);
    }

    /** Zwycięzca 0=a, 1=b. Natychmiastowa propagacja do kolejnej rundy i przestawienie kursora. */
    public void advanceWithWinner(int winnerIdx) {
        if (isFinished()) return;

        BracketPair cur = currentPairObj();
        if (cur == null) return;

        if (!cur.isDone()) {
            cur.markWinner(winnerIdx);
        }

        // Propagacja do kolejnej rundy
        int r = currentRound;
        int i = currentPair;

        if (r + 1 < rounds.size()) {
            String winnerName = cur.winnerName();
            BracketRound next = rounds.get(r + 1);
            int nextIndex = i / 2;
            boolean goesToA = (i % 2 == 0);

            BracketPair target = next.pairs().get(nextIndex);
            if (goesToA) {
                if (target.a() == null || target.a().isBlank() || target.a().equals(winnerName)) {
                    target.setA(winnerName);
                }
            } else {
                if (target.b() == null || target.b().isBlank() || target.b().equals(winnerName)) {
                    target.setB(winnerName);
                }
            }
        }

        // Przestaw kursor na kolejną grywalną parę
        if (!moveCursorToFirstPlayable()) {
            // nic nie zostało grywalne – sprawdź finał
            if (isFinalWon()) {
                finished = true;
                finishedAt = Instant.now();
            }
        }
    }

    /** Ustaw MatchId dla bieżącej pary. */
    public void setCurrentPairMatch(MatchId id) {
        BracketPair p = currentPairObj();
        if (p == null) return;
        p.setMatchId(id);
    }

    public void replaceRounds(List<BracketRound> newRounds) {
        this.rounds.clear();
        this.rounds.addAll(newRounds);
        // po odtworzeniu ze storage dobrze od razu poprawić kursor
        moveCursorToFirstPlayable();
    }

    public Tournament restoreProgress(int currentRound, int currentPair, boolean finished, Instant finishedAt) {
        this.currentRound = currentRound;
        this.currentPair = currentPair;
        this.finished = finished;
        this.finishedAt = finishedAt;
        // zabezpieczenie po restore
        if (!finished) moveCursorToFirstPlayable();
        return this;
    }

    // ─────────────────────────────────────────────────────────────

    private boolean isPlayable(BracketPair p) {
        return p != null
                && p.a() != null
                && p.b() != null
                && !p.isDone();
    }

    /** Przesuwa kursor do pierwszej grywalnej pary (scan od runda 0 do finału). */
    private boolean moveCursorToFirstPlayable() {
        for (int r = 0; r < rounds.size(); r++) {
            BracketRound round = rounds.get(r);
            for (int i = 0; i < round.size(); i++) {
                if (isPlayable(round.pairs().get(i))) {
                    currentRound = r;
                    currentPair = i;
                    return true;
                }
            }
        }
        // brak grywalnych par – zostaw kursor jak jest, sygnalizuj false
        return false;
    }

    private boolean isFinalWon() {
        if (rounds.isEmpty()) return false;
        BracketRound last = rounds.get(rounds.size() - 1);
        return last.size() == 1 && last.pairs().get(0).isDone();
    }
}
