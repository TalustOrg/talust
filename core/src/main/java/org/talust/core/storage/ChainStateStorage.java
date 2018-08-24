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

import org.talust.common.crypto.ByteArrayTool;
import org.talust.common.crypto.Util;
import org.talust.common.crypto.Utils;
import org.talust.common.crypto.VarInt;
import org.talust.common.model.Coin;
import org.talust.common.model.Deposits;
import org.talust.common.tools.Configure;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.talust.common.model.DepositAccount;
import org.talust.common.tools.SerializationUtil;
import org.talust.common.tools.StringUtils;
import org.talust.core.model.Address;
import org.talust.core.network.MainNetworkParams;
import org.talust.storage.BaseStoreProvider;
import java.util.concurrent.atomic.AtomicLong;

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


    private byte[] TRAN_NUMBER = "tranNumber".getBytes();
    private byte[] ADDRESS_AMOUNT = "addressAmount".getBytes();
    private AtomicLong tranNumber;

    private final  static String dpos = "deposit";


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

    /**
     * 获取超级节点下的储蓄帐户,需要根据这些帐户的储蓄计算挖矿收益
     *
     * @param miningAddress
     * @return
     */
    public Deposits getDeposits(byte[] miningAddress) {
        byte[] key =getDepositSearchKey(miningAddress);
        Deposits deposits = new Deposits();
        try {
            byte[] deps = db.get(key);
            if(null!=deps){
                deposits =  SerializationUtil.deserializer(deps,Deposits.class);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return deposits;
    }


    public void addDeposits(Address address , Coin coin ,byte[] miningAddress){
        try {
            byte[] key =getDepositSearchKey(miningAddress);
            byte[] deps = db.get(key);
            if(null!=deps){
                Deposits deposits=  SerializationUtil.deserializer(deps,Deposits.class);
                deposits.getDepositAccounts().add(new DepositAccount(address.getHash160(),coin));
                try {
                    db.put(key,SerializationUtil.serializer(deposits));
                } catch (RocksDBException e) {
                    e.printStackTrace();
                }
            }

        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void  forceUpdateDeposits(String base58Address , Deposits deposits){
        Address address = Address.fromBase58(MainNetworkParams.get(),base58Address);
        byte[] key  = getDepositSearchKey(address.getHash160());
        try {
            db.put(key,SerializationUtil.serializer(deposits));
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }


    public byte[] getDepositSearchKey(byte[] miningAddress){
        byte[] key = new byte[miningAddress.length + dpos.getBytes().length];
        System.arraycopy(miningAddress, 0, key, 0, miningAddress.length);
        System.arraycopy(dpos.getBytes(), 0, key, miningAddress.length, dpos.getBytes().length);
        return key;
    }

}
