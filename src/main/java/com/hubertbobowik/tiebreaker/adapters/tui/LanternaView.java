package com.hubertbobowik.tiebreaker.adapters.tui;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.hubertbobowik.tiebreaker.domain.Match;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Swing-based replacement for the previous Lanterna view. It keeps the same API so the
 * surrounding application logic stays untouched, but renders everything on a single
 * JFrame with basic controls (buttons + keyboard shortcuts).
 */
public final class LanternaView implements AutoCloseable {

    // Intencje podczas meczu (litery)
    public enum UserIntent {POINT_A, POINT_B, UNDO, REDO, FINISH, QUIT, NONE}

    // Strzałki/Enter/Esc – do nawigacji w menu/listach
    public enum NavKey {UP, DOWN, LEFT, RIGHT, ENTER, ESC, NONE}

    private JFrame frame;
    private JPanel content;
    private JLabel titleLabel;
    private JLabel namesLabel;
    private JLabel scoreLabel;
    private JLabel elapsedLabel;
    private JLabel serverLabel;
    private JLabel helperLabel;

    private final BlockingQueue<UserIntent> intentQueue = new ArrayBlockingQueue<>(32);
    private final BlockingQueue<NavKey> navQueue = new ArrayBlockingQueue<>(32);
    private final BlockingQueue<KeyStroke> rawQueue = new ArrayBlockingQueue<>(64);

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
                frame.requestFocus();
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize UI", e);
        }
    }

    /**
     * Nieblokujące odświeżenie układu (w trybie Swingowym brak obsługi resize).
     */
    public void checkResizeAndRedraw(Match m) {
        // Swing sam dba o odświeżanie – tutaj tylko pewność, że statyczne elementy istnieją.
        renderStatic(m);
    }

    /**
     * Twarde czyszczenie ekranu (np. przy zmianie ekranu).
     */
    public void fullClear() {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.revalidate();
            content.repaint();
        });
    }

    // ── RYSOWANIE EKRANU MECZU ──────────────────────────────────

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
                    12, Font.PLAIN);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttons.add(actionButton("Punkt A", UserIntent.POINT_A));
            buttons.add(actionButton("Punkt B", UserIntent.POINT_B));
            buttons.add(actionButton("Cofnij", UserIntent.UNDO));
            buttons.add(actionButton("Przywróć", UserIntent.REDO));
            buttons.add(actionButton("Zakończ", UserIntent.FINISH));
            buttons.add(actionButton("Wyjdź", UserIntent.QUIT));

            content.add(padded(titleLabel));
            content.add(padded(namesLabel));
            content.add(padded(scoreLabel));
            content.add(padded(elapsedLabel));
            content.add(padded(serverLabel));
            content.add(padded(helperLabel));
            content.add(padded(buttons));

            content.revalidate();
            content.repaint();
        });
    }

    /**
     * Aktualizuje wyłącznie linię wyniku (bez migotania).
     */
    public void renderScoreLine(String line) {
        SwingUtilities.invokeLater(() -> {
            if (scoreLabel != null) {
                scoreLabel.setText("Wynik: " + line);
            }
        });
    }

    public void renderElapsed(String text) {
        SwingUtilities.invokeLater(() -> {
            if (elapsedLabel != null) {
                elapsedLabel.setText(text);
            }
        });
    }

    /**
     * Pasek zwycięzcy po zakończeniu meczu.
     */
    public void renderWinnerPanel(String winner) {
        SwingUtilities.invokeLater(() -> {
            helperLabel.setText("[H] historia    [Q] wyjście");
            serverLabel.setText("");
            elapsedLabel.setText("");
            JLabel win = label("Zwycięzca: " + winner, 16, Font.BOLD);
            content.add(padded(win));
            content.revalidate();
            content.repaint();
        });
    }

    // ── PRYMITYWY: MENU / LISTY ─────────────────────────────────

    public void renderSimpleMenu(String title, String[] items, int selected) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.add(padded(label(title, 18, Font.BOLD)));
            var list = new JList<>(items);
            list.setSelectedIndex(selected);
            list.setFocusable(false);
            content.add(padded(list));
            content.add(padded(label("[↑/↓] wybór   [Enter] zatwierdź   [Esc] wstecz", 12, Font.PLAIN)));
            content.revalidate();
            content.repaint();
        });
    }

    public void renderMatchesList(String title, java.util.List<String> lines, int selected) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.add(padded(label(title, 18, Font.BOLD)));
            var list = new JList<>(lines.toArray(String[]::new));
            list.setSelectedIndex(selected);
            list.setFocusable(false);
            content.add(padded(list));
            content.add(padded(label("[↑/↓] wybór   [Esc] wstecz", 12, Font.PLAIN)));
            content.revalidate();
            content.repaint();
        });
    }

    public void renderRulesPicker(
            int focusedSection, int bestOf,
            String everySetLabel, String finalSetLabel,
            String activeDescription
    ) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.add(padded(label("Wybór zasad", 18, Font.BOLD)));
            content.add(padded(label((focusedSection == 0 ? "› " : "  ") + "Best of: " + bestOf, 14, focusedSection == 0 ? Font.BOLD : Font.PLAIN)));
            content.add(padded(label((focusedSection == 1 ? "› " : "  ") + "Tiebreak w setach: " + everySetLabel, 14, focusedSection == 1 ? Font.BOLD : Font.PLAIN)));
            content.add(padded(label((focusedSection == 2 ? "› " : "  ") + "Finałowy set: " + finalSetLabel, 14, focusedSection == 2 ? Font.BOLD : Font.PLAIN)));
            content.add(padded(label("Opis:", 14, Font.BOLD)));
            JTextArea desc = new JTextArea(activeDescription);
            desc.setLineWrap(true);
            desc.setWrapStyleWord(true);
            desc.setEditable(false);
            desc.setBackground(content.getBackground());
            content.add(padded(desc));
            content.add(padded(label("[↑/↓] sekcja   [←/→] wybór   [R] domyślne   [Enter] start   [Esc] wstecz", 12, Font.PLAIN)));
            content.revalidate();
            content.repaint();
        });
    }

    /**
     * Prosty dialog: Enter = TAK, Esc = NIE.
     */
    public boolean confirm(String question) {
        final boolean[] result = {false};
        try {
            SwingUtilities.invokeAndWait(() -> {
                int res = JOptionPane.showConfirmDialog(frame, question, "Potwierdź", JOptionPane.YES_NO_OPTION);
                result[0] = (res == JOptionPane.YES_OPTION);
            });
        } catch (Exception e) {
            return false;
        }
        return result[0];
    }

    // ── WEJŚCIE KLAWIATUROWE ────────────────────────────────────

    /**
     * Nieblokujący odczyt liter dla meczu.
     */
    public UserIntent readIntent() {
        try {
            UserIntent intent = intentQueue.poll(16, TimeUnit.MILLISECONDS);
            return intent != null ? intent : UserIntent.NONE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return UserIntent.NONE;
        }
    }

    public KeyStroke readRaw() {
        try {
            return rawQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new KeyStroke(KeyType.EOF);
        }
    }

    /**
     * Blokujący odczyt nawigacji (menu/listy).
     */
    public NavKey readKey() {
        try {
            return navQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NavKey.NONE;
        }
    }

    public void renderInputForm(String title, String fieldLabel, String value) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();
            content.add(padded(label(title, 18, Font.BOLD)));
            content.add(padded(label(fieldLabel, 14, Font.PLAIN)));
            JTextField field = new JTextField(value, Math.max(20, value.length()));
            field.setEditable(false);
            content.add(padded(field));
            content.add(padded(label("[Enter] zatwierdź   [Backspace] usuń   [Esc] wstecz", 12, Font.PLAIN)));
            content.revalidate();
            content.repaint();
        });
    }

    public void renderServer(String line) {
        SwingUtilities.invokeLater(() -> {
            if (serverLabel != null) {
                serverLabel.setText(line);
            }
        });
    }

    private void installKeyHandling() {
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                NavKey nav = switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> NavKey.UP;
                    case KeyEvent.VK_DOWN -> NavKey.DOWN;
                    case KeyEvent.VK_LEFT -> NavKey.LEFT;
                    case KeyEvent.VK_RIGHT -> NavKey.RIGHT;
                    case KeyEvent.VK_ENTER -> NavKey.ENTER;
                    case KeyEvent.VK_ESCAPE -> NavKey.ESC;
                    default -> NavKey.NONE;
                };
                if (nav != NavKey.NONE) {
                    navQueue.offer(nav);
                    KeyType type = switch (nav) {
                        case UP -> KeyType.ArrowUp;
                        case DOWN -> KeyType.ArrowDown;
                        case LEFT -> KeyType.ArrowLeft;
                        case RIGHT -> KeyType.ArrowRight;
                        case ENTER -> KeyType.Enter;
                        case ESC -> KeyType.Escape;
                        default -> KeyType.Unknown;
                    };
                    rawQueue.offer(new KeyStroke(type));
                }

                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    rawQueue.offer(new KeyStroke(KeyType.Backspace));
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                char upper = Character.toUpperCase(c);
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

    private JButton actionButton(String text, UserIntent intent) {
        JButton btn = new JButton(text);
        btn.addActionListener(e -> intentQueue.offer(intent));
        return btn;
    }

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
