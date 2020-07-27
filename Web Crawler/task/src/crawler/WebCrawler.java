package crawler;

import kotlin.text.Regex;
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
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WebCrawler extends JFrame {


    JLabel title;
    private String text;
    private String url;
    private String textTitle;
    private String[][] data;


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

        title = new JLabel("");
        title.setName("TitleLabel");
        titlePanel.add(title);
        JTable table = createTable();
        table.setBackground(Color.WHITE);
        centerPanel.add(new JScrollPane(table),BorderLayout.CENTER);
        add(workFlow);
        button.addActionListener( event->{
            this.url=textField.getText();
            this.text=loadUrl(this.url);
            this.textTitle=getTitle(text);
            title.setText(textTitle);
            updateTableData(table,getLinks(text,url,textTitle));
        });

        setVisible(true);
    }

    private static String getTitle(String text) {
        if (text==null || text.isEmpty()) return null;
        Matcher matcher = Pattern.compile("<title>([^<]*)"
                , Pattern.CASE_INSENSITIVE|Pattern.MULTILINE)
                .matcher(text);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    private static String loadUrl(String url) {
        if (url==null || url.isEmpty()) return null;
        try {
            URLConnection connection = new URL(url).openConnection();
            if (!Objects.equals(connection.getContentType(),"text/html")) {
                return null;
            }
            try (InputStream input = connection.getInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        catch (IOException exception){
            return null;
        }
    }

    private JTable createTable(){
        String[] title = {"URL","Title"};
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(title);
        JTable jTable = new JTable(tableModel);
        jTable.setName("TitlesTable");
        jTable.setEnabled(false);
        return jTable;
    }
    private  void updateTableData(JTable table,String[][] data){
        DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
        tableModel.setRowCount(0);

        for (String[] line :data){
            tableModel.addRow(line);
        }
        tableModel.fireTableDataChanged();
    }

    private static String[][] getLinks(String text,String url,String title) {
        if (text==null || text.isEmpty()) return new String[0][0];
        Matcher matcher = Pattern.compile("<\\s*a[^>]*href\\s*=\\s*['\"]+([^'\" >]+)['\"][^>]*>(.*)<\\s*/a\\s*>"
                ,Pattern.CASE_INSENSITIVE|Pattern.MULTILINE).matcher(text);
        List<String[]> result = new ArrayList<>();
        result.add(new String[]{url,title});
        while (matcher.find()){
            String absoluteLink = getAbsoluteUrl(matcher.group(1),url);
            title = getTitle(loadUrl(absoluteLink));
            if (title==null || title.isEmpty()) continue;
            result.add(new String[]{absoluteLink,title});
        }
        return result.toArray(String[][]::new);
    }

    private static String getAbsoluteUrl(String url,String root)
    {
        if (url==null || url.isEmpty() || root==null || root.isEmpty()) return null;
        boolean isAbsoluteUrl = url.contains("/");
        //Убираем ведущие и конечные слеши
        url=(url.replace("/"," ")).trim().replace(" ","/");
        boolean protocolExist = Pattern.matches("http[s]?://.*",url);

        if (!isAbsoluteUrl){
            String[] paths=root.split("/");
            paths[paths.length-1]="";
            url=String.join("/",paths)+url;
        } else if (!protocolExist){
            url=root.substring(0,root.indexOf("://")+3)+url;
        }
        return url;
    }
}