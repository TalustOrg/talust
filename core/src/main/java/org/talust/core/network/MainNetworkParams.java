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

import com.alibaba.fastjson.JSONObject;
import org.talust.common.tools.Configure;
import org.talust.common.tools.Constant;
import org.talust.common.tools.FileUtil;
import org.talust.core.core.NetworkParams;
import org.talust.core.message.DefaultMessageSerializer;
import org.talust.core.model.MessageSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

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

	private String peerConfigPath = Configure.CONFIG_PATH;
	private String peerConfigFilePath = peerConfigPath + File.separator + "ConnectionConfig.json";
	private String peersFileDirPath = Configure.PEERS_PATH;
	private String peerPath = peersFileDirPath + File.separator + "peers.json";
	public String peerCont = "";

	public void peerConfigInit(){
		try {
			File config = new File(peerConfigFilePath);
			JSONObject peerConfig =  JSONObject.parseObject(FileUtil.fileToTxt(config));
			Configure.setMaxPassivityConnectCount(peerConfig.getInteger("MAX_PASSIVITY_CONNECT_COUNT"));
			Configure.setMaxActiveConnectCount(peerConfig.getInteger("MAX_ACTIVE_CONNECT_COUNT"));
			Configure.setMaxSuperActivrConnectCount(peerConfig.getInteger("MAX_SUPER_PASSIVITY_CONNECT_COUNT"));
			Configure.setMaxSuperPassivityConnectCount(peerConfig.getInteger("MAX_SUPER_ACTIVE_CONNECT_COUNT"));
			Configure.setNodeServerAddr(peerConfig.getString("NODE_SERVER_ADDR"));
			Configure.setGenesisServerAddr(peerConfig.getString("GENESIS_SERVER_ADDR"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public MainNetworkParams() {

		peerConfigInit();
		File file = new File(peersFileDirPath);
		if (!file.exists()) {
			file.mkdirs();
		}
		File peerFile = new File(peerPath);
		try {
			if (!peerFile.exists()) {
				peerFile.createNewFile();
				FileOutputStream fos = new FileOutputStream(peerFile);
				fos.write("{}".getBytes());
				fos.close();
				peerCont = "{}";
			} else {
				peerCont = FileUtil.fileToTxt(peerFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.seedManager = new RemoteSeedManager();
		JSONObject jsonObject = JSONObject.parseObject(peerCont);
		for(Object map : jsonObject.entrySet()){
			String peerIp = (String) ((Map.Entry) map).getKey();
			seedManager.add(new Seed(new InetSocketAddress(peerIp, Constant.PORT)));
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
	public MessageSerializer getSerializer(boolean parseRetain) {
		return new DefaultMessageSerializer(this);
	}


	/**
	 * 主网，普通地址以i开头
	 */
	@Override
	public int getSystemAccountVersion() {
		return 102;
	}

	/**
	 * 主网，认证地址以V开头
	 */
	@Override
	public int getCertAccountVersion() {
		return 70;
	}



}
