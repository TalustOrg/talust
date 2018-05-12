package com.talust.chain.block.model;

import io.protostuff.Tag;

import java.util.List;

//交易信息,针对UTXO模型实现的
public class Transaction {
    @Tag(1)
    private int tranType;//交易类型
    @Tag(2)
    private long tranNumber;//交易号
    @Tag(3)
    private List<TransactionIn> ins;//交易输入项
    @Tag(4)
    private List<TransactionOut> outs;//交易输出项
    @Tag(5)
    private byte[] datas;//与交易有关的数据,具体由业务类型确定

    public int getTranType() {
        return tranType;
    }

    public void setTranType(int tranType) {
        this.tranType = tranType;
    }

    public long getTranNumber() {
        return tranNumber;
    }

    public void setTranNumber(long tranNumber) {
        this.tranNumber = tranNumber;
    }

    public List<TransactionIn> getIns() {
        return ins;
    }

    public void setIns(List<TransactionIn> ins) {
        this.ins = ins;
    }

    public List<TransactionOut> getOuts() {
        return outs;
    }

    public void setOuts(List<TransactionOut> outs) {
        this.outs = outs;
    }

    public byte[] getDatas() {
        return datas;
    }

    public void setDatas(byte[] datas) {
        this.datas = datas;
    }
}
