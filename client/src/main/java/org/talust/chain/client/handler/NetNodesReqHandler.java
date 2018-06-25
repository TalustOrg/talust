package org.talust.chain.client.handler;

import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.tools.SerializationUtil;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.model.AllNodes;
import com.talust.chain.network.netty.ChannelContain;
import org.talust.chain.common.model.Message;
import org.talust.chain.common.model.MessageType;
import com.talust.chain.network.netty.queue.MessageQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

@Slf4j //请求本节点当前存储的所有网络节点
public class NetNodesReqHandler implements MessageHandler {
    private MessageQueue mq = MessageQueue.get();
    @Override
    public boolean handle(MessageChannel message) {
        AllNodes an = new AllNodes();
        Set<String> allNodeIps = ChannelContain.get().getAllNodeIps();
        String fromIp = message.getFromIp();
        if (fromIp != null) {
            List<String> allNodes = an.getNodes();
            for (String nodeIp : allNodeIps) {
                if (!nodeIp.equals(fromIp)) {
                    allNodes.add(nodeIp);
                }
            }
        }

        byte[] serializer = SerializationUtil.serializer(an);
        Message alm = new Message();
        alm.setType(MessageType.NODES_RESP.getType());
        alm.setContent(serializer);
        alm.setMsgCount(message.getMessage().getMsgCount());
        MessageChannel mc = new MessageChannel();
        mc.setMessage(alm);
        mc.setToIp(message.getFromIp());
        mq.addMessage(mc);
        log.info("向远端ip:{} 返回所请求的所有节点消息,当前网络节点数:{}", message.getFromIp(), allNodeIps.size());

        return true;
    }
}
