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

package org.talust.common.tools;

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
     * 测试存储目录
     */
    public static String TEST_DIR = SERVER_HOME + File.separator + "test";

    /**
     * 数据存储目录
     */
    public static String DATA_DIR = SERVER_HOME + File.separator + "data";
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
    public static String NODE_SERVER_ADDR = "supernode.json";

    /**
     * 创世块ip
     */
    public static String GENESIS_SERVER_ADDR = "genesis.json";

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
