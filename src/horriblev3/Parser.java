package horriblev3;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;
import com.gargoylesoftware.htmlunit.util.Cookie;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;

/**
 *
 * @author EngelEatos
 */
public class Parser {
    public Document request(String url, Set<Cookie> cookies) throws IOException{
        try {
            Document result = null;

            Connection con = Jsoup.connect(url)
               .ignoreContentType(true)
               .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64; rv:44.0) Gecko/20100101 Firefox/44.0")
               .header("Host", "horriblesubs.info")
               .header("Accept", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
               .header("Accept-Language", "de,en-US;q=0.7,en;q=0.3")
               .header("Accept-Encoding", "gzip, deflate")
               .timeout(60000)
               .followRedirects(true);
            con = setCookies(cookies, con);
            Connection.Response response = con.execute();

            if(response.statusCode() == 200){
                result = con.get();
            } else {
                System.out.println(response.statusCode() + ": " + response.statusMessage());
            }

            return result;
        } catch (HttpStatusException ex) {
            System.out.println(ex.getMessage() + ": " + ex.getStatusCode());
            return null;
        }
    }
    public List<String> parseXml(String path, String attribute) throws IOException{
        String xmlfile = Files.lines(new File(path).toPath()).collect(joining());
        Document xml = Jsoup.parse(xmlfile);
        Elements es = xml.select(attribute);
        List<String> list = new ArrayList<>();
        es.stream().forEach((e) -> {
            list.add(e.text());
        });
        return list;
    }
    private Connection setCookies(Set<Cookie> cookies, Connection con){
        Iterator<Cookie> i = cookies.iterator();
        
        while(i.hasNext()){
            Cookie c = i.next();
            con.cookie(c.getName(), c.getValue());
        }
        return con;
    }
    public Set<Cookie> getCookies(GUI gui) throws IOException, ParseException{
        String UserDir = System.getProperty("user.dir");
        File config = new File(UserDir + "/src/horriblev3/cookie.file");
        File date = new File(UserDir + "/src/horriblev3/date.file");
        
        if(config.exists() && date.exists()){
            if(expired(date)){
                gui.setStatus("Status: loading cookies");
                Set<Cookie> cookies = null;
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(config))) {
                    cookies = (Set<Cookie>) in.readObject();
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(cookies != null){
                    return cookies;
                } else {
                    return null;
                }
            }
        }
        gui.setStatus("Status: create new Cookies");
        Set<Cookie> cookies = rq();
        if(cookies != null){
            return cookies;
        } else {
            return null;
        }
        
    }
    
    private boolean expired(File date){
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
            Date stamp = dateFormat.parse(new String(Files.readAllBytes(date.toPath()), Charset.forName("UTF-8")));
            Date now = dateFormat.parse(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            long hours = (now.getTime() - stamp.getTime()) / (60*60*1000);
            return (hours+1<7);
        } catch (ParseException | IOException ex) {
            Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    private Set<Cookie> rq() throws IOException{
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
            java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF); 
            java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

            WebClient webClient = new WebClient(BrowserVersion.CHROME);
            webClient.getOptions().setCssEnabled(false);

            webClient.setIncorrectnessListener((String arg0, Object arg1) -> {});

            webClient.setCssErrorHandler(new ErrorHandler() {

                @Override
                public void warning(CSSParseException exception) throws CSSException {
                    // TODO Auto-generated method stub

                }

                @Override
                public void fatalError(CSSParseException exception) throws CSSException {
                    // TODO Auto-generated method stub

                }

                @Override
                public void error(CSSParseException exception) throws CSSException {
                    // TODO Auto-generated method stub

                }
            });
            webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {

                @Override
                public void scriptException(InteractivePage ip, com.gargoylesoftware.htmlunit.ScriptException se) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void timeoutError(InteractivePage ip, long l, long l1) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void malformedScriptURL(InteractivePage ip, String string, MalformedURLException murle) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void loadScriptError(InteractivePage ip, java.net.URL url, Exception excptn) {
                    // TODO Auto-generated method stub
                }
            });
            webClient.setHTMLParserListener(new HTMLParserListener() {

                @Override
                public void error(String string, java.net.URL url, String string1, int i, int i1, String string2) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void warning(String string, java.net.URL url, String string1, int i, int i1, String string2) {
                    // TODO Auto-generated method stub.
                }

            });

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            String url = "http://horriblesubs.info/";
        try {
            HtmlPage htmlPage = webClient.getPage(url);
        } catch (IOException | FailingHttpStatusCodeException ex) {
            Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
        }
            webClient.waitForBackgroundJavaScript(10_000);
            //System.out.println(htmlPage.asText())
            
            Set<Cookie> cookies = webClient.getCookieManager().getCookies();
            String UserDir = System.getProperty("user.dir");
            File config = new File(UserDir + "/src/horriblev3/cookie.file");
            File date = new File(UserDir + "/src/horriblev3/date.file");
            //generate timestamp
            String timestamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
            //write to file
            Files.write(date.toPath(), timestamp.getBytes());
            try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream(config))) {
                out.writeObject(cookies);
            }
            return cookies;
    }
}

