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

package org.talust.network.netty.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.model.MessageType;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.queue.MessageQueue;

import java.net.InetSocketAddress;

@Slf4j
public class NodeClientReqHandler extends SimpleChannelInboundHandler<Message> {
    private MessageQueue mq = MessageQueue.get();
    private ChannelContain cc = ChannelContain.get();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
//        LOGGER.info("有连接端接入:{}", ctx.channel().id());
        //本节点主动连接远端成功
        cc.addChannel(ctx.channel(), false);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
//        LOGGER.info("有连接端断开:{}", ctx.channel().id());
        cc.removeChannel(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg.getType() == MessageType.HEARTBEAT_RESP.getType()) {
            log.debug("收到心跳消息回复...");
            ReferenceCountUtil.release(msg);
        } else {
            InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
            String remoteIp = insocket.getAddress().getHostAddress();
            MessageChannel mc = new MessageChannel();
            mc.setMessage(msg);
            mc.setFromIp(remoteIp);
            mc.setChannelId(ctx.channel().id().asShortText());
            mq.addMessage(mc);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                Message msg = new Message();
                msg.setType(MessageType.HEARTBEAT_REQ.getType());
                ctx.writeAndFlush(msg);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        cc.removeChannel(ctx.channel());
        if (channel.isActive()){
            ctx.close();
        }
    }
}
