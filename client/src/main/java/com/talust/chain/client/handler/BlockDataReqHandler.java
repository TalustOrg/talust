package com.talust.chain.client.handler;

import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.common.tools.Constant;
import com.talust.chain.common.model.Message;
import com.talust.chain.common.model.MessageType;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.netty.queue.MessageQueue;
import com.talust.chain.storage.BlockStorage;
import lombok.extern.slf4j.Slf4j;

/**
 * 远端向当前节点请求获取某数据取内容
 */
@Slf4j
public class BlockDataReqHandler implements MessageHandler {
    private BlockStorage blockStorage = BlockStorage.get();
    private MessageQueue mq = MessageQueue.get();
    @Override
    public boolean handle(MessageChannel message) {
        MessageChannel mc = new MessageChannel();
        mc.setToIp(message.getFromIp());
        Message nodeMessage = new Message();
        nodeMessage.setMsgCount(message.getMessage().getMsgCount());
        mc.setMessage(nodeMessage);

        byte[] content = message.getMessage().getContent();
        String num = new String(content);//区块高度
        log.info("远端ip:{} 向当前节点请求区块:{} 的块数据内容...", message.getFromIp(), num);
        byte[] bh = (Constant.BH_PRIX + num).getBytes();
        byte[] hash = blockStorage.get(bh);
        if (hash != null) {
            byte[] block = blockStorage.get(hash);
            nodeMessage.setContent(block);//存储区块内容
            nodeMessage.setType(MessageType.BLOCK_RESP.getType());
            log.info("向远端ip:{} 返回本节点拥有的区块:{} 的区块内容...", message.getFromIp(), num);
        } else {
            nodeMessage.setContent(content);
            nodeMessage.setType(MessageType.ERROR_MESSAGE.getType());
            log.info("向远端ip:{} 返回错误消息,当前节点无此区块:{} 数据",message.getFromIp(), num);
        }
        mq.addMessage(mc);
        return true;
    }
}
