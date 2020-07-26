package crawler;

import javax.swing.*;
import java.awt.*;

public class WebCrawler extends JFrame {
    public WebCrawler() {
        super();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 300);
        setTitle("Simple Window");
        setBackground(Color.GRAY);
        JPanel workFlow = new JPanel();
        workFlow.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JTextArea textArea = new JTextArea();
        textArea.setName("TextArea");
        textArea.setText("HTML code?");
        textArea.setEnabled(false);
        textArea.setPreferredSize(new Dimension(200,200));

        textArea.setBackground(Color.WHITE);
        workFlow.add(textArea);
        add(workFlow);

        setVisible(true);
    }
}