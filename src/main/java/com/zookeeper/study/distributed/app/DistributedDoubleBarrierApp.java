package com.zookeeper.study.distributed.app;

import com.zookeeper.study.distributed.*;
import org.apache.zookeeper.ZooKeeper;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Test {@link com.zookeeper.study.distributed.ZookeeperDoubleBarrier}.
 */
public class DistributedDoubleBarrierApp {
    private String path;
    private final ExecutorService executorService;
    private int minAllowed;
    private ZooKeeper zookeeper;

    public DistributedDoubleBarrierApp() {
        executorService = Executors.newCachedThreadPool();
    }

    public Void test0() {
        final ZooKeeper zk = zookeeper;
        try (zk) {
            var doubleBarrier = new ZookeeperDoubleBarrier(zookeeper, path, minAllowed);
            var threadName = Thread.currentThread().getName();
            doubleBarrier.init();

            // System.out.println(threadName + " Entering barrier.");
            doubleBarrier.enter();
            System.out.println(threadName + " Entered barrier.");

            // Thread.sleep(5 * 1000L);

            // System.out.println(threadName + " Leaving barrier.");
            doubleBarrier.leave();
            System.out.println(threadName + " Leaved barrier.");

        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public void test(String path, int threadsCount) throws InterruptedException, IOException {
        this.path = path;
        this.minAllowed = threadsCount;
        this.zookeeper = ZookeeperAppHelper.zookeeperInstance();
        var callables = new ArrayList<Callable<Void>>(threadsCount);
        for (int i = 0; i < threadsCount; i++) {
            callables.add(this::test0);
        }
        executorService.invokeAll(callables);
    }

    public void close() {
        executorService.shutdown();
    }

    public static void main(String[] args) {
        var app = new DistributedDoubleBarrierApp();
        try {
            app.test("/doublebarrier", 50);
            app.test("/doublebarrier", 50);
            app.test("/doublebarrier", 50);
            app.test("/doublebarrier", 50);
            app.test("/doublebarrier", 50);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        app.close();
        System.out.println("Test done.");
    }
}
