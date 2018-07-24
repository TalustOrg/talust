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

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.util.internal.ConcurrentSet;
import lombok.extern.slf4j.Slf4j;
import org.talust.common.crypto.Utils;
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.model.MessageType;
import org.talust.common.model.SuperNode;
import org.talust.common.tools.*;
import org.talust.network.model.AllNodes;
import org.talust.network.model.MyChannel;
import org.talust.network.netty.client.NodeClient;
import org.talust.network.netty.queue.MessageQueue;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 连接管理者,用于实现底层的连接,上层应用中不需要关心本层次的连接问题
 * @author
 */

@Slf4j
public class ConnectionManager {
    private static ConnectionManager instance = new ConnectionManager();

    private ConnectionManager() {
    }

    public static ConnectionManager get() {
        return instance;
    }

    /**
     * 存储当前网络的超级节点ip地址
     */
    private Set<String> superIps = new ConcurrentSet<>();
    /**
     * 超级节点信息
     */
    private Map<String, SuperNode> superNodes = new HashMap<>();
    /**
     * 当前节点是否是超级节点
     */
    public boolean superNode = false;
    /**
     * 当前节点是否是创世ip
     */
    public boolean genesisIp = false;
    /**
     * 节点允许主动连接其他节点数
     */
    private int connSize = Configure.MAX_ACTIVE_CONNECT_COUNT;
    /**
     * 节点自身ip地址
     */
    public String selfIp = null;

    private MessageQueue mq = MessageQueue.get();
    /**
     * 存储当前节点的ip地址,可能有多个
     */
    private Set<String> myIps = new HashSet<>();
    /**
     * 主要用于判断当前节点是否可用,当同步网络完成之后,表示为可用,网络同步未完成,则表示为不可用
     */
    public AtomicBoolean amEnabled = new AtomicBoolean(false);
    /**
     * 初始化方法,主要用于定时检测节点连接情况,发现连接数过少时,就需要同步一下连接
     */
    public void init() {
        initSuperIps();
        if (!superNode) {
            normalNodeJoin();
        }else{
            superNodeJoin();
        }

    }

    /**
     * 连接普通节点
     */
    private void normalNodeJoin() {
        ChannelContain cc = ChannelContain.get();
        JSONObject peerJ = JSONObject.parseObject(PeersManager.get().peerCont);
        while(peerJ.entrySet().size()==0){
            for (String fixedIp : superIps) {
                try {
                    peerJ=getPeersOnline(fixedIp,peerJ);
                    break;
                } catch (Exception e) {
                    continue;
                }
            }
        }
        List<String> unusedIps = new ArrayList<>();
        for (Object map : peerJ.entrySet()) {
            String trust  = (String) ((Map.Entry) map).getValue();
            String peerIp = (String) ((Map.Entry) map).getKey();
            if (!"0".equals(trust)) {
                String status = connectByIp(peerIp, cc);
                if ("FULL".equals(status)) {
                    break;
                }
                switch (status) {
                    case "OK":
                        nodesJoinBroadcast(peerIp);
                        try {
                            getPeersOnline(peerIp,peerJ);
                        } catch (Exception e) {
                            continue;
                        }
                        break;
                    case "FAIL":
                            unusedIps.add((String) ((Map.Entry) map).getKey());
                            break;
                    default:break;
                }
            }
        }
        for (String ip : unusedIps) {
            peerJ.remove(ip);
        }
        String peers = peerJ.toJSONString();
        PeersManager.get().writePeersFile(peers);
        if(cc.getActiveConnectionCount()==0){
            superNodeJoin();
        }
    }

