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

import org.rocksdb.RocksIterator;
import org.talust.common.crypto.Hex;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.model.Coin;
import org.talust.common.tools.Configure;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.script.Script;
import org.talust.core.server.NtpTimeService;
import org.talust.core.transaction.BaseCommonlyTransaction;
import org.talust.core.transaction.Transaction;
import org.talust.core.transaction.TransactionInput;
import org.talust.core.transaction.TransactionOutput;
import org.talust.storage.BaseStoreProvider;

import java.util.*;
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

    private BlockStorage blockStorage = BlockStorage.get();

    public TransactionStorage(String dir) {
       super(dir);
    }


    //存放交易记录账号的key
    private final static byte[] ADDRESSES_KEY = Sha256Hash.ZERO_HASH.getBytes();
    //交易记录对应的账号列表
    private  List<byte[]> addresses = new CopyOnWriteArrayList<byte[]>();
    //我的交易列表
    private List<TransactionStore> myTxList = new CopyOnWriteArrayList<TransactionStore>();
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
     * 获取地址的最新余额和未确认的余额
     * @param hash160
     * @return Coin[]
     */
    public Coin[] getBalanceAndUnconfirmedBalance(byte[] hash160) {
        Coin balance = Coin.ZERO;
        Coin unconfirmedBalance = Coin.ZERO;

        //查询当前区块最新高度
        long bestBlockHeight = network.getBestHeight();
        long localBestBlockHeight = network.getBestBlockHeight();

        if(bestBlockHeight < localBestBlockHeight) {
            bestBlockHeight = localBestBlockHeight;
        }

        for (TransactionStore transactionStore : unspendTxList) {
            //获取转入交易转入的多少钱
            Transaction tx = transactionStore.getTransaction();
            if(!tx.isPaymentTransaction()) {
                continue;
            }

            //如果交易不可用，则标记
            boolean txAvailable = true;
            if(tx.getLockTime() < 0l
                    || (tx.getLockTime() > Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > NtpTimeService.currentTimeSeconds())
                    || (tx.getLockTime() < Definition.LOCKTIME_THRESHOLD && tx.getLockTime() > bestBlockHeight)) {
                txAvailable = false;
            }

            byte[] key = tx.getHash().getBytes();
            byte[] status = transactionStore.getStatus();

            List<TransactionOutput> outputs = tx.getOutputs();

            for (int i = 0; i < outputs.size(); i++) {
                TransactionOutput output = outputs.get(i);
                Script script = output.getScript();
                if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {

                    byte[] statueKey = new byte[key.length + 1];
                    System.arraycopy(key, 0, statueKey, 0, key.length);
                    statueKey[statueKey.length - 1] = (byte) i;

                    //交易是否已花费
                    if(status != null && status.length > 0 && status[i] == TransactionStore.STATUS_USED) {
                        continue;
                    }
                    //本笔输出是否可用
                    long lockTime = output.getLockTime();
                    if(!txAvailable || lockTime < 0l
                            || (lockTime >= Definition.LOCKTIME_THRESHOLD && lockTime > NtpTimeService.currentTimeSeconds())
                            || (lockTime < Definition.LOCKTIME_THRESHOLD && lockTime > bestBlockHeight)
                            || (i == 0 && transactionStore.getHeight() == -1l)) {
                        unconfirmedBalance = unconfirmedBalance.add(Coin.valueOf(output.getValue()));
                    } else {
                        balance = balance.add(Coin.valueOf(output.getValue()));
                    }
                }
            }
        }
        return new Coin[]{balance, unconfirmedBalance};
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

    /**
     * 处理新交易
     * @param txs
     */
    public void processNewTransaction(TransactionStore txs) {
        boolean hasUpdate = false;
        //交易是否已经存在
        for (TransactionStore transactionStore : myTxList) {
            //如果存在，则更新高度
            if(transactionStore.getTransaction().getHash().equals(txs.getTransaction().getHash())) {
                transactionStore.setHeight(txs.getHeight());
                txs = transactionStore;
                hasUpdate = true;
                //保存
                put(txs.getTransaction().getHash().getBytes(), txs.baseSerialize());
                break;
            }
        }
        Transaction tx = txs.getTransaction();

        if(!hasUpdate) {
            //如果不存在，则新增
            myTxList.add(txs);

            if(tx.isPaymentTransaction()) {
                //更新交易状态
                List<TransactionOutput> outputs = tx.getOutputs();

                List<TransactionInput> inputs = tx.getInputs();
                if(inputs != null) {
                    for (TransactionInput input : inputs) {
                        if(input.getFroms() == null || input.getFroms().size() == 0) {
                            continue;
                        }
                        for (TransactionOutput from : input.getFroms()) {
                            Sha256Hash fromTxHash = from.getParent().getHash();

                            for (TransactionStore unspendTx : unspendTxList) {
                                if(unspendTx.getTransaction().getHash().equals(fromTxHash)) {
                                    //更新内存
                                    byte[] ftxStatus = unspendTx.getStatus();
                                    ftxStatus[from.getIndex()] = TransactionStore.STATUS_USED;
                                    unspendTx.setStatus(ftxStatus);

                                    //更新存储
                                    put(unspendTx.getTransaction().getHash().getBytes(), unspendTx.baseSerialize());

                                    //查询该笔交易是否还有我没有花费的交易
                                    List<TransactionOutput> outputsTemp = unspendTx.getTransaction().getOutputs();
                                    boolean hasUnspend = false;
                                    for (TransactionOutput transactionOutput : outputsTemp) {
                                        Script script = transactionOutput.getScript();
                                        for (byte[] hash160 : addresses) {
                                            if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)
                                                    && ftxStatus[transactionOutput.getIndex()] == TransactionStore.STATUS_UNUSE) {
                                                hasUnspend = true;
                                                break;
                                            }
                                        }
                                    }
                                    if(!hasUnspend) {
                                        unspendTxList.remove(unspendTx);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
                //交易状态
                byte[] status = new byte[outputs.size()];

                for (int i = 0; i < outputs.size(); i++) {
                    TransactionOutput output = outputs.get(i);
                    Script script = output.getScript();
                    status[i] = TransactionStore.STATUS_UNUSE;

                    for (byte[] hash160 : addresses) {
                        if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)
                                && !unspendTxList.contains(txs)) {
                            unspendTxList.add(txs);
                            break;
                        }
                    }
                }
                //设置交易存储状态
                txs.setStatus(status);
            }
            //保存
            put(txs.getTransaction().getHash().getBytes(), txs.baseSerialize());
        }
    }

    /**
     * 初始化
     */
    public void init() {
        //本地交易记录对应的账号列表
        byte[] list = getBytes(ADDRESSES_KEY);
        if(list != null) {
            for (int i = 0; i < list.length; i+= Address.LENGTH) {
                byte[] hash160 = new byte[Address.LENGTH];
                System.arraycopy(list, i, hash160, 0, Address.LENGTH);
                addresses.add(hash160);
            }
        }

        //交易记录
        RocksIterator iter = db.newIterator();
        for(iter.seekToFirst(); iter.isValid(); iter.next()) {
            byte[] key = iter.key();
            if(Arrays.equals(ADDRESSES_KEY, key)) {
                continue;
            }
            byte[] value = iter.value();
            TransactionStore txs = new TransactionStore(network, value);
            myTxList.add(txs);
            //是否未花费的交易
            Transaction tx = txs.getTransaction();
            if(tx.isPaymentTransaction()) {
                byte[] status = txs.getStatus();
                List<TransactionOutput> outputs = tx.getOutputs();
                for (int i = 0; i < outputs.size(); i++) {
                    TransactionOutput output = outputs.get(i);
                    Script script = output.getScript();
                    if(status == null || status.length < i || status[i] != TransactionStore.STATUS_UNUSE || unspendTxList.contains(txs)) {
                        continue;
                    }
                    for (byte[] hash160 : addresses) {
                        if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
                            unspendTxList.add(txs);
                        }
                    }
                }
            }
        }
    }

    public boolean reloadTransaction(List<byte[]> hash160s) {

        clean();

        //写入新列表
        byte[] addressesBytes = new byte[hash160s.size() * Address.LENGTH];
        for (int i = 0; i < hash160s.size(); i++) {
            System.arraycopy(hash160s.get(i), 0, addressesBytes, i * Address.LENGTH, Address.LENGTH);
        }
        put(ADDRESSES_KEY, addressesBytes);

        this.addresses = hash160s;

        //遍历区块写入相关交易
        myTxList = blockStorage.loadRelatedTransactions(hash160s);
        unspendTxList = new CopyOnWriteArrayList<>();

        for (TransactionStore txs : myTxList) {
            put(txs.getTransaction().getHash().getBytes(), txs.baseSerialize());

            //是否未花费的交易
            Transaction tx = txs.getTransaction();
            if(tx.isPaymentTransaction()) {
                byte[] status = txs.getStatus();
                List<TransactionOutput> outputs = tx.getOutputs();
                for (int i = 0; i < outputs.size(); i++) {
                    TransactionOutput output = outputs.get(i);
                    Script script = output.getScript();
                    if(status == null || status.length < i || status[i] != TransactionStore.STATUS_UNUSE || unspendTxList.contains(txs)) {
                        continue;
                    }
                    for (byte[] hash160 : this.addresses) {
                        if(script.isSentToAddress() && Arrays.equals(script.getChunks().get(2).data, hash160)) {
                            unspendTxList.add(txs);
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * 清理数据
     */
    public void clean() {
        //清除老数据
        RocksIterator   iter = db.newIterator();
        for(iter.seekToFirst(); iter.isValid(); iter.next()) {
            byte[] key = iter.key();
            delete(key);
        }
        //写入新列表
        byte[] addressesBytes = new byte[addresses.size() * Address.LENGTH];
        for (int i = 0; i < addresses.size(); i++) {
            System.arraycopy(addresses.get(i), 0, addressesBytes, i * Address.LENGTH, Address.LENGTH);
        }
        put(ADDRESSES_KEY, addressesBytes);
    }

    public boolean addAddress(byte[] hash160) {
        addresses.add(hash160);
        //写入新列表
        byte[] addressesByte = new byte[addresses.size() * Address.LENGTH];
        for (int i = 0; i < addresses.size(); i++) {
            System.arraycopy(addresses.get(i), 0, addressesByte, i * Address.LENGTH, Address.LENGTH);
        }
        put(ADDRESSES_KEY, addressesByte);
        return true;
    }

    public boolean addAddress(List<Account> newAccoountList){
        for(int i=0 ; i<newAccoountList.size(); i++){
            addresses.add(newAccoountList.get(i).getAddress().getHash160());
        }
        //写入新列表
        byte[] addressesByte = new byte[addresses.size() * Address.LENGTH];
        for (int i = 0; i < addresses.size(); i++) {
            System.arraycopy(addresses.get(i), 0, addressesByte, i * Address.LENGTH, Address.LENGTH);
        }
        put(ADDRESSES_KEY, addressesByte);
        return true;
    }

    /**
     * 通过交易ID查询交易
     * @param hash
     * @return TransactionStore
     */
    public TransactionStore getTransaction(Sha256Hash hash) {
        return blockStorage.getTransaction(hash.getBytes());
    }



    public List<byte[]> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<byte[]> addresses) {
        this.addresses = addresses;
    }
}
