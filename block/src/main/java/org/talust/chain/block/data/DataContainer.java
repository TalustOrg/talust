package org.talust.chain.block.data;

import org.talust.chain.common.crypto.Utils;

import java.util.ArrayList;
import java.util.List;
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

    private List<byte[]> datas = new ArrayList<>();
    private int max_record_count = 1000;//一次最多能够打包的记录条数

    /**
     * 新增一条记录
     *
     * @param record
     */
    public void addRecord(byte[] record) {
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
    public void removeRecord(byte[] record) {
        try {
            lock.writeLock().lock();
            for (int idx = 0; idx < datas.size(); idx++) {
                byte[] data = datas.get(idx);
                if (Utils.equals(data, record)) {
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
    public List<byte[]> getBatchRecord() {
        try {
            lock.writeLock().lock();
            if (datas.size() < max_record_count) {
                List<byte[]> tmp = datas;
                datas = new ArrayList<>();
                return tmp;
            } else {
                List<byte[]> sub = new ArrayList<>(max_record_count);
                for (int idx = 0; idx < max_record_count; idx++) {
                    sub.add(datas.get(idx));
                }
                List<byte[]> tmp = new ArrayList<>();
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
