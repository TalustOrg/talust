package org.talust.chain.block.data;

import io.netty.util.internal.ConcurrentSet;

import java.util.Set;

//交易缓存,主要用于存储交易临时状态的,即处于接收到交易数据,还未正式打包存储区块的情况
public class TransactionCache {
    private static TransactionCache instance = new TransactionCache();

    private TransactionCache() {
    }

    public static TransactionCache get() {
        return instance;
    }

    private Set<String> outTmpDisable = new ConcurrentSet<>();

    public void disableOut(long tranNumber, int item) {
        String its = tranNumber + "-" + item;
        outTmpDisable.add(its);
    }

    public boolean isDisable(long tranNumber, int item) {
        String its = tranNumber + "-" + item;
        return outTmpDisable.contains(its);
    }

    public void removeDisable(long tranNumber, int item) {
        String its = tranNumber + "-" + item;
        outTmpDisable.remove(its);
    }


}
