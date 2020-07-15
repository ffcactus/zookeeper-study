package com.zookeeper.study.distributed.app;

import com.zookeeper.study.*;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Helper class for Zookeeper Application.
 */
public class ZookeeperAppHelper {
    private static final String PROPERTIES_FILE = "zookeeper.properties";
    private static final String ZOOKEEPER_1_HOSTNAME = "zookeeper.1.hostname";
    private static final String ZOOKEEPER_2_HOSTNAME = "zookeeper.2.hostname";
    private static final String ZOOKEEPER_3_HOSTNAME = "zookeeper.3.hostname";
    private static final int SESSION_TIMEOUT = 100 * 1000;

    public static ZooKeeper zookeeperInstance() throws IOException, InterruptedException {
        var countDownLatch = new CountDownLatch(1);
        var prop = new Properties();
        try (var inputStream = HelloWorld.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (inputStream == null) {
                throw new FileNotFoundException(PROPERTIES_FILE);
            }
            prop.load(inputStream);
        }
        var hosts = prop.getProperty(ZOOKEEPER_1_HOSTNAME) + ","
                + prop.getProperty(ZOOKEEPER_2_HOSTNAME) + ","
                + prop.getProperty(ZOOKEEPER_3_HOSTNAME);
        var zk = new ZooKeeper(hosts, SESSION_TIMEOUT, event -> {
            if (Watcher.Event.KeeperState.SyncConnected.equals(event.getState())) {
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        return zk;

    }
}
