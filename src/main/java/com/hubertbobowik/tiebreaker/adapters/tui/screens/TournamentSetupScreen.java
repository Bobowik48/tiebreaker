package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.application.TournamentService;
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

        while (true) {
            view.renderSimpleMenu(
                    "NOWY TURNIEJ — ROZMIAR",
                    new String[]{
                            "Rozmiar drabinki: " + sizes[idx],
                            "Zatwierdź (Enter)",
                            "Wstecz (Esc)"
                    },
                    0
            );

            var key = view.readKey();
            switch (key) {
                case UP -> idx = (idx + sizes.length - 1) % sizes.length;
                case DOWN -> idx = (idx + 1) % sizes.length;
                case ENTER -> {
                    int size = sizes[idx];

                    // 2) imiona
                    List<String> names = inputNames(size);
                    if (names == null) return Result.CANCELLED;

                    // 3) utworzenie turnieju + podgląd/losowanie/zatwierdzenie
                    Tournament t = tournamentService.create(size, names);
                    while (true) {
                        Tournament maybe = previewAndControls(t);
                        if (maybe == null) return Result.CANCELLED;
                        t = maybe; // tylko po Enter
                        t.confirmSeeding();
                        tournamentService.save(t);
                        return new Result(t.id(), false);
                    }
                }
                case ESC -> {
                    return Result.CANCELLED;
                }
                default -> { /* ignore */ }
            }
        }
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
                    continue; // PRZEŁÓŻ render jeszcze raz, bez finalizacji
                }
                if (c == 'E') {
                    List<String> edited = inputNames(t.size());
                    if (edited == null) return null;
                    Tournament tmp = tournamentService.create(t.size(), edited);
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
