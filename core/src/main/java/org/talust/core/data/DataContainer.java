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
import org.talust.common.crypto.Utils;
import org.talust.core.transaction.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//数据容器,节点接收到的数据,先对接收到的数据进行校验,然后
public class DataContainer {
    private static DataContainer instance = new DataContainer();

    private DataContainer() {
    }

    public static DataContainer get() {
        return instance;
    }

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private List<Transaction> datas = new ArrayList<>();
    private int max_record_count = 1000;//一次最多能够打包的记录条数

    /**
     * 新增一条记录
     *
     * @param record
     */
    public void addRecord(Transaction record) {
        try {
            lock.readLock().lock();
            datas.add(record);
        } finally {
            lock.readLock().unlock();
        }
    }


    /**
     * 删除一条记录
     *
     * @param record
     */
    public void removeRecord(Transaction record) {
        try {
            lock.writeLock().lock();
            for (int idx = 0; idx < datas.size(); idx++) {
                Transaction data = datas.get(idx);
                if (data.equals(record)) {
                    datas.remove(idx);
                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取一次打包的数据集合
     *
     * @return
     */
    public List<Transaction> getBatchRecord() {
        try {
            lock.writeLock().lock();
            if (datas.size() < max_record_count) {
                List<Transaction> tmp = datas;
                datas = new ArrayList<>();
                return tmp;
            } else {
                List<Transaction> sub = new ArrayList<>(max_record_count);
                for (int idx = 0; idx < max_record_count; idx++) {
                    sub.add(datas.get(idx));
                }
                List<Transaction> tmp = new ArrayList<>();
                int size = datas.size();
                for (int idx = max_record_count; idx < size; idx++) {
                    tmp.add(datas.get(idx));
                }
                datas.clear();
                datas = tmp;
                return sub;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


}
