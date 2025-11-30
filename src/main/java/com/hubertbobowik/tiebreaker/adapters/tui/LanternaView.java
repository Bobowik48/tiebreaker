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
// ▼ do drabinki
import com.hubertbobowik.tiebreaker.domain.BracketPair;
import com.hubertbobowik.tiebreaker.domain.BracketRound;
import com.hubertbobowik.tiebreaker.domain.Tournament;

import java.io.IOException;
import java.util.List;

public class LanternaView implements AutoCloseable {

    // Intencje podczas meczu (litery)
    public enum UserIntent {POINT_A, POINT_B, UNDO, REDO, FINISH, QUIT, NONE}

    // Strzałki/Enter/Esc – do nawigacji w menu/listach
    public enum NavKey {UP, DOWN, LEFT, RIGHT, ENTER, ESC, NONE}

    private Screen screen;
    private TextGraphics g;
    private TerminalSize lastSize;

    private boolean staticDrawn = false;

    // Layout podstawowego ekranu meczu
    private static final int TITLE_ROW = 1;
    private static final int NAMES_ROW = 3;
    private static final int SCORE_ROW = 5;
    private static final int HELP_ROW = 8;

    // ── ŻYCIE EKRANU ─────────────────────────────────────────────

    public void open() throws IOException {
        DefaultTerminalFactory factory = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(120, 30))
                .setPreferTerminalEmulator(true); // stabilniej na Win/Mac

        screen = factory.createScreen();
        screen.startScreen();
        screen.setCursorPosition(null);

        g = screen.newTextGraphics();
        fullClear();

