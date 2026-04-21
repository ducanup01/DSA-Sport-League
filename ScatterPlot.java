import java.awt.*;
import javax.swing.*;

public class ScatterPlot extends JPanel {

    double[] xData;
    double[] yData;

    public ScatterPlot(double[] xData, double[] yData) {
        this.xData = xData;
        this.yData = yData;
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
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;

        for (int i = 0; i < xData.length; i++) {
            minX = Math.min(minX, xData[i]);
            maxX = Math.max(maxX, xData[i]);
            minY = Math.min(minY, yData[i]);
            maxY = Math.max(maxY, yData[i]);
        }

        // Draw axes
        g2.drawLine(padding, height - padding, width - padding, height - padding); // X-axis
        g2.drawLine(padding, padding, padding, height - padding); // Y-axis

        // Plot points
        for (int i = 0; i < xData.length; i++) {
            int x = (int) (padding + (xData[i] - minX) / (maxX - minX) * (width - 2 * padding));
            int y = (int) (height - padding - (yData[i] - minY) / (maxY - minY) * (height - 2 * padding));

            g2.fillOval(x - 3, y - 3, 6, 6);
        }

        // Labels
        g2.drawString("Skill", width / 2, height - 10);
        g2.drawString("Elo", 10, height / 2);
    }
}