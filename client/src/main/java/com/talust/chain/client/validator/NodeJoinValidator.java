package com.talust.chain.client.validator;

import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.network.MessageValidator;
import com.talust.chain.network.netty.queue.MessageQueueHolder;

/**
 * 节点加入时的验证
 */
public class NodeJoinValidator implements MessageValidator {
    private MessageQueueHolder mqHolder = MessageQueueHolder.get();

    @Override
    public boolean check(MessageChannel messageChannel) {
        String ip = new String(messageChannel.getMessage().getContent());
        String time = messageChannel.getMessage().getTime().toString();
        byte[] identifier = (ip + time).getBytes();
        boolean checkRepeat = mqHolder.checkRepeat(identifier);
        if (!checkRepeat) {//消息无重复
            return true;
        }
        return false;
    }
}
