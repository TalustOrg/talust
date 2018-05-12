package com.talust.chain.network.model;

import io.netty.channel.Channel;

//对通道的封装
public class MyChannel {
    private Channel channel;
    private String localIp;
    private String remoteIp;
    //是否被动连接,为真表示此channel为被动连接,即远端连接本节点获得的,为假则表示此channel为主动连接远端获得的
    private boolean isPassive;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public boolean isPassive() {
        return isPassive;
    }

    public void setPassive(boolean passive) {
        isPassive = passive;
    }
}
