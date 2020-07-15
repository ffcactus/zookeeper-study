package com.zookeeper.study;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.*;

import java.io.*;
import java.util.concurrent.*;

/**
 * Create a node named "/mynode" and assign string "HelloWorld" to it.
 */
public class HelloWorld {
    private static final String HOSTS = "192.168.81.128:2181,192.128.81.168:2182,192.168.81.128:2183";
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
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            try (var zk = new ZooKeeper(HOSTS, SESSION_TIMEOUT, new HelloWorldWatcher(countDownLatch))) {
                countDownLatch.await();

                if (zk.exists(NODE_PATH, null) == null) {
                    System.err.println("Node path not exists " + NODE_PATH);
                    zk.create(NODE_PATH, NODE_CONTENT.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
                Stat stat = new Stat();
                var bs = zk.getData(NODE_PATH, false, stat);
                System.out.println("Node content is " + new String(bs));

            }
        } catch (IOException | KeeperException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

}
