package com.hubertbobowik.tiebreaker.adapters.tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.hubertbobowik.tiebreaker.domain.Match;

import java.io.IOException;

public final class LanternaView implements AutoCloseable {

    public enum UserIntent {POINT_A, POINT_B, UNDO, REDO, QUIT, NONE}

    private Screen screen;
    private TextGraphics g;
    private TerminalSize lastSize;

    private boolean staticDrawn = false;
    private int lastA = Integer.MIN_VALUE;
    private int lastB = Integer.MIN_VALUE;

    // Layout (możesz zmieniać)
    private static final int TITLE_ROW = 1;
    private static final int NAMES_ROW = 3;
    private static final int SCORE_ROW = 5;
    private static final int HELP_ROW  = 8;

    public void open() throws IOException {
        DefaultTerminalFactory factory = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(100, 30));

        // For Windows/Mac stability — prefer emulator if possible
        factory.setPreferTerminalEmulator(true);

        screen = factory.createScreen();
        screen.startScreen();
        screen.setCursorPosition(null);

        g = screen.newTextGraphics();

        screen.clear();
        screen.refresh(Screen.RefreshType.COMPLETE);

        lastSize = screen.getTerminalSize();
        staticDrawn = false;
        lastA = Integer.MIN_VALUE;
        lastB = Integer.MIN_VALUE;
    }

    /** Wołaj w każdej iteracji pętli zanim przeczytasz klawisz. */
    public void checkResizeAndRedraw(Match m) throws IOException {
        TerminalSize newSize = screen.doResizeIfNecessary();
        TerminalSize current = screen.getTerminalSize();
        boolean resized = (newSize != null && !newSize.equals(lastSize)) || !current.equals(lastSize);

        if (resized) {
            lastSize = current;
            staticDrawn = false;
            lastA = Integer.MIN_VALUE;
            lastB = Integer.MIN_VALUE;
            screen.refresh(Screen.RefreshType.COMPLETE);
        }

        // Za małe okno
        if (current.getColumns() < 40 || current.getRows() < 10) {
            screen.clear();
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(1, 1, "Powieksz okno (min 40x10), aby wyswietlic UI.", SGR.BOLD);
            screen.refresh();
            staticDrawn = false;
            return;
        }

        if (!staticDrawn) {
            renderStatic(m);
            renderScore(m);
        }
    }

    public void renderStatic(Match m) throws IOException {
        if (staticDrawn) return;

        screen.clear();

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, TITLE_ROW, "TIEBREAKER — TRYB TEKSTOWY", SGR.BOLD);

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, NAMES_ROW, m.playerA() + "   vs   " + m.playerB());

        g.setForegroundColor(TextColor.ANSI.CYAN);
        g.putString(2, SCORE_ROW, "Wynik: ");

        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, HELP_ROW,
                "[A] punkt dla A   [B] punkt dla B   [U] cofnij   [R] przywróć   [Q] wyjście");

        screen.refresh();
        staticDrawn = true;
    }

    /** Aktualizacja tylko wyniku, bez migotania. */
    public void renderScore(Match m) throws IOException {
        int a = m.pointsA();
        int b = m.pointsB();

        if (a != lastA || b != lastB) {
            g.setForegroundColor(TextColor.ANSI.GREEN);
            String padded = padRight(a + " : " + b, 9);
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

    /** Nieblokujący input — bez zacięć. */
    public UserIntent readIntent() throws IOException {
        KeyStroke ks = screen.pollInput();
        if (ks == null) {
            // delikatny oddech CPU
            try { Thread.sleep(16); } catch (InterruptedException ignored) {}
            return UserIntent.NONE;
        }

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

        if (ks.getKeyType() == KeyType.EOF) return UserIntent.QUIT;
        return UserIntent.NONE;
    }

    @Override
    public void close() {
        if (screen != null) {
            try { screen.stopScreen(); } catch (Exception ignored) {}
        }
    }
}
