package com.yc.zookeeper;

import com.yc.zookeeper.util.ZkHelper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class Test7 {
    private static ZooKeeper zk;
    private static ZkHelper zkHelper;

    public static void delete(String path) throws InterruptedException, KeeperException {
        zk.delete(path, zk.exists(path, true).getVersion());
    }


    public static void main(String[] args) {
        String path="/MyFirstZnode2";
        try {
            zkHelper=new ZkHelper();
            zk=zkHelper.connect();
            delete(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
