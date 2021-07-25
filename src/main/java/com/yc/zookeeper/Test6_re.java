package com.yc.zookeeper;

import com.yc.zookeeper.util.ZkHelper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;

public class Test6_re {
    private static ZooKeeper zk;
    private static ZkHelper zkHelper;

    public static Stat znode_exists(String path) throws InterruptedException, KeeperException {
        return zk.exists(path, true);
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        String path="/";
        try {
            zkHelper=new ZkHelper();
            zk=zkHelper.connect();
            showTree(path,0);
        }catch (Exception e){
            e.printStackTrace();
        }

//        Stat stat=znode_exists(path);
//        if(stat==null){
//            System.out.println("path:"+path+" 没有子节点.");
//            return;
//        }
//        List<String> children=zk.getChildren(path,false);
//        System.out.println("path:"+path+"  子节点如下:");
//        for (int i=0;i<children.size();i++){
//            System.out.println(children.get(i));
//        }
    }

    public static void showTree(String path,int level){
        Stat stat=null;
        try {
            stat=znode_exists(path);
        }catch (Exception e){

        }
        StringBuffer sb=new StringBuffer();
        for (int i = 0; i < level; i++) {
            sb.append("    ");
        }
        System.out.println(sb.toString()+path);
        List<String> children=null;
        try{
            children=zk.getChildren(path,false);
        }catch (Exception e){
            //如果children为空,则报空指针异常，且说明该节点为/，不必执行下面语句
            return;//中断递归
        }

        for (int i = 0; i <children.size() ; i++) {
            String sonPath=children.get(i);
            if (level==0){
                showTree(path+sonPath,level+1);
            }else {
                showTree(path+"/"+sonPath,level+1);
            }
        }
    }

}
