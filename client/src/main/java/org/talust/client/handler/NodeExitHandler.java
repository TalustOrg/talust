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
import org.talust.common.model.MessageChannel;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.Configure;
import org.talust.network.MessageHandler;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.ConnectionManager;
import org.talust.network.netty.PeersManager;
import org.talust.network.netty.queue.MessageQueueHolder;

@Slf4j //节点退出网络
public class NodeExitHandler implements MessageHandler {
    private int maxSuperActiveConnectCount =  Configure.MAX_SUPER_ACTIVE_CONNECT_COUNT;
    private int maxActiveConnectCount =  Configure.MAX_ACTIVE_CONNECT_COUNT;
    @Override
    public boolean handle(MessageChannel message) {
        String ip = new String(message.getMessage().getContent());
        log.info("接收到节点ip:{} 退出的消息...", ip);
        String time = message.getMessage().getTime().toString();
        String identifier = (ip + time);
        boolean checkRepeat = CacheManager.get().checkRepeat(identifier, Configure.BLOCK_GEN_TIME);
        if (!checkRepeat) {
            MessageQueueHolder.get().broadMessage(message);
        }
        PeersManager.get().removePeer(ip);
        return true;
    }
}