    /**
     * 链接单个节点
     */
    private String connectByIp(String ip, ChannelContain cc) {
        try {
            int nowConnSize = cc.getActiveConnectionCount();
            if (nowConnSize < connSize) {
                log.info("我允许的主动连接总数:{},当前主动连接总数:{},连接我的总数:{},准备连接的ip:{}",
                        connSize, cc.getActiveConnectionCount(), cc.getPassiveConnCount(), ip);
                boolean needConnection = true;
                for (MyChannel channel : cc.getMyChannels()) {
                    String remoteIP = channel.getRemoteIp();
                    if (remoteIP.equals(ip)) {
                        needConnection = false;
                    }
                }
                if (needConnection) {
                    log.info("本节点连接目标ip地址:{}", ip);
                    NodeClient nodeClient = new NodeClient();
                    Channel connect = nodeClient.connect(ip, Constant.PORT);
                    cc.addChannel(connect, false);
                }
                return "OK";
            } else {
                return "FULL";
            }
        } catch (Throwable e) {
            return "FAIL";
        }
    }
    /**
     * 获取连接节点的已连接peers 数据
     */
    public JSONObject getPeersOnline(String peersIp,JSONObject peersNow) throws Exception {
        NodeClient nc = new NodeClient();
        JSONObject peers = new JSONObject();
        Channel channel = nc.connect(peersIp, Constant.PORT);
        ChannelContain.get().addChannel(channel, false);
        Message nm = new Message();
        nm.setType(MessageType.NODES_REQ.getType());
        log.info("向节点ip:{} 请求当前网络的所有节点...", peersIp);
        InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.remoteAddress();
        String remoteIp = inetSocketAddress.getAddress().getHostAddress();
        MessageChannel message = SynRequest.get().synReq(nm, remoteIp);
        if (message != null) {
            peers  = SerializationUtil.deserializer(message.getMessage().getContent(), JSONObject.class);
            peers.putAll(peersNow);
            PeersManager.get().writePeersFile(peers.toJSONString());
            if (peers != null && peers.keySet().size()>0) {
                log.info("节点ip:{} 返回当前网络的所有节点数:{}", peersIp,  peers.keySet().size());
            }
        }
        return  peers;
    }

