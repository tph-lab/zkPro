package com.yc.publish_subscribe;


import com.yc.zookeeper.util.ZkHelper;
import org.apache.zookeeper.ZooKeeper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 调度类：主程序入口
 * 1.创建ManageServer
 * 2.创建指定数据量的Work Server工作服务器
 * 3.自行到/command节点下手工加入命令
 */
public class SubscribeZkClient {

    private static final int WORK_SERVER=5;//Work Server的数量

    private static final String CONFIG_PATH="/config";
    private static final String COMMAND_PATH="/command";
    private static final String SERVERS_PATH="/servers";

    public static void main(String[] args){
        //存放zkClients的
        List<ZooKeeper> zkClients=new ArrayList<>();
        //存放工作服务器的
        List<WorkServer> workServers=new ArrayList<>();
        //只需要有一个
        ManageServer manageServer=null;


        try{
            //创建一个默认的配置(为了创建config节点时，默认设置的)
            ServerConfig defaultConfig=new ServerConfig();
            defaultConfig.setDbPwd("123456");
            defaultConfig.setDbUrl("jdbc:mysql://localhost:3306/mydb");
            defaultConfig.setDbUser("root");

            ZkHelper zkHelper=new ZkHelper();
            //实例化一个Manage Sever
            ZooKeeper clientManage=zkHelper.connect();
            //SERVERS_PATH可以列出/servers下的服务列表    COMMAND_PATH从/command节点取出命令，执行   CONFIG_PATH命令执行可能需要对/config节点进行内容等操作
            manageServer=new ManageServer(SERVERS_PATH,COMMAND_PATH,CONFIG_PATH,clientManage,defaultConfig);
            //启动ManageServer     监听/command节点数据变化和/servers孩子列表变化
            manageServer.start();

            //创建指定个数的工作服务器workServer，注册到/servers下
            //1.在/servers/下注册      2.监听/config节点
            for (int i = 0; i < WORK_SERVER; i++) {
                ZooKeeper client=zkHelper.connect();
                zkClients.add(client);

                ServerData serverData=new ServerData();
                serverData.setId(i);
                serverData.setName("WorkServer#"+i);
                serverData.setAddress("192.168.1."+i);
                //TODO:还可以加入更多的服务信息
                //需要客户端去执行操作     SERVERS_PATH在servers节点中注册自己，
                // CONFIG_PATH获取最新的config信息，还有监听
                //serverData需要config节点的信息
                //serverData需要有节点名（使用了address）
                WorkServer workServer=new WorkServer(client,SERVERS_PATH,CONFIG_PATH,defaultConfig,serverData);
                workServers.add(workServer);
                workServer.start();//启动工作服务器
            }
            //阻塞主程序
            System.out.println("输入回车键退出....");
            new BufferedReader(new InputStreamReader(System.in)).readLine();

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            System.out.println("shut down......");
            for (WorkServer workServer:workServers){
                try {
                    workServer.stop();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            for (ZooKeeper client:zkClients){
                try {
                    client.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


    }

}
