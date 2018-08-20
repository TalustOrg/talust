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

import org.talust.common.crypto.Hex;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.tools.Configure;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;
import org.talust.core.model.Address;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.script.Script;
import org.talust.core.server.NtpTimeService;
import org.talust.core.transaction.Transaction;
import org.talust.core.transaction.TransactionOutput;
import org.talust.storage.BaseStoreProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//交易存储，存储于自身账户有关的交易
@Slf4j
public class TransactionStorage extends BaseStoreProvider {
    private static TransactionStorage instance = new TransactionStorage();

    private TransactionStorage() {
        this(Configure.DATA_TRANSACTION);
    }

    public static TransactionStorage get() {
        return instance;
    }

    private NetworkParams network  = MainNetworkParams.get();

    public TransactionStorage(String dir) {
        super(dir);
    }

    //存放交易记录账号的key
    private final static byte[] ADDRESSES_KEY = Sha256Hash.ZERO_HASH.getBytes();
    //交易记录对应的账号列表
    private List<byte[]> addresses = new CopyOnWriteArrayList<byte[]>();
    //我的交易列表
    private List<TransactionOutput> myTxList = new CopyOnWriteArrayList<TransactionOutput>();
    //未花费的交易
    private List<TransactionStore> unspendTxList = new CopyOnWriteArrayList<TransactionStore>();


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
    public List<TransactionOutput> getNotSpentTransactionOutputs(byte[] hash160) {

        List<TransactionOutput> txs = new ArrayList<TransactionOutput>();

        //查询当前区块最新高度
        long bestBlockHeight = network.getBestHeight();
        long localBestBlockHeight = network.getBestBlockHeight();

        if(bestBlockHeight < localBestBlockHeight) {
            bestBlockHeight = localBestBlockHeight;
        }

        for (TransactionStore transactionStore : unspendTxList) {

            //交易状态
            byte[] status = transactionStore.getStatus();

            Transaction tx = transactionStore.getTransaction();

            //如果不是转账交易，则跳过
            if(!tx.isPaymentTransaction()) {
                continue;
            }

            //如果交易不可用，则跳过
            if(tx.getLockTime() < 0l
                    || (tx.getLockTime() > Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > NtpTimeService.currentTimeSeconds())
                    || (tx.getLockTime() < Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > bestBlockHeight)) {
                continue;
            }

            List<TransactionOutput> outputs = tx.getOutputs();

            //遍历交易输出
            for (int i = 0; i < outputs.size(); i++) {
                TransactionOutput output = outputs.get(i);
                Script script = output.getScript();
                if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {

                    //交易是否已花费
                    if(status[i] == TransactionStore.STATUS_USED) {
                        continue;
                    }
//
//					//链上状态是否可用
//					byte[] statueKey = new byte[key.length + 1];
//					System.arraycopy(key, 0, statueKey, 0, key.length);
//					statueKey[statueKey.length - 1] = (byte) i;
//
//					byte[] content = chainstateStoreProvider.getBytes(statueKey);
//					if(content == null) {
//						continue;
//					}

                    //本笔输出是否可用
                    long lockTime = output.getLockTime();
                    if(lockTime < 0l
                            || (lockTime > Definition.LOCKTIME_THRESHOLD && lockTime > NtpTimeService.currentTimeSeconds())
                            || (lockTime < Definition.LOCKTIME_THRESHOLD && lockTime > bestBlockHeight) ) {
                        continue;
                    } else {
                        txs.add((TransactionOutput) output);
                    }
                }
            }
        }
        return txs;
    }

    /**
     * 获取制定地址集合所有未花费的交易输出
     * @return List<TransactionOutput>
     */
    public HashMap<String,List<TransactionOutput>> getNotSpentTransactionOutputs(List<byte[]> hash160s) {

        HashMap<String,List<TransactionOutput>> txs = new HashMap<String,List<TransactionOutput>>();

        //查询当前区块最新高度
        long bestBlockHeight = network.getBestHeight();
        long localBestBlockHeight =  network.getBestBlockHeight();

        if(bestBlockHeight < localBestBlockHeight) {
            bestBlockHeight = localBestBlockHeight;
        }


        for(int j=0;j<hash160s.size();j++){
            byte[] hash160 = hash160s.get(j);
            //log.info("find user"+ Hex.encode(hash160));
            ArrayList<TransactionOutput> unSpentOutputs= new ArrayList<TransactionOutput>();
            for (TransactionStore transactionStore : unspendTxList){
                //交易状态

                byte[] status = transactionStore.getStatus();
                Transaction tx = transactionStore.getTransaction();
                List<TransactionOutput> outputs = tx.getOutputs();
                //如果不是转账交易，则跳过
                if(!tx.isPaymentTransaction()) {
                    continue;
                }

                //如果交易不可用，则跳过
                if(tx.getLockTime() < 0l
                        || (tx.getLockTime() > Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > NtpTimeService.currentTimeSeconds())
                        || (tx.getLockTime() < Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > bestBlockHeight)) {
                    continue;
                }

                //遍历交易输出
                for (int i = 0; i < outputs.size(); i++) {
                    TransactionOutput output = outputs.get(i);
                    Script script = output.getScript();
                    if (script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
                        log.info("find output"+ Hex.encode(script.getChunks().get(2).data));
                        //交易是否已花费
                        if (status[i] == TransactionStore.STATUS_USED) {
                            continue;
                        }
                        //本笔输出是否可用
                        long lockTime = output.getLockTime();
                        if (lockTime < 0l
                                || (lockTime > Definition.LOCKTIME_THRESHOLD && lockTime > NtpTimeService.currentTimeSeconds())
                                || (lockTime < Definition.LOCKTIME_THRESHOLD && lockTime > bestBlockHeight)) {
                            continue;
                        } else {
                            unSpentOutputs.add((TransactionOutput) output);
                        }
                    }
                }
            }
            txs.put(new Address(network,hash160).getBase58() ,unSpentOutputs);
        }
        return txs;
    }


//    //初始化所有交易中对应本地已经存在的地址的相关交易
//    public void init() {
//       List<Account> accounts =  AccountStorage.get().getAccounts();
//        RocksIterator iter = db.newIterator();
//        for(iter.seekToFirst(); iter.isValid(); iter.next()) {
//            System.out.println("iter key:" + new String(iter.key()) + ", iter value:" + new String(iter.value()));
//            byte[] key = iter.key();
//            if(Arrays.equals(ADDRESSES_KEY, key)) {
//                continue;
//            }
//            byte[] value = iter.value();
//            TransactionOut out =  SerializationUtil.deserializer(value,TransactionOut.class);
//            mineTxList.add(out);
//        }
//    }


}
