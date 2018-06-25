package org.talust.chain.block.model;

//交易输出状态
public enum OutStatus {

    DISABLE(0), //不可用
    ENABLE(1),//可用
    LOCKED(2), //锁定状态,一般指这部分钱用于挖矿所用,解锁后变成可用状态
    END(10000)//结束,主要是为了开发时新增消息类型方便/
    ;


    private int type;

    /**
     * @param type
     */
    OutStatus(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static OutStatus getMessageType(int type) {
        for (OutStatus b : OutStatus.values()) {
            if (b.getType() == type) {
                return b;
            }
        }
        return null;
    }

}
