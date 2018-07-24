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

package org.talust.block.model;

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
