/*
 * MIT License
 *
 * Copyright (c) 2017-2018 talust.org talust.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.talust.network.netty;

import org.talust.common.model.MessageChannel;
import org.talust.common.model.MessageType;

import org.talust.common.model.Message;
import io.netty.channel.Channel;
import io.netty.util.internal.ConcurrentSet;
import lombok.extern.slf4j.Slf4j;
import org.talust.common.tools.Constant;
import org.talust.network.model.MyChannel;
import org.talust.network.netty.client.NodeClient;
import org.talust.network.netty.queue.MessageQueue;

import java.net.InetSocketAddress;
import java.util.*;
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
        if (superIps.contains(remoteIp)) {
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
            }
            allNodeIps.remove(remoteIp);
            Message message = new Message();
            message.setType(MessageType.NODE_EXIT.getType());
            message.setContent(remoteIp.getBytes());
            MessageChannel mc = new MessageChannel();
            mc.setMessage(message);
            mq.addMessage(mc);
        }
    }


    public synchronized void removeChannelNoBroad(Channel sc) {
        if (sc != null) {
            InetSocketAddress insocket = (InetSocketAddress) sc.remoteAddress();
            String remoteIp = insocket.getAddress().getHostAddress();
            if (mapChannel.containsKey(remoteIp)) {
                mapChannel.remove(remoteIp);
                superChannel.remove(remoteIp);
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

    public boolean validateIpIsConnected(String  ip){
        return  ChannelContain.get().mapChannel.containsKey(ip);
    }

    public Channel getChannelByIp(String ip){
        return  ChannelContain.get().mapChannel.get(ip).getChannel();
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
     * 向所有的超级节点发送消息
     */
    public void sendMessageToSuperNode( Message message){
        log.info("向所有超级节点扩散消息");
        if(null!=superIps&&superIps.size()>0){
            for(String ip : superIps){
                sendMessage(ip,message);
            }
        }
    }
    /**
     * 向随机的一个超级节点发送消息
     */
    public void sendMessageToRandomSuperNode(Message message){
        log.info("向随机超级节点扩散消息");
        List<String> snodes = new ArrayList<>();
        if(null!=superChannel&&superChannel.size()>0){
            snodes = new ArrayList<>(superChannel.size());
            for (String fixedIp : superChannel.keySet()) {
                snodes.add(fixedIp);
            }
        }else if(null!=superIps){
            snodes = new ArrayList<>(superIps.size());
            for (String fixedIp : superIps) {
                snodes.add(fixedIp);
            }
        }
        int size = snodes.size();
        if (size > 0) {
            Random rand = new Random();
            int selNode = rand.nextInt(size);
            String node = snodes.get(selNode);
            if(IsIpConnect(node)){
                sendMessage(node,message);
            }else{
                sendMessageWhenNotConnected(node,message);
            }

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
    /**
     * 查询该IP 是否已经被连接
     */
    public boolean IsIpConnect(String ip){
       return  mapChannel.containsKey(ip);
    }

    /**
     * 连接IP发送消息后移除连接
     */
    public void sendMessageWhenNotConnected(String ip , Message message){
        try {
            NodeClient tmpnc = new NodeClient();
            Channel  connect = tmpnc.connect(ip, Constant.PORT);
            addChannel(connect, false);
            sendMessage(ip,message);
            removeChannelNoBroad(connect);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
