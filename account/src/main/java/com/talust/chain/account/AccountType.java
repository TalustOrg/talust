package com.talust.chain.account;

//账户类型
public enum AccountType {

    ROOT(1), //根帐户
    TALUST(2),//平台运营类型账户
    MINING(3),//挖矿的帐户,由平台运营帐户创建
    VERIFY(4),//平台运营管理员帐户
    ADMIN(5),//业务方即企业方管理员
    HR(6),//企业方HR
    USER(7)//普通用户
    ;


    private int type;

    /**
     * @param type
     */
    AccountType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static AccountType getAccountType(int type) {
        for (AccountType b : AccountType.values()) {
            if (b.getType() == type) {
                return b;
            }
        }
        return null;
    }

}
