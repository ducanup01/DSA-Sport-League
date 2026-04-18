import java.time.Instant;

public class Match {
    long timeStamp;
    Player player1, player2;
    boolean player1Wins;

    public static void main(String[] args)
    {
        Instant now = Instant.now();
        long timestamp = (now.getEpochSecond() * 1_000_000_000L) + now.getNano();
        System.out.println("Unix Epoch Nanoseconds: " + timestamp);
    }
}
