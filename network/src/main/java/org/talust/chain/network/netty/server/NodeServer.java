package org.talust.chain.network.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.talust.chain.network.netty.DecodeHandler;
import org.talust.chain.network.netty.EncodeHandler;

import java.util.concurrent.TimeUnit;

@Slf4j
public class NodeServer {

    public void bind(int port) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(2 * Runtime.getRuntime().availableProcessors());
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChildChannelHandler());

            ChannelFuture f = b.bind(port).sync();
            log.info("本地网络服务开启,打开端口 : {}", port);
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(final SocketChannel ch) {
            ch.pipeline()
                    .addLast(new DecodeHandler())
                    .addLast(new EncodeHandler())
                    .addLast(new IdleStateHandler(120, 0, 0, TimeUnit.SECONDS))
                    .addLast(new NodeServerRespHandler());
        }
    }

//    public static void main(String[] args) {
//        try {
//            new NodeServer().bind(12000);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
