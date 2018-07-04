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


package org.talust.chain.block.data;

import io.netty.util.internal.ConcurrentSet;

import java.util.Set;

//交易缓存,主要用于存储交易临时状态的,即处于接收到交易数据,还未正式打包存储区块的情况
public class TransactionCache {
    private static TransactionCache instance = new TransactionCache();

    private TransactionCache() {
    }

    public static TransactionCache get() {
        return instance;
    }

    private Set<String> outTmpDisable = new ConcurrentSet<>();

    public void disableOut(long tranNumber, int item) {
        String its = tranNumber + "-" + item;
        outTmpDisable.add(its);
    }

    public boolean isDisable(long tranNumber, int item) {
        String its = tranNumber + "-" + item;
        return outTmpDisable.contains(its);
    }

    public void removeDisable(long tranNumber, int item) {
        String its = tranNumber + "-" + item;
        outTmpDisable.remove(its);
    }


}
