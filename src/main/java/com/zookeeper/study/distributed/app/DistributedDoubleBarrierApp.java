package com.zookeeper.study.distributed.app;

import com.zookeeper.study.distributed.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.*;

/**
 * Test {@link com.zookeeper.study.distributed.ZookeeperDoubleBarrier}.
 */
public class DistributedDoubleBarrierApp {
    private SyncPrimitive.Barrier barrier;
    private final ExecutorService executorService;
    private static final Logger logger = LogManager.getLogger(DistributedDoubleBarrierApp.class);

    public DistributedDoubleBarrierApp() {
        executorService = Executors.newCachedThreadPool();
    }

    public Void test0() {
        try {
            var threadName = Thread.currentThread().getName();

            logger.info("{} Entering barrier.", threadName);
            barrier.enter(threadName);
            logger.info("{} Entered barrier.", threadName);

            Thread.sleep(3000L);

            logger.info("{} Leaving barrier.", threadName);
            barrier.leave(threadName);
            logger.info("{} Leaved d barrier.", threadName);

        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public void test(SyncPrimitive.Barrier barrier, int threadsCount) throws InterruptedException, IOException {
        this.barrier = barrier;
        var callables = new ArrayList<Callable<Void>>(threadsCount);
        for (int i = 0; i < threadsCount; i++) {
            callables.add(this::test0);
        }
        logger.info("invokeAll begin");
        executorService.invokeAll(callables);
        logger.info("invokeAll done");
    }

    public void close() {
        executorService.shutdown();
    }

    public static void main(String[] args) {
        int concurrency = 10;
        try {

            var barrier = new SyncPrimitive.Barrier(ZookeeperAppHelper.HOSTS, "/doublebarrier", concurrency);
            var app = new DistributedDoubleBarrierApp();
            for (int i = 0; i < 100; i++) {
                app.test(barrier, concurrency);
                logger.warn("--- round {} ---", i);
            }
            app.close();
            logger.info("Test done.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}

// https://zookeeper.apache.org/doc/r3.4.6/zookeeperTutorial.html