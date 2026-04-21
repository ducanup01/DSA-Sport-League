import java.util.Random;

public class Tournament {

    static Random random = new Random();
    public static void main(String[] args)
    {
        int numberOfPlayers = 10000;

        Player[] playerPool = new Player[numberOfPlayers];
        for (int i = 0; i < numberOfPlayers; i++)
        {
            int randomSkillPoint = (int) (random.nextGaussian() * 200) + 1000;
            int randomPerformanceDeviation = (int) (random.nextGaussian() * 50) + 200;
            playerPool[i] = new Player(randomSkillPoint, randomPerformanceDeviation);
        }

        int simulations = 10_000_000;

        for (int i = 0; i < simulations; i++)
        {
            int randomPlayer1 = Math.abs(random.nextInt()) % numberOfPlayers;
            int randomPlayer2 = Math.abs(random.nextInt()) % numberOfPlayers;
            playerPool[randomPlayer1].playsWith(playerPool[randomPlayer2]);
        }



        double[] skill = new double[numberOfPlayers];

        for (int i = 0; i < numberOfPlayers; i++) {
            skill[i] = playerPool[i].skillPoint;
        }
        
        Visualizer.showHistogram(skill, 30, "Skill Distribution");
    }
}
