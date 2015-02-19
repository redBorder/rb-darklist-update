package net.redborder.darklistupdate.managers;

import net.redborder.darklistupdate.utils.ConfigFile;
import net.redborder.darklistupdate.utils.logger.RbLogger;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andresgomez on 18/2/15.
 */
public class ZkManager {

    private static InterProcessSemaphoreMutex mutex;
    private static CuratorFramework client;
    private static Logger log = RbLogger.getLogger(ZkManager.class.getName());


    public static void init() {
        RetryPolicy retryPolicy = new RetryNTimes(Integer.MAX_VALUE, 30000);
        client = CuratorFrameworkFactory.newClient(ConfigFile.getInstance().getZkConnect(), retryPolicy);
        client.start();
        mutex = new InterProcessSemaphoreMutex(client, "/rb_darklist/mutex");
    }

    public static void end(){
        try {
            mutex.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        client.close();
    }

    public synchronized static void updateRevision(Integer revision) {
        try {
            mutex.acquire();
            Integer rev;
            if (client.checkExists().forPath("/rb_darklist/revision") == null) {
                client.create().withMode(CreateMode.EPHEMERAL).forPath("/rb_darklist/revision", String.valueOf(revision).getBytes());
                rev = revision;
            } else {

                Integer tmpRevision = Integer.valueOf(new String(client.getData().forPath("/rb_darklist/revision"), "UTF-8"));


                if (tmpRevision < revision) {
                    client.setData().forPath("/rb_darklist/revision", String.valueOf(revision).getBytes());
                    rev = revision;
                } else {
                    rev = tmpRevision;
                }

            }

            System.out.println("Revision update, new revision is " + rev);

            mutex.release();
        } catch (Exception ex) {
            System.out.println("Update revision failed. ");
            ex.printStackTrace();
        }
    }


    public synchronized static Integer queryRevision() {
        Integer revision = null;
        try {
            mutex.acquire();
            if (client.checkExists().forPath("/rb_darklist/revision") != null)
                revision = Integer.valueOf(new String(client.getData().forPath("/rb_darklist/revision"), "UTF-8"));
            mutex.release();
        } catch (Exception ex) {
            System.out.println("Query revision failed. ");
            ex.printStackTrace();
        }

        return revision;
    }

}
