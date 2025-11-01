package com.hubertbobowik.tiebreaker.adapters.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.hubertbobowik.tiebreaker.domain.Match;

import java.io.IOException;

/**
 * Minimalny, ale solidny widok w Lanternie:
 * - rysuje statyczny layout raz
 * - aktualizuje TYLKO linie ze stanem punktów (partial redraw)
 * - mapuje klawisze: A/B (punkt), U (undo - wkrótce), R (redo - wkrótce), Q (quit)
 */
public final class LanternaView implements AutoCloseable {

    public enum UserIntent { POINT_A, POINT_B, UNDO, REDO, QUIT, NONE }

    private Screen screen;
    private TextGraphics g;

    private boolean staticDrawn = false;
    private int lastA = Integer.MIN_VALUE;
    private int lastB = Integer.MIN_VALUE;

    // Stałe pozycje elementów (jeden punkt od lewej, dwa od góry)
    private static final int TITLE_ROW = 1;
    private static final int NAMES_ROW = 3;
    private static final int SCORE_ROW = 5;
    private static final int HELP_ROW  = 8;

    public void open() throws IOException {
        var factory = new DefaultTerminalFactory();
        factory.setInitialTerminalSize(new TerminalSize(60, 14));
        screen = factory.createScreen();
        screen.startScreen();
        screen.setCursorPosition(null);
        g = screen.newTextGraphics();
    }

    public void renderStatic(Match m) throws IOException {
        if (staticDrawn) return;

        screen.clear();

        // Tytuł
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, TITLE_ROW, "TIEBREAKER — TEXT UI", SGR.BOLD);

        // Zawodnicy
        g.putString(2, NAMES_ROW,
                m.playerA() + "   vs   " + m.playerB());

        // Opis klawiszy
        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, HELP_ROW, "[A] point A   [B] point B   [U] undo   [R] redo   [Q] quit");

        screen.refresh();
        staticDrawn = true;
    }

    /** Tylko aktualizacja wyniku – żadnego pełnego czyszczenia ekranu. */
    public void renderScore(Match m) throws IOException {
        int a = m.pointsA();
        int b = m.pointsB();

        if (a != lastA || b != lastB) {
            // label
            g.setForegroundColor(TextColor.ANSI.CYAN);
            g.putString(2, SCORE_ROW, "Score: ");

            // wartość
            g.setForegroundColor(TextColor.ANSI.GREEN);
            // nadpisz starą wartość bez zostawiania śmieci
            String score = a + " : " + b;
            String padded = padRight(score, 9); // trochę luzu na nadpisanie dłuższych wartości
            g.putString(10, SCORE_ROW, padded);

            screen.refresh();

            lastA = a;
            lastB = b;
        }
    }

    private static String padRight(String s, int len) {
        if (s.length() >= len) return s;
        return s + " ".repeat(len - s.length());
    }

    /** Zwraca intencję użytkownika (blokująco czeka na klawisz). */
    public UserIntent readIntent() throws IOException {
        KeyStroke ks = screen.readInput();
        if (ks == null) return UserIntent.NONE;

        if (ks.getKeyType() == KeyType.Character) {
            char c = Character.toUpperCase(ks.getCharacter());
            return switch (c) {
                case 'A' -> UserIntent.POINT_A;
                case 'B' -> UserIntent.POINT_B;
                case 'U' -> UserIntent.UNDO;
                case 'R' -> UserIntent.REDO;
                case 'Q' -> UserIntent.QUIT;
                default  -> UserIntent.NONE;
            };
        }
        if (ks.getKeyType() == KeyType.EOF) {
            return UserIntent.QUIT;
        }
        return UserIntent.NONE;
    }

    @Override
    public void close() {
        if (screen != null) {
            try { screen.stopScreen(); } catch (Exception ignored) {}
        }
    }
}
