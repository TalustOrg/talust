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

package org.talust.core.transaction;

import io.protostuff.Tag;
import org.talust.common.crypto.ECKey;
import org.talust.core.model.Address;
import org.talust.core.model.Coin;
import org.talust.core.script.Script;
import org.talust.core.script.ScriptBuilder;

//交易输出
public class TransactionOut {
    @Tag(1)
    private long amount;//数额
    @Tag(2)
    private byte[] address;//目标地址
    @Tag(3)
    private long enableHeight;//可用的高度,即超过设定的高度才可以使用,通过此设置,可实现锁定功能
    @Tag(4)
    private Script script;
    @Tag(5)
    private byte[] scriptBytes;
    @Tag(6)
    private Transaction parent;
    @Tag(7)
    private TransactionIn spentBy;

    @Tag(8)
    private Integer coinBaseType;


    public TransactionOut() {

    }

    public TransactionOut(Transaction parent) {
        this.parent = parent;
    }

    public TransactionOut(Transaction parent, Coin value, Address to) {
        this(parent, value, 0l, to);
    }
    public TransactionOut(Transaction parent, Coin value, long lockTime, Address to) {
        this(parent, value, lockTime, ScriptBuilder.createOutputScript(to).getProgram());
    }
    public TransactionOut(Transaction parent, Coin value, ECKey to) {
        this(parent, value, 0l, ScriptBuilder.createOutputScript(to).getProgram());
    }
    public TransactionOut(Transaction parent, Coin value,  byte[] scriptBytes) {
        this(parent, value, 0l, scriptBytes);
    }
    public TransactionOut(Transaction parent, Coin value, long lockTime, byte[] scriptBytes) {
        this.parent = parent;
        this.amount = value.value;
        this.enableHeight = lockTime;
        this.scriptBytes = scriptBytes;
        this.script = new Script(scriptBytes);
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public long getEnableHeight() {
        return enableHeight;
    }

    public void setEnableHeight(long enableHeight) {
        this.enableHeight = enableHeight;
    }

    public Script getScript() {
        return script;
    }

    public void setScript(Script script) {
        this.script = script;
    }

    public byte[] getScriptBytes() {
        return scriptBytes;
    }

    public void setScriptBytes(byte[] scriptBytes) {
        this.scriptBytes = scriptBytes;
    }

    public Transaction getParent() {
        return parent;
    }

    public void setParent(Transaction parent) {
        this.parent = parent;
    }

    public TransactionIn getSpentBy() {
        return spentBy;
    }

    public void setSpentBy(TransactionIn spentBy) {
        this.spentBy = spentBy;
    }

    public Integer getCoinBaseType() {
        return coinBaseType;
    }

    public void setCoinBaseType(Integer coinBaseType) {
        this.coinBaseType = coinBaseType;
    }
}
