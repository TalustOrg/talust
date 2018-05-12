package com.talust.chain.network.netty.client;


import com.talust.chain.network.netty.DecodeHandler;
import com.talust.chain.network.netty.EncodeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class NodeClient {
    public Channel connect(String host, int port) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .handler(new ChildChannelHandler());
            Channel channel = b.connect(host, port).sync().channel();
            return channel;
        } catch (Exception e) {
            throw new Exception("网络连接失败...");
        }
    }

    public static class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(final SocketChannel ch) {
            ch.pipeline()
                    .addLast(new EncodeHandler())
                    .addLast(new DecodeHandler())
                    .addLast(new IdleStateHandler(120, 30, 0, TimeUnit.SECONDS))
                    .addLast(new NodeClientReqHandler());

        }

    }

//    public static void main(String[] args) {
//        try {
//            Channel ch = new NodeClient().connect("192.168.16.129", 9999);
//            // 控制台输入
//            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//            for (; ; ) {
//                String line = in.readLine();
//                if (line == null) {
//                    continue;
//                }
//                Message msg = new Message();
//                msg.setType(MessageType.NODES_REQ.getType());
//                msg.setContent(line.getBytes());
//                ch.writeAndFlush(msg);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
