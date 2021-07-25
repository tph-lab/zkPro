package com.yc.master_selection;

import com.yc.zookeeper.util.ZkHelper;
import org.apache.zookeeper.ZooKeeper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LeaderSelectorZkClient {

    //启动的服务个数
    private static final int CLIENT_QTY=10;

    public static void main(String[] args) {
        //保存所有zkClient的列表
        List<ZooKeeper> clients=new ArrayList<>();
        //保存所有服务器的列表
        List<WorkServer> workServers=new ArrayList<>();
        ZkHelper zkHelper=new ZkHelper();
        try {
            for (int i = 0; i < CLIENT_QTY; i++) {//模拟创建10个服务器并启动
                //创建zkClient
                ZooKeeper client=zkHelper.connect();
                clients.add(client);
                //创建serverData
                RunningData runningData=new RunningData();
                runningData.setCid(Long.valueOf(i));
                runningData.setName("Client #"+i);
                //创建服务
                WorkServer workServer=new WorkServer(runningData);
                workServer.setZkClient(client);
                //将此服务添加到集合中
                workServers.add(workServer);
                workServer.start();
            }
            System.out.println("敲回车键输出!\n");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("服务关闭...");
            for(WorkServer workServer:workServers){
                try {
                    workServer.stop();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            for(ZooKeeper client:clients){
                try {
                    client.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
