/*
 * MIT License
 *
 * Copyright (c) 2017-2018 talust.org talust.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.talust.client.handler;

import lombok.extern.slf4j.Slf4j;
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.model.MessageType;
import org.talust.common.tools.Constant;
import org.talust.network.MessageHandler;
import org.talust.network.netty.queue.MessageQueue;
import org.talust.storage.BlockStorage;

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
