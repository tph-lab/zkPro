package com.yc.zookeeper;

import com.yc.zookeeper.util.ZkHelper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class Test5 {
    private static ZooKeeper zk;
    private static ZkHelper zkHelper;

    public static void update(String path,byte[] data) throws InterruptedException, KeeperException {
        //注意设置版本号
       zk.setData(path,data,zk.exists(path,true).getVersion());
    }


    public static void main(String[] args) {
        String path="/MyFirstZnode2";
        //数据要转成    byte[]
        byte[] data="Success".getBytes();
        try {
            zkHelper=new ZkHelper();
            zk=zkHelper.connect();
           update(path,data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
