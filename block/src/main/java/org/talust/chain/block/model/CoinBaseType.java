package org.talust.chain.block.model;

/**
 * coinbase类型
 */
public enum CoinBaseType {

    MINING(1), //挖矿所得
    DEPOSITION(2);//储蓄所得

    private int type;

    /**
     * @param type
     */
    CoinBaseType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static CoinBaseType getMessageType(int type) {
        for (CoinBaseType b : CoinBaseType.values()) {
            if (b.getType() == type) {
                return b;
            }
        }
        return null;
    }

}
