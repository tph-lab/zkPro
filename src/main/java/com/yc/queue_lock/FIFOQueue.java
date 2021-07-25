package com.yc.queue_lock;
import lombok.SneakyThrows;
import org.I0Itec.zkclient.ExceptionUtil;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式先入先出队列
 * 如何使用Zookeeper来实现队列
 * 不能确定，谁先进入队列？解决：利用顺序节点来实现
 */
public class FIFOQueue <T>{

    static AtomicInteger integer=new AtomicInteger(1);
    //客户端,需要客户端去操作zookeeper
    protected final  ZkClient zkClient;
    //可以看成虚拟头节点
    protected final String root;
    //  /queue/n_000000000010
    protected static final String Node_name="n_";


    public FIFOQueue(ZkClient zkClient, String root) {
        this.zkClient = zkClient;
        this.root = root;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        //创建一个zookeeper的客户端
        ZkClient zkClient=new ZkClient("node1:2181,node2:2181,node3:2181");
        //创建一个任务队列
        FIFOQueue<Integer> queue=new FIFOQueue<Integer>(zkClient,"/root");
        new Thread(new ConsumerThread(queue)).start();
        new Thread(new ConsumerThread(queue)).start();
        new Thread(new ProducerThread(queue)).start();
        new Thread(new ProducerThread(queue)).start();
        new Thread(new ProducerThread(queue)).start();
    }



    //获取root下的子节点，然后截取到序号部分进行升序排序
    //取出序号最小的并删除
    T poll() {
        //获取节点列表
        List<String> childrens=zkClient.getChildren(root);
        if (childrens.size()==0){
            return null;
        }
        //使用collections集合进行排序
        //截取到任务编号之后进行升序排序
        Collections.sort(childrens, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {//升序排序
                //Node_name="n_"
                //以node_name为界，截取任务编号
                return getNodeNumber(o1,Node_name).compareTo(getNodeNumber(o2,Node_name));
            }
        });
        //排完序之后，取第一个
        //n_00000000001
        String litterNode=childrens.get(0);
        String fullPath=root.concat("/").concat(litterNode);
        //读取节点信息
        T data=zkClient.readData(fullPath);
        zkClient.delete(fullPath);
        return data;
    }

    //以node_name为界，截取任务编号
    private String getNodeNumber(String str, String node_name) {
        int index=str.lastIndexOf(node_name);
        if(index>=0){
            index+=Node_name.length();
            return index<=str.length()?str.substring(index):"";
        }
        return str;
    }

     static class ProducerThread implements Runnable {

        FIFOQueue<Integer> queue;
        public ProducerThread() {
        }

        public ProducerThread(FIFOQueue<Integer> queue) {
            this.queue=queue;
        }
        @SneakyThrows
        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                Thread.sleep((long) (Math.random()*500));//随机睡眠一下
                //是原子性操作
                queue.offer(FIFOQueue.integer.getAndIncrement());
            }
        }
    }

    //将节点offer到队列中，但是要保证顺序
    private boolean offer(T element) {
        String path=root.concat("/").concat(Node_name);
        try {
            //创建节点和原生 API 相比，原生只能传 byte 数组，这里可以传 Object。而且父节点不存在可以指定是否创建。
            //创建有序节点，并且放入数据
            String node=zkClient.createEphemeralSequential(path,element);
            System.out.println("节点"+node+"添加了"+element.toString()+"元素");
        }catch (ZkNoNodeException e){
            zkClient.createPersistent(root);
            offer(element);
        }catch (Exception e){
            throw ExceptionUtil.convertToRuntimeException(e);
        }
        return true;
    }


     static class ConsumerThread implements Runnable {
        FIFOQueue<Integer> queue;

        public ConsumerThread() {
        }
        public ConsumerThread(FIFOQueue<Integer> queue) {
            this.queue=queue;
        }

        //取出一个任务
        @SneakyThrows
        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                Thread.sleep((long) (Math.random()*1000));//随机睡眠一下
                System.out.println(Thread.currentThread()+"取出了:"+queue.poll());
            }
        }
    }
}
