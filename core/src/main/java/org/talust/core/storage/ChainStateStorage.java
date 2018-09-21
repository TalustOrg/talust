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

import com.alibaba.fastjson.JSONObject;
import org.talust.common.crypto.*;
import org.talust.common.model.Coin;
import org.talust.common.model.Deposits;
import org.talust.common.tools.Configure;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.talust.common.model.DepositAccount;
import org.talust.common.tools.SerializationUtil;
import org.talust.core.core.Definition;
import org.talust.core.model.Address;
import org.talust.core.model.TxValidator;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.transaction.Transaction;
import org.talust.core.transaction.TransactionOutput;
import org.talust.storage.BaseStoreProvider;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    private Lock consensusLocker = new ReentrantLock();

    private final static String dpos = "deposit";


    public void put(byte[] key, byte[] value) {
        try {
            db.put(key, value);
        } catch (RocksDBException e) {
            e.printStackTrace();
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

    /**
     * 获取超级节点下的储蓄帐户,需要根据这些帐户的储蓄计算挖矿收益
     *
     * @param miningAddress
     * @return
     */
    public Deposits getDeposits(byte[] miningAddress) {
        byte[] key = getDepositSearchKey(miningAddress);
        Deposits deposits = new Deposits();
        try {
            byte[] deps = db.get(key);
            if (null != deps) {
                deposits = SerializationUtil.deserializer(deps, Deposits.class);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return deposits;
    }


    public void addDeposits(byte[] hash160, Coin coin, byte[] miningAddress, Sha256Hash txHash) {
        consensusLocker.lock();
        try {
            byte[] key = getDepositSearchKey(miningAddress);
            byte[] deps = db.get(key);
            if (null != deps) {
                Deposits deposits = SerializationUtil.deserializer(deps, Deposits.class);
                List<DepositAccount> depositAccountList = deposits.getDepositAccounts();
                Coin hadDeposCoin = Coin.ZERO;
                DepositAccount realAcccount = null;
                if (null != depositAccountList) {
                    String hashString = Base58.encode(hash160);
                    for (DepositAccount depositAccount : depositAccountList) {
                        String test = Base58.encode(depositAccount.getAddress());
                        boolean is = hashString.equals(test);
                        if (is) {
                            hadDeposCoin = depositAccount.getAmount();
                            realAcccount = depositAccount;
                            depositAccountList.remove(depositAccount);
                            break;
                        }
                    }
                } else {
                    depositAccountList = new ArrayList<>();
                }
                if (null != realAcccount) {
                    realAcccount.getTxHash().add(txHash);
                    realAcccount.setAmount(realAcccount.getAmount().add(coin));
                    depositAccountList.add(realAcccount);
                } else {
                    List<Sha256Hash> txlist = new ArrayList<>();
                    txlist.add(txHash);
                    depositAccountList.add(new DepositAccount(hash160, hadDeposCoin.add(coin), txlist));
                    deposits.setDepositAccounts(depositAccountList);
                }
                db.put(key, SerializationUtil.serializer(deposits));
            } else {
                Deposits deposits = new Deposits();
                List<DepositAccount> depositAccountList = new ArrayList<>();
                List<Sha256Hash> txlist = new ArrayList<>();
                txlist.add(txHash);
                DepositAccount depositAccount = new DepositAccount(hash160, coin, txlist);
                depositAccountList.add(depositAccount);
                deposits.setDepositAccounts(depositAccountList);
                db.put(key, SerializationUtil.serializer(deposits));
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        consensusLocker.unlock();
    }


    /**
     * 回滚过程中的共识重新加入
     *
     * @param tx
     */
    public void revokedConsensus(Transaction tx) {

        //重新加入共识账户列表中
        //验证当前交易是否已经存在于列表中
        DepositAccount depositAccount = getDepositAccountByTx(tx);
        //注册共识的交易
        if (null != depositAccount) {
            Sha256Hash txhash = tx.getInput(0).getFroms().get(0).getParent().getHash();
            TransactionStore regTxStore = BlockStorage.get().getTransaction(txhash.getBytes());
            if (regTxStore == null) {
                return;
            }
            Transaction regTx = regTxStore.getTransaction();
            addConsensus(regTx);
        }
    }


    public DepositAccount getDepositAccountByTx(Transaction tx) {
        Sha256Hash txHash = tx.getHash();
        List<TransactionOutput> outputs = tx.getOutputs();
        for (TransactionOutput output : outputs) {
            if (output.getLockTime() == Definition.LOCKTIME_THRESHOLD - 1) {
                String hash160 = Base58.encode(output.getScript().getChunks().get(2).data);
                Deposits deposits = getDeposits(tx.getInputs().get(0).getScriptSig().getChunks().get(2).data);
                for (DepositAccount depositAccount : deposits.getDepositAccounts()) {
                    if (Base58.encode(depositAccount.getAddress()).equals(hash160)) {
                        if (depositAccount.getTxHash().contains(txHash)) {
                            return depositAccount;
                        }
                    }
                }
            }
        }
        return null;
    }


    public byte[] getDepositSearchKey(byte[] miningAddress) {
        byte[] key = new byte[miningAddress.length + dpos.getBytes().length];
        System.arraycopy(miningAddress, 0, key, 0, miningAddress.length);
        System.arraycopy(dpos.getBytes(), 0, key, miningAddress.length, dpos.getBytes().length);
        return key;
    }

    public void removeDeposit(byte[] miningAddress, byte[] hash160) {
        try {
            byte[] key = getDepositSearchKey(miningAddress);
            byte[] deps = db.get(key);
            if (null != deps) {
                Deposits deposits = SerializationUtil.deserializer(deps, Deposits.class);
                List<DepositAccount> depositAccountList = deposits.getDepositAccounts();
                String hashString = Base58.encode(hash160);
                for (DepositAccount depositAccount : depositAccountList) {
                    String test = Base58.encode(depositAccount.getAddress());
                    boolean is = hashString.equals(test);
                    if (is) {
                        depositAccountList.remove(depositAccount);
                        break;
                    }
                }
                deposits.setDepositAccounts(depositAccountList);
                db.put(key, SerializationUtil.serializer(deposits));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 共识节点加入
     *
     * @param tx
     */
    public void addConsensus(Transaction tx) {
        consensusLocker.lock();
        try {
            Sha256Hash txHash = tx.getHash();
            List<TransactionOutput> outputs = tx.getOutputs();
            for (TransactionOutput output : outputs) {
                if (output.getLockTime() == Definition.LOCKTIME_THRESHOLD - 1) {
                    byte[] hash160 = output.getScript().getChunks().get(2).data;
                    long value = output.getValue();
                    addDeposits(hash160, Coin.valueOf(value), tx.getInputs().get(0).getScriptSig().getChunks().get(2).data, txHash);
                }
            }
        } catch (Exception e) {
            log.error("出错了{}", e.getMessage(), e);
        } finally {
            consensusLocker.unlock();
        }
    }

    public List<TxValidator> checkConsensus(List<Transaction> txList){
        consensusLocker.lock();
        List<TxValidator> needRemove = new ArrayList<>();
        try{
            Map<byte[],List<TxValidator>> checkList =  new HashMap<>();
            for(Transaction transaction :txList) {
                byte[] miningAddress = transaction.getInputs().get(0).getScriptSig().getChunks().get(2).data;
                List<TxValidator> txValidators = new ArrayList<>();
                if(transaction.getType()==Definition.TYPE_REG_CONSENSUS){
                    long value = 0l;
                    byte[] hash160 =null;
                    for (TransactionOutput output : transaction.getOutputs()) {
                        if (output.getLockTime() == Definition.LOCKTIME_THRESHOLD - 1) {
                             hash160 = output.getScript().getChunks().get(2).data;
                             value = output.getValue();
                        }
                    }
                    TxValidator txValidator = new TxValidator(value,hash160,miningAddress,transaction);
                    txValidators.add(txValidator);
                }
                checkList.remove(miningAddress);
                checkList.put(miningAddress,txValidators);
            }
            Set<byte[]> miningAddrs = checkList.keySet();
            for(byte[] addr : miningAddrs){
                byte[] key = getDepositSearchKey(addr);
                byte[] deps = db.get(key);
                if (null != deps) {
                    Deposits deposits = SerializationUtil.deserializer(deps, Deposits.class);
                    List<DepositAccount> depositAccountList = deposits.getDepositAccounts();
                    List<TxValidator> txValidators =checkList.get(addr);
                    for(DepositAccount depositAccount:depositAccountList){
                        TxValidator txValidator =  new TxValidator(depositAccount.getAmount().value,depositAccount.getAddress(),addr,depositAccount);
                        txValidators.add(txValidator);
                    }
                    checkList.remove(addr);
                    checkList.put(addr,txValidators);
                }
            }
            //移除账户进行迭代查询，若没有key则不处理，存在key则验证是否有相同hash与value，有的话进行移除处理
            for(Transaction transaction :txList) {
                byte[] miningAddress = transaction.getInputs().get(0).getScriptSig().getChunks().get(2).data;
                if(checkList.containsKey(miningAddress)){
                    List<TxValidator> txValidators =checkList.get(miningAddress);
                    if(transaction.getType()==Definition.TYPE_REM_CONSENSUS){
                        byte[] nodeAddress = transaction.getInputs().get(0).getScriptSig().getChunks().get(2).data;
                        TransactionStore oldTx =   TransactionStorage.get().getTransaction(transaction.getInput(0).getFroms().get(0).getParent().getHash());
                        TransactionOutput transactionOutput = oldTx.getTransaction().getOutputs().get(0);
                        byte[] hash160 = transactionOutput.getScript().getChunks().get(2).data;
                        TxValidator txValidator = new TxValidator(transaction.getOutput(0).getValue(),hash160,nodeAddress,transaction);
                        //存在key则验证是否有相同hash与value，有的话进行移除处理
                        txValidators = checkHashAndValue(txValidators,txValidator);
                    }
                    checkList.remove(miningAddress);
                    checkList.put(miningAddress,txValidators);
                }else{
                    continue;
                }
            }
            //账户hash值去重，value相加，而后排序。
            Collection<List<TxValidator>> lists = checkList.values();
            for(List<TxValidator> txValidators : lists ){
                if(txValidators.size()>100){
                    //倒序排序
                    txValidators.sort(new Comparator<TxValidator>() {
                        @Override
                        public int compare(TxValidator o1, TxValidator o2) {
                            return Long.compare(o2.getValue(),o1.getValue());
                        }
                    });
                    needRemove.addAll(txValidators.subList(99,txValidators.size()));
                }else{
                    continue;
                }
            }
        }catch (Exception e){
        }finally {
            consensusLocker.unlock();
        }
        return needRemove;
    }

    public List<TxValidator> checkHashAndValue(List<TxValidator> txValidators ,TxValidator txValidator){
        List<TxValidator> resp = txValidators;
        for(TxValidator txv:resp){
            if(txValidator.getValue()==txv.getValue()){
                String txAddr = Base58.encode(txValidator.getAddress());
                String txvAddr = Base58.encode(txv.getAddress());
                if (txvAddr.equals(txAddr)){
                    resp.remove(txv);
                    break;
                }
            }
        }
        return resp;
    }

    /**
     * 退出共识
     *
     * @param tx
     */
    public void removeConsensus(Transaction tx) throws Exception {
        byte[] nodeAddress = tx.getInputs().get(0).getScriptSig().getChunks().get(2).data;
        TransactionStore oldTx =   TransactionStorage.get().getTransaction(tx.getInput(0).getFroms().get(0).getParent().getHash());
        TransactionOutput transactionOutput = oldTx.getTransaction().getOutputs().get(0);
        byte[] hash160 = transactionOutput.getScript().getChunks().get(2).data;
        this.removeDeposit(nodeAddress, hash160);
    }
}
