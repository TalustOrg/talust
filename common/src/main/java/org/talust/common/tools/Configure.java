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
     * peers 文件储存目录
     */
    public static String PEERS_PATH  = DATA_DIR + File.separator + "peers";
    /**
     * config 文件储存目录
     */
    public static String CONFIG_PATH  = DATA_DIR + File.separator + "config";

    /**
     * 业务节点最大被动连接数
     */
    public static int MAX_PASSIVITY_CONNECT_COUNT  ;
    /**
     * 业务节点最大主动连接数
     */
    public static int MAX_ACTIVE_CONNECT_COUNT;
    /**
     * 超级节点被动连接数
     */
    public static int MAX_SUPER_PASSIVITY_CONNECT_COUNT ;
    /**
     * 超级节点主动连接数
     */
    public static int MAX_SUPER_ACTIVE_CONNECT_COUNT ;

    /**
     * 区块生成间隔时间，单位秒
     */
    public static int BLOCK_GEN_TIME = 6;

    /**
     * 超级节点获取地址
     */
    public static String NODE_SERVER_ADDR ;

    /**
     * 创世块ip
     */
    public static String GENESIS_SERVER_ADDR ;


    public static void setMaxPassivityConnectCount(int maxPassivityConnectCount) {
        MAX_PASSIVITY_CONNECT_COUNT = maxPassivityConnectCount;
    }
    public static void setMaxActiveConnectCount(int maxActiveConnectCount) {
        MAX_ACTIVE_CONNECT_COUNT = maxActiveConnectCount;
    }
    public static void setMaxSuperPassivityConnectCount(int maxSuperPassivityConnectCount) {
        MAX_SUPER_PASSIVITY_CONNECT_COUNT = maxSuperPassivityConnectCount;
    }
    public static void setMaxSuperActivrConnectCount(int maxSuperActivrConnectCount) {
        MAX_SUPER_ACTIVE_CONNECT_COUNT = maxSuperActivrConnectCount;
    }
    public static void setNodeServerAddr(String nodeServerAddr) {
        NODE_SERVER_ADDR = nodeServerAddr;
    }
    public static void setGenesisServerAddr(String genesisServerAddr) {
        GENESIS_SERVER_ADDR = genesisServerAddr;
    }
}
