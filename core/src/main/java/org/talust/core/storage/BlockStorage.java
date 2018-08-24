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

import org.talust.common.crypto.Sha256Hash;
import org.talust.common.crypto.Utils;
import org.talust.common.exception.VerificationException;
import org.talust.common.model.Coin;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.Configure;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import org.talust.common.tools.RandomUtil;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.core.model.Block;
import org.talust.core.model.BlockHeader;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.script.Script;
import org.talust.core.transaction.Output;
import org.talust.core.transaction.Transaction;
import org.talust.core.transaction.TransactionInput;
import org.talust.core.transaction.TransactionOutput;
import org.talust.storage.BaseStoreProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.talust.core.filter.BloomFilter;

//区块存储
@Slf4j
public class BlockStorage extends BaseStoreProvider {

    private final static Lock blockLock = new ReentrantLock();
    private static BlockStorage instance = new BlockStorage();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private ChainStateStorage chainStateStorage = ChainStateStorage.get();

    private AccountStorage accountStorage = AccountStorage.get();

    private BlockStorage() {
        this(Configure.DATA_BLOCK);
    }

    //最新区块hash缓存
    private byte[] bestHashCacher = null;
    //账户过滤器，用于判断交易是否与我有关
    private BloomFilter accountFilter = new BloomFilter(100000, 0.0001, RandomUtil.randomLong());
    ;

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

    /**
     * 保存区块完整的区块信息
     *
     * @param blockStore
     * @throws IOException
     */
    public void saveBlock(BlockStore blockStore) throws IOException, VerificationException {
        blockLock.lock();
        try {
            //最新的区块
            BlockHeaderStore bestBlockHeader = getBestBlockHeader();
            //判断当前要保存的区块，是否是在最新区块之后
            //保存创始块则不限制
            Block block = blockStore.getBlock();
            Sha256Hash hash = block.getHash();

            Sha256Hash preHash = block.getPreHash();
            if (preHash == null) {
                throw new VerificationException("要保存的区块缺少上一区块的引用");
            } else if (bestBlockHeader == null && Arrays.equals(bestBlockKey, preHash.getBytes()) && block.getHeight() == 0L) {
                //创世块则通过
            } else if (bestBlockHeader != null && bestBlockHeader.getBlockHeader().getHash().equals(preHash) &&
                    bestBlockHeader.getBlockHeader().getHeight() + 1 == block.getHeight()) {
                //要保存的块和最新块能连接上，通过
            } else {
                throw new VerificationException("错误的区块，保存失败");
            }

            if (blockStore.getNextHash() == null) {
                blockStore.setNextHash(Sha256Hash.ZERO_HASH);
            }

            network.setBestHeight(blockStore.getBlock().getHeight());
            //先保存交易，再保存区块，保证区块体不出错
            //保存交易
            for (int i = 0; i < block.getTxCount(); i++) {
                Transaction tx = block.getTxs().get(i);
                log.info("区块包含的交易类型为："+tx.getType());
                TransactionStore txs = new TransactionStore(network, tx, block.getHeight(), null);
                db.put(tx.getHash().getBytes(), txs.baseSerialize());
                saveChainstate(block, txs);
            }

            //保存块头
            byte[] blockHeaderBytes = blockStore.serializeHeaderToBytes();
            db.put(hash.getBytes(), blockHeaderBytes);


            byte[] heightBytes = new byte[4];
            Utils.uint32ToByteArrayBE(block.getHeight(), heightBytes, 0);

            db.put(heightBytes, hash.getBytes());

            //更新最新区块
            db.put(bestBlockKey, hash.getBytes());
            bestHashCacher = hash.getBytes();

            //更新上一区块的指针
            if (!Sha256Hash.ZERO_HASH.equals(block.getPreHash())) {
                BlockHeaderStore preBlockHeader = getHeader(block.getPreHash().getBytes());
                preBlockHeader.setNextHash(block.getHash());
                db.put(preBlockHeader.getBlockHeader().getHash().getBytes(), preBlockHeader.baseSerialize());
            }

        } catch (Exception e) {
            log.info("保存区块出错：", e);
        } finally {
            blockLock.unlock();
        }
    }


