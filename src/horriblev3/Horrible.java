package horriblev3;


import horriblev3.help_classes.Row;
import horriblev3.help_classes.Anime;
import com.gargoylesoftware.htmlunit.util.Cookie;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import to.uploaded.Uploaded;
import to.uploaded.exception.DownloadFailedException;
import to.uploaded.exception.LoginFailedException;
import to.uploaded.file.UPDownload;


/**
 *
 * @author EngelEatos
 */
public class Horrible {
//GLOBAL-VARS
    
    private static List<Anime> animes = new ArrayList<>();
    private static List<String> animeConfig = new ArrayList<>();
    private static int count = 0;
    private static Set<Cookie> cookies;
    private static String downloadDir = "D:\\Horrible\\";

//MAIN-FUNCTIONS
    
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws to.uploaded.exception.LoginFailedException
     */
    
    public static void main(String[] args) throws IOException, LoginFailedException, ParseException {
        GUI gui = new GUI();
        //disable btns
        gui.setStateBtnStart(false);
        gui.setStateBtnUpdate(false);
        gui.setStateBtnSettings(false);
        
        gui.setVisible(true);
        loadAnimes();
        cookies = new Parser().getCookies(gui);
        
        if(animeConfig.get(0) == null && cookies != null){
            JOptionPane.showMessageDialog(null, "No Config file availibaly", "Error: Function Main at line 3", JOptionPane.ERROR_MESSAGE);
        } else {
            gui.setStateBtnStart(true);
            gui.setStatus("Status: Cookies successfully loaded");
            startTimer(gui);            
        }

    }
    
    private static void _init(GUI gui) throws IOException{
        gui.clearTable();
        count = 0;
        animes.clear();
        String url = "http://horriblesubs.info/lib/latest.php?nextid=";
        int end = 15;
        for(int i=0; i <= end; i++){
            gui.setStatus("Status: Collecting " + i + "/" + end);
            collectAnimes(url + i, cookies);   
        }
        //Collections.sort(animes, comparator);
        output(gui); 
    }
    
    
    private static void collectAnimes(String url, Set<Cookie> cookies) throws IOException{
        Document doc = new Parser().request(url, cookies);
       
        if(doc == null) { return; }
        doc.select("table.release-info").remove();
        Elements release = doc.select("div.release-links");
        List<Element> lst = removeShit(release);
        
        lst.stream().forEach((Element r) -> {
            String tmp = r.select("td.dl-label").text();
            Anime ani = new Anime();
            ani.title = getTitle(tmp);
            ani.name = getName(tmp);
            ani.file = r.text().split("]")[0] + "].mkv"; 
            ani.DL = getUL(r);
            ani.ep = getEP(ani.title);
            animes.add(ani);
        });
    }
       
