package com.zookeeper.study.distributed;

import org.apache.logging.log4j.*;
import org.apache.zookeeper.*;

/**
 * Zookeeper implementation of {@link DistributedDoubleBarrier}. Each threads, no matter if it is from the same
 * application, have to create the instance with the same constructor parameters.
 */
public class ZookeeperDoubleBarrier implements DistributedDoubleBarrier, Watcher {
    private static final Logger logger = LogManager.getLogger(ZookeeperDoubleBarrier.class);
    private final ZooKeeper zookeeper;
    private final String path;
    private final int count;
    private String ephemeralNode;
    private static final Object mutex = new Object();

    /**
     * Create a double barrier by using a Zookeeper service.
     *
     * @param zookeeper zookeeper service. User have to close it manually.
     * @param path      the path of the root node for this double barrier, have to end with '/'.
     * @param count     the minimal procedure for this barrier to open.
     */
    public ZookeeperDoubleBarrier(ZooKeeper zookeeper, String path, int count) {
        this.zookeeper = zookeeper;
        this.zookeeper.register(this);
        this.path = path;
        this.count = count;
    }

    /**
     * Initialize the DoubleBarrier. If this DoubleBarrier is considered initialized, nothing will happen.
     *
     * @throws InterruptedException If the transaction is interrupted.
     */
    public void init() throws InterruptedException {
        try {
            zookeeper.create(path, Integer.toString(count).getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException e1) {
            if (!e1.code().equals(KeeperException.Code.NODEEXISTS)) {
                throw new IllegalStateException(e1);
            }
        }
    }

    /**
     * Enter the barrier.
     *
     * @throws InterruptedException If the transaction is interrupted.
     */
    public void enter(String threadName) throws InterruptedException {
        try {
            // add a node to root.
            var nodePath = path + '/' + Thread.currentThread().getName();
            ephemeralNode = zookeeper.create(nodePath, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.info("{} created {}.", threadName, ephemeralNode);
            // watch the existence of a node named ready, it is used to allow the pass.
            while (true) {
                synchronized (mutex) {
                    var children = zookeeper.getChildren(path, true);
                    if (children.size() < count) {
                        mutex.wait();
                    } else {
                        return;
                    }
                }
            }
        } catch (KeeperException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Leave the barrier.
     *
     * @throws InterruptedException If the transaction is interrupted.
     */
    public void leave(String threadName) throws InterruptedException {
        try {
            zookeeper.delete(ephemeralNode, 0);
            logger.info("{} deleted {}.", threadName, ephemeralNode);
            while (true) {
                synchronized (mutex) {
                    var children = zookeeper.getChildren(path, true);
                    if (!children.isEmpty()) {
                        mutex.wait();
                    } else {
                        return;
                    }
                }
            }
        } catch (KeeperException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * Release the barrier.
     *
     * @throws InterruptedException If the transaction is interrupted.
     */
    @Override
    public void release() throws InterruptedException {
        try {
            zookeeper.delete(path, -1);
        } catch (KeeperException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized void process(WatchedEvent event) {
        synchronized (mutex) {
            mutex.notifyAll();
        }
    }
}
