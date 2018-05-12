package com.talust.chain.block.mining;

import lombok.extern.slf4j.Slf4j;

@Slf4j //挖矿规则
public class MiningRule {

    /**
     * 获取矿机挖矿币数,不同的区块高度时,所获取的奖励是不一样的
     *
     * @return
     */
    public static double getBaseCoin(int height) {

        return 16;//先写死,后面再写具体的规则
    }

    /**
     * 获取
     * @param height
     * @return
     */
    public static double getDepositCoin(int height){

        return 256;
    }


}
