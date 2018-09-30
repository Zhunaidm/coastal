
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class FuzzUI implements Runnable {
    private JFrame mainFrame;
    private JLabel headerLabel;
    private JLabel classLabel;
    private JLabel modeLabel;
    private static JLabel timeLabel;
    private static JLabel operationLabel;
    private static JLabel coverageLabel;
    private static JLabel pathsLabel;
    private static JLabel queueLabel;
    private static JLabel crashesLabel;
    private static JLabel runsLabel;
    private JPanel controlPanel;
    private static String mode;

    public FuzzUI() {
        prepareGUI();
        populateGUI();
    }

    public void run() {
        int count = 0;
        while (true) {
            count++;
            timeLabel.setText("Execution Time: " + JAFL.getExecutionTime());

            int operation = JAFL.getCurrentOperation();

            switch (operation) {
            case 0:
                operationLabel.setText("Current Operation: Flip Bits");
                break;
            case 1:
                operationLabel.setText("Current Operation: Flip Bytes");
                break;
            case 2:
                operationLabel.setText("Current Operation: Arith Inc");
                break;
            case 3:
                operationLabel.setText("Current Operation: Arith Dec");
                break;
            case 4:
                operationLabel.setText("Current Operation: Replace");
                break;
            case 5:
                operationLabel.setText("Current Operation: Havoc");
                break;
            default:
                break;
            }

            coverageLabel.setText("Coverage: " + JAFL.getCoverage());
            pathsLabel.setText("No. Paths: " + JAFL.getNumberPaths());
            queueLabel.setText("Queue Size: " + JAFL.getQueueSize());
            crashesLabel.setText("Crashes: " + JAFL.getNumberCrashes());
            runsLabel.setText("Number of executions: " + JAFL.getNumberRuns());
        }
    }

    private void prepareGUI() {
        mainFrame = new JFrame("Fuzzer UI");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(600, 600);
        mainFrame.setLayout(new GridLayout(5, 1));

        headerLabel = new JLabel("", JLabel.CENTER);
        classLabel = new JLabel("", JLabel.CENTER);
        modeLabel = new JLabel("", JLabel.CENTER);

        int modeValue = JAFL.getMode();
        if (modeValue == 0) {
            mode = "Blind Fuzzing";
        } else {
            mode = "Worst Case Fuzzing";
        }

        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(100);
            }
        });
        controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(4, 2));
        controlPanel.setBackground(Color.DARK_GRAY);
        mainFrame.getContentPane().setBackground(Color.BLACK);
        mainFrame.add(headerLabel);
        mainFrame.add(classLabel);
        mainFrame.add(modeLabel);
        mainFrame.add(controlPanel);

    }

    private void populateGUI() {
        headerLabel.setText("JAFL");
        headerLabel.setForeground(Color.YELLOW);
        headerLabel.setFont(new Font("Serif", Font.BOLD, 32));
        classLabel.setText("Class Name: " + JAFL.getClassName());
        classLabel.setForeground(Color.YELLOW);
        modeLabel.setText("Fuzzing Mode: " + mode);
        modeLabel.setForeground(Color.YELLOW);
        timeLabel = new JLabel("Execution Time: ", JLabel.LEFT);
        timeLabel.setForeground(Color.ORANGE);
        operationLabel = new JLabel("Current Operation: ", JLabel.LEFT);
        operationLabel.setForeground(Color.ORANGE);
        coverageLabel = new JLabel("Coverage: ", JLabel.LEFT);
        coverageLabel.setForeground(Color.ORANGE);
        pathsLabel = new JLabel("No. Paths: ", JLabel.LEFT);
        pathsLabel.setForeground(Color.ORANGE);
        queueLabel = new JLabel("Queue Size: ", JLabel.LEFT);
        queueLabel.setForeground(Color.ORANGE);
        crashesLabel = new JLabel("Crashes: ", JLabel.LEFT);
        crashesLabel.setForeground(Color.ORANGE);
        runsLabel = new JLabel("Number of executions: ", JLabel.LEFT);
        runsLabel.setForeground(Color.ORANGE);
        controlPanel.add(timeLabel);
        controlPanel.add(operationLabel);
        controlPanel.add(coverageLabel);
        controlPanel.add(pathsLabel);
        controlPanel.add(queueLabel);
        controlPanel.add(crashesLabel);
        controlPanel.add(runsLabel);
        mainFrame.setVisible(true);
    }

}