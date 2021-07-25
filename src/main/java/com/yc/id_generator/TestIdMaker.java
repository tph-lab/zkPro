package com.yc.id_generator;

public class TestIdMaker {
    public static void main(String[] args) throws Exception {
        IdMaker idMaker=new IdMaker("/NameService/IdGen","ID");
        // 连接客户端，创建父节点root
        idMaker.start();
        try {
            for (int i = 0; i < 5; i++) {
                String id=idMaker.generatedId(IdMaker.RemoveMethod.DELAY);
                System.out.println(id);
            }
        }finally {
            idMaker.stop();
        }
    }
}