        lastSize = screen.getTerminalSize();
        staticDrawn = false;
    }

    /**
     * Wołaj w pętli – dba o poprawne odrysowanie po resize.
     */
    public void checkResizeAndRedraw(Match m) throws IOException {
        TerminalSize newSize = screen.doResizeIfNecessary();
        TerminalSize current = screen.getTerminalSize();
        boolean resized = (newSize != null && !newSize.equals(lastSize)) || !current.equals(lastSize);

        if (resized) {
            lastSize = current;
            staticDrawn = false;
            screen.refresh(Screen.RefreshType.COMPLETE);
        }

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
            renderScoreLine(m.scoreLine());
        }
    }

    /**
     * Twarde czyszczenie ekranu (np. przy zmianie ekranu).
     */
    public void fullClear() throws IOException {
        screen.clear();
        screen.refresh(Screen.RefreshType.COMPLETE);
        staticDrawn = false;
    }

    // ── RYSOWANIE EKRANU MECZU ──────────────────────────────────

    public void renderStatic(Match m) throws IOException {
        if (staticDrawn) return;

        screen.clear();

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, TITLE_ROW, "TIEBREAKER — TRYB TEKSTOWY", SGR.BOLD);

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, NAMES_ROW, m.playerA() + "   vs   " + m.playerB());

        g.setForegroundColor(TextColor.ANSI.CYAN);
        g.putString(2, SCORE_ROW, "Wynik: ");

        // ▼ pierwsze imiona w promptach
        String firstA = firstNameOf(m.playerA());
        String firstB = firstNameOf(m.playerB());
        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, HELP_ROW,
                "[A] punkt dla " + firstA +
                        "   [B] punkt dla " + firstB +
                        "   [U] cofnij   [R] przywróć   [X] zakończ   [Q] wyjście"
        );

        screen.refresh();
        staticDrawn = true;
    }

    /**
     * Aktualizuje wyłącznie linię wyniku (bez migotania).
     */
    public void renderScoreLine(String line) throws IOException {
        g.setForegroundColor(TextColor.ANSI.CYAN);
        g.putString(2, SCORE_ROW, "Wynik: ");
        g.setForegroundColor(TextColor.ANSI.GREEN);
        g.putString(10, SCORE_ROW, padRight(line, 32));
        screen.refresh();
    }

    public void renderElapsed(String text) throws IOException {
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, SCORE_ROW + 1, padRight(text, 40));
        screen.refresh();
    }

    /**
     * Pasek zwycięzcy po zakończeniu meczu.
     */
    public void renderWinnerPanel(String winner) throws IOException {
        // wyczyść sekcję pod wynikiem (czas, pomoc, stare opcje)
        clearLine(SCORE_ROW + 1);
        clearLine(SCORE_ROW + 2);
        clearLine(SCORE_ROW + 3);
        clearLine(HELP_ROW);

        // zwycięzca
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, SCORE_ROW + 2, padRight("Zwycięzca: " + winner, 60), SGR.BOLD);

        // tylko to, co chcemy: [H] historia, [Q] wyjście
        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, SCORE_ROW + 3, padRight("[H] historia    [Q] wyjście", 60));

        screen.refresh();
    }

    public void renderWinnerPanelTournament(String winner) throws IOException {
        clearLine(SCORE_ROW + 1);
        clearLine(SCORE_ROW + 2);
        clearLine(SCORE_ROW + 3);
        clearLine(HELP_ROW);

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, SCORE_ROW + 2, padRight("Zwycięzca: " + winner, 60), SGR.BOLD);

        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, SCORE_ROW + 3, padRight("[B] drabinka    [Q] wyjście", 60));

        screen.refresh();
    }

    // ── PRYMITYWY: MENU / LISTY ─────────────────────────────────

    public void renderSimpleMenu(String title, String[] items, int selected) throws IOException {
        fullClear();
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, 1, title, SGR.BOLD);

        for (int i = 0; i < items.length; i++) {
            boolean sel = (i == selected);
            g.setForegroundColor(sel ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE);
            g.putString(4, 3 + i, (sel ? "› " : "  ") + items[i]);
        }
        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, 3 + items.length + 1, "[↑/↓] wybór   [Enter] zatwierdź   [Esc] wstecz");
        screen.refresh();
    }

    public void renderMatchesList(String title, java.util.List<String> lines, int selected) throws IOException {
        // 🔧 złap resize zanim policzymy szerokość
        TerminalSize newSize = screen.doResizeIfNecessary();
        if (newSize != null && !newSize.equals(lastSize)) {
            lastSize = newSize;
            screen.refresh(Screen.RefreshType.COMPLETE);
        }

        fullClear();

        int w = screen.getTerminalSize().getColumns();
        int usable = Math.max(10, w - 6); // marginesy

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, 1, title, SGR.BOLD);

        for (int i = 0; i < lines.size(); i++) {
            boolean sel = (i == selected);
            g.setForegroundColor(sel ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE);

            String text = lines.get(i);
            String toDraw = (text.length() > usable) ? clip(text, usable) : text;

            g.putString(4, 3 + i, (sel ? "› " : "  ") + toDraw);
        }

        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, 3 + lines.size() + 1, "[↑/↓] wybór   [Esc] wstecz");
        screen.refresh();
    }

    public void renderRulesPicker(
            int focusedSection, int bestOf,
            String everySetLabel, String finalSetLabel,
            String activeDescription
    ) throws IOException {
        fullClear();
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, 1, "Wybór zasad", SGR.BOLD);

        drawRow(3, "Best of", String.valueOf(bestOf), focusedSection == 0);
        drawRow(5, "Tiebreak w setach", everySetLabel, focusedSection == 1);
        drawRow(7, "Finałowy set", finalSetLabel, focusedSection == 2);

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, 9, "Opis:", SGR.BOLD);

        g.setForegroundColor(TextColor.ANSI.GREEN);
        putWrap(activeDescription, 2, 10, 94);

        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, 13, "[↑/↓] sekcja   [←/→] wybór   [R] domyślne   [Enter] start   [Esc] wstecz");
        screen.refresh();
    }

    private void drawRow(int y, String name, String value, boolean focused) throws IOException {
        g.setForegroundColor(focused ? TextColor.ANSI.GREEN : TextColor.ANSI.WHITE);
        String line = String.format("%-18s: %s", name, value);
        String text = (focused ? "> " : "  ") + line;

        if (focused) {
            g.putString(2, y, text, SGR.BOLD);
        } else {
            g.putString(2, y, text);
        }
    }

    private void putWrap(String txt, int x, int y, int width) throws IOException {
        String[] w = txt.split(" ");
        StringBuilder row = new StringBuilder();
        int cy = y;
        for (String s : w) {
            if (row.length() + s.length() + 1 > width) {
                g.putString(x, cy++, row.toString());
                row.setLength(0);
            }
            if (row.length() > 0) row.append(' ');
            row.append(s);
        }
        if (row.length() > 0) g.putString(x, cy, row.toString());
    }

    /**
     * Prosty dialog: Enter = TAK, Esc = NIE.
     */
    public boolean confirm(String question) throws IOException {
        fullClear();
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, 2, question, SGR.BOLD);
        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, 4, "[Enter] tak    [Esc] nie");
        screen.refresh();
        while (true) {
            KeyStroke ks = screen.readInput();
            if (ks == null) continue;
            if (ks.getKeyType() == KeyType.Enter) return true;
            if (ks.getKeyType() == KeyType.Escape) return false;
        }
    }

    // ── WEJŚCIE KLAWIATUROWE ────────────────────────────────────

    public UserIntent readIntent() throws IOException {
        KeyStroke ks = screen.pollInput();
        if (ks == null) {
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
                case 'X' -> UserIntent.FINISH;
                case 'Q' -> UserIntent.QUIT;
                default -> UserIntent.NONE;
            };
        }
        if (ks.getKeyType() == KeyType.EOF) return UserIntent.QUIT;
        return UserIntent.NONE;
    }

    public com.googlecode.lanterna.input.KeyStroke readRaw() throws IOException {
        return screen.readInput();
    }

    public NavKey readKey() throws IOException {
        KeyStroke ks = screen.readInput();
        if (ks == null) return NavKey.NONE;
        return switch (ks.getKeyType()) {
            case ArrowUp -> NavKey.UP;
            case ArrowDown -> NavKey.DOWN;
            case ArrowLeft -> NavKey.LEFT;
            case ArrowRight -> NavKey.RIGHT;
            case Enter -> NavKey.ENTER;
            case Escape -> NavKey.ESC;
            default -> NavKey.NONE;
        };
    }

    public void renderInputForm(String title, String label, String value) throws IOException {
        fullClear();
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, 1, title, SGR.BOLD);

        g.setForegroundColor(TextColor.ANSI.CYAN);
        g.putString(2, 4, label);

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(4, 6, padRight(value, Math.max(20, value.length())));

        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, 9, "[Enter] zatwierdź   [Backspace] usuń   [Esc] wstecz");

        screen.refresh();
    }

    public void renderServer(String line) throws IOException {
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(2, SCORE_ROW + 2, padRight(line, 50));
        screen.refresh();
    }

    // ── NOWOŚĆ: GRAFICZNA DRABINKA ─────────────────────────────

    /**
     * Rysuje graficzną drabinkę z łącznikami. Winnerzy są widoczni od razu,
     * a aktualny mecz zaznaczony zieloną strzałką.
     */
    public void renderBracket(Tournament t) throws IOException {
        fullClear();

        int cols = screen.getTerminalSize().getColumns();
        int rows = screen.getTerminalSize().getRows();

        g.setForegroundColor(TextColor.ANSI.WHITE);
        String title = "PODGLĄD DRABINKI — " + t.size() + (t.isFinished() ? " [ZAKOŃCZONY]" : "");
        g.putString(2, 1, clip(title, Math.max(20, cols - 4)), SGR.BOLD);

        int roundsCount = t.rounds().size();
        int colWidth = Math.max(24, Math.min(36, (cols - 6) / Math.max(1, roundsCount)));
        int leftX = 2;
        int baseGap = 3;
        int topOffset = 3;

        for (int r = 0; r < roundsCount; r++) {
            var round = t.rounds().get(r);
            int x = leftX + r * colWidth;

            g.setForegroundColor(TextColor.ANSI.CYAN);
            g.putString(x, topOffset - 1, "Runda " + (r + 1));

            int block = baseGap * (1 << r);
            int yStart = topOffset;

            for (int i = 0; i < round.size(); i++) {
                var p = round.pairs().get(i);
                int y = yStart + i * block;

                boolean isCurrent = (r == t.currentRound() && i == t.currentPair() && !t.isFinished());

                String a = p.a() == null || p.a().isBlank() ? "—" : p.a();
                String b = p.b() == null || p.b().isBlank() ? "—" : p.b();

                // 0 = A wygrał, 1 = B wygrał, -1 = jeszcze nie
                int winnerIdx = -1;
                if (p.isDone()) {
                    Integer w = p.winner();
                    if (w != null && (w == 0 || w == 1)) winnerIdx = w;
                }

                // rysuj boks; zwycięzca ma zielony wiersz
                putBox(x, y, colWidth - 2, a, b, isCurrent, winnerIdx);

                // łączniki do kolejnej kolumny
                if (r < roundsCount - 1) {
                    int xRight = x + (colWidth - 2);
                    int midA = y;
                    int midB = y + 1;
                    int joinY = y + (block / 2);

                    g.setForegroundColor(TextColor.ANSI.WHITE);
                    hLine(xRight, midA, 3);
                    hLine(xRight, midB, 3);
                    vLine(xRight + 3, Math.min(midA, midB), Math.max(midA, midB));
                    if (block >= 2) {
                        vLine(xRight + 3, Math.min(midA, midB), joinY);
                        hLine(xRight + 3, joinY, 3);
                    }
                }
            }
        }

        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, rows - 2, clip("[Esc] wstecz   Zielony wiersz = zwycięzca   Zielona strzałka = aktualny mecz", Math.max(10, cols - 4)));
        screen.refresh();
    }

    // ── PRYMITYWY RYSUJĄCE DLA DRABINKI ─────────────────────────

    private void putBox(int x, int y, int w, String a, String b, boolean current, int winnerIdx) throws IOException {
        String top = "┌" + "─".repeat(Math.max(2, w - 2)) + "┐";
        String bot = "└" + "─".repeat(Math.max(2, w - 2)) + "┘";
        String lineA = "│ " + padRight(a, Math.max(0, w - 4)) + " │";
        String lineB = "│ " + padRight(b, Math.max(0, w - 4)) + " │";

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x, y - 1, top);

        // A – podświetl jeśli winnerIdx==0; zielona strzałka jeśli current
        if (winnerIdx == 0) g.setForegroundColor(TextColor.ANSI.GREEN);
        else g.setForegroundColor(TextColor.ANSI.WHITE);
        String leftIndicatorA = current ? "►" : " ";
        g.putString(x, y, leftIndicatorA + lineA.substring(1)); // zamieniamy pierwszy znak na znacznik

        // B – podświetl jeśli winnerIdx==1
        if (winnerIdx == 1) g.setForegroundColor(TextColor.ANSI.GREEN);
        else g.setForegroundColor(TextColor.ANSI.WHITE);
        String leftIndicatorB = " ";
        g.putString(x, y + 1, leftIndicatorB + lineB.substring(1));

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x, y + 2, bot);
    }

    private String highlight(String s) {
        // Tu tylko zwracamy s; atrybut BOLD załatwiamy przez osobne wywołanie,
        // ale lanterna nie miesza łatwo SGR per fragment. Utrzymujemy spójny wygląd.
        return s;
    }

    private void hLine(int x, int y, int len) {
        if (len <= 0) return;
        g.putString(x, y, "─".repeat(len));
    }

    private void vLine(int x, int y1, int y2) {
        int from = Math.min(y1, y2);
        int to = Math.max(y1, y2);
        for (int yy = from; yy <= to; yy++) {
            g.putString(x, yy, "│");
        }
    }

    private static String safeName(String s, int max) {
        if (s == null || s.isBlank()) return padRight("-", Math.max(1, max));
        return clip(s, Math.max(1, max));
    }

    private static String padRight(String s, int len) {
        if (s.length() >= len) return s;
        return s + " ".repeat(len - s.length());
    }

    private static String clip(String s, int max) {
        if (max <= 0) return "";
        if (s.length() <= max) return s;
        if (max <= 1) return "…";
        return s.substring(0, max - 1) + "…";
    }

    private static String firstNameOf(String full) {
        if (full == null || full.isBlank()) return "A";
        String s = full.trim();
        int sp = s.indexOf(' ');
        return sp > 0 ? s.substring(0, sp) : s;
    }

    private void clearLine(int y) throws IOException {
        int w = screen.getTerminalSize().getColumns();
        g.setForegroundColor(TextColor.ANSI.DEFAULT);
        g.putString(0, y, " ".repeat(Math.max(0, w)));
    }

    @Override
    public void close() {
        if (screen != null) {
            try {
                screen.stopScreen();
            } catch (Exception ignored) {
            }
        }
    }
}