package com.talust.chain.block.model;

import io.protostuff.Tag;

//交易的输入项,考虑到一个用户可能会有多个帐户向某一个用户转账
//因此对一个用户来说
public class TransactionIn {
    @Tag(1)
    private long tranNumber;//交易输入中对应的交易号
    @Tag(2)
    private int item;//交易号中的交易输出的第几项

    public long getTranNumber() {
        return tranNumber;
    }

    public void setTranNumber(long tranNumber) {
        this.tranNumber = tranNumber;
    }

    public int getItem() {
        return item;
    }

    public void setItem(int item) {
        this.item = item;
    }
}
