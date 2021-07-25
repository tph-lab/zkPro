package com.yc.zookeeper;

import com.yc.zookeeper.util.ZkHelper;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public class Test1_zkHelper {
    private static ZooKeeper zk;
    private static ZkHelper zkHelper;

    public static void main(String[] args) throws IOException, InterruptedException {
        zkHelper=new ZkHelper();
        zk=zkHelper.connect();
        zkHelper.close();
        System.out.println("客户端运行完毕，关闭连接....");
    }
}
