package com.zookeeper.study.distributed;

import org.apache.logging.log4j.*;
import org.apache.zookeeper.*;

import java.util.concurrent.*;

/**
 * Zookeeper implementation of {@link DistributedDoubleBarrier}. Each threads, no matter if it is from the same
 * application, have to create the instance with the same constructor parameters.
 */
public class ZookeeperDoubleBarrier implements DistributedDoubleBarrier {
    private static final Logger logger = LogManager.getLogger(ZookeeperDoubleBarrier.class);
    private static final String READY_NODE = "/ready";
    private final ZooKeeper zookeeper;
    private final String path;
    private final int count;
    private String ephemeralNode;
    private final Object leaveMutex;

    /**
     * Create a double barrier by using a Zookeeper service.
     *
     * @param zookeeper zookeeper service. User have to close it manually.
     * @param path      the path of the root node for this double barrier, should not end with '/'.
     * @param count     the minimal procedure for this barrier to open.
     */
    public ZookeeperDoubleBarrier(ZooKeeper zookeeper, String path, int count) {
        this.zookeeper = zookeeper;
        this.path = path;
        this.count = count;
        leaveMutex = new Object();
    }

    /**
     * A copy constructor for multiple threads app. For multiple threads app, each of the thread that need the same
     * barrier should have a new copy.
     *
     * @param from the barrier to copy from.
     */
    public ZookeeperDoubleBarrier(ZookeeperDoubleBarrier from) {
        this.zookeeper = from.zookeeper;
        this.path = from.path;
        this.count = from.count;
        this.ephemeralNode = from.ephemeralNode;
        leaveMutex = new Object();
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

        /*
         * Implementation overview:
         * A child znode named "ready" is used to notify the waiting procedures. Every procedure creates an ephemeral
         * znode and register a watcher for the creation of "ready" znode. Before waiting for "ready znode", the
         * procedure will check the count of the children. If the count of children equal or great than the count,
         * it create the "ready" znode.
         */
        try {
            var latch = new CountDownLatch(1);
            // add a node to root.
            var nodePath = path + '/' + Thread.currentThread().getName();
            ephemeralNode = zookeeper.create(nodePath, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.info("{} created {}.", threadName, ephemeralNode);
            // set the watcher before we create the ready znode.
            if (zookeeper.exists(path + READY_NODE, event -> latch.countDown()) != null) {
                return;
            }


            var children = zookeeper.getChildren(path, false);
            logger.info("{} found {} children.", () -> threadName, children::size);
            if (children.size() >= count) {
                createReadyIfNotExist();
            }
            latch.await();
        } catch (KeeperException e) {
            throw new IllegalStateException(e);
        }
    }

    private void createReadyIfNotExist() throws InterruptedException, KeeperException {
        try {
            zookeeper.create(path + READY_NODE, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException e) {
            if (!e.code().equals(KeeperException.Code.NODEEXISTS)) {
                throw e;
            }
        }
    }

    private void removeReadyIfExist() throws InterruptedException, KeeperException {
        try {
            zookeeper.delete(path + READY_NODE, -1);
        } catch (KeeperException e) {
            if (!e.code().equals(KeeperException.Code.NONODE)) {
                throw e;
            }
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
                synchronized (leaveMutex) {
                    var children = zookeeper.getChildren(path, event -> {
                        synchronized (leaveMutex) {
                            leaveMutex.notifyAll();
                        }
                    });
                    if (children.size() > 1) {
                        leaveMutex.wait();
                    } else {
                        removeReadyIfExist();
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

}
