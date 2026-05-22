import java.awt.*;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class LeagueDashboard extends JFrame {
    private final JTextField playersField = new JTextField("10000", 7);
    private final JTextField kField = new JTextField("24", 5);
    private final JTextField searchField = new JTextField(8);

    private final JButton runButton = new JButton("Run");
    private final JButton pauseButton = new JButton("Pause");
    private final JButton searchButton = new JButton("Search ID");

    private final JLabel statusLabel = new JLabel("Ready");
    private final LiveChartPanel chartPanel = new LiveChartPanel();
    private final AVLTreePanel avlTreePanel = new AVLTreePanel();

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[] {"Rank", "ID", "Elo", "Won", "Played"}, 0
    );

    private final JTable leaderboardTable = new JTable(tableModel);
    private final JTextArea profileArea = new JTextArea(8, 30);

    private Player[] players = new Player[0];
    private AVLTree tree;
    private Random random = new Random();

    private int kCoefficient = 24;
    private int targetMatches = 0;
    private int matchesPlayed = 0;
    private int selectedPlayerId = -1;

    private final int delayMs = 150;
    private final int matchesPerTick = 500;

    private final Timer timer = new Timer(delayMs, e -> runStep());

    public LeagueDashboard() {
        super("Sport League Simulator");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(buildControls(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        runButton.addActionListener(e -> startSimulation());
        pauseButton.addActionListener(e -> togglePause());
        searchButton.addActionListener(e -> searchPlayerById());

        pauseButton.setEnabled(false);

        leaderboardTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && leaderboardTable.getSelectedRow() >= 0) {
                int row = leaderboardTable.getSelectedRow();
                selectedPlayerId = Integer.parseInt(tableModel.getValueAt(row, 1).toString());
                searchField.setText(String.valueOf(selectedPlayerId));
                updateProfile();
                avlTreePanel.setSelectedPlayerId(selectedPlayerId);
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBackground(new Color(245, 246, 248));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        panel.add(new JLabel("Players"));
        panel.add(playersField);

        panel.add(new JLabel("K"));
        panel.add(kField);

        panel.add(runButton);
        panel.add(pauseButton);

        panel.add(new JLabel("Search Player ID"));
        panel.add(searchField);
        panel.add(searchButton);

        return panel;
    }

    private JSplitPane buildContent() {
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));

        leaderboardTable.setRowHeight(26);
        leaderboardTable.setFillsViewportHeight(true);
        leaderboardTable.setShowGrid(false);
        leaderboardTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));

        JScrollPane tablePane = new JScrollPane(leaderboardTable);
        tablePane.setBorder(BorderFactory.createTitledBorder("Leaderboard from AVL Tree"));

        profileArea.setEditable(false);
        profileArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        profileArea.setText("Search a player ID or click leaderboard\nto view live profile stats.");

        JScrollPane profilePane = new JScrollPane(profileArea);
        profilePane.setBorder(BorderFactory.createTitledBorder("Live Player Profile"));

        rightPanel.add(tablePane, BorderLayout.CENTER);
        rightPanel.add(profilePane, BorderLayout.SOUTH);

        JScrollPane treePane = new JScrollPane(avlTreePanel);
        treePane.setBorder(BorderFactory.createTitledBorder("Live AVL Tree Structure"));
        treePane.getVerticalScrollBar().setUnitIncrement(16);
        treePane.getHorizontalScrollBar().setUnitIncrement(16);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartPanel, treePane);
        leftSplit.setResizeWeight(0.58);
        leftSplit.setContinuousLayout(true);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightPanel);
        split.setResizeWeight(0.70);
        split.setPreferredSize(new Dimension(1220, 760));
        split.setContinuousLayout(true);

        return split;
    }

    private void startSimulation() {
        int n = Math.max(2, readInt(playersField, 10000));
        kCoefficient = readInt(kField, 24);

        random = new Random();
        players = new Player[n];
        tree = new AVLTree();

        matchesPlayed = 0;
        selectedPlayerId = -1;
        targetMatches = n * 1000;

        for (int i = 0; i < n; i++) {
            int skill = (int) (random.nextGaussian() * 200) + 1000;
            int deviation = (int) (random.nextGaussian() * 50) + 200;

            players[i] = new Player(i, skill, deviation);
            tree.insert(players[i]);
        }

        profileArea.setText("Search a player ID or click leaderboard\nto view live profile stats.");

        setInputsEnabled(false);
        pauseButton.setEnabled(true);
        pauseButton.setText("Pause");

        updateDashboard();
        timer.start();
    }

    private void runStep() {
        if (players.length < 2 || tree == null) return;

        int remaining = targetMatches - matchesPlayed;
        int batch = Math.min(matchesPerTick, remaining);

        for (int i = 0; i < batch; i++) {
            playOneMatchWithK();
        }

        matchesPlayed += batch;
        updateDashboard();

        if (matchesPlayed >= targetMatches) {
            timer.stop();
            setInputsEnabled(true);
            pauseButton.setEnabled(false);
            statusLabel.setText("Finished " + matchesPlayed + " matches.");
        }
    }

    private void playOneMatchWithK() {
        int a = random.nextInt(players.length);
        int b = random.nextInt(players.length);

        if (a == b) return;

        Player p1 = players[a];
        Player p2 = players[b];

        tree.delete(p1);
        tree.delete(p2);

        p1.gamesPlayed++;
        p2.gamesPlayed++;

        double performance1 = p1.performance();
        double performance2 = p2.performance();

        if (performance1 >= performance2) {
            updateElo(p1, p2);
        } else {
            updateElo(p2, p1);
        }

        tree.insert(p1);
        tree.insert(p2);
    }

    private void updateElo(Player winner, Player loser) {
        double expectedWinnerScore =
            1.0 / (1.0 + Math.pow(10, (loser.eloPoints - winner.eloPoints) / 400.0));

        double change = kCoefficient * (1.0 - expectedWinnerScore);

        winner.eloPoints += change;
        loser.eloPoints -= change;
        winner.gamesWon++;
    }

    private void updateDashboard() {
        double[] skill = new double[players.length];
        double[] elo = new double[players.length];

        for (int i = 0; i < players.length; i++) {
            skill[i] = players[i].skillPoint;
            elo[i] = players[i].eloPoints;
        }

        chartPanel.setData(skill, elo);
        fillLeaderboard(tree.getTopK(10));
        avlTreePanel.setTree(tree, selectedPlayerId);

        if (selectedPlayerId >= 0 && selectedPlayerId < players.length) {
            updateProfile();
            reselectPlayer();
        }

        statusLabel.setText(
            "Running: " + matchesPlayed + " / " + targetMatches +
            " matches | K = " + kCoefficient
        );
    }

    private void fillLeaderboard(List<Player> topPlayers) {
        tableModel.setRowCount(0);

        int rank = 1;
        for (Player p : topPlayers) {
            tableModel.addRow(new Object[] {
                rank++,
                p.id,
                String.format("%.1f", p.eloPoints),
                p.gamesWon,
                p.gamesPlayed
            });
        }
    }

    private void searchPlayerById() {
        if (players.length == 0) {
            profileArea.setText("Run the simulation first.");
            return;
        }

        try {
            int id = Integer.parseInt(searchField.getText().trim());

            if (id < 0 || id >= players.length) {
                profileArea.setText("Player ID must be between 0 and " + (players.length - 1) + ".");
                return;
            }

            selectedPlayerId = id;
            updateProfile();
            reselectPlayer();
            avlTreePanel.setSelectedPlayerId(selectedPlayerId);
        } catch (NumberFormatException e) {
            profileArea.setText("Please enter a valid player ID.");
        }
    }

    private void updateProfile() {
        if (selectedPlayerId < 0 || selectedPlayerId >= players.length) return;

        Player p = players[selectedPlayerId];

        double winRate = p.gamesPlayed == 0
            ? 0
            : p.gamesWon * 100.0 / p.gamesPlayed;

        profileArea.setText(
            "LIVE PLAYER PROFILE\n" +
            "------------------------\n" +
            "ID:            " + p.id + "\n" +
            "Games Played:  " + p.gamesPlayed + "\n" +
            "Games Won:     " + p.gamesWon + "\n" +
            "Win Rate:      " + String.format("%.1f%%", winRate) + "\n" +
            "Elo:           " + String.format("%.1f", p.eloPoints) + "\n\n" +
            "SIMULATION\n" +
            "------------------------\n" +
            "Matches:       " + matchesPlayed + " / " + targetMatches + "\n" +
            "K Coefficient: " + kCoefficient
        );
    }

    private void reselectPlayer() {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            int id = Integer.parseInt(tableModel.getValueAt(row, 1).toString());

            if (id == selectedPlayerId) {
                leaderboardTable.setRowSelectionInterval(row, row);
                return;
            }
        }

        leaderboardTable.clearSelection();
    }

    private void togglePause() {
        if (timer.isRunning()) {
            timer.stop();
            pauseButton.setText("Resume");
            statusLabel.setText("Paused at " + matchesPlayed + " / " + targetMatches + " matches");
        } else {
            timer.start();
            pauseButton.setText("Pause");
        }
    }

    private int readInt(JTextField field, int fallback) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            field.setText(String.valueOf(fallback));
            return fallback;
        }
    }

    private void setInputsEnabled(boolean enabled) {
        playersField.setEnabled(enabled);
        kField.setEnabled(enabled);
        runButton.setEnabled(enabled);
    }

    private static class AVLTreePanel extends JPanel {
    private static final int MAX_VISIBLE_DEPTH = 4;
    private static final int NODE_RADIUS = 22;
    private static final int LEVEL_GAP = 72;
    private static final int TOP_PADDING = 70;
    private static final int SIDE_PADDING = 36;

    private TreeNode root;
    private int selectedPlayerId = -1;
    private int visibleNodeCount = 0;
    private int totalTreeHeight = 0;
    private int totalNodeCount = 0;

    private String message = "Run the simulation to see the AVL tree structure.";

    AVLTreePanel() {
        setBackground(new Color(250, 251, 253));
        setPreferredSize(new Dimension(900, 360));
        setMinimumSize(new Dimension(700, 320));
        setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    void setTree(AVLTree tree, int selectedPlayerId) {
        this.root = tree == null ? null : tree.getRoot();
        this.selectedPlayerId = selectedPlayerId;

        this.totalTreeHeight = root == null ? 0 : root.height;
        this.visibleNodeCount = countVisible(root, 1);
        this.totalNodeCount = tree == null ? 0 : tree.getNodeCount();

        this.message = root == null
            ? "Run the simulation to see the AVL tree structure."
            : "Compact view: first " + MAX_VISIBLE_DEPTH + " levels shown.";

        repaint();
    }

    void setSelectedPlayerId(int selectedPlayerId) {
        this.selectedPlayerId = selectedPlayerId;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawHeader(g2);

        if (root == null) {
            drawEmptyState(g2);
            return;
        }

        int panelWidth = getWidth();
        int startX = panelWidth / 2;
        int startY = TOP_PADDING + 34;

        int initialGap = Math.max(70, panelWidth / 4);

        drawEdges(g2, root, startX, startY, initialGap, 1);
        drawNodes(g2, root, startX, startY, initialGap, 1);

        drawLegend(g2);
    }

    private void drawHeader(Graphics2D g2) {
        g2.setColor(new Color(26, 31, 39));
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString("AVL Tree Structure", SIDE_PADDING, 28);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(90, 98, 110));

        String summary =
            message +
            "  |  Total nodes: " + totalNodeCount +
            "  |  Full height: " + totalTreeHeight +
            "  |  Visible nodes: " + visibleNodeCount;

        g2.drawString(summary, SIDE_PADDING, 49);
    }

    private void drawEmptyState(Graphics2D g2) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        g2.setColor(new Color(234, 238, 244));
        g2.fillRoundRect(centerX - 190, centerY - 48, 380, 96, 24, 24);

        g2.setColor(new Color(70, 78, 90));
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        drawCenteredString(g2, "No AVL tree yet", centerX, centerY - 8);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        drawCenteredString(g2, "Click Run to build and visualize the tree.", centerX, centerY + 17);
    }

    private void drawEdges(Graphics2D g2, TreeNode node, int x, int y, int gap, int depth) {
        if (node == null || depth >= MAX_VISIBLE_DEPTH) return;

        int childY = y + LEVEL_GAP;
        int nextGap = Math.max(48, gap / 2);

        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(new Color(170, 180, 194));

        if (node.left != null) {
            int childX = x - gap;
            g2.drawLine(x, y + NODE_RADIUS, childX, childY - NODE_RADIUS);
            drawEdges(g2, node.left, childX, childY, nextGap, depth + 1);
        }

        if (node.right != null) {
            int childX = x + gap;
            g2.drawLine(x, y + NODE_RADIUS, childX, childY - NODE_RADIUS);
            drawEdges(g2, node.right, childX, childY, nextGap, depth + 1);
        }
    }

    private void drawNodes(Graphics2D g2, TreeNode node, int x, int y, int gap, int depth) {
        if (node == null || depth > MAX_VISIBLE_DEPTH) return;

        drawSingleNode(g2, node, x, y);

        if (depth == MAX_VISIBLE_DEPTH) {
            drawMoreMarker(g2, node, x, y);
            return;
        }

        int childY = y + LEVEL_GAP;
        int nextGap = Math.max(48, gap / 2);

        if (node.left != null) {
            drawNodes(g2, node.left, x - gap, childY, nextGap, depth + 1);
        }

        if (node.right != null) {
            drawNodes(g2, node.right, x + gap, childY, nextGap, depth + 1);
        }
    }

    private void drawSingleNode(Graphics2D g2, TreeNode node, int x, int y) {
        Player p = node.player;
        int balance = height(node.left) - height(node.right);
        boolean selected = p != null && p.id == selectedPlayerId;

        Color fillColor = getNodeColor(balance, selected);
        Color borderColor = selected
            ? new Color(202, 126, 22)
            : new Color(61, 70, 82);

        g2.setColor(new Color(0, 0, 0, 28));
        g2.fillOval(
            x - NODE_RADIUS + 2,
            y - NODE_RADIUS + 3,
            NODE_RADIUS * 2,
            NODE_RADIUS * 2
        );

        g2.setColor(fillColor);
        g2.fillOval(
            x - NODE_RADIUS,
            y - NODE_RADIUS,
            NODE_RADIUS * 2,
            NODE_RADIUS * 2
        );

        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(selected ? 3f : 1.4f));
        g2.drawOval(
            x - NODE_RADIUS,
            y - NODE_RADIUS,
            NODE_RADIUS * 2,
            NODE_RADIUS * 2
        );

        g2.setColor(new Color(23, 29, 38));
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        drawCenteredString(g2, "ID " + p.id, x, y - 3);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        drawCenteredString(g2, String.format("%.0f", p.eloPoints), x, y + 10);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.setColor(new Color(85, 92, 103));
        drawCenteredString(g2, "h" + node.height + " b" + balance, x, y + NODE_RADIUS + 14);
    }

    private Color getNodeColor(int balance, boolean selected) {
        if (selected) return new Color(255, 243, 183);

        if (balance == 0) {
            return new Color(219, 237, 255);
        }

        if (Math.abs(balance) == 1) {
            return new Color(222, 242, 226);
        }

        return new Color(255, 224, 224);
    }

    private void drawMoreMarker(Graphics2D g2, TreeNode node, int x, int y) {
        if (node.left == null && node.right == null) return;

        g2.setColor(new Color(110, 118, 130));
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        drawCenteredString(g2, "...", x, y + NODE_RADIUS + 29);
    }

    private void drawLegend(Graphics2D g2) {
        int baseY = getHeight() - 42;
        int x = SIDE_PADDING;

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(new Color(82, 90, 101));
        g2.drawString(
            "Each node shows: Player ID, Elo, height, and balance factor. Balance = left height - right height.",
            x,
            baseY
        );

        drawLegendItem(g2, x, baseY + 22, new Color(219, 237, 255), "balanced");
        drawLegendItem(g2, x + 105, baseY + 22, new Color(222, 242, 226), "slightly tilted");
        drawLegendItem(g2, x + 235, baseY + 22, new Color(255, 243, 183), "selected player");
    }

    private void drawLegendItem(Graphics2D g2, int x, int y, Color color, String label) {
        g2.setColor(color);
        g2.fillRoundRect(x, y - 12, 15, 15, 5, 5);

        g2.setColor(new Color(85, 94, 106));
        g2.drawRoundRect(x, y - 12, 15, 15, 5, 5);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(label, x + 22, y);
    }

    private int height(TreeNode node) {
        return node == null ? 0 : node.height;
    }

    private int countVisible(TreeNode node, int depth) {
        if (node == null || depth > MAX_VISIBLE_DEPTH) return 0;

        return 1
            + countVisible(node.left, depth + 1)
            + countVisible(node.right, depth + 1);
    }

    private void drawCenteredString(Graphics2D g2, String text, int centerX, int baselineY) {
        FontMetrics metrics = g2.getFontMetrics();
        int x = centerX - metrics.stringWidth(text) / 2;
        g2.drawString(text, x, baselineY);
    }
}

    private static class LiveChartPanel extends JPanel {
        private double[] skill = new double[0];
        private double[] elo = new double[0];

        LiveChartPanel() {
            setPreferredSize(new Dimension(740, 660));
            setBackground(Color.WHITE);
        }

        void setData(double[] skill, double[] elo) {
            this.skill = skill;
            this.elo = elo;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int half = getHeight() / 2;

            drawScatter(g2, new Rectangle(60, 40, getWidth() - 100, half - 80));
            drawHistogram(g2, new Rectangle(60, half + 40, getWidth() - 100, half - 90));
        }

        private void drawScatter(Graphics2D g2, Rectangle r) {
            drawAxes(g2, r, "Skill vs Elo", "Skill", "Elo");

            if (skill.length == 0) return;

            Bounds xb = getBounds(skill);
            Bounds yb = getBounds(elo);

            for (int i = 0; i < skill.length; i++) {
                int x = scale(skill[i], xb.min, xb.max, r.x, r.x + r.width);
                int y = scale(elo[i], yb.min, yb.max, r.y + r.height, r.y);

                if (elo[i] > 1700) {
                    g2.setColor(new Color(40, 130, 90, 130));
                } else if (elo[i] < 1300) {
                    g2.setColor(new Color(180, 80, 80, 120));
                } else {
                    g2.setColor(new Color(70, 120, 190, 110));
                }

                g2.fillOval(x - 2, y - 2, 4, 4);
            }
        }

        private void drawHistogram(Graphics2D g2, Rectangle r) {
            drawAxes(g2, r, "Elo Distribution", "Elo", "Players");

            if (elo.length == 0) return;

            int bins = 24;
            Bounds b = getBounds(elo);
            int[] counts = new int[bins];
            double range = Math.max(1, b.max - b.min);

            for (double value : elo) {
                int index = (int) ((value - b.min) / range * bins);
                index = Math.max(0, Math.min(bins - 1, index));
                counts[index]++;
            }

            int maxCount = 1;
            for (int c : counts) {
                maxCount = Math.max(maxCount, c);
            }

            int barWidth = Math.max(1, r.width / bins);
            g2.setColor(new Color(45, 145, 105));

            for (int i = 0; i < bins; i++) {
                int h = (int) (counts[i] / (double) maxCount * r.height);
                int x = r.x + i * barWidth;
                int y = r.y + r.height - h;
                g2.fillRect(x, y, Math.max(1, barWidth - 2), h);
            }
        }

        private void drawAxes(Graphics2D g2, Rectangle r, String title, String xLabel, String yLabel) {
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString(title, r.x, r.y - 12);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawLine(r.x, r.y + r.height, r.x + r.width, r.y + r.height);
            g2.drawLine(r.x, r.y, r.x, r.y + r.height);

            g2.setColor(Color.GRAY);
            g2.drawString(xLabel, r.x + r.width / 2 - 15, r.y + r.height + 28);
            g2.drawString(yLabel, 10, r.y + r.height / 2);
        }

        private int scale(double value, double min, double max, int screenMin, int screenMax) {
            double range = max - min == 0 ? 1 : max - min;
            return (int) (screenMin + (value - min) / range * (screenMax - screenMin));
        }

        private Bounds getBounds(double[] values) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            for (double v : values) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }

            return new Bounds(min, max);
        }

        private static class Bounds {
            double min;
            double max;

            Bounds(double min, double max) {
                this.min = min;
                this.max = max;
            }
        }
    }
}