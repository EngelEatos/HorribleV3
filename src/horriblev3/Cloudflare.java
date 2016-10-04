/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package horriblev3;


import com.eclipsesource.v8.V8;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;


public class Cloudflare {
    final private String USERAGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:48.0) Gecko/20100101 Firefox/48.0";
    private String strUrl = null;

    public Cloudflare(String strUrl){
        this.strUrl = strUrl;
    }

    public List<HttpCookie> scrape() {
        if(strUrl == null){
            System.out.println("URL == NULL");
            return null;
        }
        try {
            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);

            URL url = new URL(strUrl);
            HttpURLConnection con;
            con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", USERAGENT);
            InputStream _is;
            if (con.getResponseCode() == 200) {
                return retrieveCookies(manager);
            } else {
                _is = con.getErrorStream();
                StringBuilder result = new StringBuilder();
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(_is))) {
                    String line;
                    while((line = rd.readLine()) != null){
                        result.append(line);
                    }
                }
                String source = result.toString();

                //extract challenge
                String challenge = regex(source, "name=\"jschl_vc\" value=\"(\\w+)\"");
                String challenge_pass = regex(source, "name=\"pass\" value=\"(.+?)\"");

                //prepare
                String builder = regex(source, "setTimeout\\(function\\(\\)\\{\\s+(var s,t,o,p,b,r,e,a,k,i,n,g,f.+?\\r?[\\s\\S]+?a\\.value =.+?)\\r?\\s+';");
                builder = builder.replaceFirst("\\s{3,}[a-z](?: = ).+form'\\);\\s+;", "").replaceFirst("a\\.value = parseInt\\(.+?\\).+", regex(builder, "a\\.value = (parseInt\\(.+?\\)).+"));

                //Execute&solve
                System.out.println(builder);
                long solved = Long.parseLong(solveJS2(builder));
                solved += url.getHost().length();
                System.out.println("SOLVED: " + solved);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }

                URI tmp = UrlToUri(url);							
                List<NameValuePair> qparams = new ArrayList<>();
                qparams.add(new BasicNameValuePair("jschl_vc", challenge));
                qparams.add(new BasicNameValuePair("jschl_answer", String.valueOf(solved)));
                qparams.add(new BasicNameValuePair("pass", challenge_pass));
                URIBuilder uriBuilder = new URIBuilder().setScheme(tmp.getScheme()).setPath("/cdn-cgi/l/chk_jschl").setHost(tmp.getHost()).setParameters(qparams);

                HttpURLConnection cookie_req = (HttpURLConnection) new URL(uriBuilder.toString()).openConnection();
                cookie_req.setRequestProperty("Referer", url.toString());
                cookie_req.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:48.0) Gecko/20100101 Firefox/48.0");
                HttpURLConnection.setFollowRedirects(false);	
                cookie_req.connect();

                System.out.println("ResponseCode: " + cookie_req.getResponseCode());
                if(cookie_req.getResponseCode() == 200){			    
                    return retrieveCookies(manager);
                } else {
                    System.out.println("Something went wrong!");
                    return null;
                }
            }
        } catch (IOException e1) {
                System.out.println(e1.getMessage());
                return null;
        }			
    }

    private URI UrlToUri(URL url){
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            return null;
        }
    }
    private List<HttpCookie> retrieveCookies(CookieManager manager){
        java.net.CookieStore cookieJar = manager.getCookieStore();
        return cookieJar.getCookies();
    }

    private String solveJS2(String script){
        V8 runtime = V8.createV8Runtime();
        int result = runtime.executeIntegerScript(script);
        runtime.release();
        return String.valueOf(result);
    }

    private String regex(String text, String regex){
        System.out.println("TEXT: " + text + "\n" + "REGEX: " + regex);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
                System.out.println("Full match: " + matcher.group(0));
            for (int i = 1; i <= matcher.groupCount(); i++) {
                System.out.println("Group " + i + ": " + matcher.group(i));
            }
            if(matcher.groupCount() >= 1){
                return matcher.group(1);
            }
        }
        return null;        
    }	
}