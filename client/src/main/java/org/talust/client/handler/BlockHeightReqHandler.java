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
import org.talust.common.tools.CacheManager;
import org.talust.core.network.MainNetworkParams;
import org.talust.network.MessageHandler;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.queue.MessageQueue;

@Slf4j//远端区块高度请求处理
public class BlockHeightReqHandler implements MessageHandler {
    private MessageQueue mq = MessageQueue.get();
    @Override
    public boolean handle(MessageChannel message) {
        MessageChannel mc = new MessageChannel();
        Message msg = new Message();
        msg.setType(MessageType.HEIGHT_RESP.getType());
        msg.setContent(Long.toString(MainNetworkParams.get().getBestHeight()).getBytes());
        msg.setMsgCount(message.getMessage().getMsgCount());
        mc.setToIp(message.getFromIp());
        mc.setMessage(msg);
        mc.setChannelId(message.getChannelId());
        mq.addMessage(mc);
        log.info("向远端ip:{} 返回本节点当前最新区块高度: {}", message.getFromIp(),MainNetworkParams.get().getBestHeight());
        return true;
    }
}
