package com.hubertbobowik.tiebreaker.adapters.tui;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.Tournament;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Graficzny widok oparty na Swingu.
 * Ma ten sam "kontrakt" co LanternaView, żeby wszystkie *Screen działały bez zmian.
 */
public final class SwingView extends LanternaView {

    private JFrame frame;
    private JPanel content;
    private JLabel titleLabel;
    private JLabel namesLabel;
    private JLabel scoreLabel;
    private JLabel elapsedLabel;
    private JLabel serverLabel;
    private JLabel helperLabel;

    private final BlockingQueue<UserIntent> intentQueue = new ArrayBlockingQueue<>(32);
    private final BlockingQueue<KeyStroke> rawQueue = new ArrayBlockingQueue<>(64);

    @Override
    public void open() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame("Tiebreaker");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.setSize(1000, 700);
                frame.setLocationRelativeTo(null);
                frame.setLayout(new BorderLayout());

                content = new JPanel();
                content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
                frame.setContentPane(new JScrollPane(content));

                installKeyHandling();
                frame.setVisible(true);
                frame.setFocusable(true);
                frame.requestFocusInWindow();
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize UI", e);
        }
    }

    @Override
    public void checkResizeAndRedraw(Match m) {
        SwingUtilities.invokeLater(() -> {
            if (content != null) {
                content.revalidate();
                content.repaint();
            }
        });
    }

    @Override
    public void fullClear() {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.revalidate();
            content.repaint();
        });
        intentQueue.clear();
        rawQueue.clear();
    }

    // ---- EKRAN MECZU -------------------------------------------------

    @Override
    public void renderStatic(Match m) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            titleLabel = label("TIEBREAKER — TRYB GRAFICZNY", 18, Font.BOLD);
            namesLabel = label(m.playerA() + "   vs   " + m.playerB(), 16, Font.PLAIN);
            scoreLabel = label("Wynik:", 16, Font.BOLD);
            elapsedLabel = label("", 14, Font.PLAIN);
            serverLabel = label("", 14, Font.PLAIN);
            helperLabel = label(
                    "[A] punkt dla " + firstNameOf(m.playerA()) +
                            "   [B] punkt dla " + firstNameOf(m.playerB()) +
                            "   [U] cofnij   [R] przywróć   [X] zakończ   [Q] wyjście",
                    12, Font.PLAIN
            );

            content.add(padded(titleLabel));
            content.add(padded(namesLabel));
            content.add(padded(scoreLabel));
            content.add(padded(elapsedLabel));
            content.add(padded(serverLabel));
            content.add(padded(helperLabel));

            // brak przycisków – sterowanie dokładnie jak w TUI: klawiaturą

            content.revalidate();
            content.repaint();
        });
    }

    @Override
    public void renderScoreLine(String line) {
        SwingUtilities.invokeLater(() -> {
            if (scoreLabel != null) {
                scoreLabel.setText("Wynik: " + line);
            }
        });
    }

    @Override
    public void renderElapsed(String text) {
        SwingUtilities.invokeLater(() -> {
            if (elapsedLabel != null) {
                elapsedLabel.setText(text);
            }
        });
    }

    @Override
    public void renderServer(String line) {
        SwingUtilities.invokeLater(() -> {
            if (serverLabel != null) {
                serverLabel.setText(line);
            }
        });
    }

    @Override
    public void renderWinnerPanel(String winner) {
        SwingUtilities.invokeLater(() -> {
            if (helperLabel != null) {
                helperLabel.setText("[H] historia    [Q] wyjście");
            }
            if (serverLabel != null) {
                serverLabel.setText("");
            }
            if (elapsedLabel != null) {
                elapsedLabel.setText("");
            }
            JLabel win = label("Zwycięzca: " + winner, 16, Font.BOLD);
            content.add(padded(win));
            content.revalidate();
            content.repaint();
        });
    }

    @Override
    public void renderWinnerPanelTournament(String winner) {
        // ma się zachowywać jak Lanterna: pasek zwycięzcy + [B] drabinka / [Q] wyjście
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            JLabel winnerLabel = label("Zwycięzca: " + winner, 16, Font.BOLD);
            JLabel helper = label("[B] drabinka    [Q] wyjście", 12, Font.PLAIN);

            content.add(padded(winnerLabel));
            content.add(padded(helper));

            content.revalidate();
            content.repaint();
        });
    }

    // ---- MENU / LISTY -----------------------------------------------

    @Override
    public void renderSimpleMenu(String title, String[] items, int selected) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.add(padded(label(title, 18, Font.BOLD)));
            JList<String> list = new JList<>(items);
            list.setSelectedIndex(selected);
            list.setFocusable(false);
            content.add(padded(list));
            content.add(padded(label("[↑/↓] wybór   [Enter] zatwierdź   [Esc] wstecz", 12, Font.PLAIN)));
            content.revalidate();
            content.repaint();
        });
    }

    @Override
    public void renderMatchesList(String title, java.util.List<String> lines, int selected) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.add(padded(label(title, 18, Font.BOLD)));
            JList<String> list = new JList<>(lines.toArray(String[]::new));
            list.setSelectedIndex(selected);
            list.setFocusable(false);
            content.add(padded(list));
            content.add(padded(label("[↑/↓] wybór   [Esc] wstecz", 12, Font.PLAIN)));
            content.revalidate();
            content.repaint();
        });
    }

    @Override
    public void renderRulesPicker(
            int focusedSection, int bestOf,
            String everySetLabel, String finalSetLabel,
            String activeDescription
    ) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.add(padded(label("Wybór zasad", 18, Font.BOLD)));

            content.add(padded(label(
                    (focusedSection == 0 ? "› " : "  ") + "Best of: " + bestOf,
                    14, focusedSection == 0 ? Font.BOLD : Font.PLAIN)));

            content.add(padded(label(
                    (focusedSection == 1 ? "› " : "  ") + "Tiebreak w setach: " + everySetLabel,
                    14, focusedSection == 1 ? Font.BOLD : Font.PLAIN)));

            content.add(padded(label(
                    (focusedSection == 2 ? "› " : "  ") + "Finałowy set: " + finalSetLabel,
                    14, focusedSection == 2 ? Font.BOLD : Font.PLAIN)));

            content.add(padded(label("Opis:", 14, Font.BOLD)));
            JTextArea desc = new JTextArea(activeDescription);
            desc.setLineWrap(true);
            desc.setWrapStyleWord(true);
            desc.setEditable(false);
            desc.setBackground(content.getBackground());
            content.add(padded(desc));

            content.add(padded(label(
                    "[↑/↓] sekcja   [←/→] wybór   [R] domyślne   [Enter] start   [Esc] wstecz",
                    12, Font.PLAIN)));

            content.revalidate();
            content.repaint();
        });
    }

    // ---- FORMULARZ IMIEN --------------------------------------------

    @Override
    public void renderInputForm(String title, String fieldLabel, String value) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.add(padded(label(title, 18, Font.BOLD)));
            content.add(padded(label(fieldLabel, 14, Font.PLAIN)));

            JTextField field = new JTextField(value, 24);
            field.setEditable(false); // input idzie przez KeyStroke
            field.setFocusable(false);
            content.add(padded(field));

            content.add(padded(label(
                    "[Enter] zatwierdź   [Backspace] usuń   [Esc] wstecz",
                    12, Font.PLAIN)));

            content.revalidate();
            content.repaint();

            frame.requestFocusInWindow();
        });
    }

    // ---- DRABINKA ---------------------------------------------------

    @Override
    public void renderBracket(Tournament tournament) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.add(padded(label("DRABINKA TURNIEJU", 18, Font.BOLD)));

            StringBuilder sb = new StringBuilder();

            var rounds = tournament.rounds();
            for (int r = 0; r < rounds.size(); r++) {
                var round = rounds.get(r);
                sb.append("Runda ").append(r + 1).append('\n');

                for (int i = 0; i < round.size(); i++) {
                    var p = round.pairs().get(i);

                    String a = (p.a() == null || p.a().isBlank()) ? "—" : p.a();
                    String b = (p.b() == null || p.b().isBlank()) ? "—" : p.b();

                    sb.append("  ").append(a).append("  vs  ").append(b);

                    if (p.isDone()) {
                        String winnerName;
                        if (p.winner() == null) {
                            winnerName = "—";
                        } else if (p.winner() == 0) {
                            winnerName = a;
                        } else {
                            winnerName = b;
                        }
                        sb.append("    [zwycięzca: ").append(winnerName).append("]");
                    }
                    sb.append('\n');
                }
                sb.append('\n');
            }

            JTextArea area = new JTextArea(sb.toString());
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            area.setEditable(false);
            area.setLineWrap(false);
            area.setWrapStyleWord(true);
            area.setBackground(content.getBackground());

            content.add(padded(area));
            content.add(padded(label("[Esc] powrót", 12, Font.PLAIN)));

            content.revalidate();
            content.repaint();
        });
    }

    // ---- DIALOG POTWIERDZENIA ---------------------------------------

    @Override
    public boolean confirm(String question) {
        final boolean[] result = {false};
        try {
            SwingUtilities.invokeAndWait(() -> {
                JOptionPane optionPane = new JOptionPane(
                        question,
                        JOptionPane.QUESTION_MESSAGE,
                        JOptionPane.YES_NO_OPTION
                );
                // bez wymuszonego preferredSize – pack() dobierze wielkość do tekstu
                JDialog dialog = optionPane.createDialog(frame, "Potwierdź");
                dialog.setResizable(false);
                dialog.pack();
                dialog.setLocationRelativeTo(frame);
                dialog.setVisible(true);

                Object value = optionPane.getValue();
                int res = (value instanceof Integer) ? (Integer) value : JOptionPane.NO_OPTION;
                result[0] = (res == JOptionPane.YES_OPTION);
            });
        } catch (Exception e) {
            return false;
        }
        return result[0];
    }

    // ---- WEJŚCIE ----------------------------------------------------

    @Override
    public UserIntent readIntent() {
        try {
            UserIntent intent = intentQueue.poll(16, TimeUnit.MILLISECONDS);
            return intent != null ? intent : UserIntent.NONE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return UserIntent.NONE;
        }
    }

    @Override
    public KeyStroke readRaw() {
        try {
            return rawQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new KeyStroke(KeyType.EOF);
        }
    }

    @Override
    public NavKey readKey() {
        try {
            KeyStroke ks = rawQueue.take();
            if (ks == null) {
                return NavKey.NONE;
            }
            return switch (ks.getKeyType()) {
                case ArrowUp -> NavKey.UP;
                case ArrowDown -> NavKey.DOWN;
                case ArrowLeft -> NavKey.LEFT;
                case ArrowRight -> NavKey.RIGHT;
                case Enter -> NavKey.ENTER;
                case Escape -> NavKey.ESC;
                default -> NavKey.NONE;
            };
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NavKey.NONE;
        }
    }

    // ---- KEY LISTENER -----------------------------------------------

    private void installKeyHandling() {
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                KeyStroke ks = null;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> ks = new KeyStroke(KeyType.ArrowUp);
                    case KeyEvent.VK_DOWN -> ks = new KeyStroke(KeyType.ArrowDown);
                    case KeyEvent.VK_LEFT -> ks = new KeyStroke(KeyType.ArrowLeft);
                    case KeyEvent.VK_RIGHT -> ks = new KeyStroke(KeyType.ArrowRight);
                    case KeyEvent.VK_ENTER -> ks = new KeyStroke(KeyType.Enter);
                    case KeyEvent.VK_ESCAPE -> ks = new KeyStroke(KeyType.Escape);
                    case KeyEvent.VK_BACK_SPACE -> ks = new KeyStroke(KeyType.Backspace);
                    default -> {
                    }
                }

                if (ks != null) {
                    rawQueue.offer(ks);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                char upper = Character.toUpperCase(c);

                // mapowanie liter na UserIntent – jak w TUI
                switch (upper) {
                    case 'A' -> intentQueue.offer(UserIntent.POINT_A);
                    case 'B' -> intentQueue.offer(UserIntent.POINT_B);
                    case 'U' -> intentQueue.offer(UserIntent.UNDO);
                    case 'R' -> intentQueue.offer(UserIntent.REDO);
                    case 'X' -> intentQueue.offer(UserIntent.FINISH);
                    case 'Q' -> intentQueue.offer(UserIntent.QUIT);
                    default -> {
                    }
                }

                if (!Character.isISOControl(c)) {
                    rawQueue.offer(new KeyStroke(c, false, e.isAltDown()));
                }
            }
        });
    }

    // ---- MAŁE POMOCNICZE --------------------------------------------

    private JPanel padded(JComponent comp) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }

    private JLabel label(String text, int size, int style) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(style, (float) size));
        return lbl;
    }

    private static String firstNameOf(String full) {
        if (full == null || full.isBlank()) return "A";
        String s = full.trim();
        int sp = s.indexOf(' ');
        return sp > 0 ? s.substring(0, sp) : s;
    }

    @Override
    public void close() {
        if (frame != null) {
            frame.dispose();
        }
    }
}
