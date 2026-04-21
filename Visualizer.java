import javax.swing.*;

public class Visualizer {

    public static void showScatterPlot(double[] x, double[] y, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new ScatterPlot(x, y));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    public static void showHistogram(double[] data, int bins, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
        frame.add(new HistogramPlot(data, bins));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}