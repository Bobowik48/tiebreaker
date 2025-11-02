package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;
import com.hubertbobowik.tiebreaker.domain.Rules;

import static com.hubertbobowik.tiebreaker.domain.Rules.TieBreakMode.*;

public final class RulesScreen {

    public interface Callback {
        void onConfirm(Rules rules);

        void onCancel();
    }

    private final LanternaView view;
    private final Callback cb;

    private int focused = 0;               // 0=BestOf, 1=EverySet, 2=Final
    private int bestOf = 3;
    private Rules.TieBreakMode every = CLASSIC7;
    private Rules.TieBreakMode fin = CLASSIC7;

    public RulesScreen(LanternaView view, Callback cb) {
        this.view = view;
        this.cb = cb;
    }

    public void show() throws Exception {
        redraw();
        while (true) {
            switch (view.readKey()) {
                case UP -> {
                    focused = Math.max(0, focused - 1);
                    redraw();
                }
                case DOWN -> {
                    focused = Math.min(2, focused + 1);
                    redraw();
                }
                case LEFT -> {
                    move(-1);
                    redraw();
                }
                case RIGHT -> {
                    move(1);
                    redraw();
                }
                case ENTER -> {
                    cb.onConfirm(new Rules(bestOf, every, fin));
                    return;
                }
                case ESC -> {
                    cb.onCancel();
                    return;
                }
                case NONE -> {
                }
            }
        }
    }

    private void move(int dir) {
        if (focused == 0) {
            bestOf = (dir > 0) ? 5 : 3;
            return;
        }
        var arr = new Rules.TieBreakMode[]{CLASSIC7, TB10, NONE};
        Rules.TieBreakMode cur = (focused == 1) ? every : fin;
        int i = 0;
        for (int k = 0; k < arr.length; k++)
            if (arr[k] == cur) {
                i = k;
                break;
            }
        int n = Math.floorMod(i + dir, arr.length);
        if (focused == 1) every = arr[n];
        else fin = arr[n];
    }

    private void redraw() throws Exception {
        String desc = switch (focused) {
            case 0 -> dBestOf(bestOf);
            case 1 -> dEvery(every);
            default -> dFinal(fin);
        };

        view.renderRulesPicker(
                focused, bestOf, label(every), label(fin), desc
        );
    }

    private static String label(Rules.TieBreakMode m) {
        return switch (m) {
            case CLASSIC7 -> "TB7";
            case TB10 -> "TB10";
            case NONE -> "NONE";
        };
    }

    private static String dBestOf(int bo) {
        return bo == 5
                ? "Best of 5: mecz wygrywasz po trzech setach. Wersja dłuższa, intensywna."
                : "Best of 3: mecz wygrywasz po dwóch setach. Standard w tourze.";
    }

    private static String dEvery(Rules.TieBreakMode m) {
        return switch (m) {
            case CLASSIC7 -> "Tiebreak do 7 punktów w każdym secie, przewaga dwóch punktów obowiązuje.";
            case TB10 -> "Super tiebreak do 10 punktów zamiast decydującego gema w secie.";
            case NONE -> "Bez tiebreaka w setach podstawowych. Grasz do przewagi dwóch gemów.";
        };
    }

    private static String dFinal(Rules.TieBreakMode m) {
        return switch (m) {
            case CLASSIC7 -> "Finałowy set kończy tiebreak do 7.";
            case TB10 -> "Finałowy set kończy super tiebreak do 10.";
            case NONE -> "Finałowy set bez tiebreaka. Do przewagi dwóch gemów.";
        };
    }
}
