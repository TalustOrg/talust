package org.talust.chain.consensus.handler;

import lombok.extern.slf4j.Slf4j;
import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.network.MessageHandler;
import org.talust.chain.network.netty.SynRequest;

@Slf4j//接收到远端返回的master响应数据
public class MasterRespHandler implements MessageHandler {
    @Override
    public boolean handle(MessageChannel message) {
        log.info("远端ip:{} 远端返回的master响应数据...", message.getFromIp());
        SynRequest.get().synResp(message);
        return true;
    }

}
