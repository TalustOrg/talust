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

		BlockStore gengsisBlock = new BlockStore(this, Hex.decode("0000000000000000000000000000000000000000000000000000000000000000000000005e72852afd85a3f66200393d06db87440fea91168056a00fe480dee060bd94cc4bddaa5b000000008221026a9c1b0b0e6c0722374b210552e74c12aa1508f38b9a9945285b39ac4f14225076a9142ae694f9a17085d5ac49d8ebe8f638401d9acf0f88463044022076cac5d05e23f28234dc7119f3295fe258fa8e2cf0cb6d99857ead86b68fbdb1022053c78e2532d7ae0ea62381a3d6d435a691ad0f6bcb288e9e7073881de6fe65bcac020101000000010012117468697320612067656e6773697320747800000000010000000000000000000000001975c3142ae694f9a17085d5ac49d8ebe8f638401d9acf0f88ac4bddaa5b000000000101000000010012117468697320612067656e6773697320747800000000010000000000000000000000001975c3142ae694f9a17085d5ac49d8ebe8f638401d9acf0f88ac4bddaa5b00000000"));

		Sha256Hash merkleHash = gengsisBlock.getBlock().buildMerkleHash();

		if(log.isDebugEnabled()) {
			log.debug("the gengsis block merkle hash is : {}", merkleHash);
		}
		Utils.checkState("5e72852afd85a3f66200393d06db87440fea91168056a00fe480dee060bd94cc".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");

		if(log.isDebugEnabled()) {
			log.debug("the gengsis block hash key is : {}", gengsisBlock.getBlock().getHash());
		}
		Utils.checkState("159616d91086e1441bc04c1babdc20036d002e55ad1d6a8fe6e3f5236cf94bb9".equals(Hex.encode(gengsisBlock.getBlock().getHash().getBytes())), "the gengsis block hash is error");

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
