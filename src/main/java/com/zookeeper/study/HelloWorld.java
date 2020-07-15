package com.zookeeper.study;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Create a node named "/mynode" and assign string "HelloWorld" to it.
 */
public class HelloWorld {
    private static final String PROPERTIES_FILE = "zookeeper.properties";
    private static final String ZOOKEEPER_1_HOSTNAME = "zookeeper.1.hostname";
    private static final String ZOOKEEPER_2_HOSTNAME = "zookeeper.2.hostname";
    private static final String ZOOKEEPER_3_HOSTNAME = "zookeeper.3.hostname";
    private static final int SESSION_TIMEOUT = 10 * 1000;
    private static final String NODE_PATH = "/mynode";
    private static final String NODE_CONTENT = "HelloWorld";

    static class HelloWorldWatcher implements Watcher {
        private final CountDownLatch countDownLatch;

        HelloWorldWatcher(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void process(WatchedEvent event) {
            if (Event.KeeperState.SyncConnected.equals(event.getState())) {
                System.out.println("Zookeeper connected. " + event);
                countDownLatch.countDown();
            }
        }
    }

    public static void main(String[] args) {
        var countDownLatch = new CountDownLatch(1);
        var prop = new Properties();
        try (var inputStream = HelloWorld.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (inputStream == null) {
                throw new FileNotFoundException(PROPERTIES_FILE);
            }

            prop.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return;
        }
        var hosts = prop.getProperty(ZOOKEEPER_1_HOSTNAME) + ","
                + prop.getProperty(ZOOKEEPER_2_HOSTNAME) + ","
                + prop.getProperty(ZOOKEEPER_3_HOSTNAME);
        try (var zk = new ZooKeeper(hosts, SESSION_TIMEOUT, new HelloWorldWatcher(countDownLatch))) {
            countDownLatch.await();
            if (zk.exists(NODE_PATH, null) == null) {
                System.err.println("Node path not exists " + NODE_PATH);
                zk.create(NODE_PATH, NODE_CONTENT.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            Stat stat = new Stat();
            var bs = zk.getData(NODE_PATH, false, stat);
            System.out.println("Node content is " + new String(bs));
        } catch (IOException | KeeperException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();

        }
    }
}