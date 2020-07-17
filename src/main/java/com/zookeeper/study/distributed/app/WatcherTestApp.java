package com.zookeeper.study.distributed.app;

import org.apache.logging.log4j.*;
import org.apache.zookeeper.*;

import java.io.*;

public class WatcherTestApp {
    private final ZooKeeper zooKeeper;
    private final String root;
    private final int count;
    private static final Logger logger = LogManager.getLogger(DistributedDoubleBarrierApp.class);

    public WatcherTestApp(ZooKeeper zooKeeper, String root, int count) {
        this.zooKeeper = zooKeeper;
        this.root = root;
        this.count = count;
    }

    private static class ChildWatcher implements Watcher {
        private final ZooKeeper zk;
        private final String root;

        public ChildWatcher(ZooKeeper zk, String root) {
            this.zk = zk;
            this.root = root;
        }
        @Override
        public void process(WatchedEvent event) {
            logger.info("Children watch start.");
            try {
                zk.getChildren(root, this);
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Children watch end.");
        }
    }

    public void test() {
        try {
            zooKeeper.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zooKeeper.getChildren(root, new ChildWatcher(zooKeeper, root));
            for (int i = 0; i < count; i++) {
                zooKeeper.create(root + "/child", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
                logger.info("Children created.");
            }
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            var zk = ZookeeperAppHelper.zookeeperInstance();
            var app = new WatcherTestApp(zk, "/parent", 10);
            app.test();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
