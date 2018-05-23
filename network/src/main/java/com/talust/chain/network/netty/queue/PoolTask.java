package com.talust.chain.network.netty.queue;

import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.MessageValidator;
import com.talust.chain.network.netty.ChannelContain;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

/**
 * 线程池任务
 */
@Slf4j
public class PoolTask implements Runnable, Serializable {
    private static final long serialVersionUID = 0;
    //保存任务所需要的数据
    private MessageChannel message;
    private MessageValidator validator;
    private List<MessageHandler> handlers;

    public PoolTask(MessageChannel works, MessageValidator validator, List<MessageHandler> handler) {
        this.message = works;
        this.validator = validator;
        this.handlers = handler;
    }

    @Override
    public void run() {
        try {
            String toIP = message.getToIp();
            if (toIP != null && toIP.length() > 0) {
                //说明接收到的消息是要求往toChannel这个通道发送数据,此种业务逻辑目前暂不进行验证合理性,直接发即可
                ChannelContain.get().sendMessage(toIP, message.getMessage());
            } else {//明确要求自身节点进行处理的消息
                boolean check = true;//
                if (validator != null) {
                    check = validator.check(message);
                }
                if (check) {//校验正确
                    for (MessageHandler handler : handlers) {
                        handler.handle(message);
                    }
                }
            }
        } catch (Throwable e) {
            log.error("消息处理错误:", e);
        }
        message = null;
        validator = null;
        handlers = null;
    }
}
