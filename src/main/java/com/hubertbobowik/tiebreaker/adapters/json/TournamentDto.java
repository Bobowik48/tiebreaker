package com.hubertbobowik.tiebreaker.adapters.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubertbobowik.tiebreaker.domain.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TournamentDto {
    public final String id;
    public final int size;
    public final List<String> players;
    public final int currentRound;
    public final int currentPair;
    public final boolean finished;
    public final String createdAt;
    public final String finishedAt;

    public final List<List<PairDto>> rounds;

    public static final class PairDto {
        public final String a;
        public final String b;
        public final String matchId;
        public final Integer winner;

        @JsonCreator
        public PairDto(
                @JsonProperty("a") String a,
                @JsonProperty("b") String b,
                @JsonProperty("matchId") String matchId,
                @JsonProperty("winner") Integer winner
        ) {
            this.a = a;
            this.b = b;
            this.matchId = matchId;
            this.winner = winner;
        }

        static PairDto from(BracketPair p) {
            return new PairDto(
                    p.a(),
                    p.b(),
                    p.matchId() == null ? null : p.matchId().value(),
                    p.winner()
            );
        }
    }

    @JsonCreator
    public TournamentDto(
            @JsonProperty("id") String id,
            @JsonProperty("size") int size,
            @JsonProperty("players") List<String> players,
            @JsonProperty("currentRound") int currentRound,
            @JsonProperty("currentPair") int currentPair,
            @JsonProperty("finished") boolean finished,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("finishedAt") String finishedAt,
            @JsonProperty("rounds") List<List<PairDto>> rounds
    ) {
        this.id = id;
        this.size = size;
        this.players = players;
        this.currentRound = currentRound;
        this.currentPair = currentPair;
        this.finished = finished;
        this.createdAt = createdAt;
        this.finishedAt = finishedAt;
        this.rounds = rounds;
    }

    public static TournamentDto fromDomain(Tournament t) {
        List<List<PairDto>> rs = new ArrayList<>();
        for (BracketRound r : t.rounds()) {
            List<PairDto> ps = new ArrayList<>();
            for (BracketPair p : r.pairs()) {
                ps.add(PairDto.from(p));
            }
            rs.add(ps);
        }

        return new TournamentDto(
                t.id().value(),
                t.size(),
                new ArrayList<>(t.players()),
                t.currentRound(),
                t.currentPair(),
                t.isFinished(),
                t.createdAt().toString(),
                t.finishedAt() != null ? t.finishedAt().toString() : null,
                rs
        );
    }

    public Tournament toDomain() {
        Tournament t = new Tournament(
                new TournamentId(id),
                size,
                players
        );

        // zbuduj od nowa rundy z DTO
        List<BracketRound> roundList = new ArrayList<>();
        List<List<PairDto>> src = (this.rounds != null) ? this.rounds : List.of();
        for (List<PairDto> rp : src) {
            List<BracketPair> ps = new ArrayList<>();
            for (PairDto p : rp) {
                ps.add(new BracketPair(
                        p.a,
                        p.b,
                        p.matchId == null ? null : new MatchId(p.matchId),
                        p.winner
                ));
            }
            roundList.add(new BracketRound(ps));
        }

        // zamiast refleksji:
        t.replaceRounds(roundList);

        t.restoreProgress(
                currentRound,
                currentPair,
                finished,
                finishedAt == null ? null : Instant.parse(finishedAt)
        );

        return t;
    }
}
