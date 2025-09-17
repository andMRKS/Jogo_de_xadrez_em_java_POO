package view;

import controller.AIPlayer;
import controller.Game;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import model.board.Position;
import model.pieces.Pawn;
import model.pieces.Piece;

public class ChessGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final Color LIGHT_SQ = new Color(234, 235, 209);
    private static final Color DARK_SQ = new Color(111, 143, 114);
    private static final Color HILITE_SELECTED = new Color(255, 217, 25);
    private static final Color HILITE_LEGAL = new Color(60, 143, 70, 150);
    private static final Color HILITE_LASTMOVE = new Color(201, 179, 131, 175);

    private static final Border BORDER_SELECTED = new MatteBorder(2, 2, 2, 2, HILITE_SELECTED);
    private static final Border BORDER_LEGAL = new MatteBorder(2, 2, 2, 2, HILITE_LEGAL);
    private static final Border BORDER_LASTMOVE = new MatteBorder(2, 2, 2, 2, HILITE_LASTMOVE);

    private final Game game;

    private final JPanel boardPanel;
    private final JButton[][] squares = new JButton[8][8];

    private final JLabel status;
    private final JTextArea history;
    private final JScrollPane historyScroll;

    private JCheckBoxMenuItem pcAsBlack;
    private JCheckBoxMenuItem pcVsPcItem;
    private JMenuItem newGameItem, quitItem;
    private AIPlayer.Difficulty aiDifficulty = AIPlayer.Difficulty.MEDIUM;

    private Position selected = null;
    private List<Position> legalForSelected = new ArrayList<>();

    private Position lastFrom = null, lastTo = null;

    private boolean aiThinking = false;

    public ChessGUI() {
        super("ChessGame");

        Color darkBg = new Color(45, 45, 45);
        Color midBg = new Color(60, 60, 60);
        Color lightText = new Color(220, 220, 220);

        UIManager.put("control", midBg);
        UIManager.put("text", lightText);
        UIManager.put("nimbusBase", new Color(30, 30, 30));
        UIManager.put("nimbusFocus", HILITE_SELECTED);
        UIManager.put("nimbusLightBackground", midBg);
        UIManager.put("nimbusSelectionBackground", HILITE_SELECTED);
        UIManager.put("Panel.background", darkBg);
        UIManager.put("TextArea.background", midBg);
        UIManager.put("TextArea.foreground", lightText);
        UIManager.put("TextArea.caretForeground", lightText);
        UIManager.put("Label.foreground", lightText);
        UIManager.put("Button.background", midBg);
        UIManager.put("Button.foreground", lightText);
        UIManager.put("MenuBar.background", darkBg);
        UIManager.put("Menu.foreground", lightText);
        UIManager.put("MenuItem.foreground", lightText);
        UIManager.put("CheckBoxMenuItem.foreground", lightText);
        UIManager.put("JSpinner.background", midBg);

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            Font uiFont = new Font("Segoe UI", Font.PLAIN, 14);
            UIManager.put("Label.font", uiFont);
            UIManager.put("Button.font", uiFont);
            UIManager.put("Menu.font", uiFont);
            UIManager.put("MenuItem.font", uiFont);
            UIManager.put("CheckBoxMenuItem.font", uiFont);
            UIManager.put("RadioButtonMenuItem.font", uiFont);
            UIManager.put("TextField.font", uiFont);
            UIManager.put("ComboBox.font", uiFont);
            UIManager.put("Spinner.font", uiFont);
            UIManager.put("ToolTip.font", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("TextArea.font", new Font("Segoe UI", Font.PLAIN, 13));

            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {
        }

        this.game = new Game();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        setJMenuBar(buildMenuBar());

        boardPanel = new JPanel(new GridLayout(8, 8, 0, 0));
        boardPanel.setBackground(new Color(240, 242, 245));
        boardPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                final int rr = r;
                final int cc = c;
                JButton b = new JButton();
                b.setMargin(new Insets(0, 0, 0, 0));
                b.setFocusPainted(false);
                b.setOpaque(true);
                b.setBorderPainted(true);
                b.setContentAreaFilled(true);
                b.setFont(b.getFont().deriveFont(Font.BOLD, 24f));
                b.addActionListener(e -> handleClick(new Position(rr, cc)));
                squares[r][c] = b;
                boardPanel.add(b);
            }
        }

        status = new JLabel("Vez: Brancas");
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        history = new JTextArea(14, 22);
        history.setEditable(false);
        history.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        historyScroll = new JScrollPane(history);

        JPanel rightPanel = new JPanel(new BorderLayout(6, 6));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel histLabel = new JLabel("Histórico de lances:");
        histLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        rightPanel.add(histLabel, BorderLayout.NORTH);
        rightPanel.add(historyScroll, BorderLayout.CENTER);
        rightPanel.add(buildSideControls(), BorderLayout.SOUTH);

        add(boardPanel, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        boardPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refresh();
            }
        });

        setMinimumSize(new Dimension(920, 680));
        setLocationRelativeTo(null);

        setupAccelerators();

        setVisible(true);
        refresh();
        maybeTriggerAI();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JMenu gameMenu = new JMenu("Jogo");

        newGameItem = new JMenuItem("Novo Jogo");
        newGameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        newGameItem.addActionListener(e -> doNewGame());

        pcAsBlack = new JCheckBoxMenuItem("PC joga com as Pretas");
        pcAsBlack.setSelected(false);
        pcAsBlack.addActionListener(e -> {
            if (pcAsBlack.isSelected()) {
                pcVsPcItem.setSelected(false);
                pcVsPcItem.setEnabled(false);
                maybeTriggerAI();
            } else {
                pcVsPcItem.setEnabled(true);
            }
        });

        pcVsPcItem = new JCheckBoxMenuItem("PC vs PC");
        pcVsPcItem.setSelected(false);
        pcVsPcItem.addActionListener(e -> {
            if (pcVsPcItem.isSelected()) {
                pcAsBlack.setSelected(false);
                pcAsBlack.setEnabled(false);
                maybeTriggerAI();
            } else {
                pcAsBlack.setEnabled(true);
            }
        });

        JMenu difficultyMenu = new JMenu("Dificuldade IA");
        ButtonGroup difficultyGroup = new ButtonGroup();
        JRadioButtonMenuItem easy = new JRadioButtonMenuItem("Fácil");
        easy.addActionListener(e -> aiDifficulty = AIPlayer.Difficulty.EASY);
        JRadioButtonMenuItem medium = new JRadioButtonMenuItem("Médio", true);
        medium.addActionListener(e -> aiDifficulty = AIPlayer.Difficulty.MEDIUM);
        JRadioButtonMenuItem hard = new JRadioButtonMenuItem("Difícil");
        hard.addActionListener(e -> aiDifficulty = AIPlayer.Difficulty.HARD);
        difficultyGroup.add(easy);
        difficultyGroup.add(medium);
        difficultyGroup.add(hard);
        difficultyMenu.add(easy);
        difficultyMenu.add(medium);
        difficultyMenu.add(hard);

        quitItem = new JMenuItem("Sair");
        quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        quitItem.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

        gameMenu.add(newGameItem);
        gameMenu.addSeparator();
        gameMenu.add(pcAsBlack);
        gameMenu.add(pcVsPcItem);
        gameMenu.add(difficultyMenu);
        gameMenu.addSeparator();
        gameMenu.add(quitItem);

        mb.add(gameMenu);
        return mb;
    }

    private JPanel buildSideControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton btnNew = new JButton("Novo Jogo");
        btnNew.addActionListener(e -> doNewGame());
        panel.add(btnNew);

        JCheckBox cb = new JCheckBox("PC (Pretas)");
        cb.setSelected(pcAsBlack.isSelected());
        cb.addActionListener(e -> pcAsBlack.setSelected(cb.isSelected()));
        panel.add(cb);

        return panel;
    }

    private void setupAccelerators() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "newGame");
        getRootPane().getActionMap().put("newGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doNewGame();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "quit");
        getRootPane().getActionMap().put("quit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(ChessGUI.this, WindowEvent.WINDOW_CLOSING));
            }
        });
    }

    private void doNewGame() {
        selected = null;
        legalForSelected.clear();
        lastFrom = lastTo = null;
        aiThinking = false;
        game.newGame();
        refresh();
        maybeTriggerAI();
    }

    private void handleClick(Position clicked) {
        if (game.isGameOver() || aiThinking || pcVsPcItem.isSelected()) return;

        if (pcAsBlack.isSelected() && !game.whiteToMove()) return;

        Piece p = game.board().get(clicked);

        if (selected == null) {
            if (p != null && p.isWhite() == game.whiteToMove()) {
                selected = clicked;
                legalForSelected = game.legalMovesFrom(selected);
            }
        } else {
            List<Position> legals = game.legalMovesFrom(selected);
            if (legals.contains(clicked)) {
                Character promo = null;
                Piece moving = game.board().get(selected);
                if (moving instanceof Pawn && game.isPromotion(selected, clicked)) {
                    promo = askPromotion();
                }
                lastFrom = selected;
                lastTo = clicked;

                game.move(selected, clicked, promo);

                selected = null;
                legalForSelected.clear();

                refresh();
                maybeAnnounceEnd();
                maybeTriggerAI();
                return;
            } else if (p != null && p.isWhite() == game.whiteToMove()) {
                selected = clicked;
                legalForSelected = game.legalMovesFrom(selected);
            } else {
                selected = null;
                legalForSelected.clear();
            }
        }
        refresh();
    }

    private Character askPromotion() {
        String[] opts = {"Rainha", "Torre", "Bispo", "Cavalo"};
        int ch = JOptionPane.showOptionDialog(
                this,
                "Escolha a peça para promoção:",
                "Promoção",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opts,
                opts[0]
        );
        return switch (ch) {
            case 1 -> 'R';
            case 2 -> 'B';
            case 3 -> 'N';
            default -> 'Q';
        };
    }

    private void maybeTriggerAI() {
        if (game.isGameOver()) return;

        boolean isAITurn = pcVsPcItem.isSelected() || (pcAsBlack.isSelected() && !game.whiteToMove());
        if (!isAITurn) return;

        aiThinking = true;
        String player = game.whiteToMove() ? "Brancas" : "Pretas";
        status.setText("Vez: " + player + " — PC pensando...");

        new SwingWorker<AIPlayer.Move, Void>() {
            @Override
            protected AIPlayer.Move doInBackground() throws Exception {
                if (pcVsPcItem.isSelected()) {
                    Thread.sleep(500);
                }
                return AIPlayer.findBestMove(game, aiDifficulty);
            }

            @Override
            protected void done() {
                try {
                    AIPlayer.Move chosen = get();
                    if (chosen != null && !game.isGameOver()) {
                        lastFrom = chosen.from;
                        lastTo = chosen.to;
                        Character promo = null;
                        Piece moving = game.board().get(lastFrom);
                        if (moving instanceof Pawn && game.isPromotion(lastFrom, lastTo)) {
                            promo = 'Q';
                        }
                        game.move(lastFrom, lastTo, promo);
                    }
                } catch (Exception ignored) {
                }

                aiThinking = false;
                refresh();
                maybeAnnounceEnd();

                maybeTriggerAI();
            }
        }.execute();
    }

    private void refresh() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                boolean light = (r + c) % 2 == 0;
                Color base = light ? LIGHT_SQ : DARK_SQ;
                JButton b = squares[r][c];
                b.setBackground(base);
                b.setBorder(null);
                b.setToolTipText(null);
            }
        }

        if (lastFrom != null)
            squares[lastFrom.getRow()][lastFrom.getColumn()].setBorder(BORDER_LASTMOVE);
        if (lastTo != null) squares[lastTo.getRow()][lastTo.getColumn()].setBorder(BORDER_LASTMOVE);

        if (selected != null) {
            squares[selected.getRow()][selected.getColumn()].setBorder(BORDER_SELECTED);
            for (Position d : legalForSelected) {
                squares[d.getRow()][d.getColumn()].setBorder(BORDER_LEGAL);
            }
        }

        int iconSize = computeSquareIconSize();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = game.board().get(new Position(r, c));
                JButton b = squares[r][c];

                if (p == null) {
                    b.setIcon(null);
                    b.setText("");
                    continue;
                }

                char sym = p.getSymbol().charAt(0);
                ImageIcon icon = ImageUtil.getPieceIcon(p.isWhite(), sym, iconSize);
                if (icon != null) {
                    b.setIcon(icon);
                    b.setText("");
                } else {
                    b.setIcon(null);
                    b.setText(toUnicode(p.getSymbol(), p.isWhite()));
                }
            }
        }

        String side = game.whiteToMove() ? "Brancas" : "Pretas";
        String chk = game.inCheck(game.whiteToMove()) ? " — Xeque!" : "";
        if (aiThinking) {
            chk = " — PC pensando...";
        }
        status.setText("Vez: " + side + chk);

        StringBuilder sb = new StringBuilder();
        var hist = game.history();
        for (int i = 0; i < hist.size(); i++) {
            if (i % 2 == 0) sb.append((i / 2) + 1).append('.').append(' ');
            sb.append(hist.get(i)).append(' ');
            if (i % 2 == 1) sb.append('\n');
        }
        history.setText(sb.toString());
        history.setCaretPosition(history.getDocument().getLength());
    }

    private void maybeAnnounceEnd() {
        if (!game.isGameOver()) return;
        String msg;
        if (game.inCheck(game.whiteToMove())) {
            msg = "Xeque-mate! " + (game.whiteToMove() ? "Pretas" : "Brancas") + " vencem.";
        } else {
            msg = "Empate por afogamento (stalemate).";
        }
        JOptionPane.showMessageDialog(this, msg, "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
    }

    private String toUnicode(String sym, boolean white) {
        return switch (sym) {
            case "K" -> white ? "\u2654" : "\u265A";
            case "Q" -> white ? "\u2655" : "\u265B";
            case "R" -> white ? "\u2656" : "\u265C";
            case "B" -> white ? "\u2657" : "\u265D";
            case "N" -> white ? "\u2658" : "\u265E";
            case "P" -> white ? "\u2659" : "\u265F";
            default -> "";
        };
    }

    private int computeSquareIconSize() {
        JButton b = squares[0][0];
        int w = Math.max(1, b.getWidth());
        int h = Math.max(1, b.getHeight());
        int side = Math.min(w, h);
        if (side <= 1) return 64;
        return Math.max(24, side - 8);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGUI::new);
    }
}