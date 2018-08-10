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

package org.talust.client.handler;

import lombok.extern.slf4j.Slf4j;

import org.talust.common.model.MessageChannel;
import org.talust.network.MessageHandler;
import org.talust.core.storage.BlockStorage;
import org.talust.core.storage.ChainStateStorage;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 交易数据处理
 */
@Slf4j
public class TransactionHandler implements MessageHandler {
    private BlockStorage blockStorage = BlockStorage.get();
    private ChainStateStorage chainStateStorage = ChainStateStorage.get();
    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    @Override
    public boolean handle(MessageChannel message) {
        boolean result = true;
//        Transaction transaction = SerializationUtil.deserializer(message.getMessage().getContent(), Transaction.class);
//        int tranType = transaction.getTranType();
//        if (tranType == TranType.ACCOUNT.getType()) {
//            //是帐户下发的交易类型
//            result = accountPub(transaction);
//        } else if (tranType == TranType.TRANSFER.getType()) {
//            //是转账的交易类型
//        } else if (tranType == TranType.COIN_BASE.getType()) {
//            //是挖矿所得
//            coinBase(transaction);
//        } else if (tranType == TranType.DEPOSIT.getType()) {
//            //是储蓄
//        } else if (tranType == TranType.BUSINESS.getType()) {
//            //是做业务
//        }else if(tranType == TranType.FROZEN.getType()){
//            //是冻结交易
//        }
//
//        //@TODO 业务的交易类型以及储蓄的交易类型后续需要实现
        return result;
    }
//
//    /**
//     * 挖矿处理
//     *
//     * @param transaction
//     * @return
//     */
//    private boolean coinBase(Transaction transaction) {
//        List<TransactionOut> outs = transaction.getOuts();
//        if (outs != null) {
//            for (TransactionOut out : outs) {
//                String key = transaction.getTranNumber() + "-" ;
//                chainStateStorage.put(key.getBytes(), SerializationUtil.serializer(out));
//            }
//        }
//        log.info("处理完coinbase消息.交易号:{}", transaction.getTranNumber());
//        return true;
//    }
//
//    /**
//     * 帐户下发处理
//     *
//     * @param transaction
//     * @return
//     */
//    private boolean accountPub(Transaction transaction) {
//        try {
//            byte[] content = transaction.getDatas();
//            Account account = SerializationUtil.deserializer(content, Account.class);
//            int accType = account.getAccType();
//            //如果当前帐户是根帐户
//            if (accType == AccountType.ROOT.getType() && Utils.equals(account.getPublicKey(), Hex.decode(CacheManager.get().get("ROOT_PK")))) {
//                blockStorage.put(Constant.ROOT_CA, content);
//            }
//            //是挖矿的帐户
//            if (accType == AccountType.MINING.getType()) {
//                rwl.writeLock().lock();
//                try {
//                    String accountAddress = Utils.showAddress(account.getAddress());
//                    int status = account.getStatus();
//                    if (status == AccountStatus.ENABLE.getType()) {
//                        enabledMiningAddress(accountAddress);
//                    } else {
//                        disableMiningAddress(accountAddress);
//                    }
//                } finally {
//                    rwl.writeLock().unlock();
//                }
//            }
//            byte[] accAddr = account.getAddress();
//            byte[] accId = Utils.addBytes(Constant.ACC_PREFIX, accAddr);
//            blockStorage.put(accId, content);
//            log.info("处理完成帐户下发消息,下发的帐户地址:{}", Utils.showAddress(account.getAddress()));
//        } catch (Throwable e) {
//            e.printStackTrace();
//            return false;
//        }
//        return true;
//    }
//
//    /**
//     * 挖矿地址可用
//     *
//     * @param accountAddress
//     */
//    private void enabledMiningAddress(String accountAddress) {
//        List<String> adres = null;
//        byte[] bytes = blockStorage.get(Constant.MINING_ADDRESS);
//        if (bytes != null) {
//            MiningAddress miningAddress = SerializationUtil.deserializer(bytes, MiningAddress.class);
//            if (miningAddress != null) {
//                adres = miningAddress.getAddress();
//            }
//        }
//        if (adres == null) {
//            adres = new ArrayList<>();
//        }
//        if (!adres.contains(accountAddress)) {
//            adres.add(accountAddress);
//        }
//        MiningAddress miningAddres = new MiningAddress();
//        miningAddres.setAddress(adres);
//        blockStorage.put(Constant.MINING_ADDRESS, SerializationUtil.serializer(miningAddres));
//
//        //将挖矿收益地址存于缓存中
//        String cacheKey = new String(Constant.MINING_ADDRESS);
//        List<String> madress = CacheManager.get().get(cacheKey);
//        if (madress == null) {
//            madress = new ArrayList<>();
//        }
//        madress.add(accountAddress);
//        Collections.sort(madress);
//        CacheManager.get().put(cacheKey, madress);
//    }
//
//    /**
//     * 挖矿地址不可用
//     *
//     * @param accountAddress
//     */
//    private void disableMiningAddress(String accountAddress) {
//        byte[] bytes = blockStorage.get(Constant.MINING_ADDRESS);
//        if (bytes != null) {
//            MiningAddress miningAddress = SerializationUtil.deserializer(bytes, MiningAddress.class);
//            if (miningAddress != null) {
//                List<String> adres = miningAddress.getAddress();
//                adres.remove(accountAddress);
//                MiningAddress miningAddres = new MiningAddress();
//                miningAddres.setAddress(adres);
//                blockStorage.put(Constant.MINING_ADDRESS, SerializationUtil.serializer(miningAddres));
//
//                String cacheKey = new String(Constant.MINING_ADDRESS);
//                List<String> madress = CacheManager.get().get(cacheKey);
//                if (madress != null) {
//                    madress.remove(madress);
//                    CacheManager.get().put(cacheKey, madress);
//                }
//            }
//        }
//    }
}
