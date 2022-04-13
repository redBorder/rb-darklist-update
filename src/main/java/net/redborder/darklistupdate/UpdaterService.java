package net.redborder.darklistupdate;

import net.redborder.darklistupdate.managers.GridGainManager;
import net.redborder.darklistupdate.managers.HttpManager;
import net.redborder.darklistupdate.managers.ZkManager;
import net.redborder.darklistupdate.utils.logger.RbLogger;
import net.redborder.clusterizer.NotifyListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by andresgomez on 18/2/15.
 */
public class UpdaterService implements NotifyListener {

    static Logger log = RbLogger.getLogger(UpdaterService.class.getName());

    @Override
    public void time2Work() {
        Integer myRev = ZkManager.queryRevision();
        Integer currentRev = HttpManager.currentRevision();
        boolean fullData;

        System.out.println("MyRev: " + myRev + " CurrentRev: " + currentRev);
        if (myRev != null) {
            if (currentRev.equals(myRev)) {
                System.out.println("Incremental update -->");
                BufferedReader revData = HttpManager.revData(myRev-1);
                GridGainManager.incrementalForYou(prepareData(revData));
            } else {
                ZkManager.updateRevision(currentRev);
            }
            fullData = false;
        } else {
            fullData = true;
        }

        if (fullData) {
            System.out.println("Full update -->");
            BufferedReader data = HttpManager.allData();
            GridGainManager.allForYou(prepareData(data));
            ZkManager.updateRevision(currentRev);
        }

        System.out.println("\n");
    }

    public static List<String[]> prepareData(BufferedReader data) {
        System.out.println("Preparing data ...");
        List<String[]> csvAll;
        try {
            String aline;
            List<String> toParse = new ArrayList<>();

            while ((aline = data.readLine()) != null) {
                toParse.add(aline);
            }

            csvAll = csvAll(toParse);

        } catch (IOException ex) {
            ex.printStackTrace();
            csvAll = null;
        }

        try {
            data.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Prepared data!");

        return csvAll;
    }

    private static List<String[]> csvAll(List<String> toParse) throws IOException {
        List<String[]> csvAll = new ArrayList<String[]>();
        for (String aline : toParse) {
            String[] csv = aline.split("#");
            csvAll.add(csv);
        }

        return csvAll;
    }
}
