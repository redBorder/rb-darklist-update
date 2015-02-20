/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.redborder.darklistupdate.utils;

import org.ho.yaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * @author andresgomez
 */
public class ConfigFile {

    private static ConfigFile theInstance = null;
    private static final Object initMonitor = new Object();
    private final String CONFIG_FILE_PATH = "/opt/rb/var/rb-darklist/conf/darklist_config.yml";
    private Map<String, Object> _general;
    private Map<String, Object> _gridgain;

    public static ConfigFile getInstance() {
        if (theInstance == null) {
            synchronized (initMonitor) {
                try {
                    while (theInstance == null) {
                        initMonitor.wait();
                    }
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
        return theInstance;
    }

    public static void init() throws FileNotFoundException {
        synchronized (initMonitor) {
            if (theInstance == null) {
                theInstance = new ConfigFile();
                initMonitor.notifyAll();
            }
        }
    }

    /**
     * Constructor
     */
    public ConfigFile() throws FileNotFoundException {
        reload();
    }

    public void reload() throws FileNotFoundException {
        Map<String, Object> map = (Map<String, Object>) Yaml.load(new File(CONFIG_FILE_PATH));

         /* General Config */
        _general = (Map<String, Object>) map.get("general");
        _gridgain = (Map<String, Object>) map.get("gridgain");
    }


    public String getZkConnect() {
        return (String) getFromGeneral("zk_connect");
    }

    public Map<String, Object> getCacheConfig(){
        return _gridgain;
    }


    /**
     * Getter.
     *
     * @param property Property to read from the general section
     * @return Property read
     */

    public <T> T getFromGeneral(String property) {
        T ret = null;

        if (_general != null) {
            ret = (T) _general.get(property);
        }

        return ret;
    }
}
