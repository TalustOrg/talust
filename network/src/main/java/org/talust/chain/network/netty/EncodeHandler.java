package org.talust.chain.network.netty;

import org.talust.chain.common.tools.SerializationUtil;
import org.talust.chain.common.model.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 自定义编码器
 */
public class EncodeHandler extends MessageToByteEncoder {

    @Override
    public void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) {
        if (Message.class.isInstance(in)) {
            byte[] data = SerializationUtil.serializer(in);
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }
}

