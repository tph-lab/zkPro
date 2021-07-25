package com.yc.id_generator;

import com.yc.zookeeper.util.ZkHelper;
import lombok.SneakyThrows;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IdMaker {
    private ZooKeeper client=null;
    private String server;
    //zookeeper顺序节点的父节点
    private final String root;
    //顺序节点的名称
    private final String nodeName;
    //标识当前服务是否正在运行(利用了volatile的可见性)
    private volatile boolean running=false;
    //线程池
    private ExecutorService cleanExector=null;

    public IdMaker(String root, String nodeName) {
        this.root = root;
        this.nodeName = nodeName;
    }

    //启动服务
    public void start() throws Exception {
        if(running){
            throw new Exception("server has started...");
        }
        running=true;
        //初始化服务资源（连接客户端，创建父节点root）
        init();
    }

    //初始化服务资源（连接客户端，创建父节点root）
    private void init() {
        ZkHelper zkHelper=new ZkHelper();
        try {
            client=zkHelper.connect();
            //创建线程池
            cleanExector= Executors.newFixedThreadPool(10);
            //如果不存在顺序节点的父节点，则创建父节点root
            if(client.exists(root,null)==null){
                String r=client.create(root,"".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    //以传入的root和nodename为路径，创建有序节点，再按照删除类型进行删除节点
    public String generatedId(RemoveMethod removeMethod) throws Exception {
        //检测当前服务是否正在运行（即running是否为true）
        checkRunning();
        //顺序节点的完整路径      /NameService/IdGen/ID
        final String fullNodePath=root.concat("/").concat(nodeName);
        //      /NameService/IdGen/ID000000000001
        //产生一个节点后，会返回一个路径
        String ourPath=client.create(fullNodePath,"".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT_SEQUENTIAL);
        //根据传入删除的类型
        if(removeMethod.equals(RemoveMethod.IMMEDIATELY)){//立即删除
            client.delete(ourPath,client.exists(ourPath,null).getVersion());
        }else if (removeMethod.equals(RemoveMethod.DELAY)){//延迟删除
            cleanExector.execute(new Runnable() {
                @SneakyThrows       //避免异常抛出
                @Override
                public void run() {
                    client.delete(ourPath,client.exists(ourPath,null).getVersion());
                }
            });
        }
        //从顺序节点名中提取我们要的ID值
        return ExtractId(ourPath);
    }

    //从顺序节点名中提取我们要的ID值
    //   ourPath   /NameService/IdGen/ID000000000001         nodeName   ID
    private String ExtractId(String ourPath) {
        //定位ID的位置index，再加上ID的长度，在进行截取，可得000000000001
        int index= ourPath.lastIndexOf(nodeName);//找到ID的位置
        if (index>=0){
            index+=nodeName.length();
            return index<=ourPath.length()?ourPath.substring(index):"";
        }
        return ourPath;
    }

    //检测当前服务是否正在运行（即running是否为true）
    private void checkRunning() throws Exception {
        if (!running){
            throw new Exception("首先调用start...");
        }
    }

    //停止服务
    public void stop() throws Exception {
        if (!running){
            throw new Exception("server has stopped....");
        }
        running =false;
        freeResource();
    }

    //释放服务器资源
    private void freeResource() {
        //释放线程池
        //shutdown()关闭任务(run方法)，awaitTermination关闭执行器（executor）。
        cleanExector.shutdown();
        try {
            cleanExector.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        cleanExector=null;

        if (client!=null){
            try {
                client.close();
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
            client=null;
        }

    }


    //枚举(在类内部)
    public enum RemoveMethod{
        NONE,IMMEDIATELY,DELAY;
    }

}
