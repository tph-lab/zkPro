package com.yc.zookeeper;

import com.yc.zookeeper.util.YcZnodeUtil;
import com.yc.zookeeper.util.ZkHelper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

public class Test4 {

    private static ZooKeeper zk;
    private static ZkHelper zkHelper;
    private static CountDownLatch connectedSignal=new CountDownLatch(10);//只绑定监听10次

    public static Stat znode_exists(String path) throws InterruptedException, KeeperException {
        return zk.exists(path,true);
    }

    public static void main(String[] args) {
        String path="/MyFirstZnode2";
        try {
            zkHelper=new ZkHelper();
            zk=zkHelper.connect();
            Stat stat=znode_exists(path);
            if(stat==null){
                System.out.println("node不存在");
                return;
            }
            //绑定监听事件
            byte[] b=zk.getData(path,new MyWatch(path,connectedSignal,zk),stat);
            String data=new String(b,"UTF-8");
            System.out.println("主程序中获取节点:"+path+"的原始数据为:"+data);//请注意：这个数据的值
            String info=YcZnodeUtil.printZnodeInfo(stat);
            System.out.println(info);
            connectedSignal.await();//程序，阻塞在这里，直到另一个客户端操作这个节点的数据，才解锁
            System.out.println("主程序运行结束");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}


class MyWatch implements Watcher{
    private String path;
    private CountDownLatch connectedSignal;
    private ZooKeeper zk;

    public MyWatch(String path, CountDownLatch connectedSignal, ZooKeeper zk) {
        this.path = path;
        this.connectedSignal = connectedSignal;
        this.zk = zk;
    }


    @Override
    public void process(WatchedEvent watchedEvent) {//另外一个客户端操作这个节点
        if(watchedEvent.getType()==Event.EventType.NodeDataChanged){
            try {
                Stat stat=new Stat();
                byte[] bn=zk.getData(path,MyWatch.this,stat);//继续绑定器
                //字节数组转化为字符串
                String data=new String(bn,"UTF-8");
                System.out.println("监听得到的此节点:"+path+"的新数据为:"+data);//请注意：这个数据的值
                System.out.println("当前节点的stat信息:\n");
                String info=YcZnodeUtil.printZnodeInfo(stat);
                System.out.println(info);
                connectedSignal.countDown();
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            System.out.println("事件类型为:"+watchedEvent.getType());
        }

    }
}
