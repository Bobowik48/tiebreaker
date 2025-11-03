// File: src/main/java/com/hubertbobowik/tiebreaker/adapters/tui/screens/TournamentSetupScreen.java
package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.TournamentService;
import com.hubertbobowik.tiebreaker.domain.Rules;
import com.hubertbobowik.tiebreaker.domain.Tournament;
import com.hubertbobowik.tiebreaker.domain.TournamentId;

import java.util.ArrayList;
import java.util.List;

public final class TournamentSetupScreen {

    public record Result(TournamentId id, boolean cancelled) {
        public static final Result CANCELLED = new Result(null, true);

        public boolean isOk() {
            return !cancelled && id != null;
        }
    }

    private final LanternaView view;
    private final TournamentService tournamentService;

    public TournamentSetupScreen(LanternaView view, TournamentService tournamentService) {
        this.view = view;
        this.tournamentService = tournamentService;
    }

    public Result show() throws Exception {
        int[] sizes = new int[]{4, 8, 16, 32};
        int idx = 0;
        int cursor = 0; // 0 = size, 1 = rules, 2 = confirm, 3 = back

        // Presety zasad do szybkiego przełączania strzałkami
        Rules[] presets = new Rules[]{
                new Rules(3, Rules.TieBreakMode.CLASSIC7, Rules.TieBreakMode.CLASSIC7), // BO3, TB7 wszędzie
                new Rules(5, Rules.TieBreakMode.CLASSIC7, Rules.TieBreakMode.CLASSIC7), // BO5, TB7 wszędzie
                new Rules(3, Rules.TieBreakMode.CLASSIC7, Rules.TieBreakMode.NONE),     // BO3, TB7 w setach, brak TB w finale
                new Rules(3, Rules.TieBreakMode.NONE, Rules.TieBreakMode.TB10),         // BO3, brak TB w setach, superTB10 w finale
        };
        int rulesIdx = 0;

        Rules pickedRules = presets[rulesIdx];

        while (true) {
            String[] options = new String[]{
                    "Rozmiar drabinki: " + sizes[idx],
                    "Zasady: " + pickedRules.label(),
                    "Zatwierdź (Enter)",
                    "Wstecz (Esc)"
            };

            view.renderSimpleMenu(
                    "NOWY TURNIEJ — ROZMIAR",
                    options,
                    cursor
            );

            KeyStroke key = view.readRaw();
            if (key == null) {
                continue;
            }

            switch (key.getKeyType()) {
                case ArrowUp -> cursor = (cursor + 3) % 4;
                case ArrowDown -> cursor = (cursor + 1) % 4;

                case ArrowLeft -> {
                    if (cursor == 0) {
                        idx = (idx + sizes.length - 1) % sizes.length;
                    } else if (cursor == 1) {
                        rulesIdx = (rulesIdx + presets.length - 1) % presets.length;
                        pickedRules = presets[rulesIdx];
                    }
                }
                case ArrowRight -> {
                    if (cursor == 0) {
                        idx = (idx + 1) % sizes.length;
                    } else if (cursor == 1) {
                        rulesIdx = (rulesIdx + 1) % presets.length;
                        pickedRules = presets[rulesIdx];
                    }
                }

                case Enter -> {
                    if (cursor == 0) {
                        // nic – rozmiar zmieniamy strzałkami
                    } else if (cursor == 1) {
                        // Szczegółowa edycja zasad na osobnym ekranie
                        final java.util.concurrent.atomic.AtomicBoolean confirmed =
                                new java.util.concurrent.atomic.AtomicBoolean(false);
                        final java.util.concurrent.atomic.AtomicReference<Rules> tempRules =
                                new java.util.concurrent.atomic.AtomicReference<>(pickedRules);

                        RulesScreen rs = new RulesScreen(view, new RulesScreen.Callback() {
                            @Override
                            public void onConfirm(Rules rules) {
                                confirmed.set(true);
                                tempRules.set(Rules.copy(rules));
                            }

                            @Override
                            public void onCancel() { }
                        });

                        rs.show();

                        if (confirmed.get()) {
                            pickedRules = tempRules.get();
                            rulesIdx = nearestPresetIndex(presets, pickedRules);
                        }
                    } else if (cursor == 2) {
                        int size = sizes[idx];

                        List<String> names = inputNames(size);
                        if (names == null) {
                            return Result.CANCELLED;
                        }

                        // Stwórz turniej z wybranymi zasadami
                        Tournament t = tournamentService.create(size, names, pickedRules);

                        while (true) {
                            Tournament maybe = previewAndControls(t);
                            if (maybe == null) {
                                return Result.CANCELLED;
                            }
                            t = maybe;
                            t.confirmSeeding();
                            tournamentService.save(t);
                            return new Result(t.id(), false);
                        }
                    } else if (cursor == 3) {
                        return Result.CANCELLED;
                    }
                }

                case Escape -> {
                    return Result.CANCELLED;
                }
            }
        }
    }

