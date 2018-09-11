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
import org.talust.common.tools.Configure;
import org.talust.common.tools.SerializationUtil;
import org.talust.core.storage.AccountStorage;
import org.talust.network.MessageHandler;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.SynRequest;
import org.talust.network.netty.queue.MessageQueue;

@Slf4j
public class GetNodeAddressRespHandler implements MessageHandler {
    @Override
    public boolean handle(MessageChannel message) {
        log.info("远端ip:{} 返回本节点的地址消息", message.getFromIp());
        String nodeAddress = null;
        try {
            if (message != null) {
                nodeAddress = SerializationUtil.deserializer(message.getMessage().getContent(), String.class);
                log.info("请求节点ip:{} 返回地址结果为:{}", message.getFromIp(), nodeAddress);
            } else {
                log.info("请求节点ip:{}，返回地址请求失败", message.getFromIp());
            }
        } catch (Exception e) {
            log.info("请求节点ip:{}，返回地址请求异常", message.getFromIp());
        }
        //TODO  数据赋予改变，给下channel id

        //ChannelContain.get().modifyChannel();
        return true;
    }
}
