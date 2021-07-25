package com.yc.master_selection;

import lombok.Data;
import org.apache.jute.*;
import org.apache.zookeeper.server.ByteBufferInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@Data
public class RunningData implements Record {

    //服务器编号
    private Long cid;

    //服务器名
    private String name;

    //序列化
    @Override
    public void serialize(OutputArchive outputArchive, String tag) throws IOException {
        outputArchive.startRecord(this,tag);
        outputArchive.writeLong(cid,"cid");
        outputArchive.writeString(name,"name");
        outputArchive.endRecord(this,tag);
    }

    //反序列化
    @Override
    public void deserialize(InputArchive inputArchive, String tag) throws IOException {
        inputArchive.startRecord(tag);
        this.cid=inputArchive.readLong("cid");//相对应
        this.name=inputArchive.readString("name");
        inputArchive.endRecord(tag);
    }


    public static void main(String[] args) throws IOException {
        RunningData rd=new RunningData();
        rd.setCid(1L);
        rd.setName("hello");

        //序列化上面的对象
        //内存流  它可以将数据缓存到内存中，再一次性转为byte[]
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        BinaryOutputArchive boa=BinaryOutputArchive.getArchive(baos);
        //序列化对象，将其存到内存流中
        rd.serialize(boa,"header");
        byte[] bs=baos.toByteArray();

        //反序列化  bs  数组
        ByteBuffer bb=ByteBuffer.wrap(bs);
        ByteBufferInputStream bbis=new ByteBufferInputStream(bb);
        BinaryInputArchive bia=BinaryInputArchive.getArchive(bbis);
        RunningData header=new RunningData();   //空对象，用于接收数据
        //将内存流中的数据反序列化到对象中
        header.deserialize(bia,"create");

        System.out.println(header.toString());
    }
}
