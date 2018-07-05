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

package org.talust.chain.storage;

import org.talust.chain.common.tools.Configure;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.util.SizeUnit;

import java.io.File;

//交易存储
@Slf4j
public class TransactionStorage {
    private static TransactionStorage instance = new TransactionStorage();

    private TransactionStorage() {
    }

    public static TransactionStorage get() {
        return instance;
    }

    private RocksDB db;
    final Options options = new Options()
            .setCreateIfMissing(true)
            .setWriteBufferSize(8 * SizeUnit.KB)
            .setMaxWriteBufferNumber(3)
            .setMaxBackgroundCompactions(10);
    //.setCompressionType(CompressionType.SNAPPY_COMPRESSION)
    //.setCompactionStyle(CompactionStyle.UNIVERSAL);

    static {
        try {
            RocksDB.loadLibrary();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init() {
        try {
            String dataBlock = Configure.DATA_TRANSACTION;
            File file = new File(dataBlock);
            if (!file.exists()) {
                file.mkdirs();
            }
            log.info("保存交易数据路径为:{}", dataBlock);
            db = RocksDB.open(options, dataBlock);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

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

}
