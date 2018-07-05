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

package org.talust.chain.web.configuration;

import org.talust.chain.common.tools.Configure;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 初始化配置内容
 */
@Component
public class ConfigureInit {

    @Value("${connection.max}")
    private int maxConnectCount;
    @Value("${connection.max_to}")
    private int maxConnectToCount;
    @Value("${connection.super_max}")
    private int maxSuperCount;
    @Value("${node.server.address}")
    private String nodeServerAddr;
    @Value("${node.server.gensisip}")
    private String gensisIp;
    @Value("${account.root.pubkey}")
    private String rootPub;
    @Value("${account.root.sign}")
    private String rootSign;
    @Value("${account.talust.pubkey}")
    private String talustPub;
    @Value("${account.talust.sign}")
    private String talustSign;
    @Value("${account.mining.pubkey}")
    private String miningPub;
    @Value("${account.mining.sign}")
    private String miningSign;

    @PostConstruct
    public void init() {
        Configure.MAX_CONNECT_COUNT = maxConnectCount;
        Configure.MAX_CONNECT_TO_COUNT = maxConnectToCount;
        Configure.MAX_SUPER_CONNECT_COUNT = maxSuperCount;
        Configure.NODE_SERVER_ADDR = nodeServerAddr;
        Configure.GENESIS_IP = gensisIp;
        Configure.ROOT_PUB = rootPub;
        Configure.ROOT_SIGN = rootSign;
        Configure.TALUST_PUB = talustPub;
        Configure.TALUST_SIGN = talustSign;
        Configure.MINING_PUB = miningPub;
        Configure.MINING_SIGN = miningSign;
    }
}
