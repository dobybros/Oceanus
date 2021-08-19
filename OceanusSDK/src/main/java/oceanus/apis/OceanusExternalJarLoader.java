package oceanus.apis;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class OceanusExternalJarLoader {
    private String url;
    private String downloadPath = "./OceanusExternalJar/oceanus.jar";

    public OceanusExternalJarLoader(String url) {
        this.url = url;
    }

    public void loadExternalJar() {

        try {
            URL theUrl = new URL(url);
            URLConnection urlConnection = theUrl.openConnection();
            urlConnection.connect();
//            urlConnection.getHeaderField()
            OutputStream os = urlConnection.getOutputStream();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
