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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawler extends JFrame {
    private class Url{
        final String pageUrl;
        final String href;
        String title;
        final int depth;
        Url(String pageUrl,String href,int depth){
            this.pageUrl = pageUrl;
            this.href=getAbsoluteUrl(href,pageUrl);
            this.depth=depth;
        }
        private String getAbsoluteUrl(String href,String pageUrl)
        {
            if (href==null || href.isEmpty()) return null;
            boolean isAbsoluteUrl = href.contains("/");
            //Убираем ведущие и конечные слеши
            href=(href.replace("/"," ")).trim().replace(" ","/");

            boolean protocolExist = Pattern.matches("http[s]?://.*",href);

            if (!isAbsoluteUrl){
                if (pageUrl==null || pageUrl.isEmpty()) return null;
                String[] paths = href.split("//");
                if (paths.length<2) {
                    href+=".html";
                }
                paths=pageUrl.split("/");
                paths[paths.length-1]="";
                href=String.join("/",paths)+href;

            } else if (!protocolExist){
                if (pageUrl==null || pageUrl.isEmpty()) return null;
                href=pageUrl.substring(0,pageUrl.indexOf("://")+3)+href;
            }
            return href;
        }
    }

    private String url;
    private int workers;
    private int maxDepth;
    private boolean maxDepthEnabled;
    private Long timeLimitSec;
    private boolean timeLimitEnabled;
    private Long startTimeMills;
    private Long elapsedTimeMills;
    private AtomicInteger countParsedPages=new AtomicInteger(0);
    private String fileName;

    private String text;
    private String title;

    private ConcurrentLinkedQueue<Url> queue = new ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<String,String> links = new ConcurrentHashMap<>();
    private Thread[] threads;

    JLabel parsedPages;
    JLabel elapsedTimeLabel;

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
        JToggleButton run = new JToggleButton("Run",false);
        run.setName("RunButton");
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
        elapsedTimeLabel = new JLabel("0:00");
        elapsedTimePanel.add(elapsedTimeLabel,BorderLayout.WEST);
        centerPanel.add(elapsedTimePanel);
        //6
        JPanel parsedPagesPanel = new JPanel(new BorderLayout());
        parsedPages = new JLabel("0");
        parsedPages.setName("ParsedLabel");
        parsedPagesPanel.add(parsedPages,BorderLayout.WEST);
        centerPanel.add(parsedPagesPanel);
        //7
        JPanel exportPanel = new JPanel(new BorderLayout());
        JTextField export=new JTextField();
        export.setName("ExportUrlTextField");
        exportPanel.add(export,BorderLayout.CENTER);
        JButton save = new JButton("Save");
        save.setName("ExportButton");
        exportPanel.add(save,BorderLayout.EAST);
        centerPanel.add(exportPanel);

        workFlow.add(centerPanel,BorderLayout.CENTER);

        run.addActionListener((event)->{
            run.setSelected(true);
            run(url.getText()
                    ,workersCount.getText().isEmpty()?1:Integer.parseInt(workersCount.getText())
                    ,depth.getText().isEmpty()?Integer.MAX_VALUE:Integer.parseInt(depth.getText())
                    ,/*timeLimitCheckBox.isSelected()?Integer.parseInt(time.getText()):*/Integer.MAX_VALUE
                    ,this.queue
                    ,this.links
            );
            run.setSelected(false);
        });
        add(workFlow);
        setVisible(true);
    }
//Выбрать заголовок страницы
    private static String getTitle(String text) {
        if (text==null || text.isEmpty()) return null;
        Matcher matcher = Pattern.compile("<title>([^<]*)"
                , Pattern.CASE_INSENSITIVE|Pattern.MULTILINE)
                .matcher(text);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    private static String loadUntilTitle(String url){
        if (url==null || url.isEmpty()) return null;
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
            StringBuilder stb = new StringBuilder();
            String line;
            try(BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()))){
                while ((line=input.readLine())!=null){
                    String title = getTitle(line);
                    if (title!=null) return title;
                    stb.append(line);
                }
            }
            return getTitle(stb.toString());
        }
        catch (Exception exception){
            return null;
        }
    }
//Получить содержание страницы
    private static String getPage(String url) {
        if (url==null || url.isEmpty()) return null;
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
            InputStream input = new BufferedInputStream(connection.getInputStream());
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (Exception exception){
            return null;
        }
    }
//Получить ссылки на странице
    private static String[] getLinks(String text) {
        if (text==null || text.isEmpty()) return null;
        Matcher matcher = Pattern.compile("<\\s*a[^>]*href\\s*=\\s*['\"]+([^'\" >]+)['\"]"
                ,Pattern.CASE_INSENSITIVE|Pattern.MULTILINE).matcher(text);
        List<String> links = new ArrayList<>();
        while (matcher.find()){
            links.add(matcher.group(1));
        }
        return links.toArray(String[]::new);
    }
//Сохранить массив
    private static void saveTable(String filename,Map<String,String> data) throws IOException {
        String[] array = data.entrySet().stream().map(item->String.format("%s\n%s",item.getKey(),item.getValue())).toArray(String[]::new);
        Files.writeString(Paths.get(filename),String.join("\n",array),StandardCharsets.UTF_8);
    }
//
    private void thread(ConcurrentLinkedQueue<Url> queue, ConcurrentHashMap<String,String> result,int maxDepth,long maxTime){
        while (queue.size()>0){
            if (System.currentTimeMillis()>maxTime) break;

            Url url = queue.poll();
            if (url==null || url.href.isEmpty() || result.containsKey(url.href) ||url.depth>maxDepth) continue;
            String page=getPage(url.href);
            if (page==null) continue;
            title=getTitle(page);
            if (url.depth<maxDepth){
                String[] links = getLinks(page);
                for (String link : links){
                    queue.add(new Url(url.href,link,url.depth+1));
                }
            }
            countParsedPages.getAndIncrement();
            showParsedPages();
            result.put(url.href,title);
            System.out.println(url.href+"-"+title);
        }
    }
//Обновить поля в форме
    private void showParsedPages(){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                parsedPages.setText(String.format("%d",countParsedPages.get()));
                elapsedTimeLabel.setText(String.format("%d:%d"
                        ,TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()- startTimeMills)
                        ,TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()- startTimeMills)%60
                ));
            }
        });
    }
    private void run(String startPage,int threadsCount,int maxDepth,long maxTimeDuration
            ,ConcurrentLinkedQueue<Url> queue, ConcurrentHashMap<String,String> result){
        queue.clear();
        result.clear();
        countParsedPages.set(0);
        queue.add(new Url(null,startPage,0));
        this.startTimeMills = System.currentTimeMillis();

        for(int i=0;i<threadsCount;i++){
            new Thread(()->thread(queue,result,maxDepth,System.currentTimeMillis()+maxTimeDuration)).start();
        }
    }
}