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
package org.talust.core.model;


import org.talust.common.crypto.Sha256Hash;
import org.talust.common.crypto.Utils;
import org.talust.common.exception.ProtocolException;
import org.talust.core.transaction.Transaction;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * 消息序列化工具
 */
public abstract class MessageSerializer {
	
	protected static final int COMMAND_LEN = 12;

	/**
	 * 接收到消息流，解码为消息
	 * @param in
	 * @return Message
	 * @throws ProtocolException
	 * @throws IOException
	 * @throws UnsupportedOperationException
	 */
    public abstract Message deserialize(ByteBuffer in) throws ProtocolException, IOException, UnsupportedOperationException;

    /**
     * 接收到消息流，解析消息头
     * @param in
     * @return MessagePacketHeader
     * @throws ProtocolException
     * @throws IOException
     * @throws UnsupportedOperationException
     */
    public abstract MessagePacketHeader deserializeHeader(ByteBuffer in) throws ProtocolException, IOException, UnsupportedOperationException;

    /**
     * 通过消息头，解析消息主体
     * @param header
     * @param in
     * @return Message
     * @throws ProtocolException
     * @throws BufferUnderflowException
     * @throws UnsupportedOperationException
     */
    public abstract Message deserializePayload(MessagePacketHeader header, ByteBuffer in) throws ProtocolException, BufferUnderflowException, UnsupportedOperationException;

    public abstract void seekPastMagicBytes(ByteBuffer in) throws BufferUnderflowException;

    public abstract void serialize(String name, byte[] message, OutputStream out) throws IOException, UnsupportedOperationException;

    public abstract void serialize(Message message, OutputStream out) throws IOException, UnsupportedOperationException;
    /**
     * Whether the serializer will produce cached mode Messages
     */
    public abstract boolean isParseRetainMode();
    
    public static class MessagePacketHeader {
        /** The largest number of bytes that a header can represent */
        public static final int HEADER_LENGTH = COMMAND_LEN + 4 + 4;

        public final byte[] header;
        public final String command;
        public final int size;
        public final byte[] checksum;

        public MessagePacketHeader(ByteBuffer in) throws ProtocolException, BufferUnderflowException {
            header = new byte[HEADER_LENGTH];
            in.get(header, 0, header.length);

            int cursor = 0;

            // The command is a NULL terminated string, unless the command fills all twelve bytes
            // in which case the termination is implicit.
            for (; header[cursor] != 0 && cursor < COMMAND_LEN; cursor++) ;
            byte[] commandBytes = new byte[cursor];
            System.arraycopy(header, 0, commandBytes, 0, cursor);
            command = Utils.toString(commandBytes, "US-ASCII");
            cursor = COMMAND_LEN;

            size = (int) Utils.readUint32(header, cursor);
            cursor += 4;

            if (size > Message.MAX_SIZE || size < 0)
                throw new ProtocolException("Message size too large: " + size);

            // Old clients don't send the checksum.
            checksum = new byte[4];
            // Note that the size read above includes the checksum bytes.
            System.arraycopy(header, cursor, checksum, 0, 4);
            cursor += 4;
        }
    }
    
    /**
     * 解析交易
     * @param payloadBytes
     * @return Transaction
     * @throws ProtocolException
     * @throws UnsupportedOperationException
     */
    public abstract Transaction makeTransaction(byte[] payloadBytes) throws ProtocolException, UnsupportedOperationException;
    
    /**
     * 解析交易
     * @param payloadBytes
     * @param hash
     * @return Transaction
     * @throws ProtocolException
     * @throws UnsupportedOperationException
     */
    public abstract Transaction makeTransaction(byte[] payloadBytes, 
    		Sha256Hash hash) throws ProtocolException, UnsupportedOperationException;
    
    /**
     * 解析交易
     * @param payloadBytes
     * @param offset
     * @return Transaction
     * @throws ProtocolException
     */
    public abstract Transaction makeTransaction(byte[] payloadBytes, int offset) throws ProtocolException;
    
}
