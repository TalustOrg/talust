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

package org.talust.storage;

import org.rocksdb.RocksIterator;
import org.talust.account.Account;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.tools.Configure;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.talust.common.tools.SerializationUtil;
import org.talust.core.transaction.TransactionOut;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//交易存储，存储于自身账户有关的交易
@Slf4j
public  class TransactionStorage extends BaseStoreProvider {
    private static TransactionStorage instance = new TransactionStorage();

    private TransactionStorage() {
        this(Configure.DATA_TRANSACTION);
    }
    public static TransactionStorage get() {
        return instance;
    }
    public TransactionStorage(String dir) {
        super(dir);
    }
    //存放交易记录账号的key
    private final static byte[] ADDRESSES_KEY = Sha256Hash.ZERO_HASH.getBytes();
    //交易记录对应的账号列表
    private List<byte[]> addresses = new CopyOnWriteArrayList<byte[]>();
    //我的交易列表
    private List<TransactionOut> mineTxList = new CopyOnWriteArrayList<TransactionOut>();
    //未花费的交易
    private List<TransactionOut> unspendTxList = new CopyOnWriteArrayList<TransactionOut>();




    public void put(byte[] key, byte[] value) {
        try {
            db.put(key, value);
        } catch (Exception e) {
        }
    }

    public byte[] get(byte[] key) {
        try {
            return db.get(key);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    //初始化所有交易中对应本地已经存在的地址的相关交易
    public void init() {
       List<Account> accounts =  AccountStorage.get().getAccounts();
        RocksIterator iter = db.newIterator();
        for(iter.seekToFirst(); iter.isValid(); iter.next()) {
            System.out.println("iter key:" + new String(iter.key()) + ", iter value:" + new String(iter.value()));
            byte[] key = iter.key();
            if(Arrays.equals(ADDRESSES_KEY, key)) {
                continue;
            }
            byte[] value = iter.value();
            TransactionOut out =  SerializationUtil.deserializer(value,TransactionOut.class);
            mineTxList.add(out);
        }
    }






}
