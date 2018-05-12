package com.talust.chain.client.handler;

import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.netty.SynRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlockHeightRespHandler implements MessageHandler {

    @Override
    public boolean handle(MessageChannel message) {
        log.info("收到远端ip:{} 的区块高度:{}的响应", message.getFromIp(), new String(message.getMessage().getContent()));
        SynRequest.get().synResp(message);
        return true;
    }
}
