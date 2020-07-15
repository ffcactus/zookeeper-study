package com.zookeeper.study.distributed;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.*;

import java.io.*;
import java.util.*;

/**
 * Zookeeper implementation of {@link DistributedDoubleBarrier}. Each threads, no matter if it is from the same
 * application, have to create the instance with the same constructor parameters.
 */
public class ZookeeperDoubleBarrier implements DistributedDoubleBarrier {
    private final ZooKeeper zookeeper;
    private final String path;
    private final int count;
    private static final String READY_NODE = "/ready";
    private final Object entryLock;
    private final Object leaveLock;
    private String ephemeralNode;

    /**
     * Create a double barrier by using a Zookeeper service.
     *
     * @param zookeeper zookeeper service. User have to close it manually.
     * @param path the path of the root node for this double barrier, have to end with '/'.
     * @param count the minimal procedure for this barrier to open.
     */
    public ZookeeperDoubleBarrier(ZooKeeper zookeeper, String path, int count) {
        this.zookeeper = zookeeper;
        this.path = path;
        this.count = count;
        this.entryLock = new Object();
        this.leaveLock = new Object();
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
            try {
                zookeeper.delete(path + READY_NODE, -1);
            } catch (KeeperException e2) {
                if (!e2.code().equals(KeeperException.Code.NONODE)) {
                    throw new IllegalStateException(e2);
                }
            }
        }
    }

    /**
     * Enter the barrier.
     *
     * @throws InterruptedException If the transaction is interrupted.
     */
    public void enter() throws InterruptedException {
        try {
            // add a node to root.
            var nodePath = path + '/' + Thread.currentThread().getName();
            ephemeralNode = zookeeper.create(nodePath, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            System.out.println(Thread.currentThread().getName() + " created " + ephemeralNode);
            // watch the existence of a node named ready, it is used to allow the pass.
            zookeeper.exists(path + READY_NODE, event -> {
                if (event.getType() == Watcher.Event.EventType.NodeCreated) {
                    System.out.println(Thread.currentThread().getName() + " found ready.");
                    synchronized (entryLock) {
                        entryLock.notifyAll();
                    }
                }
            });
            // wait for enough nodes.
            List<String> children;
            System.out.println(Thread.currentThread().getName() + " checking children.");
            while ((children = zookeeper.getChildren(path, false)).size() < count) {
                // System.out.println(Thread.currentThread().getName() + " found " + children);
                synchronized (entryLock) {
                    entryLock.wait();
                }
            }
            System.out.println(Thread.currentThread().getName() + " found enough children.");
            createReady();
        } catch (KeeperException e) {
            throw new IllegalStateException(e);
        }
    }

    private void createReady() throws InterruptedException {
        try {
            zookeeper.create(path + READY_NODE, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.println(Thread.currentThread().getName() + " created ready.");
            System.out.println("--- Barrier opened for every one ---");
        } catch (KeeperException e) {
            if (e.code().equals(KeeperException.Code.NODEEXISTS)) {
                System.out.println(Thread.currentThread().getName() + " found ready exist.");
            } else {
                throw new IllegalStateException(e);
            }
        }
    }
    /**
     * Leave the barrier.
     *
     * @throws InterruptedException If the transaction is interrupted.
     */
    public void leave() throws InterruptedException {
        try {
            zookeeper.delete(ephemeralNode, -1);
            // wait until the children count is 1 (the ready node).
            List<String> children;
            while ((children = zookeeper.getChildren(path, event -> {
                synchronized (leaveLock) {
                    leaveLock.notifyAll();
                }
            })).size() > 1) {
                System.out.println(children);
                synchronized (leaveLock) {
                    leaveLock.wait();
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
