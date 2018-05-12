package com.talust.chain.account;

//账户状态
public enum AccountStatus {

    ENABLE(1), //可用
    DISABLE(2),//不可用
    LOCK(3),//锁定
    VALIDATED(4),//已校验
    INVALIDATED(5)//未校验

    ;


    private int type;

    /**
     * @param type
     */
    AccountStatus(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static AccountStatus getAccountType(int type) {
        for (AccountStatus b : AccountStatus.values()) {
            if (b.getType() == type) {
                return b;
            }
        }
        return null;
    }

}
