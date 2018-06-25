package org.talust.chain.client.handler;

import lombok.extern.slf4j.Slf4j;
import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.tools.CacheManager;
import org.talust.chain.common.tools.Configure;
import org.talust.chain.network.MessageHandler;
import org.talust.chain.network.netty.ChannelContain;
import org.talust.chain.network.netty.queue.MessageQueueHolder;

@Slf4j //节点退出网络
public class NodeExitHandler implements MessageHandler {

    @Override
    public boolean handle(MessageChannel message) {
        String ip = new String(message.getMessage().getContent());
        log.info("接收到节点ip:{} 退出的消息...", ip);
        String time = message.getMessage().getTime().toString();
        String identifier = (ip + time);
        boolean checkRepeat = CacheManager.get().checkRepeat(identifier, Configure.BLOCK_GEN_TIME);
        if (!checkRepeat) {//消息无重复
            ChannelContain.get().removeNodeIp(ip);
            MessageQueueHolder.get().broadMessage(message);
        }
        return true;
    }
}
