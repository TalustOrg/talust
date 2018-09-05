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


import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.model.MessageType;
import org.talust.common.tools.SerializationUtil;
import org.talust.network.MessageHandler;
import org.talust.network.model.AllNodes;
import org.talust.network.netty.PeersManager;
import org.talust.network.netty.queue.MessageQueue;


@Slf4j //请求本节点当前存储的所有网络节点
public class NetNodesReqHandler implements MessageHandler {
    private MessageQueue mq = MessageQueue.get();
    @Override
    public boolean handle(MessageChannel message) {
        JSONObject peerJ = JSONObject.parseObject(PeersManager.get().peerCont);
        byte[] serializer = SerializationUtil.serializer(peerJ);
        Message alm = new Message();
        alm.setType(MessageType.NODES_RESP.getType());
        alm.setContent(serializer);
        alm.setMsgCount(message.getMessage().getMsgCount());
        MessageChannel mc = new MessageChannel();
        mc.setMessage(alm);
        mc.setToIp(message.getFromIp());
        mq.addMessage(mc);
        log.info("向远端ip:{} 返回所请求的所有节点消息,当前网络节点数:{}", message.getFromIp(), peerJ.entrySet().size());
        return true;
    }
}
