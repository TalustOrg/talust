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

import org.spongycastle.util.Arrays;
import org.talust.common.crypto.Utils;
import org.talust.common.exception.ProtocolException;
import org.talust.core.core.NetworkParams;
import org.talust.core.transaction.Transaction;

import java.io.IOException;
import java.io.OutputStream;

public class TransactionStore extends Store {
	
	//未花费
	public final static int STATUS_UNUSE = 1;
	//已花费
	public final static int STATUS_USED = 2;
	
	private byte[] key;
	//交易内容
	private Transaction transaction;
	//交易状态，是否已花费，对应的输出是否花费，多个输出就有多个状态
	private byte[] status;
	//区块高度
	private long height;
	
	
	public TransactionStore(NetworkParams network, byte[] content) {
		super(network, content, 0);
	}
	
	public TransactionStore(NetworkParams network, byte[] content, int offset) {
		super(network, content, offset);
	}
	
	public TransactionStore(NetworkParams network, Transaction transaction) {
		super(network);
		this.transaction = transaction;
		this.height = -1l;
	}

	public TransactionStore(NetworkParams network, Transaction transaction, long height, byte[] status) {
		super(network);
		this.transaction = transaction;
		this.height = height;
		this.status = status;
	}
	
	@Override
	protected void parse() throws ProtocolException {
		int statusLength = readBytes(1)[0];
		status = readBytes(statusLength);
		height = readUint32();
		
		transaction = network.getDefaultSerializer().makeTransaction(payload, 5 + statusLength);
		length = 5 + statusLength + transaction.getLength();
	}
	
	@Override
	protected void serializeToStream(OutputStream stream) throws IOException {
		if(status == null) {
			stream.write(0);
		} else {
			stream.write(status.length);
			stream.write(status);
		}
		Utils.uint32ToByteStreamLE(height, stream);
		stream.write(transaction.baseSerialize());
	}
	
	public Transaction getTransaction() {
		return transaction;
	}
	
	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}
	@Override
	public byte[] getKey() {
		if(key == null) {
			key = transaction.getHash().getBytes();
		}
		return key;
	}

	@Override
	public void setKey(byte[] key) {
		this.key = key;
	}

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}
	public void setStatus(byte[] status) {
		this.status = status;
	}
	public byte[] getStatus() {
		return status;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof TransactionStore)) {
			return false;
		}
		return transaction.getHash().equals(((TransactionStore)obj).getTransaction().getHash());
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(transaction.getHash().getBytes());
	}
}
