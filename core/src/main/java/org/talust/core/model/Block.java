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
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.crypto.Utils;
import org.talust.common.exception.ProtocolException;
import org.talust.common.exception.VerificationException;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;
import org.talust.core.script.Script;
import org.talust.core.transaction.Transaction;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 区块消息

 */

public class Block extends BlockHeader {

	private static final Logger log = LoggerFactory.getLogger(Block.class);
	
	//交易列表
	private List<Transaction> txs;
		
	public Block(NetworkParams network, byte[] payloadBytes) {
		super(network, payloadBytes, 0);
	}
	
	public Block(NetworkParams network) {
		super(network);
	}

	/**
	 * 序列化
	 */
	@Override
	public void serializeToStream(OutputStream stream) throws IOException {
		txHashs = null;
		super.serializeToStream(stream);
		
		//交易数量
		for (int i = 0; i < txs.size(); i++) {
			Transaction tx = txs.get(i);
			stream.write(tx.baseSerialize());
		}
	}
	
	/**
	 * 反序列化
	 */
	@Override
	public void parse() throws ProtocolException {
		
		version = readUint32();
		preHash = Sha256Hash.wrap(readBytes(32));
		merkleHash = Sha256Hash.wrap(readBytes(32));
		time = readUint32();
		height = readUint32();
		scriptBytes = readBytes((int) readVarInt());
		scriptSig = new Script(scriptBytes);
		txCount = readVarInt();
		
		txs = new ArrayList<Transaction>();
		for (int i = 0; i < txCount; i++) {
			Transaction transaction = network.getDefaultSerializer().makeTransaction(payload, cursor);
			txs.add(transaction);
			cursor += transaction.getLength();
		}
		
		length = cursor;
	}
	
	/**
	 * 计算区块的梅克尔树根
	 * @return Sha256Hash
	 */
	public Sha256Hash buildMerkleHash() {
		
		List<byte[]> tree = new ArrayList<byte[]>();
        for (Transaction t : txs) {
            tree.add(t.getHash().getBytes());
        }
        int levelOffset = 0;
        for (int levelSize = txs.size(); levelSize > 1; levelSize = (levelSize + 1) / 2) {
            for (int left = 0; left < levelSize; left += 2) {
                int right = Math.min(left + 1, levelSize - 1);
                byte[] leftBytes = Utils.reverseBytes(tree.get(levelOffset + left));
                byte[] rightBytes = Utils.reverseBytes(tree.get(levelOffset + right));
                tree.add(Utils.reverseBytes(Sha256Hash.hashTwice(leftBytes, 0, 32, rightBytes, 0, 32)));
            }
            levelOffset += levelSize;
        }
        Sha256Hash merkleHash = Sha256Hash.wrap(tree.get(tree.size() - 1));
        if(this.merkleHash == null) {
        	this.merkleHash = merkleHash;
        } else {
        	try {
        		Utils.checkState(this.merkleHash.equals(merkleHash), "the merkle hash is error " + this.merkleHash +"  !=  " + merkleHash+"   blcok:" + toString());
        	} catch (Exception e) {
        		log.warn(e.getMessage(), e);
        		throw e;
			}
        }
		return merkleHash;
	}

	/**
	 * 计算区块hash
	 * @return Sha256Hash
	 */
	public Sha256Hash getHash() {
		if(hash == null) {
			hash = Sha256Hash.twiceOf(baseSerialize());
		}
//		if(!hash.equals(Sha256Hash.twiceOf(baseSerialize()))) {
//			throw new VerificationException("区块信息不正确 " + height);
//		}
		return hash;
	}
	
	/**
	 * 对区块做基本的验证
	 * 梅克尔树是否正确，coinbase交易是否正确
	 * @return boolean
	 */
	public boolean verify() {
		//验证梅克尔树根是否正确
		if(!buildMerkleHash().equals(getMerkleHash())) {
			throw new VerificationException("block merkle hash error");
		}
		
		//区块最大限制
		if(length > Definition.MAX_BLOCK_SIZE) {
			log.error("区块大小超过限制 {} , {}", getHeight(), getHash());
			throw new VerificationException("区块大小超过限制");
		}
		//每个区块只能包含一个coinbase交易，并且只能是第一个
		boolean coinbase = false;
		
		List<Transaction> txs = getTxs();
		//验证交易数量
		if(txs.size() != (int) txCount) {
			throw new VerificationException("交易数量不正确");
		}
		for (Transaction tx : txs) {
			//区块的第一个交易必然是coinbase交易，除第一个之外的任何交易都不应是coinbase交易，否则出错
			if(!coinbase) {
				if(tx.getType() != Definition.TYPE_COINBASE) {
					throw new VerificationException("区块的第一个交易必须是coinbase交易");
				}
				coinbase = true;
				continue;
			} else if(tx.getType() == Definition.TYPE_COINBASE) {
				throw new VerificationException("一个块只允许一个coinbase交易");
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Block [hash=");
		builder.append(getHash());
		builder.append(", preHash=");
		builder.append(preHash);
		builder.append(", merkleHash=");
		builder.append(merkleHash);
		builder.append(", time=");
		builder.append(time);
		builder.append(", height=");
		builder.append(height);
		builder.append(", txCount=");
		builder.append(txCount);
		builder.append("]");
		return builder.toString();
	}

	public boolean equals(BlockHeader other) {
		return hash.equals(other.hash) && preHash.equals(other.preHash) && merkleHash.equals(other.merkleHash) && 
				txCount == other.txCount && time == other.time && height == other.height;
	}

	public List<Transaction> getTxs() {
		return txs;
	}

	public void setTxs(List<Transaction> txs) {
		this.txs = txs;
	}
}
