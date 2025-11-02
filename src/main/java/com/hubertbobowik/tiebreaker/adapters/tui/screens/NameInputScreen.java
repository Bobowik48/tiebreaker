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

        a = normalize(a);
        b = normalize(b);
        return new Names(a, b);
    }

    private String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }

    /** Jedno pole tekstowe z walidacją pustego Entera + „AltGr-duble” ogarnięte. */
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
                String cur = buf.toString();
                if (cur.isBlank()) {
                    // pytamy elegancko o domyślną nazwę
                    String who = label.toLowerCase().contains("gracza 1") ? "Player A" : "Player B";
                    boolean ok = view.confirm("Pole puste. Ustawić domyślnie \"" + who + "\"?");
                    if (ok) return who;
                    else continue; // wróć do edycji
                }
                return cur;
            }
            if (kt == KeyType.Backspace) {
                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                continue;
            }
            if (kt == KeyType.Character) {
                char c = ks.getCharacter();

                // Heurystyka na AltGr: gdy Alt wciśnięty i znak jest ASCII-literą,
                // zwykle to "fałszywy" event poprzedzający właściwy znak diakrytyczny („l” przed „ł”).
                if (ks.isAltDown() && c < 128 && Character.isLetter(c)) {
                    continue;
                }

                // Pozwól na Unicode (w tym ąęćłóśżź), cyfry, spację i typowe znaki w nazwiskach.
                if (!Character.isISOControl(c) &&
                        (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '.' || c == '\'')) {
                    buf.append(c);
                }
            }
        }
    }
}
