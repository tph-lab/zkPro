package com.yc.zookeeper;

import com.yc.zookeeper.util.ZkHelper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;

/**
 * 获取子节点
 */
public class Test6 {
    private static ZooKeeper zk;
    private static ZkHelper zkHelper;

    public static Stat znode_exists(String path) throws InterruptedException, KeeperException {
        return zk.exists(path, true);
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        String path="/";
        zkHelper=new ZkHelper();
        zk=zkHelper.connect();
        Stat stat=znode_exists(path);
        if(stat==null){
            System.out.println("path:"+path+" 没有子节点.");
            return;
        }
        List<String> children=zk.getChildren(path,false);
        System.out.println("path:"+path+"  子节点如下:");
        for (int i=0;i<children.size();i++){
            System.out.println(children.get(i));
        }
    }

}
