package org.talust.chain.common.tools;

/**
 * 一些常量的定义
 */
public interface Constant {
    int PORT = 9998;//节点端口
    /**
     * 用于存储最新块的hash值
     */
    byte[] NOW_BLOCK_HASH = "now_block".getBytes();
    /**
     * 区块高度前缀,即针对每一个区块高度,都会存储此高度对应的区块hash值
     */
    String BH_PRIX = "bh_";

    /**
     * 根证书的存储key
     */
    byte[] ROOT_CA = "root_ca".getBytes();

    /**
     * 控矿收益地址
     */
    byte[] MINING_ADDRESS = "mining_address".getBytes();

    /**
     * 帐户的前缀,主要用于存储帐户地址为标识的帐户信息,一般情况下存储的是经过平台认证的信息
     */
    byte[] ACC_PREFIX = "acc_".getBytes();

}
