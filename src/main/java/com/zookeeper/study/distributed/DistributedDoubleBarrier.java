package com.zookeeper.study.distributed;

import java.io.*;

/**
 * A distributed barrier that synchronize on both entering and leaving. Every applications that is going to use this
 * double barrier have to create it's own double barrier instance with the same configuration. The configuration is
 * usually passed to the implementation's constructor. It is the same if multiple threads in an application use the same
 * double barrier.
 */
public interface DistributedDoubleBarrier {

    /**
     * Do the initialization. If this distributed double barrier is considered as initialized, nothing should happen.
     *
     * @throws InterruptedException If the transaction is interrupted.
     */
    void init() throws InterruptedException;

    /**
     * Enter the barrier.
     *
     * @throws InterruptedException If the transaction is interrupted.
     */
    void enter(String threadName) throws InterruptedException;

    /**
     * Leave the barrier.
     *
     * @throws InterruptedException If the transaction is interrupted.
     */
    void leave(String threadName) throws InterruptedException;

    /**
     * Release the barrier.
     *
     * @throws InterruptedException If the transaction is interrupted.
     */
    void release() throws InterruptedException;

}
