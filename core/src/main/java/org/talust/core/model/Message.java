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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.talust.common.crypto.*;
import org.talust.common.exception.ProtocolException;
import org.talust.core.core.NetworkParams;
import org.talust.core.transaction.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * 消息顶层基类，所有协议的实现都是继承该类
 */
public abstract class Message {

    private static final Logger log = LoggerFactory.getLogger(Message.class);

    public static final int MAX_SIZE = 0x02000000; // 32MB
    public static final int HEADER_SIZE = 512 + 4;

    public static final int UNKNOWN_LENGTH = Integer.MIN_VALUE;

    // Useful to ensure serialize/deserialize are consistent with each other.
    private static final boolean SELF_CHECK = false;
    @Transient
    protected int length = UNKNOWN_LENGTH;
    // The offset is how many bytes into the provided byte array this message payload starts at.
    @Transient
    protected int offset;
    // The cursor keeps track of where we are in the byte array as we parse it.
    // Note that it's relative to the start of the array NOT the start of the message payload.
    @Transient
    protected int cursor;

    @Transient
    protected MessageSerializer serializer;

    // The raw message payload bytes themselves.
    @Transient
    protected byte[] payload;
    @Transient
    protected int protocolVersion;
    @Transient
    protected NetworkParams network;

    protected Message() {

    }

    protected Message(NetworkParams network) {
        this.network = network;
        serializer = network.getDefaultSerializer();
    }

    protected Message(NetworkParams network, byte[] payload, int offset) throws ProtocolException {
        this(network, payload, offset, network.getProtocolVersionNum(NetworkParams.ProtocolVersion.CURRENT));
    }

    protected Message(NetworkParams network, byte[] payload, int offset, int protocolVersion) throws ProtocolException {
        this(network, payload, offset, protocolVersion, network.getDefaultSerializer(), UNKNOWN_LENGTH);
    }

    protected Message(NetworkParams network, byte[] payload, int offset, int protocolVersion, MessageSerializer serializer, int length) throws ProtocolException {
        this.serializer = serializer;
        this.protocolVersion = protocolVersion;
        this.network = network;
        this.payload = payload;
        this.cursor = this.offset = offset;
        this.length = length;

        parse();

        if (this.length == UNKNOWN_LENGTH) {
            Utils.checkState(false, "Length field has not been set in constructor for %s after parse.",
                    getClass().getSimpleName());
        }

        if (SELF_CHECK) {
            selfCheck(payload, offset);
        }

        if (!serializer.isParseRetainMode())
            this.payload = null;
    }

    protected abstract void parse() throws ProtocolException;

    private void selfCheck(byte[] payload, int offset) {
        byte[] payloadBytes = new byte[cursor - offset];
        System.arraycopy(payload, offset, payloadBytes, 0, cursor - offset);
        byte[] reserialized = baseSerialize();
        if (!Arrays.equals(reserialized, payloadBytes))
            throw new RuntimeException("Serialization is wrong: \n" +
                    Hex.encode(reserialized) + " vs \n" +
                    Hex.encode(payloadBytes));
    }

    /**
     * Returns a copy of the array returned by {@link Message#unsafeBitcoinSerialize()}, which is safe to mutate.
     * If you need extra performance and can guarantee you won't write to the array, you can use the unsafe version.
     *
     * @return a freshly allocated serialized byte array
     */
    public byte[] baseSerialize() {
        byte[] bytes = unsafeBitcoinSerialize();
        byte[] copy = new byte[bytes.length];
        System.arraycopy(bytes, 0, copy, 0, bytes.length);
        return copy;
    }

