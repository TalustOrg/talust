package org.talust.core.storage;


import org.talust.common.crypto.Sha256Hash;
import org.talust.common.crypto.UnsafeByteArrayOutputStream;
import org.talust.common.exception.ProtocolException;
import org.talust.core.core.NetworkParams;
import org.talust.core.model.BlockHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 区块头信息
 */
public class BlockHeaderStore extends Store {

	private BlockHeader blockHeader;
	//下一区块hash
	private Sha256Hash nextHash;
	
	public BlockHeaderStore(NetworkParams network) {
		super(network);
	}

	public BlockHeaderStore(NetworkParams network, byte[] content) {
		super(network, content, 0);
	}

	@Override
	public void parse() throws ProtocolException {
		blockHeader = new BlockHeader(network, payload);
		cursor = blockHeader.getLength();
		nextHash = Sha256Hash.wrap(readBytes(32));
		length = cursor - offset;
	}

	/**
	 * 序列化区块头信息
	 */
	@Override
	public void serializeToStream(OutputStream stream) throws IOException {
		
		blockHeader.serializeToStream(stream);
		stream.write(nextHash.getBytes());
    }
	
	/**
	 * 序列化区块头
	 */
	public byte[] serializeHeader() throws IOException {
		ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream();
		try {
			serializeToStream(stream);
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

	public BlockHeader getBlockHeader() {
		return blockHeader;
	}

	public void setBlockHeader(BlockHeader blockHeader) {
		this.blockHeader = blockHeader;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BlockHeaderStore [blockHeader=");
		builder.append(blockHeader);
		builder.append(", nextHash=");
		builder.append(nextHash);
		builder.append("]");
		return builder.toString();
	}
}
