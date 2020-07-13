package com.zookeeper.study;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.*;
import org.apache.zookeeper.KeeperException.Code;

import java.util.*;

/**
 * Conventionally, ZooKeeper applications are broken into two units, one which maintains the connection,
 * and the other which monitors data. In this application. This class monitors the data in the ZooKeeper tree.
 */
public class DataMonitor implements Watcher, AsyncCallback.StatCallback {
    ZooKeeper zk;
    String znode;
    Watcher chainedWatcher;
    boolean dead;
    DataMonitorListener listener;
    byte[] prevData;

    public interface DataMonitorListener {
        /**
         * The existence status of the node has changed.
         * @param data
         */
        void exists(byte[] data);

        /**
         * The ZooKeeper session is no longer valid.
         * @param rc the ZooKeeper reason code
         */
        void closing(int rc);
    }

    public DataMonitor(ZooKeeper zk, String znode, Watcher chainedWatcher, DataMonitorListener listener) {
        this.zk = zk;
        this.znode = znode;
        this.chainedWatcher = chainedWatcher;
        this.listener = listener;
//        try {
//            var stat = zk.exists(znode, false);
//            System.out.println(stat);
//        } catch (KeeperException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        // Get things started by checking if the node exists. We are going
        // to be completely event driven
        zk.exists(znode, true, this, null);
    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        boolean exists;
        // Retry errors
        switch (rc) {
            case Code.Ok -> exists = true;
            case Code.NoNode -> exists = false;
            case Code.SessionExpired, Code.NoAuth -> {
                dead = true;
                listener.closing(rc);
                return;
            }
            default -> {
                zk.exists(znode, true, this, null);
                return;
            }
        }

        byte[] b = null;
        if (exists) {
            try {
                b = zk.getData(znode, false, null);
            } catch (KeeperException e) {
                // We don't need to worry about recovering now. The watch
                // callbacks will kick off any exception handling
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
        if ((b == null && b != prevData)
                || (b != null && !Arrays.equals(prevData, b))) {
            listener.exists(b);
            prevData = b;
        }
    }

    @Override
    public void process(WatchedEvent event) {
        String path = event.getPath();
        if (event.getType() == Event.EventType.None) {
            // We are are being told that the state of the
            // connection has changed
            switch (event.getState()) {
                case SyncConnected:
                    // In this particular example we don't need to do anything
                    // here - watches are automatically re-registered with
                    // server and any watches triggered while the client was
                    // disconnected will be delivered (in order of course)
                    break;
                case Expired:
                    // It's all over
                    dead = true;
                    listener.closing(KeeperException.Code.SessionExpired);
                    break;
            }
        } else {
            if (path != null && path.equals(znode)) {
                // Something has changed on the node, let's find out
                zk.exists(znode, true, this, null);
            }
        }
        if (chainedWatcher != null) {
            chainedWatcher.process(event);
        }
    }
}
