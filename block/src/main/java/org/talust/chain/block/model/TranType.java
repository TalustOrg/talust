package org.talust.chain.block.model;

/**
 * 交易类型
 */
public enum TranType {

    COIN_BASE(1), //挖矿所得
    TRANSFER(2),//转账
    DEPOSIT(3), //储蓄
    ACCOUNT(4),//账户下发
    BUSINESS(5);//业务数据

    private int type;

    /**
     * @param type
     */
    TranType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static TranType getMessageType(int type) {
        for (TranType b : TranType.values()) {
            if (b.getType() == type) {
                return b;
            }
        }
        return null;
    }

}