    /**
     * Serialize this message to a byte array that conforms to the bitcoin wire protocol.
     * <br/>
     * This method may return the original byte array used to construct this message if the
     * following conditions are met:
     * <ol>
     * <li>1) The message was parsed from a byte array with parseRetain = true</li>
     * <li>2) The message has not been modified</li>
     * <li>3) The array had an offset of 0 and no surplus bytes</li>
     * </ol>
     * <p>
     * If condition 3 is not met then an copy of the relevant portion of the array will be returned.
     * Otherwise a full serialize will occur. For this reason you should only use this API if you can guarantee you
     * will treat the resulting array as read only.
     *
     * @return a byte array owned by this object, do NOT mutate it.
     */
    public byte[] unsafeBitcoinSerialize() {
        // 1st attempt to use a cached array.
        if (payload != null) {
            if (offset == 0 && length == payload.length) {
                // Cached byte array is the entire message with no extras so we can return as is and avoid an array
                // copy.
                return payload;
            }

            byte[] buf = new byte[length];
            System.arraycopy(payload, offset, buf, 0, length);
            return buf;
        }

        // No cached array available so serialize parts by stream.
        ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(length < 32 ? 32 : length + 32);
        try {
            baseSerializeToStream(stream);
        } catch (IOException e) {
            // Cannot happen, we are serializing to a memory stream.
        }

        if (serializer.isParseRetainMode()) {
            payload = stream.toByteArray();

            if (this instanceof Transaction) {
                Transaction tx = (Transaction) this;
                if (tx.isCompatible()) {
                    //新协议
                    byte[] newPayload = new byte[payload.length + 4];
                    System.arraycopy(payload, 0, newPayload, 0, 5);
                    Utils.uint32ToByteArrayBE(payload.length + 4, newPayload, 5);
                    System.arraycopy(payload, 5, newPayload, 9, payload.length - 4);

                    payload = newPayload;
                }
            }

            cursor = cursor - offset;
            offset = 0;
            length = payload.length;
            return payload;
        }
        // Record length. If this Message wasn't parsed from a byte stream it won't have length field
        // set (except for static length message types).  Setting it makes future streaming more efficient
        // because we can preallocate the ByteArrayOutputStream buffer and avoid resizing.
        byte[] buf = stream.toByteArray();

        if (this instanceof Transaction) {
            Transaction tx = (Transaction) this;
            if (tx.isCompatible()) {
                //新协议
                byte[] newPayload = new byte[buf.length + 4];
                System.arraycopy(buf, 0, newPayload, 0, 5);
                Utils.uint32ToByteArrayBE(buf.length + 4, newPayload, 5);
                System.arraycopy(buf, 5, newPayload, 9, buf.length - 5);

                buf = newPayload;
            }
        }

        length = buf.length;
        return buf;
    }

    public final void baseSerializeToStream(OutputStream stream) throws IOException {
        // 1st check for cached bytes.
        if (payload != null && length != UNKNOWN_LENGTH) {
            stream.write(payload, offset, length);
            return;
        }
        serializeToStream(stream);
    }

    /**
     * Serializes this message to the provided stream. If you just want the raw bytes use bitcoinSerialize().
     */
    protected void serializeToStream(OutputStream stream) throws IOException {
        log.error("Error: {} class has not implemented bitcoinSerializeToStream method.  Generating message with no payload", getClass());
    }

    /**
     * This returns a correct value by parsing the message.
     */
    public final int getMessageSize() {
        if (length == UNKNOWN_LENGTH)
            Utils.checkState(false, "Length field has not been set in %s.", getClass().getSimpleName());
        return length;
    }

    protected long readUint32() throws ProtocolException {
        try {
            long u = Utils.readUint32(payload, cursor);
            cursor += 4;
            return u;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }

    protected long readInt64() throws ProtocolException {
        try {
            long u = Utils.readInt64(payload, cursor);
            cursor += 8;
            return u;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }

    protected BigInteger readUint64() throws ProtocolException {
        // Java does not have an unsigned 64 bit type. So scrape it off the wire then flip.
        return new BigInteger(Utils.reverseBytes(readBytes(8)));
    }

    protected long readVarInt() throws ProtocolException {
        return readVarInt(0);
    }

    protected long readVarInt(int offset) throws ProtocolException {
        try {
            VarInt varint = new VarInt(payload, cursor + offset);
            cursor += offset + varint.getOriginalSizeInBytes();
            return varint.value;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }

    protected byte[] readBytes(int length) throws ProtocolException {
        if (length > MAX_SIZE) {
            throw new ProtocolException("Claimed value length too large: " + length);
        }
        try {
            byte[] b = new byte[length];
            System.arraycopy(payload, cursor, b, 0, length);
            cursor += length;
            return b;
        } catch (IndexOutOfBoundsException e) {
            throw new ProtocolException(e);
        }
    }

    protected double readDouble() {
        byte[] b = new byte[8];
        System.arraycopy(payload, cursor, b, 0, b.length);
        cursor += b.length;
        return Utils.bytes2Double(b);
    }

    protected Sha256Hash readHash() throws ProtocolException {
        return Sha256Hash.wrapReversed(readBytes(32));
    }

    protected boolean hasMoreBytes() {
        return cursor < payload.length;
    }

    protected String readStr() throws ProtocolException {
        long length = readVarInt();
        return length == 0 ? "" : Utils.toString(readBytes((int) length), "UTF-8"); // optimization for empty strings
    }

    public MessageSerializer getSerializer() {
        return serializer;
    }

    public void setSerializer(MessageSerializer serializer) {
        this.serializer = serializer;
    }

    public NetworkParams getNetwork() {
        return network;
    }

    public int getLength() {
        return length;
    }
}
