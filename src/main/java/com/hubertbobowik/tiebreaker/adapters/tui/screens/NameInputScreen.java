package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;

public final class NameInputScreen {

    public static final class Names {
        public final String a;
        public final String b;
        public Names(String a, String b) { this.a = a; this.b = b; }
    }

    private final LanternaView view;

    public NameInputScreen(LanternaView view) {
        this.view = view;
    }

    /** Zwraca null jeśli użytkownik przerwał (Esc). */
    public Names ask() throws Exception {
        String a = inputOne("Wprowadź imię i nazwisko gracza 1:", "");
        if (a == null) return null;

        String b = inputOne("Wprowadź imię i nazwisko gracza 2:", "");
        if (b == null) return null;

        // Minimalna sanityzacja: przytnij spacje brzegowe, kolejne spacje redukuj do jednej
        a = normalize(a);
        b = normalize(b);
        if (a.isBlank()) a = "Player A";
        if (b.isBlank()) b = "Player B";
        return new Names(a, b);
    }

    private String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }

    /** Jedno pole tekstowe: obsługa liter, cyfr, spacji, myślników, backspace, Enter/Esc. */
    private String inputOne(String label, String initial) throws Exception {
        StringBuilder buf = new StringBuilder(initial == null ? "" : initial);

        while (true) {
            view.renderInputForm("DANE MECZU", label, buf.toString());
            KeyStroke ks = view.readRaw();
            if (ks == null) continue;

            KeyType kt = ks.getKeyType();
            if (kt == KeyType.Escape) {
                return null; // przerwane
            }
            if (kt == KeyType.Enter) {
                // akceptuj nawet puste – uzupełnimy domyślnie wyżej
                return buf.toString();
            }
            if (kt == KeyType.Backspace) {
                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                continue;
            }
            if (kt == KeyType.Character) {
                char c = ks.getCharacter();
                if ((ks.isAltDown() || (ks.isAltDown() && ks.isCtrlDown()))
                        && c < 128 && Character.isLetter(c)) {
                    continue;
                }

                if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '.' || c == '\'') {
                    buf.append(c);
                }
            }
        }
    }
}
