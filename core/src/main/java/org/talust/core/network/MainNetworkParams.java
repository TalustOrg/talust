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

package org.talust.core.network;


import org.talust.common.crypto.Hex;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.crypto.Utils;
import org.talust.common.tools.Constant;
import org.talust.core.core.NetworkParams;
import org.talust.core.message.DefaultMessageSerializer;
import org.talust.core.model.MessageSerializer;
import org.talust.core.storage.BlockStore;
import org.talust.network.netty.ConnectionManager;
import java.net.InetSocketAddress;

/**
 * 主网参数
 */
public class MainNetworkParams extends NetworkParams {

	private static MainNetworkParams instance;
	public static synchronized MainNetworkParams get() {
		if (instance == null) {
			instance = new MainNetworkParams();
		}
		return instance;
	}



	public MainNetworkParams() {
		this.seedManager = new RemoteSeedManager();
		for(String ip :ConnectionManager.get().getSuperIps())
		{
			seedManager.add(new Seed(new InetSocketAddress(ip, Constant.PORT)));
		}
		init();
	}

	public MainNetworkParams(SeedManager seedManager, int port) {
		this.seedManager = seedManager;
		this.port = port;

		init();
	}

	private void init() {
		this.packetMagic =  63943990L;
		this.acceptableAddressCodes = new int[] {getSystemAccountVersion(), getCertAccountVersion()};
	}

	@Override
	public int getProtocolVersionNum(ProtocolVersion version) {
		return version.getVersion();
	}

	@Override
	public byte[] getCommunityManagerHash160() {
		return new byte[0];
	}

	@Override
	public BlockStore getGengsisBlock() {

		BlockStore gengsisBlock = new BlockStore(this, Hex.decode("0000000000000000000000000000000000000000000000000000000000000000000000009f3b76b3c09418def3649de4c660433d2a4e78af1491a8f856b04a7c4070bf40dd218e5b00000000832102ba2f8529b7945e8ae0988e969922f6a7e04d3f1b74402f72c3d3a95c96ac502276a914d544052e95c298569f4791081d3d703a94f4df5a88473045022100e8b6340f0823ea3fad9f965caa574b2230718f0464fbc55df73da5d3f923dbe30220485ad2f4d5713ba958ef8451120c7ebe15a2eb8b0027318e7e857f1fce84d987ac020101000000010012117468697320612067656e6773697320747800000000010000000000000000000000001975c314d544052e95c298569f4791081d3d703a94f4df5a88acdd218e5b000000000101000000010012117468697320612067656e6773697320747800000000010000000000000000000000001975c314d544052e95c298569f4791081d3d703a94f4df5a88acdd218e5b00000000"));

		Sha256Hash merkleHash = gengsisBlock.getBlock().buildMerkleHash();

		if(log.isDebugEnabled()) {
			log.debug("the gengsis block merkle hash is : {}", merkleHash);
		}
		Utils.checkState("9f3b76b3c09418def3649de4c660433d2a4e78af1491a8f856b04a7c4070bf40".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");

		if(log.isDebugEnabled()) {
			log.debug("the gengsis block hash key is : {}", gengsisBlock.getBlock().getHash());
		}
		Utils.checkState("e554549a73a87aeaa8d475fe1866035790a4bbfa3757c615bacac7b2da29f74b".equals(Hex.encode(gengsisBlock.getBlock().getHash().getBytes())), "the gengsis block hash is error");

		return gengsisBlock;
	}

	@Override
	public MessageSerializer getSerializer(boolean parseRetain) {
		return new DefaultMessageSerializer(this);
	}


	/**
	 * 主网，普通地址以T开头
	 */
	@Override
	public int getSystemAccountVersion() {
		return 84;
	}

	/**
	 * 主网，认证地址以V开头
	 */
	@Override
	public int getCertAccountVersion() {
		return 86;
	}


	/**
	 * 主网，认证地址以M开头
	 */
	@Override
	public int getMainAccountVersion() {
		return 77;
	}
}
