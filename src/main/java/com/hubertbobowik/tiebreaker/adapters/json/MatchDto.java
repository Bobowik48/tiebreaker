package com.hubertbobowik.tiebreaker.adapters.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubertbobowik.tiebreaker.domain.*;

import java.time.Instant;

public final class MatchDto {
    public final String id;
    public final String playerA;
    public final String playerB;

    // Rules
    public final int bestOf;
    public final String tbEverySet;   // NONE | CLASSIC7 | TB10
    public final String tbFinalSet;   // NONE | CLASSIC7 | TB10

    // Sety/gemy
    public final int setsA, setsB;
    public final int gamesA, gamesB;

    // Bieżący gem / TB
    public final boolean tieBreak;
    public final int tbTarget;
    public final int stepsA, stepsB, adv;
    public final int tbA, tbB;

    public final boolean finished;
    public final Integer winner; // 0/1 lub null

    // Czasy (ISO-8601), mogą nie istnieć w starych plikach
    public final String createdAt;   // np. "2025-11-02T19:45:31Z"
    public final String finishedAt;  // null lub ISO-8601

    @JsonCreator
    public MatchDto(
            @JsonProperty("id") String id,
            @JsonProperty("playerA") String playerA,
            @JsonProperty("playerB") String playerB,
            @JsonProperty("bestOf") int bestOf,
            @JsonProperty("tbEverySet") String tbEverySet,
            @JsonProperty("tbFinalSet") String tbFinalSet,
            @JsonProperty("setsA") int setsA,
            @JsonProperty("setsB") int setsB,
            @JsonProperty("gamesA") int gamesA,
            @JsonProperty("gamesB") int gamesB,
            @JsonProperty("tieBreak") boolean tieBreak,
            @JsonProperty("tbTarget") int tbTarget,
            @JsonProperty("stepsA") int stepsA,
            @JsonProperty("stepsB") int stepsB,
            @JsonProperty("adv") int adv,
            @JsonProperty("tbA") int tbA,
            @JsonProperty("tbB") int tbB,
            @JsonProperty("finished") boolean finished,
            @JsonProperty("winner") Integer winner,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("finishedAt") String finishedAt
    ) {
        this.id = id;
        this.playerA = playerA;
        this.playerB = playerB;
        this.bestOf = bestOf;
        this.tbEverySet = tbEverySet;
        this.tbFinalSet = tbFinalSet;
        this.setsA = setsA;
        this.setsB = setsB;
        this.gamesA = gamesA;
        this.gamesB = gamesB;
        this.tieBreak = tieBreak;
        this.tbTarget = tbTarget;
        this.stepsA = stepsA;
        this.stepsB = stepsB;
        this.adv = adv;
        this.tbA = tbA;
        this.tbB = tbB;
        this.finished = finished;
        this.winner = winner;
        this.createdAt = createdAt;
        this.finishedAt = finishedAt;
    }

    public static MatchDto fromDomain(Match m) {
        Rules r = m.rules();
        GameScore g = m.game();
        return new MatchDto(
                m.id().value(),
                m.playerA(),
                m.playerB(),
                r.bestOf(),
                r.tieBreakEverySet().name(),
                r.finalSetMode().name(),
                m.setsA(), m.setsB(),
                m.gamesA(), m.gamesB(),
                g.isTieBreak(),
                g.tbTarget(),
                g.stepsA(), g.stepsB(), g.adv(),
                g.tbA(), g.tbB(),
                m.isFinished(),
                m.winner(),
                m.createdAt() != null ? m.createdAt().toString() : null,
                m.finishedAt() != null ? m.finishedAt().toString() : null
        );
    }

    public Match toDomain() {
        Rules r = new Rules(
                bestOf,
                Rules.TieBreakMode.valueOf(tbEverySet),
                Rules.TieBreakMode.valueOf(tbFinalSet)
        );

        // createdAt może być null w starych plikach -> fallback na teraz
        Instant created = (createdAt != null) ? Instant.parse(createdAt) : Instant.now();

        Match m = new Match(new MatchId(id), playerA, playerB, r, created);
        GameScore gs = new GameScore(tieBreak, tbTarget, stepsA, stepsB, adv, tbA, tbB);
        m = m.restore(setsA, setsB, gamesA, gamesB, gs, finished, winner);

        if (finishedAt != null) {
            m.setFinishedAtFromStorage(Instant.parse(finishedAt));
        }
        return m;
    }
}
