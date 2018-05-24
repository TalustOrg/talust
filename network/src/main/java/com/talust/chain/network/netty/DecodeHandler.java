package com.talust.chain.network.netty;

import com.talust.chain.common.model.Message;
import com.talust.chain.common.tools.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 自定义解码器
 */
@Slf4j
public class DecodeHandler extends ByteToMessageDecoder {

    @Override
    public final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();
        int dataLength = in.readInt();
        if (dataLength < 0) {
            ctx.close();
        }

        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] body = new byte[dataLength];
        in.readBytes(body);
        Message obj = SerializationUtil.deserializer(body, Message.class);
        out.add(obj);
    }
}

