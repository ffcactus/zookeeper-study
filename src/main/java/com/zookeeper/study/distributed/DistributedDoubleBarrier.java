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
     * @throws Exception Generally caller need to handle the specific exception thrown by implementation.
     */
    void init() throws Exception;

    /**
     * Enter the barrier.
     *
     * @throws IOException Generally caller need to handle the specific exception thrown by implementation.
     */
    void enter() throws Exception;

    /**
     * Leave the barrier.
     *
     * @throws IOException Generally caller need to handle the specific exception thrown by implementation.
     */
    void leave() throws Exception;

    /**
     * Release the barrier.
     *
     * @throws Exception Generally caller need to handle the specific exception thrown by implementation.
     */
    void release() throws IOException;

}
