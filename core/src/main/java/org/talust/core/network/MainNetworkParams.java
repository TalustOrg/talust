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

		BlockStore gengsisBlock = new BlockStore(this, Hex.decode("0000000000000000000000000000000000000000000000000000000000000000000000001ab5fa8571945d7d251e5b331c043b75e6057c930fbc755f60c503fe5bba00cffba58c5b00000000832103ad7519e36d7e2908b84d83468a396744cb0cda8f0559830a697a150c1aeb52bd76a91417aa3fad211da0c590ebaf5147c41a93220d412d884730450221009abf0fa3674758d2a93da9753cb060b5183a4be9520ad41fe3a32c95c0dd5af202205dbc7be85d22cb069c465a1cb93239fa4a1bdeb2f590dc59dca10a89cf0d5747ac020101000000010012117468697320612067656e6773697320747800000000010000000000000000000000001975c31417aa3fad211da0c590ebaf5147c41a93220d412d88acfba58c5b000000000101000000010012117468697320612067656e6773697320747800000000010000000000000000000000001975c31417aa3fad211da0c590ebaf5147c41a93220d412d88acfba58c5b00000000"));

		Sha256Hash merkleHash = gengsisBlock.getBlock().buildMerkleHash();

		if(log.isDebugEnabled()) {
			log.debug("the gengsis block merkle hash is : {}", merkleHash);
		}
		Utils.checkState("1ab5fa8571945d7d251e5b331c043b75e6057c930fbc755f60c503fe5bba00cf".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");

		if(log.isDebugEnabled()) {
			log.debug("the gengsis block hash key is : {}", gengsisBlock.getBlock().getHash());
		}
		Utils.checkState("091799a143e46146c8e659377b8143dbb4f900054567d239dfda33c496447cf7".equals(Hex.encode(gengsisBlock.getBlock().getHash().getBytes())), "the gengsis block hash is error");

		return gengsisBlock;
	}

	@Override
	public MessageSerializer getSerializer(boolean parseRetain) {
		return new DefaultMessageSerializer(this);
	}


	/**
	 * 主网，普通地址以i开头
	 */
	@Override
	public int getSystemAccountVersion() {
		return 85;
	}

	/**
	 * 主网，认证地址以V开头
	 */
	@Override
	public int getCertAccountVersion() {
		return 70;
	}



}
