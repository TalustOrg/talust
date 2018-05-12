package com.talust.chain.network.netty.client;


import com.talust.chain.common.model.Message;
import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.common.model.MessageType;
import com.talust.chain.network.netty.ChannelContain;
import com.talust.chain.network.netty.queue.MessageQueue;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

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
        if (channel.isActive()) ctx.close();
    }
}
