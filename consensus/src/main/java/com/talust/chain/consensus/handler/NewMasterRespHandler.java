package com.talust.chain.consensus.handler;

import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.netty.SynRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j//接收到远端返回的新master响应数据
public class NewMasterRespHandler implements MessageHandler {
    @Override
    public boolean handle(MessageChannel message) {
        log.info("远端ip:{} 远端返回了新master响应数据...", message.getFromIp());
        SynRequest.get().synResp(message);
        return true;
    }

}
