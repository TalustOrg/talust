package com.talust.chain.common.model;

import io.protostuff.Tag;

//储蓄帐户,与挖矿有关
public class DepositAccount {
    @Tag(1)//帐户地址
    private byte[] address;
    @Tag(2)//储蓄数量
    private double amount;

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
