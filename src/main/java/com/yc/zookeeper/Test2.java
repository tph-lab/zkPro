package com.yc.zookeeper;

import com.yc.zookeeper.util.ZkHelper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Test2 {
    private static ZooKeeper zk;
    private static ZkHelper zkHelper;

    public static void create(String path,byte []data) throws InterruptedException, KeeperException {
        //ACL:schema:ID:permission
        // schema:   world   ip     digest      auth
        //ID:        anyone  ip地址 用户名、密码   用户


        String r=zk.create(path,data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        System.out.println("创建了节点:"+r);
    }

    public static void main(String[] args) {
        String path="/MyFirstZnode2";
        //数据要转成    byte[]
        byte[] data="hello yc55你好，世界".getBytes();
        zkHelper=new ZkHelper();
        try {
            zk=zkHelper.connect();
            create(path,data);
            zkHelper.close();
            System.out.println("客户端运行完毕，关闭连接...");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
