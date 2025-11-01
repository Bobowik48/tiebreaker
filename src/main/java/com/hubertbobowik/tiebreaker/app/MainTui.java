package com.hubertbobowik.tiebreaker.app;

import java.util.Scanner;
import com.hubertbobowik.tiebreaker.application.MatchService;
import com.hubertbobowik.tiebreaker.application.impl.MatchServiceImpl;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.MatchId;

public final class MainTui {
    public static void main(String[] args) {
        MatchService service = new MatchServiceImpl();
        MatchId matchId = new MatchId("LOCAL-0001");

        Scanner in = new Scanner(System.in);
        System.out.println("=== TieBreaker — bootstrap ===");
        System.out.println("[A] point A  [B] point B  [Q] quit");

        while (true) {
            Match m = service.getMatch(matchId);
            System.out.println(m.playerA() + " vs " + m.playerB() + "   score: " + m.pointsA() + " : " + m.pointsB());
            System.out.print("> ");
            String s = in.nextLine().trim().toUpperCase();
            switch (s) {
                case "A" -> service.addPoint(matchId, 0);
                case "B" -> service.addPoint(matchId, 1);
                case "Q" -> { System.out.println("Bye!"); return; }
                default  -> System.out.println("Unknown command");
            }
        }
    }
}
