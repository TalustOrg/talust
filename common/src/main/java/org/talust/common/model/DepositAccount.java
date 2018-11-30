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

package org.talust.common.model;

import io.protostuff.Tag;
import org.talust.common.crypto.Sha256Hash;

import java.util.List;

//储蓄帐户,与挖矿有关
public class DepositAccount {
    @Tag(1)//帐户地址
    private byte[] address;
    @Tag(2)//储蓄数量
    private Coin amount;
    @Tag(3)//储蓄交易hash
    private List<Sha256Hash> txHash;

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public Coin getAmount() {
        return amount;
    }

    public void setAmount(Coin amount) {
        this.amount = amount;
    }

    public List<Sha256Hash> getTxHash() {
        return txHash;
    }

    public void setTxHash(List<Sha256Hash> txHash) {
        this.txHash = txHash;
    }

    public DepositAccount(byte[] address , Coin coin , List<Sha256Hash> txHash){
        this.address=address;
        this.amount=coin;
        this.txHash=txHash;
    }
}
