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
import org.talust.core.script.Script;

import java.util.ArrayList;
import java.util.List;

//交易的输入项,考虑到一个用户可能会有多个帐户向某一个用户转账
//因此对一个用户来说
public class TransactionIn {
    public static final long NO_SEQUENCE = 0xFFFFFFFFL;
    @Tag(1)
    private List<TransactionOut> froms;
    @Tag(2)
    private Transaction parent;
    @Tag(3)
    private long sequence;
    @Tag(4)
    private byte[] scriptBytes;
    @Tag(5)
    private Script scriptSig;

    public List<TransactionOut> getFroms() {
        return froms;
    }

    public void setFroms(List<TransactionOut> froms) {
        this.froms = froms;
    }

    public Transaction getParent() {
        return parent;
    }

    public void setParent(Transaction parent) {
        this.parent = parent;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public Script getScriptSig() {
        return scriptSig;
    }

    public void setScriptSig(Script scriptSig) {
        this.scriptSig = scriptSig;
    }


    public TransactionIn() {
    }

    public TransactionIn(TransactionOut from) {
        super();
        this.froms = new ArrayList<TransactionOut>();
        this.froms.add(from);

        parent = from.getParent();

        this.sequence = NO_SEQUENCE;
    }

    public byte[] getScriptBytes() {
        return scriptBytes;
    }

    public void setScriptBytes(byte[] scriptBytes) {
        this.scriptBytes = scriptBytes;
    }
    public boolean addFrom(TransactionOut from) {
        if(froms == null) {
            froms = new ArrayList<>();
        }
        return froms.add(from);
    }
}
