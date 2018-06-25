package org.talust.chain.common.model;


import org.talust.chain.common.tools.SerializationUtil;
import io.protostuff.Tag;

/**
 * 平台消息的封装
 */
public class Message {
    @Tag(1)//消息类型
    private Integer type;
    @Tag(2)//消息内容
    private byte[] content;
    @Tag(3)//内容的签名者,指的是密钥对中的公钥
    private byte[] signer;
    @Tag(4)//内容的签名者所签的内容,一般指是对内容的hash值进行的签名
    private byte[] signContent;
    @Tag(5)//消息发送时间戳
    private Integer time;
    @Tag(6)//消息计数器,由一个AtomicLong不断累加,主要针对请求响应模型的,通过此来定位请求点
    private Long msgCount;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }


    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Long getMsgCount() {
        return msgCount;
    }

    public void setMsgCount(Long msgCount) {
        this.msgCount = msgCount;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public byte[] getSigner() {
        return signer;
    }

    public void setSigner(byte[] signer) {
        this.signer = signer;
    }

    public byte[] getSignContent() {
        return signContent;
    }

    public void setSignContent(byte[] signContent) {
        this.signContent = signContent;
    }

    public static void main(String[] args) {
        String cont = "hello world";
        String signer = "zs";
        String signContent = "signContent";
        Message nm = new Message();
        nm.setContent(cont.getBytes());
        nm.setSigner(signer.getBytes());
        nm.setSignContent(signContent.getBytes());
        byte[] serializer = SerializationUtil.serializer(nm);
        Message deserializer = SerializationUtil.deserializer(serializer, Message.class);
        System.out.println(new String(deserializer.getContent()));
        System.out.println(deserializer.getSigner());
        System.out.println(new String(deserializer.getSignContent()));
    }

}

