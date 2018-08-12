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
package org.talust.core.storage;

import org.talust.common.model.DepositAccount;
import org.talust.common.tools.Configure;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.util.SizeUnit;
import org.talust.storage.BaseStoreProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

//交易帐户余额存储,用于存储每一个帐户的每一个交易过来的余额,针对UTXO模型实现的
@Slf4j
public class ChainStateStorage extends BaseStoreProvider {
    private static ChainStateStorage instance = new ChainStateStorage();

    private ChainStateStorage() {
        this(Configure.DATA_CHAINSTATE);
    }

    public static ChainStateStorage get() {
        return instance;
    }

    public ChainStateStorage(String dir) {
        super(dir);
    }


    private byte[] TRAN_NUMBER = "tranNumber".getBytes();
    private byte[] ADDRESS_AMOUNT = "addressAmount".getBytes();
    private AtomicLong tranNumber;


    public void put(byte[] key, byte[] value)  {
        try {
            db.put(key, value);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }

    }

    public byte[] get(byte[] key)  {
        try {
            return db.get(key);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void initTranNumber() {
        try {
            byte[] bytes = db.get(TRAN_NUMBER);
            if (bytes != null) {
                tranNumber = new AtomicLong(Long.parseLong(new String(bytes)));
            } else {//交易号默认从100开始
                tranNumber = new AtomicLong(100);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public synchronized long newTranNumber() {
        return tranNumber.addAndGet(1);
    }

    public void saveTranNumber() {
        try {
            db.put(TRAN_NUMBER, Long.toString(tranNumber.get()).getBytes());
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取超级节点下的储蓄帐户,需要根据这些帐户的储蓄计算挖矿收益
     *
     * @param miningAddress
     * @return
     */
    public List<DepositAccount> getDeposits(String miningAddress) {
        List<DepositAccount> deposits = new ArrayList<>();
        return deposits;
    }


}