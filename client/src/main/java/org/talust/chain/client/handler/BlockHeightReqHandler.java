package org.talust.chain.client.handler;

import lombok.extern.slf4j.Slf4j;
import org.talust.chain.common.model.Message;
import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.model.MessageType;
import org.talust.chain.common.tools.CacheManager;
import org.talust.chain.network.MessageHandler;
import org.talust.chain.network.netty.queue.MessageQueue;

@Slf4j//远端区块高度请求处理
public class BlockHeightReqHandler implements MessageHandler {
    private MessageQueue mq = MessageQueue.get();
    @Override
    public boolean handle(MessageChannel message) {
        MessageChannel mc = new MessageChannel();
        Message msg = new Message();
        msg.setType(MessageType.HEIGHT_RESP.getType());
        msg.setContent(Integer.toString(CacheManager.get().getCurrentBlockHeight()).getBytes());
        msg.setMsgCount(message.getMessage().getMsgCount());
        mc.setToIp(message.getFromIp());
        mc.setMessage(msg);
        mq.addMessage(mc);
        log.info("向远端ip:{} 返回本节点当前最新区块高度:{}", message.getFromIp(), CacheManager.get().getCurrentBlockHeight());
        return true;
    }
}
