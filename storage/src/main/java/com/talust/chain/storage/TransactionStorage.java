package com.talust.chain.storage;

import com.talust.chain.common.tools.Configure;
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
