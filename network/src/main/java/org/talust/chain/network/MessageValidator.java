package org.talust.chain.network;

import org.talust.chain.common.model.MessageChannel;

/**
 * 校验器,对接收过来的底层数据进行校验
 */
public interface MessageValidator {
    /**
     * 校验
     *
     * @param messageChannel
     * @return
     */
    boolean check(MessageChannel messageChannel);
}