    private int nearestPresetIndex(Rules[] presets, Rules picked) {
        for (int i = 0; i < presets.length; i++) {
            Rules r = presets[i];
            if (r.bestOf() == picked.bestOf()
                    && r.tieBreakEverySet() == picked.tieBreakEverySet()
                    && r.finalSetMode() == picked.finalSetMode()) {
                return i;
            }
        }
        return 0;
    }

    private List<String> inputNames(int size) throws Exception {
        List<String> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String label = "Gracz " + (i + 1) + " — Imię i nazwisko:";
            String name = inputOne(label, "");
            if (name == null) {
                boolean sure = view.confirm("Przerwać wprowadzanie uczestników?");
                if (sure) return null;
                i--;
                continue;
            }
            name = normalize(name);
            if (name.isBlank()) {
                boolean auto = view.confirm("Nie wpisano imienia. Ustawić domyślnie 'Player " + (i + 1) + "'?");
                if (auto) name = "Player " + (i + 1);
                else {
                    i--;
                    continue;
                }
            }
            out.add(name);
        }
        return out;
    }

    private Tournament previewAndControls(Tournament t) throws Exception {
        while (true) {
            // render
            List<String> lines = new ArrayList<>();
            lines.add("DRABINKA (" + t.size() + "):");
            var round0 = t.rounds().get(0);
            for (int i = 0; i < round0.size(); i++) {
                var p = round0.pairs().get(i);
                lines.add("  M" + (i + 1) + ": " + p.a() + " vs " + p.b());
            }
            lines.add("");
            lines.add("[Enter] zatwierdź   [R] wylosuj ponownie   [E] edytuj imiona   [Esc] wstecz");

            view.renderMatchesList("PODGLĄD DRABINKI", lines, 0);

            KeyStroke ks = view.readRaw();
            if (ks == null) continue;

            if (ks.getKeyType() == KeyType.Escape) return null;  // anuluj
            if (ks.getKeyType() == KeyType.Enter) return t;     // potwierdź

            if (ks.getKeyType() == KeyType.Character) {
                char c = Character.toUpperCase(ks.getCharacter());
                if (c == 'R') {
                    t.shuffle();
                    continue; // odśwież render
                }
                if (c == 'E') {
                    List<String> edited = inputNames(t.size());
                    if (edited == null) return null;
                    Tournament tmp = tournamentService.create(t.size(), edited, t.rules());
                    tournamentService.delete(t.id());
                    tournamentService.save(tmp);
                    t = tmp;   // podmień i renderuj od nowa
                    continue;
                }
            }
        }
    }

    private String inputOne(String label, String initial) throws Exception {
        StringBuilder buf = new StringBuilder(initial == null ? "" : initial);
        while (true) {
            view.renderInputForm("UCZESTNICY TURNIEJU", label, buf.toString());
            KeyStroke ks = view.readRaw();
            if (ks == null) continue;

            KeyType kt = ks.getKeyType();
            if (kt == KeyType.Escape) return null;
            if (kt == KeyType.Enter) return buf.toString();

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

    private String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }
}