    public BlockHeaderStore getBestBlockHeader() {
        blockLock.lock();
        byte[] bestBlockHash = null;
        try {
            if (bestHashCacher == null) {
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
        if (bestBlockHash == null) {
            return null;
        } else {
            return getHeader(bestBlockHash);
        }
    }

    private void saveChainstate(Block block, TransactionStore txs) throws IOException {
        Transaction tx = txs.getTransaction();
        //TODO 下面的代码请使用状态模式重构
        if (tx.isPaymentTransaction()) {
            //转账交易
            //coinbase交易没有输入
            if (tx.getType() != Definition.TYPE_COINBASE) {
                List<TransactionInput> inputs = tx.getInputs();
                for (TransactionInput input : inputs) {
                    if (input.getFroms() == null || input.getFroms().size() == 0) {
                        continue;
                    }
                    for (TransactionOutput from : input.getFroms()) {
                        //对上一交易的引用以及索引值
                        Sha256Hash fromId = from.getParent().getHash();
                        int index = from.getIndex();

                        byte[] key = new byte[fromId.getBytes().length + 1];

                        System.arraycopy(fromId.getBytes(), 0, key, 0, key.length - 1);
                        key[key.length - 1] = (byte) index;

                        chainStateStorage.put(key, new byte[]{TransactionStore.STATUS_USED});
                    }
                }
            }
            //添加输出
            List<TransactionOutput> outputs = tx.getOutputs();
            for (TransactionOutput output : outputs) {
                Sha256Hash id = tx.getHash();
                int index = output.getIndex();

                byte[] key = new byte[id.getBytes().length + 1];

                System.arraycopy(id.getBytes(), 0, key, 0, key.length - 1);
                key[key.length - 1] = (byte) index;

                chainStateStorage.put(key, new byte[]{TransactionStore.STATUS_UNUSE});
            }

        }
        checkIsMineAndUpdate(txs);
    }

    /**
     * 检查交易是否与我有关，并且更新状态
     *
     * @param txs
     */
    public void checkIsMineAndUpdate(final TransactionStore txs) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Transaction transaction = txs.getTransaction();

                boolean isMine = checkTxIsMine(transaction);
                if (isMine) {
                    updateMineTx(txs);
                }
            }
        });
    }

    public boolean checkTxIsMine(Transaction tx) {
        return checkTxIsMine(tx, null);
    }

    /**
     * 检查交易是否跟我有关
     *
     * @param transaction
     * @return boolean
     */
    public boolean checkTxIsMine(Transaction transaction, byte[] hash160) {
        //是否是跟自己有关的交易
        if (transaction.isPaymentTransaction()) {
            //普通交易
            //输入
            List<TransactionInput> inputs = transaction.getInputs();
            if (inputs != null && inputs.size() > 0) {
                for (TransactionInput input : inputs) {
                    if (input.getFroms() == null || input.getFroms().size() == 0) {
                        continue;
                    }
                    for (TransactionOutput from : input.getFroms()) {
                        //对上一交易的引用以及索引值
                        Sha256Hash fromId = from.getParent().getHash();
                        int index = from.getIndex();

                        TransactionStore txStore = getTransaction(fromId.getBytes());
                        if (txStore == null) {
                            return false;
                        }
                        from = (TransactionOutput) txStore.getTransaction().getOutput(index);

                        Script script = from.getScript();
                        if (hash160 == null && script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data) ||
                                hash160 != null && script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
                            return true;
                        }
                    }
                }
            }
            //输出
            List<TransactionOutput> outputs = transaction.getOutputs();
            for (TransactionOutput output : outputs) {
                Script script = output.getScript();
                if (hash160 == null && script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data) ||
                        hash160 != null && script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
                    if (transaction.getType() == Definition.TYPE_COINBASE) {
                        return true;
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //更新与自己相关的交易
    public void updateMineTx(TransactionStore txs) {
        accountStorage.loadBalanceFromChainstateAndUnconfirmedTransaction(accountStorage.getAccountHash160s());
        TransactionStorage.get().processNewTransaction(txs);
    }


    /**
     * 重新加载相关的所有交易，意味着会遍历整个区块
     * 该操作一遍只会在账号导入之后进行操作
     *
     * @param hash160s
     * @return List<TransactionStore>  返回交易列表
     */
    public List<TransactionStore> loadRelatedTransactions(List<byte[]> hash160s) {
        blockLock.lock();
        try {
            accountFilter.init();
            for (byte[] hash160 : hash160s) {
                accountFilter.insert(hash160);
            }

            //从创始快开始遍历所有区块
            BlockStore blockStore = network.getGengsisBlock();
            Sha256Hash nextHash = blockStore.getBlock().getHash();

            List<TransactionStore> mineTxs = new ArrayList<TransactionStore>();
            while (!nextHash.equals(Sha256Hash.ZERO_HASH)) {
                BlockStore nextBlockStore = getBlock(nextHash.getBytes());

                Block block = nextBlockStore.getBlock();

                List<Transaction> txs = block.getTxs();

                for (Transaction tx : txs) {

                    //普通交易
                    if (tx.isPaymentTransaction()) {
                        //获取转入交易转入的多少钱
                        List<TransactionOutput> outputs = tx.getOutputs();

                        if (outputs == null) {
                            continue;
                        }
                        //过滤掉coinbase里的0交易
                        if (tx.getType() == Definition.TYPE_COINBASE && outputs.get(0).getValue() == 0l) {
                            continue;
                        }

                        //交易状态
                        byte[] status = new byte[outputs.size()];
                        //交易是否跟我有关
                        boolean isMineTx = false;

                        for (int i = 0; i < outputs.size(); i++) {
                            Output output = outputs.get(i);
                            Script script = output.getScript();

                            if (script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data)) {
                                status[i] = TransactionStore.STATUS_UNUSE;
                                isMineTx = true;
                                break;
                            }
                        }
                        List<TransactionInput> inputs = tx.getInputs();
                        if (inputs != null && inputs.size() > 0) {
                            for (TransactionInput input : inputs) {
                                if (input.getFroms() == null || input.getFroms().size() == 0) {
                                    continue;
                                }
                                for (TransactionOutput from : input.getFroms()) {
                                    Sha256Hash fromTxHash = from.getParent().getHash();

                                    for (TransactionStore transactionStore : mineTxs) {
                                        Transaction mineTx = transactionStore.getTransaction();
                                        if (mineTx.getHash().equals(fromTxHash)) {
                                            //对上一交易的引用以及索引值
                                            TransactionOutput output = (TransactionOutput) mineTx.getOutput(from.getIndex());
                                            Script script = output.getScript();
                                            if (script.isSentToAddress() && accountFilter.contains(script.getChunks().get(2).data)) {
                                                transactionStore.getStatus()[from.getIndex()] = TransactionStore.STATUS_USED;
                                                isMineTx = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        //除单纯的转账交易外，还有可能有业务逻辑附带代币交易的
                        if (!isMineTx && tx.getType() != Definition.TYPE_PAY &&
                                tx.getType() != Definition.TYPE_COINBASE) {
                            isMineTx = checkTxIsMine(tx);
                        }

                        if (isMineTx) {
                            mineTxs.add(new TransactionStore(network, tx, block.getHeight(), status));
                        }
                    } else {
                        boolean isMine = checkTxIsMine(tx);
                        if (isMine) {
                            mineTxs.add(new TransactionStore(network, tx, block.getHeight(), new byte[]{}));
                        }
                    }
                }
                nextHash = nextBlockStore.getNextHash();
            }

            return mineTxs;
        } finally {
            blockLock.unlock();
        }
    }

    /**
     * 获取区块
     *
     * @param height
     * @return BlockStore
     */
    public BlockStore getBlockByHeight(long height) {
        return getBlockByHeader(getHeaderByHeight(height));
    }


    /**
     * 获取区块头信息
     *
     * @param height
     * @return BlockHeaderStore
     */
    public BlockHeaderStore getHeaderByHeight(long height) {
        byte[] heightBytes = new byte[4];
        Utils.uint32ToByteArrayBE(height, heightBytes, 0);

        byte[] hash = null;
        try {
            hash = db.get(heightBytes);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        if (hash == null) {
            return null;
        }
        return getHeader(hash);
    }

    /**
     * 获取完整的区块信息
     *
     * @param hash
     * @return BlockStore
     */

    public BlockStore getBlock(byte[] hash) {
        BlockHeaderStore header = getHeader(hash);
        if (header == null) {
            return null;
        }
        return getBlockByHeader(header);
    }

    /**
     * 通过区块头获取区块的完整信息，主要是把交易详情查询出来
     *
     * @param header
     * @return BlockStore
     */
    public BlockStore getBlockByHeader(BlockHeaderStore header) {
        //交易列表
        List<Transaction> txs = new ArrayList<Transaction>();

        BlockHeader blockHeader = header.getBlockHeader();
        if (blockHeader.getTxHashs() != null) {
            for (Sha256Hash txHash : header.getBlockHeader().getTxHashs()) {
                TransactionStore tx = getTransaction(txHash.getBytes());
                if (tx == null) {
                    log.error("Block height {} , tx {} not found", header.getBlockHeader().getHeight(), txHash);
                    continue;
                }
                txs.add(tx.getTransaction());
            }
        }

        BlockStore blockStore = new BlockStore(network);

        Block block = new Block(network);
        block.setTxs(txs);
        block.setVersion(header.getBlockHeader().getVersion());
        block.setHash(header.getBlockHeader().getHash());
        block.setHeight(header.getBlockHeader().getHeight());
        block.setMerkleHash(header.getBlockHeader().getMerkleHash());
        block.setPreHash(header.getBlockHeader().getPreHash());
        block.setTime(header.getBlockHeader().getTime());
        block.setTxCount(header.getBlockHeader().getTxCount());
        block.setScriptBytes(header.getBlockHeader().getScriptBytes());

        blockStore.setBlock(block);
        blockStore.setNextHash(header.getNextHash());

        return blockStore;
    }


    /**
     * 获取一笔交易
     *
     * @param hash
     * @return TransactionStore
     */
    public TransactionStore getTransaction(byte[] hash) {
        byte[] content = new byte[0];
        try {
            content = db.get(hash);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        if (content == null) {
            return null;
        }
        TransactionStore store = new TransactionStore(network, content);
        store.setKey(hash);

        return store;
    }

    /**
     * 获取区块头信息
     *
     * @param hash
     * @return BlockHeaderStore
     */
    public BlockHeaderStore getHeader(byte[] hash) {

        byte[] content = null;
        content = CacheManager.get().get(hash.toString());
        if (content == null) {
            try {
                content = db.get(hash);
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
        }

        if (content == null) {
            return null;
        }
        BlockHeaderStore blockHeaderStore = new BlockHeaderStore(network, content);
        blockHeaderStore.getBlockHeader().setHash(Sha256Hash.wrap(hash));
        return blockHeaderStore;
    }
}
