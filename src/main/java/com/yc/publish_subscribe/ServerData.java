package com.yc.publish_subscribe;

import lombok.Data;

/**
 * 服务器的基本信息
 */
@Data
public class ServerData {

    private String address;
    private Integer id;
    private String name;
}
