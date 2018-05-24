package com.talust.chain.client.handler;

import com.talust.chain.block.data.DataContainer;
import com.talust.chain.block.model.Block;
import com.talust.chain.common.crypto.Hex;
import com.talust.chain.common.crypto.Sha256Hash;
import com.talust.chain.common.model.Message;
import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.common.tools.CacheManager;
import com.talust.chain.common.tools.Constant;
import com.talust.chain.common.tools.SerializationUtil;
import com.talust.chain.consensus.Conference;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.netty.queue.MessageQueueHolder;
import com.talust.chain.storage.BlockStorage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j//其他节点广播出来的区块数据用于广播的
public class BlockArrivedBroadHandler implements MessageHandler {
    private MessageQueueHolder mqHolder = MessageQueueHolder.get();

    @Override
    public boolean handle(MessageChannel messageChannel) {
        log.info("接收到远端ip:{} 发送过来的区块数据,准备进行广播...", messageChannel.getFromIp());
        mqHolder.broadMessage(messageChannel);
        return true;
    }

}
