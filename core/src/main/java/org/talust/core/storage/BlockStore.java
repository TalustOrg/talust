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
package org.talust.core.storage;


import org.springframework.data.mongodb.core.mapping.Document;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.crypto.UnsafeByteArrayOutputStream;
import org.talust.common.crypto.Utils;
import org.talust.common.crypto.VarInt;
import org.talust.common.exception.ProtocolException;
import org.talust.core.core.NetworkParams;
import org.talust.core.model.Block;
import org.talust.core.transaction.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 区块完整信息
 */
@Document(collection = "blockStore")
public class BlockStore extends Store {

	//下一区块hash
	private Sha256Hash nextHash;
	private Block block;
	
	public BlockStore(NetworkParams network) {
		super(network);
	}

	public BlockStore(NetworkParams network, byte[] content) {
		super(network, content, 0);
	}
	
	public BlockStore(NetworkParams network, Block block) {
		super(network);
		this.block = block;
		nextHash = Sha256Hash.ZERO_HASH;
	}

	@Override
	public void parse() throws ProtocolException {
		
		block = new Block(network, payload);
		nextHash = Sha256Hash.wrap(readBytes(32));
		length = cursor;
	}

	/**
	 * 序列化区块
	 */
	@Override
	public void serializeToStream(OutputStream stream) throws IOException {
		
		block.serializeToStream(stream);
		stream.write(nextHash.getBytes());
    }
	
	/**
	 * 序列化头信息
	 */
	public byte[] serializeHeaderToBytes() throws IOException {
		ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream();
		
		try {
			Utils.uint32ToByteStreamLE(block.getVersion(), stream);
			stream.write(block.getPreHash().getBytes());
			stream.write(block.getMerkleHash().getBytes());
			Utils.uint32ToByteStreamLE(block.getTime(), stream);
			Utils.uint32ToByteStreamLE(block.getHeight(), stream);

			stream.write(new VarInt(block.getScriptBytes().length).encode());
			stream.write(block.getScriptBytes());
			//交易数量
			stream.write(new VarInt(block.getTxCount()).encode());
			for (Transaction tx : block.getTxs()) {
				stream.write(tx.getHash().getBytes());
			}
			stream.write(nextHash.getBytes());
			return stream.toByteArray();
		} finally {
			stream.close();
		}
	}
	
	public Sha256Hash getNextHash() {
		return nextHash;
	}

	public void setNextHash(Sha256Hash nextHash) {
		this.nextHash = nextHash;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}
	
}
