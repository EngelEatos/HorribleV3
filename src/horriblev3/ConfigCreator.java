package horriblev3;

import com.gargoylesoftware.htmlunit.util.Cookie;
import java.io.IOException;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author EngelEatos
 */
public class ConfigCreator {
    private final Set<Cookie> cookies;
    
    public ConfigCreator(Set<Cookie> cookies) {
        this.cookies = cookies;
    }
    
    public void start() throws IOException{
        String url = "http://horriblesubs.info/current-season/";
        Document doc = new Parser().request(url, cookies);
        Elements items = doc.select("div.ind-show.linkful");
        for(Element item : items){
            System.out.println(item.select("a[href]").attr("title"));
        }
    }
}
