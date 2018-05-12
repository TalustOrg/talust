package com.talust.chain.block.model;

import io.protostuff.Tag;

//交易输出
public class TransactionOut {
    @Tag(1)
    private int item;//交易输出的第几项
    @Tag(2)
    private double amount;//数额
    @Tag(3)
    private byte[] address;//目标地址
    @Tag(4)
    private int time;//块的时间,即本次交易在哪一个块中得到了确认
    @Tag(5)
    private int status;//状态
    @Tag(6)
    private Integer coinBaseType;//coinbase类型,目前分为两种,一种是矿机所得,一种是储蓄所得
    @Tag(7)
    private int enableHeight;//可用的高度,即超过设定的高度才可以使用,通过此设置,可实现锁定功能

    public int getItem() {
        return item;
    }

    public void setItem(int item) {
        this.item = item;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Integer getCoinBaseType() {
        return coinBaseType;
    }

    public void setCoinBaseType(Integer coinBaseType) {
        this.coinBaseType = coinBaseType;
    }

    public int getEnableHeight() {
        return enableHeight;
    }

    public void setEnableHeight(int enableHeight) {
        this.enableHeight = enableHeight;
    }
}
