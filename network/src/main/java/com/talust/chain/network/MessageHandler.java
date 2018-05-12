package com.talust.chain.network;

import com.talust.chain.common.model.MessageChannel;

/**
 * 处理器
 */
public interface MessageHandler {
    boolean handle(MessageChannel messageChannel);
}
