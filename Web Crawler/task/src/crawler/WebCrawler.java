package crawler;

import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WebCrawler extends JFrame {
    private String text;
    public WebCrawler() {
        super();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 300);
        setTitle("Web Crawler");
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
        workFlow.add(topPanel,BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        workFlow.add(centerPanel,BorderLayout.CENTER);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        centerPanel.add(titlePanel,BorderLayout.NORTH);

        JLabel header = new JLabel("Title: ");
        titlePanel.add(header);

        JLabel title = new JLabel("");
        title.setName("TitleLabel");
        titlePanel.add(title);

        JTable table = createTable();
        table.setBackground(Color.WHITE);
        centerPanel.add(new JScrollPane(table),BorderLayout.CENTER);

        add(workFlow);

        button.addActionListener( event->{
                try {
                    text=(loadUrl(textField.getText()));
                    title.setText(getTitle(text));
                } catch (IOException e) {
                    e.printStackTrace();
                }
        });

        setVisible(true);
    }

    private static String getTitle(String text) {
        Matcher matcher = Pattern.compile("<title>([^<]*)"
                , Pattern.CASE_INSENSITIVE|Pattern.MULTILINE)
                .matcher(text);
        matcher.find();
        return matcher.group(1);
    }

    private static String loadUrl(String url) throws IOException {
        try(InputStream input = new BufferedInputStream(new URL(url).openStream())){
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private JTable createTable(){
        String[] title = {"URL","Title"};
        JTable jTable = new JTable();
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(title);
        tableModel.addRow(new String[]{"",""});
        return new JTable(tableModel);
    }
    private Stream<String[]> getLinks(String text){
        Matcher matcher = Pattern.compile("<\\s*a[^>]*href\\s*=\\s*['\"]+([^\\'\" >]+)[\\'\"]>(.*)\\<\\s*\\/a\\s*>"
                ,Pattern.CASE_INSENSITIVE|Pattern.MULTILINE).matcher(text);
        List<String[]> result = new ArrayList();
        while (matcher.find()){
            result.add(new String[]{matcher.group(1),matcher.group(2)});
        }
        return result.stream();
    }
    




}