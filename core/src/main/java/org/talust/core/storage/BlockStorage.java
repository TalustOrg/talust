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

import org.springframework.beans.factory.annotation.Autowired;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.Configure;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import org.rocksdb.util.SizeUnit;
import org.talust.core.core.ByteHash;
import org.talust.core.core.NetworkParams;
import org.talust.core.model.BlockHeader;
import org.talust.core.network.MainNetworkParams;
import org.talust.storage.BaseStoreProvider;

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//区块存储
@Slf4j
public class BlockStorage extends BaseStoreProvider {

    private final static Lock blockLock = new ReentrantLock();
    private static BlockStorage instance = new BlockStorage();

    private BlockStorage() {
        this(Configure.DATA_BLOCK);
    }
    //最新区块hash缓存
    private byte[] bestHashCacher = null;
    public static BlockStorage get() {
        return instance;
    }

    protected NetworkParams network = MainNetworkParams.get();

    //最新区块标识
    private final static byte[] bestBlockKey = Sha256Hash.ZERO_HASH.getBytes();

    public BlockStorage(String dir) {
        super(dir);
    }

    @Override
    public byte[] get(byte[] key) {
        try {
            return db.get(key);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    public BlockHeaderStore getBestBlockHeader() {
        blockLock.lock();
        byte[] bestBlockHash = null;
        try {
            if(bestHashCacher == null) {
                bestBlockHash = db.get(bestBlockKey);
                bestHashCacher = bestBlockHash;
            } else {
                bestBlockHash = bestHashCacher;
            }
        } catch (Exception e) {
            return null;
        } finally {
            blockLock.unlock();
        }
        if(bestBlockHash == null) {
            return null;
        } else {
            return getHeader(bestBlockHash);
        }
    }

    /**
     * 获取区块头信息
     * @param hash
     * @return BlockHeaderStore
     */
    public BlockHeaderStore getHeader(byte[] hash) {

        byte[] content = null;
        content = CacheManager.get().get(hash.toString());
        if(content == null) {
            try {
                content = db.get(hash);
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
        }

        if(content == null) {
            return null;
        }
        BlockHeaderStore blockHeaderStore = new BlockHeaderStore(network, content);
        blockHeaderStore.getBlockHeader().setHash(Sha256Hash.wrap(hash));
        return blockHeaderStore;
    }
}
