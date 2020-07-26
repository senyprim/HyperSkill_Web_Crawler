package crawler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebCrawler extends JFrame {
    public WebCrawler() {
        super();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 300);
        setTitle("Simple Window");
        setBackground(Color.GRAY);
        JPanel workFlow = new JPanel();
        workFlow.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        workFlow.setLayout(new BorderLayout());


        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel,BoxLayout.X_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));

        JTextField textField = new JTextField();
        textField.setName("UrlTextField");
        topPanel.add(textField);

        JButton button = new JButton("Get text!");
        button.setName("RunButton");
        topPanel.add(button);

        JTextArea textArea = new JTextArea();
        textArea.setName("HtmlTextArea");
        textArea.setEnabled(false);
        textArea.setPreferredSize(new Dimension(200,200));
        textArea.setBackground(Color.WHITE);
        workFlow.add(topPanel,BorderLayout.NORTH);
        workFlow.add(textArea,BorderLayout.CENTER);
        add(workFlow);

        button.addActionListener( event->{
                try {
                    textArea.setText(loadUrl(textField.getText()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
        });

        setVisible(true);
    }
    private static String loadUrl(String url) throws IOException {
        try(InputStream input = new BufferedInputStream(new URL(url).openStream())){
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }


}