    /**
     * 连接到超级节点
     */
    private void superNodeJoin() {
        List<String> snodes = new ArrayList<>(superIps.size());
        for (String fixedIp : superIps) {
            snodes.add(fixedIp);
        }
        log.info("连接到网络,当前节点ip:{},全网超级节点数:{}", this.selfIp, snodes.size());
        ChannelContain cc = ChannelContain.get();
        int size = snodes.size();
        if (size > 0) {
            List<String> nodes = null;
            Random rand = new Random();
            while (size > 0) {
                //确保能够成功连接到一台节点获得当前所有连接
                int selNode = rand.nextInt(size);
                //随机选择一台固定节点以获取当前所有可用的网络节点
                String node = snodes.get(selNode);
                try {
                    getPeersOnline(node,new JSONObject());
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                snodes.remove(selNode);
                size = snodes.size();
            }
            if (nodes != null && nodes.size() > 0) {
                List<String> sIps = new ArrayList<>();
                List<String> normalIps = new ArrayList<>();
                for (String nodeIp : nodes) {
                    cc.addNodeIp(nodeIp);
                    //将这些返回的节点加入到本地用于存储网络节点记录中
                    log.info("返回的节点ip:{}", nodeIp);
                    if (this.superIps.contains(nodeIp)) {
                        sIps.add(nodeIp);
                    } else {
                        normalIps.add(nodeIp);
                    }
                }
                log.info("当前所有网络中,超级节点数:{},普通节点数:{}", sIps.size(), normalIps.size());
                connectAllSuperNode(sIps, cc);

            }
        }
    }

    /**
     * 对象节点通知本节点加入
     */
    public void nodesJoinBroadcast(String ip){
        Message nodeMessage = new Message();
        nodeMessage.setType(MessageType.NODE_JOIN.getType());
        nodeMessage.setContent(selfIp.getBytes());
        log.info("向当前网络发送当前节点ip:{}", selfIp);
        MessageChannel mc = new MessageChannel();
        mc.setMessage(nodeMessage);
        mc.setToIp(ip);
        mq.addMessage(mc);
    }


    /**
     * 连接所有超级节点
     *
     * @param nodeIps
     * @param cc
     */
    private void connectAllSuperNode(List<String> nodeIps, ChannelContain cc) {
        for (String ip : nodeIps) {
            try {//依次连接各个ip
                boolean needConnection = true;
                for (MyChannel channel : cc.getMyChannels()) {
                    String remoteIP = channel.getRemoteIp();
                    if (remoteIP.equals(ip)) {
                        needConnection = false;
                    }
                }
                if (needConnection) {
                    log.info("本节点连接超级节点目标ip地址:{}", ip);
                    NodeClient tmpnc = new NodeClient();
                    Channel connect = tmpnc.connect(ip, Constant.PORT);
                    cc.addChannel(connect, false);
                    InetSocketAddress insocket = (InetSocketAddress) connect.localAddress();
                    selfIp = insocket.getAddress().getHostAddress();
                    nodesJoinBroadcast(ip);
                }
            } catch (Throwable e) {
            }
        }
    }


    /**
     * 初始化固定超级服务器ip地址,用于当前节点的连接所用
     */
    private void initSuperIps() {
        try {
            Enumeration<?> e1 = NetworkInterface.getNetworkInterfaces();
            while (e1.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) e1.nextElement();
                Enumeration<?> e2 = ni.getInetAddresses();
                while (e2.hasMoreElements()) {
                    InetAddress ia = (InetAddress) e2.nextElement();
                    if (ia instanceof Inet6Address){
                        continue;
                    }
                    myIps.add(ia.getHostAddress());
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        JSONObject gip = getJsonFile(Configure.GENESIS_SERVER_ADDR);
        JSONObject root = gip.getJSONObject("root");
        CacheManager.get().put("ROOT_PK", root.get("publickey"));
        CacheManager.get().put("ROOT_SIGN", root.get("sign"));
        JSONObject talust = gip.getJSONObject("talust");
        CacheManager.get().put("TALUST_PK", talust.get("publickey"));
        CacheManager.get().put("TALUST_SIGN", talust.get("sign"));
        if (myIps.contains(gip.getString("genesisIp"))) {
            genesisIp = true;
        }
        JSONObject ips = getJsonFile(Configure.NODE_SERVER_ADDR);
        List<JSONObject> minings = new ArrayList<>();
        for (Object map : ips.entrySet()) {
            JSONObject ipContent = (JSONObject) ((Map.Entry) map).getValue();
            String ip = ipContent.getString("ip");
            minings.add(ipContent);
            SuperNode snode = new SuperNode();
            snode.setCode(Integer.parseInt((String) ((Map.Entry) map).getKey()));
            snode.setIp(ip);
            snode.setAddress(Utils.showAddress(Utils.getAddress(ipContent.getBytes("miningPublicKey"))));
            if (!myIps.contains(ip)) {
                superIps.add(ip);
            } else {
                superNode = true;
            }
            superNodes.put(ip, snode);
        }
        CacheManager.get().put("MININGS", minings);
        if (null == selfIp) {
            selfIp = myIps.iterator().next();
        }
        log.info("获得超级节点数为:{}", superIps.size());
    }

    public JSONObject getJsonFile(String filePath) {
        JSONObject jsonObject = null;
        try {
            String input = getIps(filePath);
            jsonObject = JSONObject.parseObject(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * 获取超级服务器节点信息
     *
     * @param filePath
     * @return
     */
    private String getIps(String filePath) {
        int HttpResult;
        String ee = new String();
        try {
            URL url = new URL(filePath);
            URLConnection urlconn = url.openConnection();
            urlconn.connect();
            HttpURLConnection httpconn = (HttpURLConnection) urlconn;
            HttpResult = httpconn.getResponseCode();
            if (HttpResult != HttpURLConnection.HTTP_OK)
            {
                log.error("无法连接到服务器获取节点列表...");
            } else {
                InputStreamReader isReader = new InputStreamReader(urlconn.getInputStream());
                BufferedReader reader = new BufferedReader(isReader);
                StringBuffer buffer = new StringBuffer();
                String line;
                line = reader.readLine();
                while (line != null) {
                    buffer.append(line);
                    line = reader.readLine();
                }
                ee = buffer.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ee;
    }

    /**
     * 获取超级网络节点
     */
    public Set<String> getSuperIps() {
        return superIps;
    }

    /**
     * 判断ip是否是本节点的ip
     */
    public boolean isSelfIp(String ip) {
        return myIps.contains(ip);
    }

    /**
     * 判断当前ip是否是超级节点
     */
    public boolean isSuperNode() {
        for (String myIp : myIps) {
            if (superIps.contains(myIp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回当前节点的ip地址,即对外的ip地址
     */
    public String getSelfIp() {
        return this.selfIp;
    }

    public Collection<SuperNode> getSuperNodes() {
        return this.superNodes.values();
    }

    /**
     * 获取超级节点的信息
     */
    public SuperNode getSuperNodeByIp(String superIp) {
        return superNodes.get(superIp);
    }

}