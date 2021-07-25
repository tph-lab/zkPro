package com.yc.zookeeper.util;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ZkHelper {
    private static String connectionString="node1:2181,node2:2181,node3:2181";
    private static int sessionTimeout=2000;
    private static ZooKeeper zkClient=null;//声明Zookeeper实例以连接zookeeper中的集合
    //CountDownLatch用于停止（等待）主进程，直到客户端与zookeeper集合连接，这是一个信号
    //阻塞
    final CountDownLatch connectedSignal=new CountDownLatch(1);

    //连接zookeeper集合的方法
    public ZooKeeper connect() throws IOException, InterruptedException {
        System.out.println("zk客户端初始化中....");
        //连接Zookeeper集群服务器
        zkClient=new ZooKeeper(connectionString, sessionTimeout, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                //收到事件通知后的回调函数（用户的业务逻辑）
                System.out.println("事件信息"+ event.getType()+"---"+event.getPath()+"---"+event.getState());
                if(event.getState()== Event.KeeperState.SyncConnected){//只有回调的状态是SyncConnected，才算连接上，才激活这个判断
                    System.out.println("zk客户端建立与服务器的连接");
                    connectedSignal.countDown();//只有连接建立了，再释放这把锁，这样主进程才可以继续运行
                }
            }

        });
        connectedSignal.await();//在主进程中await，阻塞   不为0，就阻塞
        System.out.println("客户端主进程运行完");
        return zkClient;
    }

    public void close() throws InterruptedException {
        zkClient.close();
    }

    public String getConnectionString(){
        return connectionString;
    }

}
