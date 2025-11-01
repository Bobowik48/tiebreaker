package com.hubertbobowik.tiebreaker.adapters.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;

public final class MatchDto {
    public final String id;
    public final String playerA;
    public final String playerB;
    public final int pointsA;
    public final int pointsB;

    @JsonCreator
    public MatchDto(
            @JsonProperty("id") String id,
            @JsonProperty("playerA") String playerA,
            @JsonProperty("playerB") String playerB,
            @JsonProperty("pointsA") int pointsA,
            @JsonProperty("pointsB") int pointsB
    ) {
        this.id = id;
        this.playerA = playerA;
        this.playerB = playerB;
        this.pointsA = pointsA;
        this.pointsB = pointsB;
    }

    public static MatchDto fromDomain(Match m) {
        return new MatchDto(
                m.id().value(),
                m.playerA(),
                m.playerB(),
                m.pointsA(),
                m.pointsB()
        );
    }

    public Match toDomain() {
        Match match = new Match(new MatchId(id), playerA, playerB);
        // odtworzenie „klikniętych” punktów
        while (match.pointsA() < pointsA) match.addPointFor(0);
        while (match.pointsB() < pointsB) match.addPointFor(1);
        return match;
    }
}
