package com.talust.chain.storage;

import com.talust.chain.common.model.DepositAccount;
import com.talust.chain.common.model.SuperNode;
import com.talust.chain.common.tools.Configure;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.util.SizeUnit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

//交易帐户余额存储,用于存储每一个帐户的每一个交易过来的余额,针对UTXO模型实现的
@Slf4j
public class ChainStateStorage {
    private static ChainStateStorage instance = new ChainStateStorage();

    private ChainStateStorage() {
    }

    public static ChainStateStorage get() {
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
            String dataBlock = Configure.DATA_CHAINSTATE;
            File file = new File(dataBlock);
            if (!file.exists()) {
                file.mkdirs();
            }
            log.info("保存区块链状态数据路径为:{}", dataBlock);
            db = RocksDB.open(options, dataBlock);
            //初始化交易号,便于打包交易时使用
            initTranNumber();
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

    private byte[] TRAN_NUMBER = "tranNumber".getBytes();
    private AtomicLong tranNumber;

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
