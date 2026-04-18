public class Tournament {
    public static void main(String[] args)
    {
        Player player1 = new Player(1100, 100);
        Player player2 = new Player(800, 200);

        int simulations = 1_000_000;

        for (int i = 0; i < simulations; i++)
            player1.playsWith(player2);

        IO.println("Player 1's winrate: " + (double) player1.gamesWon/simulations);

        IO.println("Player 1's Elo: " + player1.eloPoints);
        IO.println("Player 2's Elo: " + player2.eloPoints);
    }
}
