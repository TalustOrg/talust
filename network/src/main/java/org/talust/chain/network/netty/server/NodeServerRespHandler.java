package org.talust.chain.network.netty.server;


import org.talust.chain.common.model.MessageChannel;
import com.talust.chain.network.netty.queue.MessageQueue;
import org.talust.chain.common.model.Message;
import org.talust.chain.common.model.MessageType;
import com.talust.chain.network.netty.ChannelContain;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class NodeServerRespHandler extends SimpleChannelInboundHandler<Message> {
    private MessageQueue mq = MessageQueue.get();
    private ChannelContain cc = ChannelContain.get();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        //远端主动连接本节点,本节点是被动的连接
        cc.addChannel(ctx.channel(), true);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        cc.removeChannel(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg.getType() == MessageType.HEARTBEAT_REQ.getType()) {
            log.debug("收到心跳消息包...");
            Message heartBeat = new Message();
            heartBeat.setType(MessageType.HEARTBEAT_RESP.getType());
            ctx.writeAndFlush(heartBeat);
            ReferenceCountUtil.release(msg);
        } else {
            //将收到的消息放入消息队列中,由另外的线程进行处理,以免得阻塞消息的接收\
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
            if (event.state() == IdleState.READER_IDLE) {
                InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
                String remoteIp = insocket.getAddress().getHostAddress();
                log.error("对方ip:{}掉线了", remoteIp);
                cc.removeChannel(ctx.channel());
                ctx.disconnect();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        cc.removeChannel(ctx.channel());
        if (channel.isActive()){
            ctx.close();
        }
    }
}

