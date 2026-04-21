import java.awt.*;
import javax.swing.*;

public class HistogramPlot extends JPanel {

    double[] data;
    int bins;

    public HistogramPlot(double[] data, int bins) {
        this.data = data;
        this.bins = bins;
        setPreferredSize(new Dimension(800, 600));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int width = getWidth();
        int height = getHeight();
        int padding = 50;

        // Find min/max
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (double v : data) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }

        double range = (max - min == 0) ? 1 : (max - min);
        double binSize = range / bins;

        int[] counts = new int[bins];

        // Fill bins
        for (double v : data) {
            int bin = (int) ((v - min) / binSize);
            if (bin >= bins) bin = bins - 1;
            counts[bin]++;
        }

        // Find max count (for scaling)
        int maxCount = 0;
        for (int c : counts) {
            maxCount = Math.max(maxCount, c);
        }

        // Draw axes
        g2.drawLine(padding, height - padding, width - padding, height - padding);
        g2.drawLine(padding, padding, padding, height - padding);

        // Draw bars
        int chartWidth = width - 2 * padding;
        int chartHeight = height - 2 * padding;
        int barWidth = chartWidth / bins;

        for (int i = 0; i < bins; i++) {
            int barHeight = (int) ((counts[i] / (double) maxCount) * chartHeight);

            int x = padding + i * barWidth;
            int y = height - padding - barHeight;

            g2.fillRect(x, y, barWidth - 2, barHeight);
        }

        g2.drawString("Skill", width / 2, height - 10);
        g2.drawString("Player count", 10, height / 2);
    }
}