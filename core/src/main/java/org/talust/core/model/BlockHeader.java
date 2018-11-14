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


import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Transient;
import org.talust.core.core.ECKey;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.crypto.Utils;
import org.talust.common.crypto.VarInt;
import org.talust.common.exception.AccountEncryptedException;
import org.talust.common.exception.ProtocolException;
import org.talust.common.exception.VerificationException;
import org.talust.core.core.NetworkParams;
import org.talust.core.script.Script;
import org.talust.core.script.ScriptBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.spongycastle.crypto.tls.TlsUtils.readUint32;

/**
 * 区块头信息
 */
@Slf4j
public class BlockHeader extends Message {

    //区块版本
    protected long version;
    //区块hash
    protected Sha256Hash hash;
    //上一区块hash
    protected Sha256Hash preHash;
    //梅克尔树根节点hash
    protected Sha256Hash merkleHash;
    //时间戳，单位（秒）
    protected long time;
    //区块高度
    protected long height;
    //交易数
    protected long txCount;
    //签名脚本，包含共识打包人信息和签名，签名是对以上信息的签名
    @Transient
    protected byte[] scriptBytes;
    @Transient
    protected Script scriptSig;
    @Transient
    protected List<Sha256Hash> txHashs;

    public BlockHeader(NetworkParams network) {
        super(network);
    }

    public BlockHeader(NetworkParams network, byte[] payload, int offset) throws ProtocolException {
        super(network, payload, offset);
    }

    public BlockHeader(NetworkParams network, byte[] payload) throws ProtocolException {
        super(network, payload, 0);
    }

    public void parse() throws ProtocolException {
        version = readUint32();
        preHash = Sha256Hash.wrap(readBytes(32));
        merkleHash = Sha256Hash.wrap(readBytes(32));
        time = readUint32();
        height = readUint32();
        scriptBytes = readBytes((int) readVarInt());

        scriptSig = new Script(scriptBytes);

        txCount = readVarInt();

        txHashs = new ArrayList<Sha256Hash>();
        for (int i = 0; i < txCount; i++) {
            txHashs.add(Sha256Hash.wrap(readBytes(32)));
        }

        length = cursor - offset;
    }

    /**
     * 序列化区块头
     */
    @Override
    public void serializeToStream(OutputStream stream) throws IOException {
        Utils.uint32ToByteStreamLE(version, stream);
        stream.write(preHash.getBytes());
        stream.write(merkleHash.getBytes());
        Utils.uint32ToByteStreamLE(time, stream);
        Utils.uint32ToByteStreamLE(height, stream);

        stream.write(new VarInt(scriptBytes.length).encode());
        stream.write(scriptBytes);

        //交易数量
        stream.write(new VarInt(txCount).encode());
        if (txHashs != null) {
            for (int i = 0; i < txCount; i++) {
                stream.write(txHashs.get(i).getBytes());
            }
        }
    }

    /**
     * 获取区块头部sha256 hash，用于签名验证
     *
     * @return byte[]
     * @throws IOException
     */
    public Sha256Hash getHeaderHash() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            Utils.uint32ToByteStreamLE(version, stream);
            stream.write(preHash.getBytes());
            stream.write(merkleHash.getBytes());
            Utils.int64ToByteStreamLE(time, stream);
            Utils.uint32ToByteStreamLE(height, stream);
            //交易数量
            stream.write(new VarInt(txCount).encode());
            return Sha256Hash.twiceOf(stream.toByteArray());
        } catch (IOException e) {
            throw e;
        } finally {
            stream.close();
        }
    }

    /**
     * 获取区块头信息
     *
     * @return BlockHeader
     */
    public BlockHeader getBlockHeader() {
        BlockHeader blockHeader = new BlockHeader(network, baseSerialize());
        return blockHeader;
    }

    /**
     * 运行区块签名脚本
     */
    public void verifyScript() throws VerificationException {
        //运行验证脚本
        BlockHeader temp = new BlockHeader(network, baseSerialize());
        try {
            scriptSig.runVerify(temp.getHeaderHash());
        } catch (IOException e) {
            throw new VerificationException(e);
        }
    }

    /**
     * 签名区块头信息
     *
     * @param account
     */
    public void sign(Account account) {
        scriptBytes = null;
        Sha256Hash headerHash = null;
        try {
            headerHash = getHeaderHash();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        //是否加密
        if (!account.isCertAccount() && account.isEncrypted()) {
            throw new AccountEncryptedException();
        }
        //普通账户
        ECKey key = account.getEcKey();
        ECKey.ECDSASignature ecSign = key.sign(headerHash);
        byte[] sign = ecSign.encodeToDER();

        scriptSig = ScriptBuilder.createSystemAccountScript(account.getAddress().getHash160(), key.getPubKey(true), sign);
        scriptBytes = scriptSig.getProgram();

        length += scriptBytes.length;
    }

    @Override
    public String toString() {
        return "BlockHeader [hash=" + hash + ", preHash=" + preHash + ", merkleHash=" + merkleHash + ", txCount="
                + txCount + ", time=" + time + ", height=" + height + "]";
    }

    public boolean equals(BlockHeader other) {
        return hash.equals(other.hash) && preHash.equals(other.preHash) && merkleHash.equals(other.merkleHash) &&
                txCount == other.txCount && time == other.time && height == other.height;
    }

    /**
     * 获得区块的打包人
     *
     * @return byte[]
     */
    public byte[] getHash160() {
        return scriptSig.getAccountHash160();
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Sha256Hash getHash() {
        return hash;
    }

    public void setHash(Sha256Hash hash) {
        this.hash = hash;
    }

    public Sha256Hash getPreHash() {
        return preHash;
    }

    public void setPreHash(Sha256Hash preHash) {
        this.preHash = preHash;
    }

    public Sha256Hash getMerkleHash() {
        return merkleHash;
    }

    public void setMerkleHash(Sha256Hash merkleHash) {
        this.merkleHash = merkleHash;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public long getTxCount() {
        return txCount;
    }

    public void setTxCount(long txCount) {
        this.txCount = txCount;
    }

    public List<Sha256Hash> getTxHashs() {
        return txHashs;
    }

    public void setTxHashs(List<Sha256Hash> txHashs) {
        this.txHashs = txHashs;
    }

    public byte[] getScriptBytes() {
        return scriptBytes;
    }

    public void setScriptBytes(byte[] scriptBytes) {
        this.scriptBytes = scriptBytes;
        scriptSig = new Script(scriptBytes);
    }

    public Script getScriptSig() {
        return scriptSig;
    }

    public void setScriptSig(Script scriptSig) {
        this.scriptSig = scriptSig;
    }

}
