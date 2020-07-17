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
    private final ZookeeperDoubleBarrier barrier;
    private final ExecutorService executorService;
    private static final Logger logger = LogManager.getLogger(DistributedDoubleBarrierApp.class);

    /**
     * Constructor the application by an initialized barrier.
     * @param barrier the initialized barrier.
     */
    public DistributedDoubleBarrierApp(ZookeeperDoubleBarrier barrier) {
        this.barrier = barrier;
        executorService = Executors.newCachedThreadPool();
    }

    public Void test0() {
        try {
            var threadName = Thread.currentThread().getName();
            var newBarrier = new ZookeeperDoubleBarrier(barrier);
            logger.info("{} Entering barrier.", threadName);
            newBarrier.enter(threadName);
            logger.info("{} Entered barrier.", threadName);

//            Thread.sleep(3000L);

            logger.info("{} Leaving barrier.", threadName);
            newBarrier.leave(threadName);
            logger.info("{} Leaved barrier.", threadName);

        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public void test(int threadsCount) throws InterruptedException {
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
        int concurrency = 5;
        try {
            var zk = ZookeeperAppHelper.zookeeperInstance();
            var barrier = new ZookeeperDoubleBarrier(zk, "/doublebarrier", concurrency * 2);
            // barrier.init();
            var app = new DistributedDoubleBarrierApp(barrier);
            for (int i = 0; i < 5; i++) {
                app.test(concurrency);
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