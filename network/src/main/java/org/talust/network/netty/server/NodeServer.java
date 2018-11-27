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

package org.talust.network.netty.server;

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
import org.talust.network.netty.DecodeHandler;
import org.talust.network.netty.EncodeHandler;

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
            log.info("本地网络服务开启,打开端口:{}", port);
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
