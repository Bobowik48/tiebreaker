package com.hubertbobowik.tiebreaker.adapters.tui.screens;

import com.hubertbobowik.tiebreaker.adapters.tui.LanternaView;

import java.io.IOException;
import java.util.Random;

public final class ServeSettingsScreen {
    private final LanternaView view;
    private final String nameA;
    private final String nameB;

    public ServeSettingsScreen(LanternaView view, String nameA, String nameB) {
        this.view = view;
        this.nameA = nameA;
        this.nameB = nameB;
    }

    /**
     * @return 0 (A) lub 1 (B), null gdy Esc
     */
    public Integer ask() throws Exception {
        int idx = 0; // 0=A,1=B
        while (true) {
            render(idx);
            switch (view.readKey()) {
                case LEFT -> idx = (idx + 1) % 2;
                case RIGHT -> idx = (idx + 1) % 2;
                case UP, DOWN -> idx ^= 1;
                case ENTER -> {
                    return idx;
                }
                case ESC -> {
                    return null;
                }
                default -> {
                }
            }
        }
    }

    private void render(int idx) throws IOException {
        view.fullClear();
        var g = nameA + " (A)";
        var h = nameB + " (B)";
        view.renderSimpleMenu("USTAWIENIA SERWISU", new String[]{
                "Zaczyna: " + (idx == 0 ? "► " + g : g),
                "Zaczyna: " + (idx == 1 ? "► " + h : h),
                "[T] Rzut monetą"
        }, 0);
        // Podpowiedzi:
        // Uwaga: używamy readRaw aby obsłużyć T
    }

    // W MainTui obsłużemy 'T' → losowanie
}
