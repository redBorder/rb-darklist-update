package net.redborder.darklistupdate.managers;

import net.redborder.darklistupdate.utils.http.HttpURLs;
import net.redborder.darklistupdate.utils.logger.RbLogger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.util.logging.Logger;

/**
 * Created by andresgomez on 18/2/15.
 */
public class HttpManager {

    static Logger log = RbLogger.getLogger(HttpManager.class.getName());

    public synchronized static Integer currentRevision() {
        Integer rev = 0;

        try {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(HttpURLs.URL + HttpURLs.CURRENT_REVISION);

            HttpResponse response = httpclient.execute(httppost);

            HttpEntity entity = response.getEntity();

            InputStream instream = entity.getContent();

            String theString = IOUtils.toString(instream).trim();

            rev = Integer.valueOf(theString.trim());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rev;
    }

    public synchronized static BufferedReader allData() {
        BufferedReader buffer = null;

        try {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(HttpURLs.URL + HttpURLs.ALL_DATA);

            System.out.println("Downloading all list ...");
            HttpResponse response = httpclient.execute(httppost);

            HttpEntity entity = response.getEntity();

            InputStream instream = entity.getContent();

            Reader readerCsv = new InputStreamReader(instream);
            buffer = new BufferedReader(readerCsv);
            System.out.println("Downloaded all list!");


        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer;
    }

    public synchronized static BufferedReader revData(Integer rev) {
        BufferedReader buffer = null;

        try {
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(HttpURLs.URL + HttpURLs.ONE_REVISION + rev);

            System.out.println("Downloading incremental list ...");

            HttpResponse response = httpclient.execute(httppost);

            HttpEntity entity = response.getEntity();

            InputStream instream = entity.getContent();

            Reader readerCsv = new InputStreamReader(instream);
            buffer = new BufferedReader(readerCsv);

            System.out.println("Downloaded incremental list!");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer;
    }
}
