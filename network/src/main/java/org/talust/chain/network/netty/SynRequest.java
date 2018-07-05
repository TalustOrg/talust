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

package org.talust.chain.network.netty;

import lombok.extern.slf4j.Slf4j;
import org.talust.chain.common.model.Message;
import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.tools.MessageCount;
import org.talust.chain.common.tools.SyncFuture;
import org.talust.chain.network.netty.queue.MessageQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 异步请求处理
 */
@Slf4j
public class SynRequest {

    private static SynRequest instance = new SynRequest();

    private SynRequest() {
    }

    public static SynRequest get() {
        return instance;
    }

    private MessageQueue mq = MessageQueue.get();

    //用于存储异步请求时,某个消息ID,对应的异步回调
    private Map<Long, SyncFuture<MessageChannel>> synMap = new ConcurrentHashMap<>();

    /**
     * 添加异步请求
     *
     * @param message 节点消息
     * @param toIp    需要通过该通道进行发送数据
     */
    public MessageChannel synReq(Message message, String toIp) throws Exception {
        SyncFuture<MessageChannel> sync = new SyncFuture<>();
        long mc = MessageCount.msgCount.addAndGet(1);
        if (mc > 65530) {
            MessageCount.msgCount.set(0);
        }
        synMap.put(mc, sync);
        message.setMsgCount(mc);
        MessageChannel mch = new MessageChannel();
        mch.setMessage(message);
        mch.setToIp(toIp);
        mq.addMessage(mch);
        MessageChannel retMsg = sync.get(3, TimeUnit.SECONDS);
        synMap.remove(mc);
        return retMsg;//返回的要么为空,要么有值
    }

    //异步响应
    public void synResp(MessageChannel message) {
        SyncFuture<MessageChannel> syncFuture = synMap.remove(message.getMessage().getMsgCount());
        if (syncFuture != null) {
            syncFuture.setResponse(message);
        }
    }

}
