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


package org.talust.core.core;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talust.common.tools.Constant;
import org.talust.core.model.MessageSerializer;
import org.talust.core.network.SeedManager;

/**
 * 网络参数，网络协议参数配置都在本类下面
 */
public abstract class NetworkParams {
	
	protected Logger log = LoggerFactory.getLogger(getClass());
	
    public static final String ID_MAINNET = "org.talsut.prod";
    public static final String ID_TESTNET = "org.talust.test";
	
	protected String id;
	
	//p2p网络端口
	protected int port = Constant.PORT;
	//网络协议魔法参数
	protected long packetMagic;
	
    //允许的地址前缀
    protected int[] acceptableAddressCodes = {};

	protected SeedManager seedManager;

	//消息序列化工具
	protected transient MessageSerializer defaultSerializer = null;
	
	//区块存储提供器

	//网络最新高度
	protected long bestHeight;
	
	/**
	 * 获取默认的消息序列化工具
	 * @return {@link MessageSerializer}
	 */
    public final MessageSerializer getDefaultSerializer() {
    	//简单的单例
        if (null == this.defaultSerializer) {
            synchronized(this) {
            	//没有初始化，那么现在开始初始化
                if (null == this.defaultSerializer) {
                    this.defaultSerializer = getSerializer(false);
                }
            }
        }
        return defaultSerializer;
    }
	

    /**
     * 不同的网络可能用到不同的消息序列化工具，这里交给具体的子类去实现
     * @param parseRetain
     * @return {@link MessageSerializer}
     */
    public abstract MessageSerializer getSerializer(boolean parseRetain);
	
    /**
     * 获取协议的版本号
     * @param version 协议版本 {@link ProtocolVersion}
     * @return int
     */
	public abstract int getProtocolVersionNum(final ProtocolVersion version);

	/**
	 * 获取该网络的社区管理账号的hash160
	 */
	public abstract byte[] getCommunityManagerHash160();

	public static enum ProtocolVersion {
        CURRENT(1);

        private final int version;

        ProtocolVersion(final int version) {
            this.version = version;
        }

        public int getVersion() {
            return version;
        }
    }
    
	/**
	 * 获取最新区块高度
	 * @return long
	 */
//	public long getBestBlockHeight() {
//		BlockHeaderStore blockHeader = blockStoreProvider.getBestBlockHeader();
//		if(blockHeader == null) {
//			return 0l;
//		} else {
//			return blockHeader.getBlockHeader().getHeight();
//		}
//	}
	
	/**
	 * 获取最新区块头信息
	 * @return long
	 */
//	public BlockHeader getBestBlockHeader() {
//		BlockHeaderStore bestBlockHeaderStore = blockStoreProvider.getBestBlockHeader();
//		if(bestBlockHeaderStore == null) {
//			return getGengsisBlock().getBlock();
//		}
//		return bestBlockHeaderStore.getBlockHeader();
//	}
	
	/**
	 * 返回当前本地区块的状态，是否是最新状态
	 * @return boolean
	 */
//	public boolean blockIsNewestStatus() {
//		return bestHeight != 0l && getBestBlockHeight() >= bestHeight - 2;
//	}
	
    /**
     * 运行的地址前缀
     * @return int[]
     */
    public int[] getAcceptableAddressCodes() {
        return acceptableAddressCodes;
    }
	
	/**
	 * 得到普通账户的地址版本号
	 * @return int
	 */
	public abstract int getSystemAccountVersion();
	
	/**
	 * 得到认证账户的地址版本号
	 * @return int
	 */
	public abstract int getCertAccountVersion();

	/**
	 * 当前服务运行在哪个网络上面
	 * @return long
	 */
	public int getLocalServices() {
		if(ID_MAINNET.equals(id)){
			return 1;
		} else if(ID_TESTNET.equals(id)) {
			return 2;
		}
		return 0;
	}
	
	public int getPort() {
		return port;
	}
	
	public long getPacketMagic() {
        return packetMagic;
    }
    
    public String getId() {
		return id;
	}

//	public void setBlockStoreProvider(BlockStoreProvider blockStoreProvider) {
//		this.blockStoreProvider = blockStoreProvider;
//	}

	public long getBestHeight() {
		return bestHeight;
	}

	public void setBestHeight(long bestHeight) {
		this.bestHeight = bestHeight;
	}

	public SeedManager getSeedManager() {
		return seedManager;
	}
}
