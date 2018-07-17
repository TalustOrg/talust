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
import org.apache.commons.io.FileUtils;
import org.talust.common.crypto.Utils;
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.model.MessageType;
import org.talust.common.model.SuperNode;
import org.talust.common.tools.Configure;
import org.talust.common.tools.Constant;
import org.talust.common.tools.SerializationUtil;
import org.talust.network.model.AllNodes;
import org.talust.network.model.MyChannel;
import org.talust.network.netty.client.NodeClient;
import org.talust.network.netty.queue.MessageQueue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

//连接管理者,用于实现底层的连接,上层应用中不需要关心本层次的连接问题
@Slf4j
public class ConnectionManager {
    private static ConnectionManager instance = new ConnectionManager();

    private ConnectionManager() {
    }

    public static ConnectionManager get() {
        return instance;
    }

    //存储当前网络的超级节点ip地址
    private Set<String> superIps = new ConcurrentSet<>();
    //超级节点信息
    private Map<String, SuperNode> superNodes = new HashMap<>();
    //当前节点是否是超级节点
    public boolean superNode = false;
    //当前节点是否是创世ip
    public boolean genesisIp = false;
    private int connSize = Configure.MAX_CONNECT_TO_COUNT;//节点允许主动连接其他节点数
    public String selfIp = null;//节点自身ip地址
    private MessageQueue mq = MessageQueue.get();
    //存储当前节点的ip地址,可能有多个
    private Set<String> myIps = new HashSet<>();
    //主要用于判断当前节点是否可用,当同步网络完成之后,表示为可用,网络同步未完成,则表示为不可用
    public AtomicBoolean amEnabled = new AtomicBoolean(false);

    /**
     * 初始化方法,主要用于定时检测节点连接情况,发现连接数过少时,就需要同步一下连接
     */
    public void init() {
        initSuperIps();
        connectToNet();
    }

    /**
     * 连接到网络各个节点
     */
    private void connectToNet() {
        List<String> snodes = new ArrayList<>(superIps.size());
        for (String fixedIp : superIps) {
            snodes.add(fixedIp);
        }
        log.info("连接到网络,当前节点ip:{},可以连接的网络ip数:{}", this.selfIp, snodes.size());
        ChannelContain cc = ChannelContain.get();
        int size = snodes.size();
        if (size > 0) {
            List<String> nodes = null;
            Random rand = new Random();
            while (size > 0) {//确保能够成功连接到一台节点获得当前所有连接
                int selNode = rand.nextInt(size);//随机选择一台固定节点以获取当前所有可用的网络节点
                String node = snodes.get(selNode);
                try {
                    NodeClient nc = new NodeClient();
                    Channel channel = nc.connect(node, Constant.PORT);
                    ChannelContain.get().addChannel(channel, false);
                    Message nm = new Message();
                    nm.setType(MessageType.NODES_REQ.getType());
                    log.info("向节点ip:{} 请求当前网络的所有节点...", node);
                    InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
                    String remoteIp = insocket.getAddress().getHostAddress();
                    MessageChannel message = SynRequest.get().synReq(nm, remoteIp);
                    if (message != null) {//说明有数据返回,即请求成功
                        AllNodes allNodes = SerializationUtil.deserializer(message.getMessage().getContent(), AllNodes.class);
                        if (allNodes != null && allNodes.getNodes() != null) {
                            nodes = allNodes.getNodes();
                            log.info("节点ip:{} 返回当前网络的所有节点数:{}", node, nodes.size());
                            break;
                        }
                    }
                } catch (Throwable e) {
                    log.info("连接ip:{} 失败,未能成功连接该节点...", node);
                }
                //如果到此处则说明当前的节点是有问题的,则重新选择另外一个节点
                snodes.remove(selNode);
                size = snodes.size();
            }
            if (nodes != null && nodes.size() > 0) {//
                List<String> sIps = new ArrayList<>();
                List<String> normalIps = new ArrayList<>();
                for (String nodeIp : nodes) {
                    cc.addNodeIp(nodeIp);//将这些返回的节点加入到本地用于存储网络节点记录中
                    log.info("返回的节点ip:{}", nodeIp);
                    if (this.superIps.contains(nodeIp)) {
                        sIps.add(nodeIp);
                    } else {
                        normalIps.add(nodeIp);
                    }
                }
                log.info("当前所有网络中,超级节点数:{},普通节点数:{}", sIps.size(), normalIps.size());
                if (superNode) {//如果当前是超级节点,则依次连接所有的超级节点
                    log.info("当前节点是超级节点,将会连接所有网络的超级节点...");
                    connectAllSuperNode(sIps, cc);
                } else {
                    Collections.shuffle(normalIps);
                    connectNode(normalIps, cc);//优先连接普通节点
                    Collections.shuffle(sIps);
                    connectNode(sIps, cc);//然后再连接超级节点
                }
                if (selfIp != null && selfIp.length() > 0) {
                    Message nodeMessage = new Message();
                    nodeMessage.setType(MessageType.NODE_JOIN.getType());
                    nodeMessage.setContent(selfIp.getBytes());
                    log.info("向当前网络发送当前节点ip:{}", selfIp);
                    MessageChannel mc = new MessageChannel();
                    mc.setMessage(nodeMessage);
                    mq.addMessage(mc);
                }
            }
        }
    }

