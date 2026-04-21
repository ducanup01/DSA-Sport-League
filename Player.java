import java.util.*;

public class Player {
    double eloPoints;
    int skillPoint, performanceDeviation;
    int gamesWon, gamesPlayed;

    static Random random = new Random();

    public Player(int skillPoint, int performanceDeviation)
    {
        this.skillPoint = skillPoint;
        this.performanceDeviation = performanceDeviation;
        this.eloPoints = 1500;
    }

    public double performance()
    {
        return this.skillPoint + this.performanceDeviation * random.nextGaussian();
    }

    public void playsWith(Player player2) {
        double p1 = this.performance();
        double p2 = player2.performance();

        this.gamesPlayed++;
        player2.gamesPlayed++;

        if (p1 >= p2) this.beats(player2);
        else player2.beats(this);
    }   
    
    public void beats(Player player2)
    {
        int K_coefficient = 24;
        double expectedPlayer1Score = 1 / (1 + Math.pow(10, (player2.eloPoints - this.eloPoints)/400));
        this.eloPoints += K_coefficient * (1 - expectedPlayer1Score);
        this.gamesWon++;
        player2.eloPoints -= K_coefficient * (1 - expectedPlayer1Score);
    }

    public static void main(String[] args)
    {

    }
}
