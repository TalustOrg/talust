package org.talust.chain.client.handler;

import lombok.extern.slf4j.Slf4j;
import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.network.MessageHandler;
import org.talust.chain.network.netty.queue.MessageQueueHolder;

@Slf4j//其他节点广播出来的区块数据用于广播的
public class BlockArrivedBroadHandler implements MessageHandler {
    private MessageQueueHolder mqHolder = MessageQueueHolder.get();

    @Override
    public boolean handle(MessageChannel messageChannel) {
        log.info("接收到远端ip:{} 发送过来的区块数据,准备进行广播...", messageChannel.getFromIp());
        mqHolder.broadMessage(messageChannel);
        return true;
    }

}
