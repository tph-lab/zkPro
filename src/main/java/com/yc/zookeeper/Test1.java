package com.yc.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.List;

public class Test1 {

    private static String connectionString="node1:2181,node2:2181,node3:2181";
    private static int sessionTimeout=2000;
    private static ZooKeeper zkClient=null;

    public static void main(String[] args) throws IOException {
        //监听器这个线程是守护线程，即一旦守护的线程结束，守护线程也将结束
        zkClient=new ZooKeeper(connectionString, sessionTimeout, new Watcher() {
            @Override
            public void process(WatchedEvent event) {

                //收到事件通知后的回调函数（用户的业务逻辑）
                System.out.println("事件信息"+ event.getType()+"---"+event.getPath()+"---"+event.getState());
                //再次启动监听
                try {
                    List<String> list=zkClient.getChildren("/",true);
                    if(list!=null){
                        for(String s:list){
                            System.out.println(s);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        System.out.println("zk信息："+zkClient);
        //请注意：TODO  连接的信息是connecting，表示还在连接中，但程序已经停止了
        //解决方案：ZkHelper.java类，加入一个线程等待类
    }

}
