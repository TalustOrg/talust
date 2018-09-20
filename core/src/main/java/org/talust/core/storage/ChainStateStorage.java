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
import org.talust.core.network.MainNetworkParams;
import org.talust.core.transaction.Transaction;
import org.talust.core.transaction.TransactionOutput;
import org.talust.storage.BaseStoreProvider;

import java.util.ArrayList;
import java.util.List;
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
    }

    public void forceUpdateDeposits(String base58Address, Deposits deposits) {
        Address address = Address.fromBase58(MainNetworkParams.get(), base58Address);
        byte[] key = getDepositSearchKey(address.getHash160());
        try {
            db.put(key, SerializationUtil.serializer(deposits));
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
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

    /**
     * 节点共识增加共识金
     */

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
