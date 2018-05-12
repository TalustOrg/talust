package com.talust.chain.client.handler;

import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.consensus.Conference;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.netty.ChannelContain;
import com.talust.chain.network.netty.queue.MessageQueueHolder;
import lombok.extern.slf4j.Slf4j;

@Slf4j //节点退出网络
public class NodeExitHandler implements MessageHandler {
    private MessageQueueHolder mqHolder = MessageQueueHolder.get();

    @Override
    public boolean handle(MessageChannel message) {
        String ip = new String(message.getMessage().getContent());
        log.info("接收到节点ip:{} 退出的消息...", ip);
        String time = message.getMessage().getTime().toString();
        byte[] identifier = (ip + time).getBytes();
        boolean checkRepeat = mqHolder.checkRepeat(identifier);
        if (!checkRepeat) {//消息无重复
            ChannelContain.get().removeNodeIp(ip);
            mqHolder.broadMessage(message);
        }
        return true;
    }
}
