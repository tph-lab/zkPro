package com.yc.publish_subscribe;

import org.apache.jute.BinaryInputArchive;
import org.apache.jute.BinaryOutputArchive;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.ByteBufferInputStream;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * 代表工作服务器：1.在/servers/下注册      2.监听/config节点
 */
public class WorkServer {

    //zookeeper客户端
    private ZooKeeper zkClient;
    //zookeeper的servers节点路径,服务注册发现
    private String serversPath;
    //zookeeper的config节点路径
    private String configPath;
    //服务的配置信息(分布式配置或全局配置)，存在zk中的
    private ServerConfig serverConfig;
    //当前工作服务器的基本信息(workserver工作节点的基本信息)
    private ServerData serverData;

    private Watcher watcher;

    public WorkServer(ZooKeeper zkClient, String serversPath, String configPath, ServerConfig serverConfig, ServerData serverData) {
        this.zkClient = zkClient;
        this.serversPath = serversPath;
        this.configPath = configPath;
        this.serverConfig = serverConfig;
        this.serverData = serverData;

        //监听  /config中的分布式数据配置信息的变化，一旦这个配置信息发生变化，则所有的工作server的数据库连接都要发生变化
        this.watcher=new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                try {
                    //节点内容数据变化，则获取新的配置信息
                    if(event.getType()==Event.EventType.NodeDataChanged){
                        Stat stat=new Stat();
                        //获取信息（状态和内容），且继续绑定监听器，进行监听
                        byte[] result=zkClient.getData(configPath,watcher,stat);
                        //因为存的时候，是序列化存储的，所以取出之后也要反序列化
                        ByteBuffer bb=ByteBuffer.wrap(result);
                        ByteBufferInputStream bbis=new ByteBufferInputStream(bb);
                        BinaryInputArchive bia=BinaryInputArchive.getArchive(bbis);
                        ServerConfig header=new ServerConfig();
                        header.deserialize(bia,"create");

                        //TODO:创建本server的数据库连接池或者jdbc.Connection，或者mybatis，jpa的数据源
                        System.out.println("/config节点发生变化...");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        };
    }

    //启动工作服务器
    public void start(){
        System.out.println("Work   Server   start...");
        //服务器初始化(注册自己到servers，然后开始监听)
        initRunning();
    }

    //服务器初始化(注册自己到servers，然后开始监听)
    private void initRunning() {
        //1.注册自己到zk中的/servers/下（//我是一个工作服务器，在servers节点中注册自己，并把config节点的信息，写入自己的节点）
        registMe();
        //2.绑定监听器，以监听/config节点内容的变化
        Stat stat=new Stat();
        try {
            zkClient.getData(configPath,this.watcher,stat);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //我是一个工作服务器，在servers节点中注册自己，并把config节点的信息，写入自己的节点
    private void registMe() {
        //str1.concat(str2);
        //意思是将str2连接到str1的尾部
        String mePath=serversPath.concat("/").concat(serverData.getAddress());

        try {
            //（设置command节点的内容，然后由mannageServer执行命令，将对应的数据写入到config节点中）将config节点信息写入到/servers/xxx中，这个节点必须是临时节点,因为zkclint一旦掉线，必须删除该节点
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            BinaryOutputArchive boa=BinaryOutputArchive.getArchive(baos);
            serverConfig.serialize(boa,"header");
            //尝试创建服务列表下的临时节点
            String r=zkClient.create(mePath,baos.toByteArray(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    //停止服务器
    public void stop(){
        System.out.println("Work   Server   stop...");
    }

    //更新自己（工作服务器）的配置信息
    private void updateConfig(ServerConfig serverConfig){
        this.serverConfig=serverConfig;
    }

}
