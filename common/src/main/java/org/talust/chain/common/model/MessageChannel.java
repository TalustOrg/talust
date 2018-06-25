package org.talust.chain.common.model;

/**
 * 与消息通道有送的封装
 */
public class MessageChannel {
    //底层收发的消息
    private Message message;
    //从哪个channel发出来的消息,主要用于内部传递消息时所用,一般不传到网络中
    private String fromIp;
    //准备发往哪一个channel,只在本地队列流转时有用,平时一般不使用
    private String toIp;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public String getFromIp() {
        return fromIp;
    }

    public void setFromIp(String fromIp) {
        this.fromIp = fromIp;
    }

    public String getToIp() {
        return toIp;
    }

    public void setToIp(String toIp) {
        this.toIp = toIp;
    }
}
