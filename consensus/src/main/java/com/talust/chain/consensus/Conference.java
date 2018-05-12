package com.talust.chain.consensus;

import com.talust.chain.block.model.Block;
import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.common.model.SuperNode;
import com.talust.chain.common.tools.CacheManager;
import com.talust.chain.common.tools.SerializationUtil;
import com.talust.chain.common.tools.ThreadPool;
import com.talust.chain.network.netty.ConnectionManager;
import com.talust.chain.common.model.Message;
import com.talust.chain.common.model.MessageType;
import com.talust.chain.network.model.MyChannel;
import com.talust.chain.network.netty.ChannelContain;
import com.talust.chain.network.netty.SynRequest;
import com.talust.chain.storage.BlockStorage;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

@Slf4j //进入共识的会议,里面含有各个会议成员
public class Conference {
    private static Conference instance = new Conference();

    private Conference() {
    }

    public static Conference get() {
        return instance;
    }

    //当前的master
    private SuperNode master;
    //构造一个线程池
    private ThreadPoolExecutor threadPool = ThreadPool.get().threadPool;

    /**
     * 获取当前的master节点
     *
     * @return
     */
    public SuperNode reqNetMaster() {
        Collection<MyChannel> superChannels = ChannelContain.get().getSuperChannels();
        int superSize = superChannels.size();
        if (superSize > 0) {//说明当前超级节点网络有多个
            int needOkNumber = superSize / 2;//需要回应的数量
            //向每一个超级节点请求加入结果
            log.info("当前节点ip:{} 向各个超级节点请求当前master节点,其他超级节点数量为:{}", ConnectionManager.get().getSelfIp(), superSize);

            List<Future<String>> results = new ArrayList<>();
            for (final MyChannel superChannel : superChannels) {
                Future<String> submit = threadPool.submit(() -> {
                    Message nodeMessage = new Message();
                    nodeMessage.setType(MessageType.MASTER_REQ.getType());//master请求
                    MessageChannel nm = SynRequest.get().synReq(nodeMessage, superChannel.getRemoteIp());
                    if (nm != null) {
                        byte[] content = nm.getMessage().getContent();
                        if (content != null) {//ip
                            return new String(content);
                        }
                    }
                    return null;
                });
                results.add(submit);
            }

            Map<String, Integer> rc = new HashMap<>();
            while (true) {
                boolean isOk = false;
                for (Future<String> result : results) {
                    try {
                        boolean done = result.isDone();
                        if (done) {
                            String conferenceMemeber = result.get();
                            Integer number = rc.get(conferenceMemeber);
                            if (number == null) {
                                number = 0;
                            }
                            number++;
                            rc.put(conferenceMemeber, number);
                            if (number > needOkNumber) {
                                isOk = true;
                            }
                            results.remove(result);
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (isOk) {
                    break;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Iterator<Map.Entry<String, Integer>> it = rc.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> next = it.next();
                Integer value = next.getValue();
                if (value > needOkNumber) {
                    master = ConnectionManager.get().getSuperNodeByIp(next.getKey());
                    return master;
                }
            }
        } else {
            boolean superNode = ConnectionManager.get().superNode;
            if (superNode) {//如果当前节点是超级节点,则启动共识机制
                master = ConnectionManager.get().getSuperNodeByIp(ConnectionManager.get().getSelfIp());
                return master;
            }
        }
        return null;
    }

    /**
     * 改变当前master节点
     */
    public void changeMaster() {
        String currentBlockGenIp = CacheManager.get().getCurrentBlockGenIp();
        Collection<SuperNode> superNodes = ConnectionManager.get().getSuperNodes();
        List<SuperNode> sns = new ArrayList<>();
        for (SuperNode superNode : superNodes) {
            sns.add(superNode);
        }
        Collections.sort(sns, Comparator.comparing(SuperNode::getIp));

        boolean selOk = false;
        SuperNode nextMaster = sns.get(0);
        for (SuperNode superNode : sns) {
            if (selOk) {
                nextMaster = superNode;
                break;
            }
            if (superNode.getIp().equals(currentBlockGenIp)) {
                selOk = true;
            }
        }
        this.master = nextMaster;
    }

    public SuperNode getMaster() {
        return master;
    }

}
