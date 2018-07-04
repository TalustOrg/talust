/*
 * MIT License
 *
 * Copyright (c) 2017-2018 talust.org talust.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.talust.chain.block.model;

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
