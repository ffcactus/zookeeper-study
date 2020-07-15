package com.zookeeper.study.distributed.app;

import com.zookeeper.study.distributed.*;

import java.io.*;

/**
 * Test {@link com.zookeeper.study.distributed.ZookeeperDoubleBarrier}.
 */
public class DistributedDoubleBarrierApp {
    private static final String PATH = "/doublebarrier";

    public static void main(String[] args) {
        try (var zookeeper = ZookeeperAppHelper.zookeeperInstance()) {
            var doubleBarrier = new ZookeeperDoubleBarrier(zookeeper, PATH, 2);
            doubleBarrier.init();
            System.out.println("Entering barrier.");
            doubleBarrier.enter();
            System.out.println("Entered barrier.");
            Thread.sleep(5 * 1000L);
            System.out.println("Leaving barrier.");
            doubleBarrier.leave();
            System.out.println("Leaved barrier.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
