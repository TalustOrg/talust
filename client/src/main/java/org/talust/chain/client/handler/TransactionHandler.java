package org.talust.chain.client.handler;

import lombok.extern.slf4j.Slf4j;
import org.talust.chain.account.Account;
import org.talust.chain.account.AccountStatus;
import org.talust.chain.account.AccountType;
import org.talust.chain.account.MiningAddress;
import org.talust.chain.block.model.TranType;
import org.talust.chain.block.model.Transaction;
import org.talust.chain.block.model.TransactionOut;
import org.talust.chain.common.crypto.Hex;
import org.talust.chain.common.crypto.Utils;
import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.tools.CacheManager;
import org.talust.chain.common.tools.Configure;
import org.talust.chain.common.tools.Constant;
import org.talust.chain.common.tools.SerializationUtil;
import org.talust.chain.network.MessageHandler;
import org.talust.chain.storage.BlockStorage;
import org.talust.chain.storage.ChainStateStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        Transaction transaction = SerializationUtil.deserializer(message.getMessage().getContent(), Transaction.class);
        int tranType = transaction.getTranType();
        if (tranType == TranType.ACCOUNT.getType()) {//是帐户下发的交易类型
            result = accountPub(transaction);
        } else if (tranType == TranType.TRANSFER.getType()) {//是转账的交易类型

        } else if (tranType == TranType.COIN_BASE.getType()) {//是挖矿所得
            coinBase(transaction);
        } else if (tranType == TranType.DEPOSIT.getType()) {//是储蓄

        } else if (tranType == TranType.BUSINESS.getType()) {//是做业务

        }
        //@TODO 业务的交易类型以及储蓄的交易类型后续需要实现
        return result;
    }

    /**
     * 挖矿处理
     *
     * @param transaction
     * @return
     */
    private boolean coinBase(Transaction transaction) {
        List<TransactionOut> outs = transaction.getOuts();
        if (outs != null) {
            for (TransactionOut out : outs) {
                String key = transaction.getTranNumber() + "-" + out.getItem();
                chainStateStorage.put(key.getBytes(), SerializationUtil.serializer(out));
            }
        }
        log.info("处理完coinbase消息.交易号:{}", transaction.getTranNumber());
        return true;
    }

    /**
     * 帐户下发处理
     *
     * @param transaction
     * @return
     */
    private boolean accountPub(Transaction transaction) {
        try {
            byte[] content = transaction.getDatas();
            Account account = SerializationUtil.deserializer(content, Account.class);
            int accType = account.getAccType();
            if (accType == AccountType.ROOT.getType() && Utils.equals(account.getPublicKey(), Hex.decode(Configure.ROOT_PUB))) {//如果当前帐户是根帐户
                blockStorage.put(Constant.ROOT_CA, content);
            }

            if (accType == AccountType.MINING.getType()) {//是挖矿的帐户
                rwl.writeLock().lock();
                try {
                    String accountAddress = Utils.showAddress(account.getAddress());
                    int status = account.getStatus();
                    if (status == AccountStatus.ENABLE.getType()) {
                        enabledMiningAddress(accountAddress);
                    } else {
                        disableMiningAddress(accountAddress);
                    }
                } finally {
                    rwl.writeLock().unlock();
                }
            }

            byte[] accAddr = account.getAddress();
            byte[] accId = Utils.addBytes(Constant.ACC_PREFIX, accAddr);
            blockStorage.put(accId, content);
            log.info("处理完成帐户下发消息,下发的帐户地址:{}", Utils.showAddress(account.getAddress()));
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 挖矿地址可用
     *
     * @param accountAddress
     */
    private void enabledMiningAddress(String accountAddress) {
        List<String> adres = null;
        byte[] bytes = blockStorage.get(Constant.MINING_ADDRESS);
        if (bytes != null) {
            MiningAddress miningAddress = SerializationUtil.deserializer(bytes, MiningAddress.class);
            if (miningAddress != null) {
                adres = miningAddress.getAddress();
            }
        }
        if (adres == null) {
            adres = new ArrayList<>();
        }
        if (!adres.contains(accountAddress)) {
            adres.add(accountAddress);
        }
        MiningAddress miningAddres = new MiningAddress();
        miningAddres.setAddress(adres);
        blockStorage.put(Constant.MINING_ADDRESS, SerializationUtil.serializer(miningAddres));

        //将挖矿收益地址存于缓存中
        String cacheKey = new String(Constant.MINING_ADDRESS);
        List<String> madress = CacheManager.get().get(cacheKey);
        if (madress == null) {
            madress = new ArrayList<>();
        }
        madress.add(accountAddress);
        Collections.sort(madress);
        CacheManager.get().put(cacheKey, madress);
    }

    /**
     * 挖矿地址不可用
     *
     * @param accountAddress
     */
    private void disableMiningAddress(String accountAddress) {
        byte[] bytes = blockStorage.get(Constant.MINING_ADDRESS);
        if (bytes != null) {
            MiningAddress miningAddress = SerializationUtil.deserializer(bytes, MiningAddress.class);
            if (miningAddress != null) {
                List<String> adres = miningAddress.getAddress();
                adres.remove(accountAddress);
                MiningAddress miningAddres = new MiningAddress();
                miningAddres.setAddress(adres);
                blockStorage.put(Constant.MINING_ADDRESS, SerializationUtil.serializer(miningAddres));

                String cacheKey = new String(Constant.MINING_ADDRESS);
                List<String> madress = CacheManager.get().get(cacheKey);
                if (madress != null) {
                    madress.remove(madress);
                    CacheManager.get().put(cacheKey, madress);
                }
            }
        }
    }
}
