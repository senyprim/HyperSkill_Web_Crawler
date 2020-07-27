package crawler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.swing.GroupLayout.Alignment.LEADING;

public class WebCrawler extends JFrame {

    JLabel titleLabel;
    private String text;
    private String url;
    private String title;


    public WebCrawler() {
        super();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 300);
        setTitle("Web Crawler");
        setBackground(Color.GRAY);
        //Добавляем рабочее пространство с бордером в 5пикселей
        JPanel workFlow = new JPanel(new BorderLayout());
        workFlow.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        //Левая панель с метками
        JPanel labelPanel = new JPanel(new GridLayout(7,1,10,10));
        labelPanel.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        labelPanel.add(new JLabel("Start URL:"));
        labelPanel.add(new JLabel("Workers:"));
        labelPanel.add(new JLabel("Maximum depth:"));
        labelPanel.add(new JLabel("Time limit:"));
        labelPanel.add(new JLabel("Elapsed time:"));
        labelPanel.add(new JLabel("Parsed pages:"));
        labelPanel.add(new JLabel("Export:"));
        workFlow.add(labelPanel,BorderLayout.WEST);
        //Центральная панель
        JPanel centerPanel = new JPanel(new GridLayout(7,1,10,10));
        //Первая строка
        JPanel startURLPanel = new JPanel(new BorderLayout());
        JTextField url=new JTextField();
        url.setName("UrlTextField");
        startURLPanel.add(url,BorderLayout.CENTER);
        JButton run = new JButton("Run");
        startURLPanel.add(run,BorderLayout.EAST);
        centerPanel.add(startURLPanel);
        //Вторая строка
        JPanel workersPanel = new JPanel(new BorderLayout());
        JTextField workersCount=new JTextField();
        workersPanel.add(workersCount,BorderLayout.CENTER);
        centerPanel.add(workersPanel);
        //Третья
        JPanel maxDepthPanel = new JPanel(new BorderLayout());
        JTextField depth=new JTextField();
        depth.setName("DepthTextField");
        maxDepthPanel.add(depth,BorderLayout.CENTER);
        JCheckBox depthCheckBox = new JCheckBox("Enabled",false);
        depthCheckBox.setName("DepthCheckBox");
        maxDepthPanel.add(depthCheckBox,BorderLayout.EAST);
        centerPanel.add(maxDepthPanel);
        //4
        JPanel timeLimitPanel = new JPanel(new BorderLayout());
        JTextField time =new JTextField();
        timeLimitPanel.add(time,BorderLayout.CENTER);
        JCheckBox timeLimitCheckBox = new JCheckBox("Enabled",true);
        JPanel panel = new JPanel();
        panel.add(new JLabel("second"));
        panel.add(timeLimitCheckBox);
        timeLimitPanel.add(panel,BorderLayout.EAST);
        centerPanel.add(timeLimitPanel);
        //5
        JPanel elapsedTimePanel = new JPanel(new BorderLayout());
        JLabel elapsedTime = new JLabel("0:00");
        elapsedTimePanel.add(elapsedTime,BorderLayout.WEST);
        centerPanel.add(elapsedTimePanel);
        //6
        JPanel parsedPagesPanel = new JPanel(new BorderLayout());
        JLabel parsedPages = new JLabel("0");
        parsedPagesPanel.add(parsedPages,BorderLayout.WEST);
        centerPanel.add(parsedPagesPanel);
        //7
        JPanel exportPanel = new JPanel(new BorderLayout());
        JTextField export=new JTextField();
        export.setName("ExportUrlTextField");
        exportPanel.add(export,BorderLayout.CENTER);
        JButton save = new JButton("Save");
        exportPanel.add(save,BorderLayout.EAST);
        centerPanel.add(exportPanel);

        workFlow.add(centerPanel,BorderLayout.CENTER);
/*
        button.addActionListener( event->{
            this.url=textField.getText();
            this.text=loadUrl(this.url);
            this.title =getTitle(text);
            titleLabel.setText(title);
            updateTableData(table,getLinks(text,url, title));
        });
        exportButton.addActionListener(actionEvent -> {
            try {
                saveTable(exportField.getText(),getDataFromTable(table));
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });

         */
        add(workFlow);
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
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
            try (InputStream input = connection.getInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }catch (Exception exception){
                return null;
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
    private static void saveTable(String filename,String[] data) throws IOException {
        Files.writeString(Paths.get(filename),String.join("\n",data),StandardCharsets.UTF_8);
    }
    private static String[] getDataFromTable(JTable table){
        List<String> result = new ArrayList<>();
        DefaultTableModel model =(DefaultTableModel)table.getModel();
        for(int row = 0; row<model.getRowCount();row++){
            for (int col =0;col<model.getColumnCount();col++){
                result.add((String) model.getValueAt(row,col));
            }
        }
        return result.toArray(String[]::new);
    }
}