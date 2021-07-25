package com.yc.publish_subscribe;

import lombok.Data;
import org.apache.jute.InputArchive;
import org.apache.jute.OutputArchive;
import org.apache.jute.Record;

import java.io.IOException;

/**
 * 微服务的配置信息，要存到zk中（config节点），因为要存，所以需要实现可序列化
 */
@Data
public class ServerConfig implements Record {
    private String dbUrl;
    private String dbPwd;
    private String dbUser;


    @Override
    public void serialize(OutputArchive archive, String tag) throws IOException {
        archive.startRecord(this,tag);
        archive.writeString(dbUrl,"dbUrl");
        archive.writeString(dbPwd,"dbPwd");
        archive.writeString(dbUser,"dbUser");
        archive.endRecord(this,tag);
    }

    @Override
    public void deserialize(InputArchive archive, String tag) throws IOException {
        archive.startRecord(tag);
        this.dbUrl=archive.readString("dbUrl");
        this.dbPwd=archive.readString("dbPwd");
        this.dbUser=archive.readString("dbUser");
        archive.endRecord(tag);
    }
}
