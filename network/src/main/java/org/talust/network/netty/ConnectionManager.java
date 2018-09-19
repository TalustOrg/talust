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
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.model.MessageType;
import org.talust.common.model.SuperNode;
import org.talust.common.tools.*;
import org.talust.network.netty.client.NodeClient;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 连接管理者,用于实现底层的连接,上层应用中不需要关心本层次的连接问题
 *
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
     * 出块节点存储
     */
    public String masterIp;
    /**
     * 超级节点信息
     */
    private Map<String, SuperNode> superNodes = new HashMap<>();


    private ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1);
    private AtomicBoolean genRunning = new AtomicBoolean(false);
    /**
     * 当前节点是否是超级节点
     */
    public boolean superNode = false;

    /**
     * 节点自身ip地址
     */
    public String selfIp = null;

    /**
     * 存储当前节点的ip地址,可能有多个
     */
    private Set<String> myIps = new HashSet<>();


    /**
     * 初始化方法,主要用于定时检测节点连接情况,发现连接数过少时,就需要同步一下连接
     * 业务节点需要请求是否可以连接，而后断开，再进行连接再进行文件获取
     * 如果不得已而去连接超级节点的话，则会请求是否可以连接，然后断开，再去连接以及获取peers文件
     */
    public void init() {
        initSuperIps();
    }

    /**
     * 初始化固定超级服务器ip地址,用于当前节点的连接所用
     */
    private void initSuperIps() {
        myIps.addAll(IpUtil.getOutIps());
        log.info("本地外网IP抓取为：" + myIps.toString());
        JSONObject ips = getJsonFile(Configure.NODE_SERVER_ADDR);
        List<String> minings = new ArrayList<>();
        for (Object map : ips.entrySet()) {
            JSONObject ipContent = (JSONObject) ((Map.Entry) map).getValue();
            String ip = ipContent.getString("ip");
            minings.add(ipContent.getString("address"));
            SuperNode snode = new SuperNode();
            snode.setCode(Integer.parseInt((String) ((Map.Entry) map).getKey()));
            snode.setIp(ip);
            snode.setAddress(ipContent.getString("address"));
            snode.setId((String) ((Map.Entry) map).getKey());
            if (!myIps.contains(ip)) {
                superIps.add(ip);
            } else {
                log.info("当前节点IP为：{} 确认为超级节点", ip);
                superNode = true;
                selfIp = ip;
            }
            superNodes.put(ip, snode);
        }
        CacheManager.get().put(new String(Constant.MINING_ADDRESS), minings);
        if (!superNode && null == selfIp) {

            myIps.addAll(IpUtil.getInIps());
            selfIp = myIps.iterator().next();
            log.info("非超级节点变更自身IP为：{}", selfIp);
        }
        log.info("获得超级节点数为:{}", superIps.size());

        long delay = 0;
        service.scheduleAtFixedRate(() -> {
            if (genRunning.get()) {
                int activeSize = ChannelContain.get().getActiveConnectionCount();
                int passiveSize = ChannelContain.get().getPassiveConnCount();
                log.info("网络检查，获取当前网络连接状态:主动连接：{},被动连接：{}", activeSize, passiveSize);
                if (activeSize == 0 && passiveSize == 0) {
                    if (superNode) {
                        superNodeJoin();
                    } else {
                        normalNodeJoin();
                    }
                }
            }
        }, delay, Configure.NET_CHECK_TIME, TimeUnit.SECONDS);
        log.info("启动定时任务检查网络,延时:{}...", delay);
    }


    /**
     * 开始网络监察
     */
    public void startNetCheck() {
        genRunning.set(true);
    }

    /**
     * 连接普通节点
     */
    public void normalNodeJoin() {
        ChannelContain cc = ChannelContain.get();
        JSONObject peerJ = JSONObject.parseObject(PeersManager.get().peerCont);
        List<String> unusedIps = new ArrayList<>();
        if (null != peerJ) {
            for (Object map : peerJ.entrySet()) {
                String trust = (String) ((Map.Entry) map).getValue();
                String peerIp = (String) ((Map.Entry) map).getKey();
                if (!ChannelContain.get().validateIpIsConnected(peerIp)) {
                    if (!"0".equals(trust)) {
                        String status = connectByIp(peerIp, cc);
                        if ("FULL".equals(status)) {
                            break;
                        }
                        switch (status) {
                            case "OK":
                                try {
                                    getPeersOnline(peerIp);
                                } catch (Exception e) {
                                    continue;
                                }
                                break;
                            case "FAIL":
                                unusedIps.add((String) ((Map.Entry) map).getKey());
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        if(null!=unusedIps&&unusedIps.size()>0){
            PeersManager.get().removePeerList(unusedIps);
        }
        if (cc.getActiveConnectionCount() < Configure.MAX_ACTIVE_CONNECT_COUNT) {
            superNodeJoin();
        }
    }

    /**
     * 链接单个节点
     */
    private String connectByIp(String ip, ChannelContain cc) {
        try {
            int nowConnSize = cc.getActiveConnectionCount();
            if (nowConnSize < Configure.MAX_ACTIVE_CONNECT_COUNT) {
                log.info("我允许的主动连接总数:{},当前主动连接总数:{},连接我的总数:{},准备连接的ip:{}",
                        Configure.MAX_ACTIVE_CONNECT_COUNT, cc.getActiveConnectionCount(), cc.getPassiveConnCount(), ip);
                if (!ChannelContain.get().validateIpIsConnected(ip)) {
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
    public JSONObject getPeersOnline(String peersIp) throws Exception {
        Channel channel;
        boolean isConnected = ChannelContain.get().validateIpIsConnected(peersIp);
        if (!isConnected) {
            NodeClient nc = new NodeClient();
            channel = nc.connect(peersIp, Constant.PORT);
            ChannelContain.get().addChannel(channel, false);
        } else {
            channel = ChannelContain.get().getChannelByIp(peersIp);
        }
        JSONObject peers = new JSONObject();
        Message nm = new Message();
        nm.setType(MessageType.NODES_REQ.getType());
        log.info("向节点ip:{} 请求当前网络的所有节点...", peersIp);
        InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.remoteAddress();
        String remoteIp = inetSocketAddress.getAddress().getHostAddress();
        MessageChannel message = SynRequest.get().synReq(nm, remoteIp);
        if (message != null) {
            peers = SerializationUtil.deserializer(message.getMessage().getContent(), JSONObject.class);
            if (peers != null && peers.keySet().size() > 0) {
                log.info("节点ip:{} 返回当前网络的所有节点数:{}", peersIp, peers.keySet().size());
            }
            if (peers.containsKey(selfIp)) {
                peers.remove(selfIp);
            }
            if(null!=peers){
                PeersManager.get().addPeer(peers);
            }

        }
        if (!isConnected) {
            ChannelContain.get().removeChannelNoBroad(channel);
        }
        return peers;
    }

    /**
     * 连接到超级节点
     */
    public void superNodeJoin() {
        log.info("连接网络,当前节点ip:{},全网超级节点数:{}", this.selfIp, superIps.size());
        ChannelContain cc = ChannelContain.get();
        if (superIps.size() > 0) {
            connectAllSuperNode(superIps, cc);
        }
    }

    /**
     * 连接所有超级节点
     *
     * @param nodeIps
     * @param cc
     */
    private void connectAllSuperNode(Collection<String> nodeIps, ChannelContain cc) {
        int needConCount = 0;
        if (isSuperNode()) {
            needConCount = Configure.MAX_SUPER_ACTIVE_CONNECT_COUNT - cc.getActiveConnectionCount();
        } else {
            needConCount = Configure.MAX_ACTIVE_CONNECT_COUNT - cc.getActiveConnectionCount();
        }
        for (String ip : nodeIps) {
            if (needConCount > 0) {
                try {
                    if (!ChannelContain.get().validateIpIsConnected(ip)) {
                        log.info("本节点连接超级节点目标ip地址:{}", ip);
                        NodeClient tmpnc = new NodeClient();
                        Channel connect = tmpnc.connect(ip, Constant.PORT);
                        cc.addChannel(connect, false);
                        InetSocketAddress insocket = (InetSocketAddress) connect.localAddress();
                        getPeersOnline(ip);
                        needConCount--;
                    }
                } catch (Throwable e) {
                    continue;
                }
            } else {
                break;
            }
        }
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
            if (HttpResult != HttpURLConnection.HTTP_OK) {
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
     * 交易消息传递
     */
    public void TXMessageSend(Message message) {
        if (isSuperNode()) {
            ChannelContain.get().sendMessageToSuperNode(message);
        } else {
            ChannelContain.get().sendMessageToRandomSuperNode(message);
        }
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
        return superNode;
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

    public String getMasterIp() {
        return masterIp;
    }

    public void setMasterIp(String masterIp) {
        this.masterIp = masterIp;
    }
}
