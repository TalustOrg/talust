package com.talust.chain.web.configuration;

import com.talust.chain.common.tools.Configure;
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
