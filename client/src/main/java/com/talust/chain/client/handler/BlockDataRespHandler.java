package com.talust.chain.client.handler;

import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.netty.SynRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j//接收到远端返回的区块数据
public class BlockDataRespHandler implements MessageHandler {
    @Override
    public boolean handle(MessageChannel message) {
        log.info("远端ip:{} 返回了本节点请求的区块内容...", message.getFromIp());
        SynRequest.get().synResp(message);
        return true;
    }

}
