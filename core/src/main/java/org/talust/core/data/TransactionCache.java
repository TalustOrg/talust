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


package org.talust.core.data;

import org.talust.common.crypto.Sha256Hash;
import org.talust.core.transaction.Transaction;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//交易缓存,主要用于存储交易临时状态的,即处于接收到交易数据,还未正式打包存储区块的情况
public class TransactionCache {
    private static TransactionCache instance = new TransactionCache();

    private Lock locker = new ReentrantLock();

    public static TransactionCache getInstace() {
        return instance;
    }

    private TransactionCache() {
    }

    private static final Map<Sha256Hash, Transaction> indexContainer = new HashMap<Sha256Hash, Transaction>();
    private static final ConcurrentLinkedQueue<Transaction> container = new ConcurrentLinkedQueue<Transaction>();
    private static final Map<Sha256Hash, Transaction> packageingContainer = new HashMap<Sha256Hash, Transaction>();

    public boolean  add(Transaction tx){
        locker.lock();
        try{
            if(indexContainer.containsKey(tx.getHash())) {
                return false;
            }
            boolean success = container.add(tx);
            if(success) {
                indexContainer.put(tx.getHash(), tx);
            }return success;
        } finally {
            locker.unlock();
        }
    }

    public boolean  remove(Sha256Hash hash){
        Transaction tx = indexContainer.remove(hash);
        if(tx != null) {
            packageingContainer.remove(tx.getHash());
            return container.remove(tx);
        } else {
            return false;
        }
    }

    public boolean bathRemove(Sha256Hash[] hashs) {
        for (Sha256Hash hash : hashs) {
            remove(hash);
        }
        return true;
    }

    public Transaction get() {
        Transaction tx = container.poll();
        if(tx != null) {
            indexContainer.remove(tx.getHash());
            packageingContainer.put(tx.getHash(), tx);
        }
        return tx;
    }

    public Transaction get(Sha256Hash hash) {
        Transaction tx = indexContainer.get(hash);
        if(tx == null) {
            tx = packageingContainer.get(hash);
        }
        return tx;
    }

    public Transaction[] getNewest(int max) {
        List<Transaction> list = new ArrayList<Transaction>();

        while(max > 0) {
            Transaction tx = container.poll();
            if(tx == null) {
                break;
            } else {
                indexContainer.remove(tx.getHash());
            }
            list.add(tx);
            max--;
        }
        return list.toArray(new Transaction[list.size()]);
    }

    /**
     * 获取内存里面交易数量
     */
    public int getTxCount() {
        return container.size();
    }
}
