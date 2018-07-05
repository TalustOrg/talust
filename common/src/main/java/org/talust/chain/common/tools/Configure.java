package org.talust.chain.common.tools;

import java.io.File;

/**
 * 配置内容
 */
public final class Configure {

    public static String SERVER_HOME;

    static {
        SERVER_HOME = System.getProperty("user.dir");
    }

    /**
     * 数据存储目录
     */
    public static String DATA_DIR = SERVER_HOME + File.separator + "data";
    /**
     * 数据存储目录
     */
    public static String TEST = SERVER_HOME + File.separator + "test";
    /**
     * 账户存储目录
     */
    public static String DATA_ACCOUNT = DATA_DIR + File.separator + "account";
    /**
     * 区块存储目录
     */
    public static String DATA_BLOCK = DATA_DIR + File.separator + "block";
    /**
     * 区块状态存储目录
     */
    public static String DATA_CHAINSTATE = DATA_DIR + File.separator + "chainstate";
    /**
     * 与帐户有关的交易存储目录
     */
    public static String DATA_TRANSACTION = DATA_DIR + File.separator + "transaction";

    /**
     * 最大允许节点连接数  // 这里指允许其他节点接入的数量
     */
    public static int MAX_CONNECT_COUNT = 3;
    /**
     * 最大主动连接其他节点的数量,主动连接数要少于被动连接数
     */
    public static int MAX_CONNECT_TO_COUNT = 2;
    /**
     * 超级节点之间最大连接数
     */
    public static int MAX_SUPER_CONNECT_COUNT = 3;

    /**
     * 区块生成间隔时间，单位秒
     */
    public static int BLOCK_GEN_TIME = 6;

    /**
     * 超级节点获取地址
     */
    public static String NODE_SERVER_ADDR = "http://dev.talust.com:18004/ip.txt";

    /**
     * 创世块ip
     */
    public static String GENESIS_IP = "192.168.0.150";

    /**
     * 根证书公钥,需要在实际情况下进行修改
     */
    public static String ROOT_PUB = "";

    /**
     * 根证书的自签名内容
     */
    public static String ROOT_SIGN = "";

    /**
     * talust公钥,需要在实际情况下进行修改
     */
    public static String TALUST_PUB = "";

    /**
     * talust的签名内容,由根证书自成
     */
    public static String TALUST_SIGN = "";

    /**
     * 初始化挖矿公钥,需要在实际情况下进行修改
     */
    public static String MINING_PUB = "";

    /**
     * 初始化挖矿的签名内容,由talust帐户自成
     */
    public static String MINING_SIGN = "";

}
