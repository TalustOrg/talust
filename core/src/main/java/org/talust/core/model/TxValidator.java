package org.talust.core.model;/*
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

import org.talust.common.model.DepositAccount;
import org.talust.core.transaction.Transaction;

public class TxValidator {
    private long value ;
    private byte[] address;
    private byte[] nodeAddress;
    private DepositAccount depositAccount;
    private Transaction transaction;

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public DepositAccount getDepositAccount() {
        return depositAccount;
    }

    public void setDepositAccount(DepositAccount depositAccount) {
        this.depositAccount = depositAccount;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public byte[] getAddress() {
        return address;
    }

    public byte[] getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(byte[] nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public TxValidator(long value, byte[] address, byte[] nodeAddress,DepositAccount depositAccount) {
        this.value = value;
        this.address = address;
        this.nodeAddress = nodeAddress;
        this.depositAccount = depositAccount;
    }

    public TxValidator(long value, byte[] address,byte[] nodeAddress, Transaction transaction) {
        this.value = value;
        this.address = address;
        this.nodeAddress = nodeAddress;
        this.transaction = transaction;
    }
}
