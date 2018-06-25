package org.talust.chain.network.netty.queue;

import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.tools.DateUtil;

import java.util.concurrent.LinkedTransferQueue;

/**
 * 接收消息队列,即从远端发送过来的数据将存储于此
 */
public class MessageQueue {
    private static MessageQueue instance = new MessageQueue();

    private MessageQueue() {
    }

    public static MessageQueue get() {
        return instance;
    }

    private LinkedTransferQueue<MessageChannel> queue = new LinkedTransferQueue<>();

    public void addMessage(MessageChannel message) {
        if (message.getMessage() != null) {
            message.getMessage().setTime(DateUtil.getTimeSecond());
        }
        this.queue.add(message);
    }

    public MessageChannel takeMessage() throws Exception {
        return this.queue.take();
    }
}
