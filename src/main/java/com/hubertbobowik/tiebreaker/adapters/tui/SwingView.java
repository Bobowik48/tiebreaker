package com.hubertbobowik.tiebreaker.adapters.tui;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.hubertbobowik.tiebreaker.domain.Match;
import com.hubertbobowik.tiebreaker.domain.Tournament;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Graficzny widok oparty na Swingu.
 * Ma ten sam kontrakt co LanternaView, żeby wszystkie *Screen działały bez zmian.
 */
public final class SwingView extends LanternaView {

    // kolory
    private static final Color BG_COLOR = new Color(0x070B18);
    private static final Color SURFACE_COLOR = new Color(0x111827);
    private static final Color CARD_COLOR = new Color(0x1F2937);
    private static final Color ACCENT_COLOR = new Color(0x22C55E);
    private static final Color ACCENT_SOFT = new Color(0x38BDF8);
    private static final Color TEXT_PRIMARY = new Color(0xE5E7EB);
    private static final Color TEXT_SECONDARY = new Color(0x9CA3AF);

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

    // ---------------------------------------------------------------------
    //  lifecycle
    // ---------------------------------------------------------------------

    @Override
    public void open() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                UIManager.put("Panel.background", BG_COLOR);
                UIManager.put("OptionPane.background", SURFACE_COLOR);
                UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
                UIManager.put("OptionPane.buttonFont", new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                UIManager.put("List.background", CARD_COLOR);
                UIManager.put("List.foreground", TEXT_PRIMARY);
                UIManager.put("List.selectionBackground", ACCENT_COLOR);
                UIManager.put("List.selectionForeground", Color.BLACK);

                frame = new JFrame("Tiebreaker");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.setSize(1100, 720);
                frame.setLocationRelativeTo(null);
                frame.setLayout(new BorderLayout());
                frame.getContentPane().setBackground(BG_COLOR);

                content = new JPanel();
                content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
                content.setBackground(BG_COLOR);
                content.setFocusable(false);

                JScrollPane scroll = new JScrollPane(content);
                scroll.setBorder(null);
                scroll.getViewport().setBackground(BG_COLOR);
                scroll.setFocusable(false);

                frame.setContentPane(scroll);

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
            if (content != null) {
                content.removeAll();
                content.revalidate();
                content.repaint();
            }
        });
        intentQueue.clear();
        navQueue.clear();
        rawQueue.clear();
    }

    // ---------------------------------------------------------------------
    //  ekran meczu
    // ---------------------------------------------------------------------

    @Override
    public void renderStatic(Match m) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            titleLabel = label("TIEBREAKER — TRYB GRAFICZNY", 22, Font.BOLD);
            titleLabel.setForeground(ACCENT_SOFT);

            namesLabel = label(m.playerA() + "   vs   " + m.playerB(), 18, Font.PLAIN);

            scoreLabel = label("Wynik:", 18, Font.BOLD);
            elapsedLabel = label("", 14, Font.PLAIN);
            serverLabel = label("", 14, Font.PLAIN);

            helperLabel = label(
                    "[A] punkt dla " + firstNameOf(m.playerA()) +
                            "   [B] punkt dla " + firstNameOf(m.playerB()) +
                            "   [U] cofnij   [R] przywróć   [X] zakończ   [Q] wyjście",
                    12, Font.PLAIN
            );
            helperLabel.setForeground(TEXT_SECONDARY);

            JPanel headerCard = cardPanel();
            headerCard.setLayout(new BoxLayout(headerCard, BoxLayout.Y_AXIS));
            headerCard.add(titleLabel);
            headerCard.add(Box.createVerticalStrut(12));
            headerCard.add(namesLabel);
            headerCard.add(Box.createVerticalStrut(18));
            headerCard.add(scoreLabel);
            headerCard.add(Box.createVerticalStrut(6));
            headerCard.add(elapsedLabel);
            headerCard.add(Box.createVerticalStrut(6));
            headerCard.add(serverLabel);
            headerCard.add(Box.createVerticalStrut(18));
            headerCard.add(helperLabel);

            content.add(wrap(headerCard));

            content.add(Box.createVerticalGlue());
            content.revalidate();
            content.repaint();
            frame.requestFocusInWindow();
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
                elapsedLabel.setText("Czas meczu: " + text);
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

            JLabel win = label("Zwycięzca: " + winner, 18, Font.BOLD);
            win.setForeground(ACCENT_COLOR);

            JPanel card = cardPanel();
            card.add(win);

            content.add(Box.createVerticalStrut(24));
            content.add(wrap(card));
            content.revalidate();
            content.repaint();
            frame.requestFocusInWindow();
        });
    }

    @Override
    public void renderWinnerPanelTournament(String winner) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            JLabel title = label("TURNIEJ ZAKOŃCZONY", 22, Font.BOLD);
            title.setForeground(ACCENT_SOFT);
            JLabel win = label("Zwycięzca turnieju: " + winner, 18, Font.BOLD);
            win.setForeground(ACCENT_COLOR);
            JLabel helper = label("[B] drabinka    [Q] wyjście   [Esc] wstecz", 12, Font.PLAIN);
            helper.setForeground(TEXT_SECONDARY);

            JPanel card = cardPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.add(title);
            card.add(Box.createVerticalStrut(16));
            card.add(win);
            card.add(Box.createVerticalStrut(12));
            card.add(helper);

            content.add(wrap(card));
            content.revalidate();
            content.repaint();
            frame.requestFocusInWindow();
        });
    }

    // ---------------------------------------------------------------------
    //  menu / listy
    // ---------------------------------------------------------------------

    @Override
    public void renderSimpleMenu(String title, String[] items, int selected) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            JLabel t = label(title, 22, Font.BOLD);
            t.setForeground(ACCENT_SOFT);

            JList<String> list = new JList<>(items);
            list.setSelectedIndex(selected);
            list.setFocusable(false);
            list.setBackground(CARD_COLOR);
            list.setForeground(TEXT_PRIMARY);
            list.setSelectionBackground(ACCENT_COLOR);
            list.setSelectionForeground(Color.BLACK);
            list.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            JLabel helper = label(
                    "[↑/↓] wybór   [Enter] zatwierdź   [Esc] wstecz",
                    12, Font.PLAIN
            );
            helper.setForeground(TEXT_SECONDARY);

            JPanel card = cardPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.add(t);
            card.add(Box.createVerticalStrut(12));
            card.add(list);
            card.add(Box.createVerticalStrut(8));
            card.add(helper);

            content.add(wrap(card));
            content.revalidate();
            content.repaint();
            frame.requestFocusInWindow();
        });
    }

    @Override
    public void renderMatchesList(String title, java.util.List<String> lines, int selected) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            JLabel t = label(title, 22, Font.BOLD);
            t.setForeground(ACCENT_SOFT);

            JList<String> list = new JList<>(lines.toArray(String[]::new));
            list.setSelectedIndex(selected);
            list.setFocusable(false);
            list.setBackground(CARD_COLOR);
            list.setForeground(TEXT_PRIMARY);
            list.setSelectionBackground(ACCENT_COLOR);
            list.setSelectionForeground(Color.BLACK);
            list.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            JLabel helper = label("[↑/↓] wybór   [Esc] wstecz", 12, Font.PLAIN);
            helper.setForeground(TEXT_SECONDARY);

            JPanel card = cardPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.add(t);
            card.add(Box.createVerticalStrut(12));
            card.add(list);
            card.add(Box.createVerticalStrut(8));
            card.add(helper);

            content.add(wrap(card));
            content.revalidate();
            content.repaint();
            frame.requestFocusInWindow();
        });
    }

    @Override
    public void renderRulesPicker(
            int focusedSection,
            int bestOf,
            String everySetLabel,
            String finalSetLabel,
            String activeDescription
    ) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            JLabel header = label("Wybór zasad", 22, Font.BOLD);
            header.setForeground(ACCENT_SOFT);

            JLabel bestOfLabel = label(
                    (focusedSection == 0 ? "› " : "  ") + "Best of: " + bestOf,
                    14,
                    focusedSection == 0 ? Font.BOLD : Font.PLAIN
            );
            JLabel everySet = label(
                    (focusedSection == 1 ? "› " : "  ") + "Tiebreak w setach: " + everySetLabel,
                    14,
                    focusedSection == 1 ? Font.BOLD : Font.PLAIN
            );
            JLabel finalSet = label(
                    (focusedSection == 2 ? "› " : "  ") + "Finałowy set: " + finalSetLabel,
                    14,
                    focusedSection == 2 ? Font.BOLD : Font.PLAIN
            );

            JLabel descTitle = label("Opis:", 14, Font.BOLD);

            JTextArea desc = new JTextArea(activeDescription);
            desc.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            desc.setLineWrap(true);
            desc.setWrapStyleWord(true);
            desc.setEditable(false);
            desc.setFocusable(false); // klawisze idą do frame
            desc.setBackground(CARD_COLOR);
            desc.setForeground(TEXT_PRIMARY);
            desc.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            JLabel helper = label(
                    "[↑/↓] sekcja   [←/→] wybór   [R] domyślne   [Enter] start   [Esc] wstecz",
                    12,
                    Font.PLAIN
            );
            helper.setForeground(TEXT_SECONDARY);

            JPanel card = cardPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setFocusable(false);
            card.add(header);
            card.add(Box.createVerticalStrut(12));
            card.add(bestOfLabel);
            card.add(Box.createVerticalStrut(4));
            card.add(everySet);
            card.add(Box.createVerticalStrut(4));
            card.add(finalSet);
            card.add(Box.createVerticalStrut(12));
            card.add(descTitle);
            card.add(Box.createVerticalStrut(4));
            card.add(desc);
            card.add(Box.createVerticalStrut(8));
            card.add(helper);

            content.add(wrap(card));
            content.revalidate();
            content.repaint();
            frame.requestFocusInWindow();
        });
    }

    // ---------------------------------------------------------------------
    //  formularz imion
    // ---------------------------------------------------------------------

    @Override
    public void renderInputForm(String title, String fieldLabel, String value) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            JLabel header = label(title, 22, Font.BOLD);
            header.setForeground(ACCENT_SOFT);
            JLabel label = this.label(fieldLabel, 14, Font.PLAIN);

            JTextField field = new JTextField(value);
            field.setColumns(18);
            Dimension size = field.getPreferredSize();
            size.width = 280;
            size.height = 30;
            field.setPreferredSize(size);
            field.setMaximumSize(size);
            field.setEditable(false);
            field.setFocusable(false);
            field.setBackground(CARD_COLOR);
            field.setForeground(TEXT_PRIMARY);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT_SOFT, 1, true),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)
            ));

            JPanel fieldRow = new JPanel();
            fieldRow.setLayout(new BoxLayout(fieldRow, BoxLayout.X_AXIS));
            fieldRow.setOpaque(false);
            fieldRow.setFocusable(false);
            fieldRow.add(field);
            fieldRow.add(Box.createHorizontalGlue());

            JLabel helper = this.label(
                    "[Enter] zatwierdź   [Backspace] usuń   [Esc] wstecz",
                    12,
                    Font.PLAIN
            );
            helper.setForeground(TEXT_SECONDARY);

            JPanel card = cardPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.add(header);
            card.add(Box.createVerticalStrut(16));
            card.add(label);
            card.add(Box.createVerticalStrut(8));
            card.add(fieldRow);
            card.add(Box.createVerticalStrut(16));
            card.add(helper);

            content.add(wrap(card));
            content.revalidate();
            content.repaint();
            frame.requestFocusInWindow();
        });
    }

    // ---------------------------------------------------------------------
    //  drabinka turniejowa – kolorowa
    // ---------------------------------------------------------------------

    @Override
    public void renderBracket(Tournament tournament) {
        SwingUtilities.invokeLater(() -> {
            content.removeAll();

            JLabel header = label("Drabinka turnieju", 22, Font.BOLD);
            header.setForeground(ACCENT_SOFT);

            JLabel subtitle = label(
                    "Rozmiar: " + tournament.size() +
                            (tournament.isFinished() ? "  •  TURNIEJ ZAKOŃCZONY" : ""),
                    12,
                    Font.PLAIN
            );
            subtitle.setForeground(TEXT_SECONDARY);

            BracketPanel bracketPanel = new BracketPanel(tournament);
            bracketPanel.setBackground(BG_COLOR);
            bracketPanel.setFocusable(false);

            JScrollPane scroll = new JScrollPane(bracketPanel);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(BG_COLOR);
            scroll.setFocusable(false);

            JLabel helper = label(
                    "[Esc] wstecz   zielona ramka = zwycięzca pary   niebieska  strzałka  = aktualny mecz",
                    11,
                    Font.PLAIN
            );
            helper.setForeground(TEXT_SECONDARY);

            JPanel card = cardPanel();
            card.setLayout(new BorderLayout(0, 12));
            card.setFocusable(false);
            JPanel headerBox = new JPanel();
            headerBox.setLayout(new BoxLayout(headerBox, BoxLayout.Y_AXIS));
            headerBox.setOpaque(false);
            headerBox.setFocusable(false);
            headerBox.add(header);
            headerBox.add(Box.createVerticalStrut(4));
            headerBox.add(subtitle);

            card.add(headerBox, BorderLayout.NORTH);
            card.add(scroll, BorderLayout.CENTER);
            card.add(helper, BorderLayout.SOUTH);

            content.add(wrap(card));
            content.revalidate();
            content.repaint();
            frame.requestFocusInWindow();
        });
    }

    // panel drabinki
    private final class BracketPanel extends JPanel {

        private static final int MARGIN_X = 40;
        private static final int MARGIN_Y = 40;
        private static final int CARD_HEIGHT = 40;
        private static final int CARD_MIN_WIDTH = 150;
        private static final int ROUND_GAP_X = 40;

        private final Tournament tournament;

        private BracketPanel(Tournament tournament) {
            this.tournament = tournament;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            if (tournament == null || tournament.rounds().isEmpty()) {
                return new Dimension(800, 400);
            }

            int roundsCount = tournament.rounds().size();
            int pairsFirstRound = tournament.rounds().get(0).size();

            int width = MARGIN_X * 2 + roundsCount * (CARD_MIN_WIDTH + ROUND_GAP_X);
            int height = MARGIN_Y * 2
                    + Math.max(1, pairsFirstRound) * (CARD_HEIGHT * 2 + 20);

            return new Dimension(width, height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (tournament == null || tournament.rounds().isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            int roundsCount = tournament.rounds().size();
            int usableWidth = width - MARGIN_X * 2;
            int colWidth = usableWidth / Math.max(1, roundsCount);
            int cardWidth = Math.max(CARD_MIN_WIDTH, colWidth - ROUND_GAP_X);

            for (int r = 0; r < roundsCount; r++) {
                var round = tournament.rounds().get(r);
                int pairs = round.size();

                int usableHeight = height - MARGIN_Y * 2;
                int blockHeight = pairs > 0 ? usableHeight / pairs : usableHeight;
                int x = MARGIN_X + r * colWidth;

                g2.setColor(TEXT_SECONDARY);
                g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
                g2.drawString("Runda " + (r + 1), x, MARGIN_Y - 10);

                for (int i = 0; i < pairs; i++) {
                    var p = round.pairs().get(i);

                    int centerY = MARGIN_Y + blockHeight * i + blockHeight / 2;
                    int y = centerY - CARD_HEIGHT;

                    String a = (p.a() == null || p.a().isBlank()) ? "—" : p.a();
                    String b = (p.b() == null || p.b().isBlank()) ? "—" : p.b();

                    boolean isCurrent =
                            (r == tournament.currentRound()
                                    && i == tournament.currentPair()
                                    && !tournament.isFinished());

                    int winnerIdx = -1;
                    if (p.isDone() && p.winner() != null) {
                        int w = p.winner();
                        if (w == 0 || w == 1) {
                            winnerIdx = w;
                        }
                    }

                    g2.setColor(CARD_COLOR);
                    g2.fillRoundRect(x, y, cardWidth, CARD_HEIGHT * 2, 12, 12);

                    if (isCurrent) {
                        g2.setColor(ACCENT_SOFT);
                    } else if (winnerIdx != -1) {
                        g2.setColor(ACCENT_COLOR);
                    } else {
                        g2.setColor(new Color(0x374151));
                    }
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(x, y, cardWidth, CARD_HEIGHT * 2, 12, 12);

                    g2.setColor(new Color(0x4B5563));
                    g2.drawLine(x, y + CARD_HEIGHT, x + cardWidth, y + CARD_HEIGHT);

                    String nameA = clip(a, 22);
                    String nameB = clip(b, 22);

                    g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
                    if (winnerIdx == 0) {
                        g2.setColor(ACCENT_COLOR);
                        g2.drawString(nameA, x + 10, y + 16);
                        g2.setColor(TEXT_SECONDARY);
                        g2.drawString(nameB, x + 10, y + CARD_HEIGHT + 16);
                    } else if (winnerIdx == 1) {
                        g2.setColor(TEXT_SECONDARY);
                        g2.drawString(nameA, x + 10, y + 16);
                        g2.setColor(ACCENT_COLOR);
                        g2.drawString(nameB, x + 10, y + CARD_HEIGHT + 16);
                    } else {
                        g2.setColor(TEXT_PRIMARY);
                        g2.drawString(nameA, x + 10, y + 16);
                        g2.drawString(nameB, x + 10, y + CARD_HEIGHT + 16);
                    }

                    if (isCurrent) {
                        g2.setColor(ACCENT_SOFT);
                        int ax = x - 12;
                        int ay = centerY;
                        int[] xs = {ax, ax + 8, ax};
                        int[] ys = {ay - 6, ay, ay + 6};
                        g2.fillPolygon(xs, ys, 3);
                    }

                    if (r < roundsCount - 1) {
                        int nextPairs = tournament.rounds().get(r + 1).size();
                        if (nextPairs > 0) {
                            int nextUsableHeight = height - MARGIN_Y * 2;
                            int nextBlock = nextUsableHeight / nextPairs;
                            int destIndex = i / 2;
                            int destCenterY = MARGIN_Y
                                    + nextBlock * destIndex
                                    + nextBlock / 2;

                            int startX = x + cardWidth;
                            int midX = startX + 20;
                            g2.setColor(new Color(0x4B5563));
                            g2.setStroke(new BasicStroke(1.5f));
                            g2.drawLine(startX, centerY, midX, centerY);
                            g2.drawLine(midX, centerY, midX, destCenterY);
                        }
                    }
                }
            }

            g2.dispose();
        }

        private String clip(String s, int max) {
            if (s == null) return "";
            if (s.length() <= max) return s;
            if (max <= 1) return "…";
            return s.substring(0, max - 1) + "…";
        }
    }

    // ---------------------------------------------------------------------
    //  dialog potwierdzenia
    // ---------------------------------------------------------------------

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
                int width = Math.min(700, 260 + question.length() * 6);
                optionPane.setPreferredSize(new Dimension(width, 160));
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

    // ---------------------------------------------------------------------
    //  wejście
    // ---------------------------------------------------------------------

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
            NavKey nav = navQueue.take();
            if (nav != NavKey.NONE) {
                rawQueue.clear();
            }
            return nav;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NavKey.NONE;
        }
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
                    default -> { }
                }

                if (!Character.isISOControl(c)) {
                    rawQueue.offer(new KeyStroke(c, false, e.isAltDown()));
                }
            }
        });
    }

    // ---------------------------------------------------------------------
    //  małe pomocnicze
    // ---------------------------------------------------------------------

    private JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBackground(CARD_COLOR);
        p.setBorder(new LineBorder(new Color(0x1F2933), 1, true));
        p.setFocusable(false);
        p.setOpaque(true);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0x1F2933), 1, true),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        return p;
    }

    private JPanel wrap(JComponent comp) {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.X_AXIS));
        outer.setBackground(BG_COLOR);
        outer.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        outer.setFocusable(false);
        outer.add(comp);
        outer.add(Box.createHorizontalGlue());
        return outer;
    }

    private JLabel label(String text, int size, int style) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(Font.SANS_SERIF, style, size));
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    private static String firstNameOf(String full) {
        if (full == null || full.isBlank()) {
            return "A";
        }
        String s = full.trim();
        int sp = s.indexOf(' ');
        return sp > 0 ? s.substring(0, sp) : s;
    }

    private String clip(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        if (max <= 1) return "…";
        return s.substring(0, max - 1) + "…";
    }

    @Override
    public void close() {
        if (frame != null) {
            frame.dispose();
        }
    }
}
