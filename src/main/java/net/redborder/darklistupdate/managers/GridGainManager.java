package net.redborder.darklistupdate.managers;

import net.redborder.darklistupdate.utils.ConfigFile;
import net.redborder.darklistupdate.utils.logger.RbLogger;
import org.gridgain.grid.Grid;
import org.gridgain.grid.GridConfiguration;
import org.gridgain.grid.GridException;
import org.gridgain.grid.GridGain;
import org.gridgain.grid.cache.GridCache;
import org.gridgain.grid.cache.GridCacheConfiguration;
import org.gridgain.grid.cache.GridCacheDistributionMode;
import org.gridgain.grid.cache.GridCacheMode;
import org.gridgain.grid.spi.discovery.tcp.GridTcpDiscoverySpi;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.GridTcpDiscoveryVmIpFinder;
import org.ho.yaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by andresgomez on 18/2/15.
 */
public class GridGainManager {

    private static List<String> _gridGainServers;
    private static Grid grid;

    public static void init() throws GridException {
        Map<String, Object> gridGainConfig = ConfigFile.getInstance().getCacheConfig();

        if (!gridGainConfig.containsKey("s3")) {
            _gridGainServers = (List<String>) gridGainConfig.get("servers");
        }

        grid = GridGain.start(initConfig());
    }

    public static void end() {
        try {
            grid.close();
        } catch (GridException e) {
            e.printStackTrace();
        }
    }

    private static GridConfiguration initConfig() {
        GridConfiguration conf = new GridConfiguration();
        List<GridCacheConfiguration> caches = new ArrayList<GridCacheConfiguration>();
        GridTcpDiscoverySpi gridTcp = new GridTcpDiscoverySpi();

        GridTcpDiscoveryVmIpFinder gridIpFinder = new GridTcpDiscoveryVmIpFinder();

        Collection<InetSocketAddress> ips = new ArrayList<>();

        try {
            conf.setLocalHost(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        if (_gridGainServers != null) {
            for (String server : _gridGainServers) {
                String[] serverPort = server.split(":");
                ips.add(new InetSocketAddress(serverPort[0], Integer.valueOf(serverPort[1])));
            }

            gridIpFinder.registerAddresses(ips);
        }

        gridTcp.setIpFinder(gridIpFinder);


        conf.setDiscoverySpi(gridTcp);

        GridCacheConfiguration cacheMobile = new GridCacheConfiguration();
        cacheMobile.setName("darklist");
        cacheMobile.setDistributionMode(GridCacheDistributionMode.CLIENT_ONLY);
        cacheMobile.setCacheMode(GridCacheMode.PARTITIONED);
        caches.add(cacheMobile);

        conf.setCacheConfiguration(caches.toArray(new GridCacheConfiguration[caches.size()]));

        return conf;
    }

    public static void allForYou(List<String[]> data) {

        System.out.println("Processing all list ...");

        List<Map> dataToSave = new ArrayList<>();
        List<String> keysToSave = new ArrayList<>();

        for (int i = 1; i < data.size(); i++) {

            String[] nextLine = data.get(i);

            Map<String, Object> map = new HashMap<>();

            Integer score = Integer.valueOf(nextLine[1]) * Integer.valueOf(nextLine[2]) * 2;

            map.put("darklist_score", score);
            map.put("darklist_score_name", giveMeScore(score));
            map.put("darklist_category", nextLine[3]);

            keysToSave.add(nextLine[0]);
            dataToSave.add(map);

        }

        System.out.println("Processed all list!");


        GridCache<String, Map<String, Object>> cache = grid.cache("darklist");

        try {
            System.out.println("Saving ... ");

            cache.clearAll();

            Map<String, Map<String, Object>> mapToSave = new HashMap<String, Map<String, Object>>();
            Integer size = dataToSave.size();

            for (int i = 0; i < dataToSave.size(); i++) {
                mapToSave.put(keysToSave.get(i), dataToSave.get(i));
            }
            System.out.println("Saved: " + mapToSave.size());

            cache.putAll(mapToSave);
        } catch (GridException e) {
            e.printStackTrace();
            System.out.println("Can't save!");
        }

    }


    public static void incrementalForYou(List<String[]> data) {
        if (!data.isEmpty()) {
            System.out.println("Processing incremental list ...");

            List<Map> dataToSave = new ArrayList<>();
            List<String> keysToSave = new ArrayList<>();
            List<String> keysToDelete = new ArrayList<>();

            for (int i = 0; i < data.size(); i++) {

                String[] nextLine = data.get(i);

                if (nextLine[0].charAt(0) == '-') {
                    keysToDelete.add(nextLine[0].substring(1, nextLine[0].length()));
                } else {
                    Map<String, Object> map = new HashMap<>();

                    Integer score = Integer.valueOf(nextLine[1]) * Integer.valueOf(nextLine[2]) * 2;

                    map.put("darklist_score", score);
                    map.put("darklist_score_name", giveMeScore(score));
                    map.put("darklist_category", nextLine[3]);


                    keysToSave.add(nextLine[0].substring(1, nextLine[0].length()));
                    dataToSave.add(map);
                }
            }

            System.out.println("Processed incremental list!");

            GridCache<String, Map<String, Object>> cache = grid.cache("darklist");


            if (keysToDelete.size() > 0) {
                try {
                    System.out.println("Deleting ... ");
                    cache.removeAll(keysToDelete);
                    System.out.println("Deleted: " + keysToDelete.size());
                } catch (Exception ex) {
                    System.out.println("Can't delete!");
                }
            }

            try {
                System.out.println("Saving ... ");

                Map<String, Map<String, Object>> mapToSave = new HashMap<String, Map<String, Object>>();
                Integer size = dataToSave.size();

                for (int i = 0; i < size; i++) {
                    mapToSave.put(keysToSave.get(i), dataToSave.get(i));
                }

                cache.putAll(mapToSave);
                System.out.println("Saved: " + mapToSave.size());
            } catch (GridException e) {
                System.out.println("Can't save!");
                e.printStackTrace();
            }
        } else {
            System.out.println("Revision is empty!");
        }
    }

    private static String giveMeScore(Integer score) {
        String score_name = "";
        if (100 >= score && score > 95) {
            score_name = "very high";
        } else if (95 >= score && score > 85) {
            score_name = "high";
        } else if (85 >= score && score > 70) {
            score_name = "medium";
        } else if (70 >= score && score > 50) {
            score_name = "low";
        } else if (50 >= score && score >= 0) {
            score_name = "very low";
        }
        return score_name;
    }
}
