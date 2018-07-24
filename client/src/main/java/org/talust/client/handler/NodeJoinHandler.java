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

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.talust.common.model.MessageChannel;
import org.talust.common.tools.Configure;
import org.talust.common.tools.Constant;
import org.talust.network.MessageHandler;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.ConnectionManager;
import org.talust.network.netty.client.NodeClient;
import org.talust.network.netty.queue.MessageQueueHolder;

@Slf4j
public class NodeJoinHandler implements MessageHandler {
    private int maxActiveConnectCount = Configure.MAX_ACTIVE_CONNECT_COUNT;
    private int maxPassivityConnectCount =  Configure.MAX_PASSIVITY_CONNECT_COUNT;
    private MessageQueueHolder mqHolder = MessageQueueHolder.get();

    @Override
    public boolean handle(MessageChannel message) {
        String ip = new String(message.getMessage().getContent());
        log.info("接收到节点ip:{} 请求加入本节点网络的消息...", ip);
        if(maxPassivityConnectCount-ChannelContain.get().getPassiveConnCount()>0){
            ChannelContain.get().addNodeIp(ip);
        }
        int activeConnectionCount = ChannelContain.get().getActiveConnectionCount();
        if (activeConnectionCount < maxActiveConnectCount) {
            //说明当前连接数过小,可以进行相互连接
            if (!ConnectionManager.get().isSelfIp(ip)) {
                log.info("当前节点要求主动连接其他节点的总结点数为:{},当前已经主动连接数为:{},将会连接ip:{}", maxActiveConnectCount, activeConnectionCount, ip);
                try {
                    NodeClient nodeClient = new NodeClient();
                    Channel connect = nodeClient.connect(ip, Constant.PORT);
                    ChannelContain.get().addChannel(connect, false);
                    //TODO  节点加入，修改文件
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        }
        return true;
    }
}
