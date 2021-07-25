package com.yc.publish_subscribe;

import com.yc.zookeeper.util.ZkHelper;
import org.apache.jute.BinaryOutputArchive;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * 主要功能:1.监听/command的变化情况，接收操作命令
 *        2.监听/servers的子节点变化情况，更新节点列表
 */
public class ManageServer {
    //zookeeper的servers节点路径
    private String serversPath;
    //zookeeper的command的节点路径
    private String commandPath;
    //zookeeper的config节点路径
    private String configPath;
    //zookeeper客户端
    private ZooKeeper zkClient;
    //服务的配置信息
    private ServerConfig config;

    //用于监听servers节点的子节点变化
    private Watcher childListener;
    //用于监听command节点数据内容的变化
    private Watcher dataListener;

    //工作服务器的列表
    List<String> workServerList;

    //设置了各属性初始值，以及两个监听器
    public ManageServer(String serversPath, String commandPath, String configPath, ZooKeeper zkClient, ServerConfig config) {
        this.serversPath = serversPath;
        this.commandPath = commandPath;
        this.configPath = configPath;
        this.zkClient = zkClient;
        this.config = config;
        //监听servers节点的子节点列表的变化，一旦内容发生改变。马上列出新的服务列表
        this.childListener=new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                //子节点列表发生变更
                if(event.getType()==Event.EventType.NodeChildrenChanged){
                    try {
                        //根据路径取子节点名列表，重新再次绑定该监听器
                        workServerList=zkClient.getChildren(serversPath,childListener);
                        System.out.println("work server list changed,new list is");
                        execList();//列出工作服务器列表
                    } catch (KeeperException e) {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        //监听command节点数据内容的变化,一旦内容发生改变，马上执行命令
        this.dataListener=new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                //  /command节点发生变化
                if(event.getType()==Event.EventType.NodeDataChanged){
                    Stat stat=new Stat();
                    try {
                        //得到  /command节点的数据，并且又绑定监听器
                        byte[] bs=zkClient.getData(commandPath,dataListener,stat);
                        String cmd=new String(bs);
                        System.out.println("cmd:"+cmd);
                        exeCmd(cmd);//执行命令
                    } catch (KeeperException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

    }

    //执行命令(list、create、modify)
    private void exeCmd(String cmd) {
        if("list".equals(cmd)){
            execList();
        }else if("create".equals(cmd)){
            execCreate();//创建config节点
        }else if("modify".equals(cmd)){
            execModify();
        }else {
            System.out.println("error command:   "+cmd);
        }
    }

    //修改config节点的内容
    private void execModify() {
        try {
            //我们随意修改confg的一个属性就可以了
            config.setDbUser(config.getDbUser()+"_modify");//TODO: 这里写死了，将来由controller发过来的值决定
            ByteArrayOutputStream boas=new ByteArrayOutputStream();
            BinaryOutputArchive boa=BinaryOutputArchive.getArchive(boas);
            config.serialize(boa,"header");
            zkClient.setData(configPath,boas.toByteArray(),zkClient.exists(configPath,false).getVersion());
        }catch (Exception e){
            execCreate();//写入时config节点还未存在，则创建
        }
    }

    //创建config节点(如果没有就创建，有就直接setData)
    private void execCreate() {
        try {
            ByteArrayOutputStream boas=new ByteArrayOutputStream();
            BinaryOutputArchive boa=BinaryOutputArchive.getArchive(boas);
            config.serialize(boa,"header");
            if(zkClient.exists(configPath,false)==null){
                try{
                    String r=zkClient.create(configPath,boas.toByteArray(), ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
                    System.out.println("创建的配置信息为:"+config.toString());
                }catch (KeeperException e){//高并发情况下，可能刚好有线程创建了config节点
                    System.out.println("配置中心节点已经存在，更新...");
                    System.out.println("更新的配置信息为:"+config.toString());
                    //config节点已经存在，则写入内容就好了     更新时，需要版本号
                    zkClient.setData(configPath,boas.toByteArray(),zkClient.exists(configPath,false).getVersion());
                }
            }else {
                System.out.println("更新的配置信息为:"+config.toString());
                //config节点已经存在，则写入内容就好了     更新时，需要版本号
                zkClient.setData(configPath,boas.toByteArray(),zkClient.exists(configPath,false).getVersion());
            }
        }catch (InterruptedException | KeeperException |IOException e) {
            e.printStackTrace();
        }

    }

    //列出工作服务器列表
    private void execList() {
        if(workServerList!=null && workServerList.size()>0){
            if(workServerList!=null && workServerList.size()>0){
                System.out.println(workServerList.toString());
            }else {
                System.out.println("暂无服务器注册");
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ZkHelper zh=new ZkHelper();
        ZooKeeper zk=zh.connect();
        //微服务的配置信息
        ServerConfig config=new ServerConfig();
        config.setDbUser("root");
        config.setDbPwd("a");
        config.setDbUrl("jdbc:localhost:1433/test");
        //管理服务器创建
        ManageServer ms=new ManageServer("/servers","/command","/config",zk,config);
        ms.start();//开始监听两种监听器
        System.out.println("敲回车键退出！\n");
        new BufferedReader(new InputStreamReader(System.in)).readLine();//阻塞
        ms.stop();//关闭两种监听器
    }

    //停止工作服务器(关闭监听)
    private void stop() {

        try {
            //监听/servers孩子列表变化
            zkClient.getChildren(serversPath,childListener);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Stat stat=new Stat();
        try {
            //监听/command节点数据变化
            zkClient.getData(commandPath,false,stat);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //启动工作服务器(开始监听)
    public void start() {
        //启动    监听/command节点数据变化和/servers孩子列表变化
        initRunning();
    }

    //监听/command节点数据变化和/servers孩子列表变化
    private void initRunning() {
        Stat stat1=new Stat();
        try {
            //监听/command节点数据变化
            zkClient.getData(commandPath,dataListener,stat1);
        } catch (Exception e) {
            execCreate();//写入时config节点还未存在，则创建
        }


        Stat stat2=new Stat();
        try {
            //监听/servers孩子列表变化
            zkClient.getChildren(serversPath,childListener,stat2);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }




}
