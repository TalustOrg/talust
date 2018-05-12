package com.talust.chain.network.netty;

import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.common.model.MessageType;
import com.talust.chain.network.model.MyChannel;
import com.talust.chain.network.netty.queue.MessageQueue;
import com.talust.chain.common.model.Message;
import io.netty.channel.Channel;
import io.netty.util.internal.ConcurrentSet;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j//socket容器
public class ChannelContain {
    private static ChannelContain instance = new ChannelContain();

    private ChannelContain() {
    }

    public static ChannelContain get() {
        return instance;
    }

    //当前节点所有的连接通道,包含主动和被动所获得的
    private Map<String, MyChannel> mapChannel = new ConcurrentHashMap<>();
    //当前节点连接的超级节点,主要针对当前节点是超级节点的情况下,需要连接所有超级节点
    private Map<String, MyChannel> superChannel = new ConcurrentHashMap<>();
    //当前整个网络节点的ip地址
    private Set<String> allNodeIps = new ConcurrentSet<>();
    //连接管理
    private ConnectionManager cm = ConnectionManager.get();
    //超级节点集合
    private Set<String> superIps = cm.getSuperIps();
    //消息队列
    private MessageQueue mq = MessageQueue.get();

    public void addChannel(Channel sc, boolean isPassive) {
        MyChannel myChannel = new MyChannel();
        myChannel.setChannel(sc);
        myChannel.setPassive(isPassive);
        InetSocketAddress insocket = (InetSocketAddress) sc.remoteAddress();
        String remoteIp = insocket.getAddress().getHostAddress();
        myChannel.setRemoteIp(remoteIp);
        InetSocketAddress loSocket = (InetSocketAddress) sc.localAddress();
        String localIp = loSocket.getAddress().getHostAddress();
        myChannel.setLocalIp(localIp);
        mapChannel.put(remoteIp, myChannel);
        if (superIps.contains(remoteIp)) {//对方是超级节点
            superChannel.put(remoteIp, myChannel);
        }
        allNodeIps.add(remoteIp);
        ConnectionManager.get().selfIp = localIp;
    }

    public synchronized void removeChannel(Channel sc) {
        if (sc != null) {
            InetSocketAddress insocket = (InetSocketAddress) sc.remoteAddress();
            String remoteIp = insocket.getAddress().getHostAddress();
            if (mapChannel.containsKey(remoteIp)) {
                mapChannel.remove(remoteIp);
                superChannel.remove(remoteIp);

                Message message = new Message();
                message.setType(MessageType.NODE_EXIT.getType());
                message.setContent(remoteIp.getBytes());
                MessageChannel mc = new MessageChannel();
                mc.setMessage(message);
                mq.addMessage(mc);
            }
            allNodeIps.remove(remoteIp);
        }
    }

    /**
     * 获取所有通道
     *
     * @return
     */
    public Collection<MyChannel> getMyChannels() {
        return mapChannel.values();
    }

    public Collection<MyChannel> getSuperChannels() {
        return superChannel.values();
    }

    /**
     * 新增一个网络节点ip
     *
     * @param ip
     */
    public void addNodeIp(String ip) {
        allNodeIps.add(ip);
        printChannel();
    }

    /**
     * 移除某个网络节点ip
     *
     * @param ip
     */
    public void removeNodeIp(String ip) {
        allNodeIps.remove(ip);
        mapChannel.remove(ip);
        superChannel.remove(ip);
        printChannel();
    }

    /**
     * 返回所有节点ip
     *
     * @return
     */
    public Set<String> getAllNodeIps() {
        return this.allNodeIps;
    }

    /**
     * 打印当前容器数
     */
    private synchronized void printChannel() {
        StringBuilder sb = new StringBuilder();
        sb.append("本节点的连接数为:");
        sb.append(mapChannel.size() + ",分别连接:");
        for (MyChannel channel : mapChannel.values()) {
            sb.append(channel.getRemoteIp() + ":" + channel.isPassive() + ",");
        }
        sb.append("全网节点有:");
        for (String allNodeIp : allNodeIps) {
            sb.append(allNodeIp + ",");
        }
        log.info("{}", sb.toString());
    }

    /**
     * 向cid所指向的通道发送消息
     *
     * @param remoteIp
     * @param message
     */
    public void sendMessage(String remoteIp, Message message) {
        MyChannel myChannel = mapChannel.get(remoteIp);
        if (myChannel != null) {
            myChannel.getChannel().writeAndFlush(message);
        }
    }

    /**
     * 返回本节点主动连接远端的连接数
     *
     * @return
     */
    public int getActiveConnectionCount() {
        int count = 0;
        for (MyChannel myChannel : mapChannel.values()) {
            boolean passive = myChannel.isPassive();
            if (!passive) {
                count++;
            }
        }
        return count;
    }

    /**
     * 返回本节点被动连接的连接数
     *
     * @return
     */
    public int getPassiveConnCount() {
        int count = 0;
        for (MyChannel myChannel : mapChannel.values()) {
            boolean passive = myChannel.isPassive();
            if (passive) {
                count++;
            }
        }
        return count;
    }

//    /**
//     * 获取某channel的ip地址
//     *
//     * @param cid
//     * @return
//     */
//    public String getChannelIp(String cid) {
//        if (cid != null && cid.length() > 0) {
//            MyChannel channel = mapChannel.get(cid);
//            if (channel != null) {
//                return channel.getRemoteIp();
//            }
//        }
//        return null;
//    }

}
