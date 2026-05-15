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

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chartPanel, rightPanel);
        split.setResizeWeight(0.68);
        split.setPreferredSize(new Dimension(1120, 680));

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