    private void connectNode(List<String> nodeIps, ChannelContain cc) {
        for (String ip : nodeIps) {
            try {//依次连接各个ip
                int nowConnSize = cc.getActiveConnectionCount();

                if (nowConnSize >= connSize) {//说明当前节点的连接数达到了允许的值
                    break;
                }
                log.error("我允许的主动连接总数:{},当前主动连接总数:{},连接我的总数:{},准备连接的ip:{}",
                        connSize, cc.getActiveConnectionCount(), cc.getPassiveConnCount(), ip);
                boolean needConnection = true;//当前ip需要进行连接
                for (MyChannel channel : cc.getMyChannels()) {
                    String remoteIP = channel.getRemoteIp();
                    if (remoteIP.equals(ip)) {//说明当前已经连接了此ip了的
                        needConnection = false;
                    }
                }
                if (needConnection) {
                    log.info("本节点连接目标ip地址:{}", ip);
                    NodeClient tmpnc = new NodeClient();
                    Channel connect = tmpnc.connect(ip, Constant.PORT);
                    cc.addChannel(connect, false);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
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
                boolean needConnection = true;//当前ip需要进行连接
                for (MyChannel channel : cc.getMyChannels()) {
                    String remoteIP = channel.getRemoteIp();
                    if (remoteIP.equals(ip)) {//说明当前已经连接了此ip了的
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
                    if (ia instanceof Inet6Address)
                        continue;
                    myIps.add(ia.getHostAddress());
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        JSONObject Gip = getJsonFile(Configure.GENESIS_SERVER_ADDR);
        if (myIps.contains(Gip.getString("genesisIp"))) {
            genesisIp = true;//说明当前节点是创世块产生的ip
        }

        JSONObject ips = getJsonFile(Configure.NODE_SERVER_ADDR);
        for(Object map : ips.entrySet()){
           JSONObject ipContent = JSONObject.parseObject((String) ((Map.Entry)map).getValue());
           String ip  = ipContent.getString("ip");
            SuperNode snode = new SuperNode();
            snode.setCode((Integer) ((Map.Entry)map).getKey());
            snode.setIp(ip);
            snode.setAddress(Utils.showAddress(Utils.getAddress(ipContent.getBytes("miningPublicKey"))));
           if(!myIps.contains(ip)){
               superIps.add(ip);
           }else{
               superNode = true;
           }
            superNodes.put(ip, snode);
        }
        if(null==selfIp){
            selfIp=  myIps.iterator().next();
        }
        log.info("获得超级节点数为:{}", superIps.size());
    }

    //TODO  configure file  reload method  need change logic
    public JSONObject getJsonFile(String filename) {
        System.out.println(  getClass().getClassLoader().getResource("").toString());
        System.out.println(  getClass().getResource("").toString());
        String path = getClass().getClassLoader().getResource(filename).toString();

        path = path.replace("\\", "/");
        if (path.contains(":")) {
            path = path.replace("file:/","");// 2
        }
        JSONObject jsonObject = null;
        try {
            String input = FileUtils.readFileToString(new File(path), "UTF-8");
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
        int HttpResult; // 服务器返回的状态
        String ee = new String();
        try {
            URL url = new URL(filePath); // 创建URL
            URLConnection urlconn = url.openConnection(); // 试图连接并取得返回状态码
            urlconn.connect();
            HttpURLConnection httpconn = (HttpURLConnection) urlconn;
            HttpResult = httpconn.getResponseCode();
            if (HttpResult != HttpURLConnection.HTTP_OK) // 不等于HTTP_OK说明连接不成功
            {
                log.error("无法连接到服务器获取节点列表...");
            } else {
                InputStreamReader isReader = new InputStreamReader(urlconn.getInputStream());
                BufferedReader reader = new BufferedReader(isReader);
                StringBuffer buffer = new StringBuffer();
                String line; // 用来保存每行读取的内容
                line = reader.readLine(); // 读取第一行
                while (line != null) { // 如果 line 为空说明读完了
                    buffer.append(line); // 将读到的内容添加到 buffer 中
                    line = reader.readLine(); // 读取下一行
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
     *
     * @return
     */
    public Set<String> getSuperIps() {
        return superIps;
    }

    /**
     * 判断ip是否是本节点的ip
     *
     * @param ip
     * @return
     */
    public boolean isSelfIp(String ip) {
        return myIps.contains(ip);
    }

    /**
     * 判断当前ip是否是超级节点
     *
     * @return
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
     *
     * @return
     */
    public String getSelfIp() {
        return this.selfIp;
    }

    public Collection<SuperNode> getSuperNodes() {
        return this.superNodes.values();
    }

    /**
     * 获取超级节点的信息
     *
     * @param superIp
     * @return
     */
    public SuperNode getSuperNodeByIp(String superIp) {
        return superNodes.get(superIp);
    }

}
