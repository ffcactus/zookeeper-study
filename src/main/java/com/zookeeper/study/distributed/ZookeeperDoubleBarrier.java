package com.zookeeper.study.distributed;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.*;

import java.io.*;

/**
 * Zookeeper implementation of {@link DistributedDoubleBarrier}. Each threads, no matter if it is from the same application,
 * have to create the instance with the same constructor parameters.
 */
public class ZookeeperDoubleBarrier implements DistributedDoubleBarrier {
    private final ZooKeeper zookeeper;
    private final String path;
    private final int count;

    public ZookeeperDoubleBarrier(ZooKeeper zookeeper, String path, int count) {
        this.zookeeper = zookeeper;
        this.path = path;
        this.count = count;
    }

    /**
     * Initialize the DoubleBarrier. If this DoubleBarrier is considered initialized, nothing will happen.
     */
    public void init() throws InterruptedException, KeeperException {
        try {
            zookeeper.create(path, Integer.toString(count).getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException e) {
            if (!e.code().equals(KeeperException.Code.NODEEXISTS)) {
                throw e;
            }
        }
    }

    public void enter() throws KeeperException, InterruptedException {
        zookeeper.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

    }

    public void leave() {

    }

    /**
     * Release the barrier.
     *
     * @throws IOException Generally this operation involves IO operation, so IOException may be thrown.
     */
    @Override
    public void release() throws IOException {

    }
}