    private static void output(GUI gui){
        animes.stream().filter((anime) -> animeConfig.contains(anime.name)).forEach( (anime -> {
            Row r = new Row();
            r.anime = anime.name;
            r.ep = anime.ep;
            String downloadPath = downloadDir + "\\" + anime.name + "\\";
            if (checkFile(anime.file, downloadPath)) {
                if (anime.DL != null) {
                    r.status = "Downloading";
                    r.progress = "0 %";
                    updateTable(gui, r);
                    try {
                        if(download(downloadPath, anime.DL, gui) == 0){
                            count += 1;
                        } 
                    } catch (LoginFailedException ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error: " + ex.getClass(), JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    r.status = "No Downloadlinks available yet";
                    r.progress = " - ";
                    updateTable(gui, r);
                }
            } else {
                r.status = "already exists";
                r.progress = "-";
                updateTable(gui, r);
            }    
        }));
        JOptionPane.showMessageDialog(null, "Downloaded " + count + " Files.", "Finished", JOptionPane.INFORMATION_MESSAGE);
    }   
    
// TABLE-FUNC
    
    private static void updateTable(GUI gui, Row r){
        TableModel tb = gui.getTableModel();
        DefaultTableModel model = (DefaultTableModel) tb;
        model.addRow(new Object[] { r.anime, r.ep, r.status, r.progress });
        gui.setTableModel(model);
        gui.center();
    }
    
    private static void updateProgress(GUI gui, String progress){
        TableModel tb = gui.getTableModel();
        tb.setValueAt(progress, tb.getRowCount()-1, 3);
        gui.setTableModel(tb);
    }
    
    private static void updateStatus(GUI gui, String status){
        TableModel tb = gui.getTableModel();
        tb.setValueAt(status, tb.getRowCount()-1, 2);
        gui.setTableModel(tb);
    }
      
// Help-Functions
    
    private static void startTimer(GUI gui){
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if(gui.start != false) {
                            gui.start = false;
                            try {
                                _init(gui);
                            } catch (IOException ex) {
                                Logger.getLogger(Horrible.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }, 0, 1000);               
    }
    
    private static final Comparator<Anime> comparator = (Anime a1, Anime a2) -> {
        if(a1 != null && a2 != null){
            int c1 = a1.name.compareTo(a2.name);
            return c1 == 0 ? a1.ep.compareTo(a2.ep) : c1;
        } else {
            return -1;
        }
    };
    
    private static boolean checkFile(String file, String Path){
        return !(new File(Path + "[HorribleSubs] " + file).exists() || new File(Path + "[HorribleSubs]_" + file.replace(" ", "_")).exists());
    }
    
    private static String regex(String pattern, String text){
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        while(m.find()){
            return m.group(1);
        }
        return null;   
    }
    
// GET-Functions for ANIME

    private static String getUL(Element r){
        Elements links = r.select("a[href]");
        for(Element link : links){
            if(link.attr("href").contains("uploaded.net") || link.attr("href").contains("ul.to")){
                return link.attr("href");
            }
        }
        return null;
    }
    
    private static String getTitle(String e){      
        return e.split(" \\[")[0];
    }
    
    private static String getEP(String e){
        return regex("-.([0-9]{1,2})", e);
    }
    
    private static String getName(String e){
        String result = null;
        int lastindex = e.lastIndexOf("-");
        if (e.length() >= lastindex){
            if(e.charAt(lastindex-1) == " ".charAt(0)){
                result = e.substring(0, lastindex-1);
            } else {
                result =  e.substring(0, lastindex);
            } 
        }
        return result;
    }
    
// DOWMLOAD
         
    private static int download(String path, String link, GUI gui) throws LoginFailedException{
        String linkid = regex("\\/file\\/(.*?)$", link);
        List<String> acc = loadAccount();
        if(acc != null && acc.size() == 2){
            Uploaded up = new Uploaded(acc.get(0), acc.get(1));
            UPDownload dl = up.factoryDownload(path, linkid);
            dl.addUPDownloadProgressListener((UPDownload upDownload) -> {
                double percent = ((100.0 / upDownload.getContentLength()) * upDownload.getProgressByte());
                DecimalFormat df = new DecimalFormat("#.##");
                updateProgress(gui, String.valueOf(df.format(percent) + " %"));
            });
            try {
                dl.start();

            } catch (DownloadFailedException ex){
                //JOptionPane.showMessageDialog(null, ex.getMessage(), "Error: " + ex.getClass(), JOptionPane.ERROR_MESSAGE);
                updateStatus(gui, ex.getMessage());
                updateProgress(gui, " - ");
                return -1;
            }
            updateStatus(gui, "Finished Download");
            return 0;
        } else {
            return -1;
        }  
    } 
 
    private static List<String> loadAccount(){
        Properties prop = new Properties();
        List<String> result = new ArrayList<>();
        //InputStream input = null;
        String path = System.getProperty("user.dir") + "/src/horriblev3/account.file";
        if(new File(path).exists()){
            try(InputStream input = new FileInputStream(path)){
                prop.load(input);
                result.add(prop.getProperty("user"));
                result.add(prop.getProperty("password"));
            } catch (IOException ex){
                System.out.println(ex.getMessage());
            }
        }
        
        return result;
    }
// Remove Everthing but not 1080p  
    
    private static List<Element> removeShit(Elements e){
        List<Element> result = new ArrayList<>();
        e.stream().filter((item) -> (!(item.className().contains("480p") || item.className().contains("720p")))).forEach((item) -> {
            result.add(item);
        });
        return result;
    }
    
// Load Anime-Config
    
    private static void loadAnimes() throws IOException{
        String UserDir = System.getProperty("user.dir");
        String config = UserDir + "/src/horriblev3/settings/animes.xml";
        if((new File(config).exists())){
            List<String> list = new Parser().parseXml(config,"Anime");
            list.stream().forEach((e) -> {
                animeConfig.add(e);
                ckDir(e);
            });            
        } else {
            animeConfig.add(null);
        }
    } 
    
    private static void ckDir(String dir){
        Path tmp = new File(downloadDir + "\\" + dir).toPath();
        if (!Files.exists(tmp)){
            try {
                Files.createDirectory(tmp);
            } catch (IOException ex) {
                Logger.getLogger(Horrible.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private static void updateAnimes(String anime){
        String url = "http://horriblesubs.info/shows/" + anime.replace(" ", "-");
        //get show id
        
    }
 }