import java.util.List;
import java.util.Random;

public class Tournament {

    static Random random = new Random();

    public static void main(String[] args) {

        int numberOfPlayers = 10000;

        Player[] playerPool = new Player[numberOfPlayers];

        // 1. CREATE PLAYERS
        for (int i = 0; i < numberOfPlayers; i++) {
            int randomSkillPoint = (int) (random.nextGaussian() * 200) + 1000;
            int randomPerformanceDeviation = (int) (random.nextGaussian() * 50) + 200;

            playerPool[i] = new Player(i, randomSkillPoint, randomPerformanceDeviation);
        }

        // 2. BUILD AVL TREE
        AVLTree tree = new AVLTree();

        for (int i = 0; i < numberOfPlayers; i++) {
            tree.insert(playerPool[i]);
        }

        // 3. SIMULATION
        int simulations = 100_000;

        for (int i = 0; i < simulations; i++) {

            int idx1 = random.nextInt(numberOfPlayers);
            int idx2 = random.nextInt(numberOfPlayers);

            if (idx1 == idx2) continue;

            Player p1 = playerPool[idx1];
            Player p2 = playerPool[idx2];

            // Remove from tree before update
            tree.delete(p1);
            tree.delete(p2);

            // Play match (updates Elo)
            p1.playsWith(p2);

            // Reinsert with updated Elo
            tree.insert(p1);
            tree.insert(p2);
        }

        // 4. PREPARE DATA
        double[] skill = new double[numberOfPlayers];
        double[] elo = new double[numberOfPlayers];

        for (int i = 0; i < numberOfPlayers; i++) {
            skill[i] = playerPool[i].skillPoint;
            elo[i] = playerPool[i].eloPoints;
        }

        // 5. VISUALIZATION
        Visualizer.showHistogram(skill, 30, "Skill Distribution");
        Visualizer.showHistogram(elo, 30, "Elo Distribution");

        Visualizer.showScatterPlot(skill, elo, "Skill vs Elo");

        // 6. LEADERBOARD (TREE)
        List<Player> topPlayers = tree.getTopK(10);

        System.out.println("\n===== TOP 10 PLAYERS =====");
        for (Player p : topPlayers) {
            System.out.println("ID: " + p.id +
                               " | Elo: " + p.eloPoints +
                               " | Skill: " + p.skillPoint);
        }

        // 7. MATCHMAKING DEMO
        Player sample = playerPool[random.nextInt(numberOfPlayers)];
        Player closest = tree.findClosest(sample.eloPoints);

        System.out.println("\nMatchmaking Example:");
        System.out.println("Player " + sample.id + " Elo: " + sample.eloPoints);
        System.out.println("Closest opponent: " + closest.id + " Elo: " + closest.eloPoints);

        System.out.println("\n===== TOP 20 TREE VIEW =====");
        tree.printTopK(20);
    }
}