package net.redborder.darklistupdate;


import net.redborder.darklistupdate.managers.GridGainManager;
import net.redborder.darklistupdate.managers.HttpManager;
import net.redborder.darklistupdate.utils.ConfigFile;
import net.redborder.darklistupdate.utils.Utils;
import net.redborder.darklistupdate.managers.ZkManager;
import net.redborder.taskassigner.ZkTasksHandler;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.ObjectMapper;
import org.gridgain.grid.GridException;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;


/**
 * Created by andresgomez on 17/2/15.
 */
public class DarklistService {

    static long INTERVAL;
    static ZkTasksHandler tasksHandler;
    static UpdaterService updater;
    static Boolean running;


    public static void init() throws FileNotFoundException, GridException {
        ConfigFile.init();
        ZkManager.init();
        GridGainManager.init();
        running = true;
        tasksHandler = new ZkTasksHandler(ConfigFile.getInstance().getZkConnect(), "/rb-darklist");
        updater = new UpdaterService();
        tasksHandler.addListener(updater);
        INTERVAL = ConfigFile.getInstance().getFromGeneral("interval") != null ? Integer.valueOf((Integer) ConfigFile.getInstance().getFromGeneral("interval")).longValue() * 60 * 1000L : 600 * 1000L;
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void end() {
        tasksHandler.end();
        GridGainManager.end();
        ZkManager.end();
        running = false;
    }

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            try {

                init();

                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        System.out.println("Exiting...");
                        end();
                    }
                });

                // Add signal to reload config
                Signal.handle(new Signal("HUP"), new SignalHandler() {
                    public void handle(Signal signal) {
                        System.out.println("Reload received!");
                        tasksHandler.reload();
                        System.out.println("Reload finished!");
                    }
                });


                while (running) {
                    tasksHandler.goToWork(true);
                    System.out.println("Next update at: " + new Date(System.currentTimeMillis() + INTERVAL));
                    try {
                        Thread.sleep(INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            List<String[]> data = UpdaterService.prepareData(HttpManager.allData());
            List<Map<String, Object>> toFile = new ArrayList<>();

            for (int i = 1; i < data.size(); i++) {

                String[] nextLine = data.get(i);

                Map<String, Object> map = new HashMap<>();

                Integer score = Integer.valueOf(nextLine[1]) * Integer.valueOf(nextLine[2]) * 2;

                map.put("darklist_score", score);
                map.put("darklist_score_name", Utils.giveMeScore(score));
                map.put("darklist_category", nextLine[3]);

                Map<String, Object> pair = new HashMap<>();
                pair.put("ip", nextLine[0]);
                pair.put("enrich_with", map);
                toFile.add(pair);
            }

            ObjectMapper mapper = new ObjectMapper();
            PrintWriter writer = new PrintWriter(args[0], "UTF-8");
            writer.print(mapper.writeValueAsString(toFile));
            writer.flush();
            writer.close();

            System.out.println("You json darklist is on: " + args[0]);

        }
    }
}
