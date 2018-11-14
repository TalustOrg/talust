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
		this.acceptableAddressCodes = new int[] {getSystemAccountVersion(), getCertAccountVersion(),getMainAccountVersion()};
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

		BlockStore gengsisBlock = new BlockStore(this, Hex.decode("0000000000000000000000000000000000000000000000000000000000000000000000008c2f2c81b4e0e106e333b342d06c35133a9aedca4a59939904aacdc4179019f315ebe35b00000000822103d02169b93eb80ec3240bfefd08b59e6a5294c27f5d743b7a92d4a2b90df6ca5d76a914ec80b937465a7edffdcd0e75702854b7ad59d27e8846304402206b6df7594e75170355d2e7c3ffd2add0313afe8fed9ebd6db3ec054c930b665902203d2de78f36e21299d803b592f2dfded256b5bde6cecb07436e7a6e445b0b4090ac020101000000010012117468697320612067656e6773697320747800000000010000000000000000000000001975c314ec80b937465a7edffdcd0e75702854b7ad59d27e88ac15ebe35b000000000101000000010012117468697320612067656e6773697320747800000000010000000000000000000000001975c314ec80b937465a7edffdcd0e75702854b7ad59d27e88ac15ebe35b00000000"));

		Sha256Hash merkleHash = gengsisBlock.getBlock().buildMerkleHash();

		if(log.isDebugEnabled()) {
			log.debug("the gengsis block merkle hash is : {}", merkleHash);
		}
		Utils.checkState("8c2f2c81b4e0e106e333b342d06c35133a9aedca4a59939904aacdc4179019f3".equals(Hex.encode(merkleHash.getBytes())), "the gengsis block merkle hash is error");

		if(log.isDebugEnabled()) {
			log.debug("the gengsis block hash key is : {}", gengsisBlock.getBlock().getHash());
		}
		Utils.checkState("f76f27b02bba17119e2700e9e3f6caea3f876aaaf19bdc480c686dc36fc97ff9".equals(Hex.encode(gengsisBlock.getBlock().getHash().getBytes())), "the gengsis block hash is error");

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
	 * TODO 加入参数集合
	 */
	@Override
	public int getMainAccountVersion() {
		return 77;
	}
}
