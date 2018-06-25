package org.talust.chain.network;

import org.talust.chain.common.model.MessageChannel;

/**
 * 处理器
 */
public interface MessageHandler {
    boolean handle(MessageChannel messageChannel);
}
