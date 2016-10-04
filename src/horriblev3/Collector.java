package horriblev3;

import com.gargoylesoftware.htmlunit.util.Cookie;
import java.io.File;
import java.io.IOException;
import java.net.HttpCookie;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import to.uploaded.exception.LoginFailedException;

/**
 *
 * @author EngelEatos
 */
public class Collector  {
    private List<HttpCookie> cookies;
    private List<Anime> animes = new ArrayList<>();
    private String downloadDir = "D:\\Horrible\\";
    int count = 0;
    
    public List<Anime> get() throws IOException, ParseException{
        animes.clear();
        String url = "http://horriblesubs.info/lib/latest.php?nextid=";
        int limit = 15;
        //cookies = new Parser().getCookies();
        
        for(int i=0; i <= limit; i++){
            collectAnimes(url + i, cookies);   
        }
        //Collections.sort(animes, comparator);
        //remove
        animes.stream().forEach(anime -> {
            
            String downloadPath = downloadDir + "\\" + anime.name + "\\";
            if (checkFile(anime.file, downloadPath)) {
                if (anime.DL != null) {
//                    try {
//                        if(download(downloadPath, anime.DL) == 0){
//                            count += 1;
//                        } 
//                    } catch (LoginFailedException ex) {
//                        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error: " + ex.getClass(), JOptionPane.ERROR_MESSAGE);
//                    }
                } else {
//                    r.status = "No Downloadlinks available yet";
//                    r.progress = " - ";
//                    updateTable(gui, r);
                }
            } else {
//                r.status = "already exists";
//                r.progress = "-";
//                updateTable(gui, r);
            }
        });
        return animes;

    }
    
    private static boolean checkFile(String file, String Path){
        return !(new File(Path + "[HorribleSubs] " + file).exists() || new File(Path + "[HorribleSubs]_" + file.replace(" ", "_")).exists());
    }
        
    private void collectAnimes(String url, List<HttpCookie> cookies) throws IOException{
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
    
    private List<Element> removeShit(Elements e){
        List<Element> result = new ArrayList<>();
        e.stream().filter((item) -> (!(item.className().contains("480p") || item.className().contains("720p")))).forEach((item) -> {
            result.add(item);
        });
        return result;
    }
    
    // GET-Functions for ANIME

    private String getUL(Element r){
        Elements links = r.select("a[href]");
        for(Element link : links){
            if(link.attr("href").contains("uploaded.net") || link.attr("href").contains("ul.to")){
                return link.attr("href");
            }
        }
        return null;
    }
    
    private String getTitle(String e){      
        return e.split(" \\[")[0];
    }
    
    private String getEP(String e){
        return regex("-.([0-9]{1,2})", e);
    }
    
    private static String regex(String pattern, String text){
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        while(m.find()){
            return m.group(1);
        }
        return null;   
    }
    
    private String getName(String e){
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
}
