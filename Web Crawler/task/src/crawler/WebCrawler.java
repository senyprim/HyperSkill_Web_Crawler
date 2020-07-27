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
        JPanel workFlow = new JPanel();
        workFlow.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        workFlow.setLayout(new BorderLayout());
        //На верх добавляем панель
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel,BoxLayout.X_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        //С текстовым полем
        JTextField textField = new JTextField();
        textField.setName("UrlTextField");
        topPanel.add(textField);
        //и кнопкой
        JButton button = new JButton("Get text!");
        button.setName("RunButton");
        topPanel.add(button);
        workFlow.add(topPanel,BorderLayout.NORTH);
        //В центр добавляем центральную панель и делим ее border layout
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        workFlow.add(centerPanel,BorderLayout.CENTER);
        //В центральную панель  сверху добавляем панель заголовков
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        centerPanel.add(titlePanel,BorderLayout.NORTH);
        //В панель заголовком добавляем метки
        JLabel header = new JLabel("Title: ");
        titlePanel.add(header);
        titleLabel = new JLabel("");
        titleLabel.setName("TitleLabel");
        titlePanel.add(titleLabel);
        //В центральную панель в центр добавляем тваблицу в скрол панеле
        JTable table = createTable();
        table.setBackground(Color.WHITE);
        centerPanel.add(new JScrollPane(table),BorderLayout.CENTER);
        //В центральную панель снизу добавляем панель экспорта
        JPanel exportPanel = new JPanel();
        exportPanel.setLayout(new BorderLayout());
        JTextField exportField = new JTextField("",20);
        exportField.setName("ExportUrlTextField");
        JButton exportButton = new JButton("Save");
        exportButton.setName("ExportButton");
        exportPanel.add(new JLabel("Export:"),BorderLayout.WEST);
        exportPanel.add(exportField,BorderLayout.CENTER);
        exportPanel.add(exportButton,BorderLayout.EAST);

        centerPanel.add(exportPanel,BorderLayout.SOUTH);
        add(workFlow);
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