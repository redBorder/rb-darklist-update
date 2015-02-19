package net.redborder.darklistupdate;


import net.redborder.darklistupdate.managers.GridGainManager;
import net.redborder.darklistupdate.managers.HttpManager;
import net.redborder.darklistupdate.utils.ConfigFile;
import net.redborder.darklistupdate.managers.ZkManager;
import net.redborder.taskassigner.ZkTasksHandler;
import org.gridgain.grid.GridException;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.FileNotFoundException;
import java.util.Date;


/**
 * Created by andresgomez on 17/2/15.
 */
public class DarklistService {

    static final long INTERVAL = 600 * 1000L;
    static ZkTasksHandler tasksHandler;
    static UpdaterService updater;
    static Boolean running;


    public static void init() throws FileNotFoundException, GridException {
        ConfigFile.init();
        ZkManager.init();
        GridGainManager.init();
        running = true;
        tasksHandler = new ZkTasksHandler(ConfigFile.getInstance().getZkConnect(), "/rb_darklist");
        updater = new UpdaterService();
        tasksHandler.addListener(updater);
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

    public static void main(String[] args) {
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
                System.out.println("Next update at: " + new Date(System.currentTimeMillis()+INTERVAL));
                try {
                    Thread.sleep(INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
