package com.yc.zookeeper;

import com.yc.zookeeper.util.YcZnodeUtil;
import com.yc.zookeeper.util.ZkHelper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class Test3 {
    private static ZooKeeper zk;
    private static ZkHelper zkHelper;

    public static Stat znode_exists(String path) throws InterruptedException, KeeperException {
        return zk.exists(path, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                System.out.println(watchedEvent.getState()+"   "+watchedEvent.getPath()+"   "+watchedEvent.getState());
            }
        });
    }

    public static void main(String[] args) {
        String path="/MyFirstZnode2";
        try {
            zkHelper=new ZkHelper();
            zk=zkHelper.connect();
            Stat stat=znode_exists(path);
            if (stat!=null){
                System.out.println("node的详细信息:\n"+ YcZnodeUtil.printZnodeInfo(stat));
            }else {
                System.out.println("node:"+path+"不存在");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
