package com.talust.chain.consensus;

import com.talust.chain.account.Account;
import com.talust.chain.account.MiningAddress;
import com.talust.chain.block.data.DataContainer;
import com.talust.chain.block.mining.MiningRule;
import com.talust.chain.block.model.*;
import com.talust.chain.common.crypto.Hex;
import com.talust.chain.common.crypto.Sha256Hash;
import com.talust.chain.common.crypto.Utils;
import com.talust.chain.common.model.*;
import com.talust.chain.common.tools.CacheManager;
import com.talust.chain.common.tools.Constant;
import com.talust.chain.common.tools.DateUtil;
import com.talust.chain.common.tools.SerializationUtil;
import com.talust.chain.network.netty.ConnectionManager;
import com.talust.chain.network.netty.queue.MessageQueue;
import com.talust.chain.storage.AccountStorage;
import com.talust.chain.storage.BlockStorage;
import com.talust.chain.storage.ChainStateStorage;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 打包工具
 */
@Slf4j
public class PackBlockTool {
    private BlockStorage blockStorage = BlockStorage.get();
    private DataContainer dataContainer = DataContainer.get();
    private ChainStateStorage chainStateStorage = ChainStateStorage.get();
    private CacheManager cu = CacheManager.get();

    //打包
    public void  pack(int packageTime) {
        try {
            Account account = AccountStorage.get().getAccount();

            List<byte[]> batchRecord = dataContainer.getBatchRecord();//批量获取需要打包的数据
            if (batchRecord == null) {
                batchRecord = new ArrayList<>();
            }

            int height = cu.getCurrentBlockHeight();
            height++;
            byte[] coinBase = getCoinBase(packageTime, height);
            if (coinBase != null) {
                batchRecord.add(coinBase);//加入挖矿奖励
            }

            BlockBody blockBody = new BlockBody();
            blockBody.setData(batchRecord);
            byte[] bb = SerializationUtil.serializer(blockBody);
            byte[] bbHash = Sha256Hash.hash(bb);
            BlockHead head = new BlockHead();
            head.setHeight(height);
            head.setTime(packageTime);
            head.setBodyHash(bbHash);
            head.setPrevBlock(CacheManager.get().getCurrentBlockHash());
            Block block = new Block();
            block.setHead(head);
            block.setBody(blockBody);
            byte[] data = SerializationUtil.serializer(block);
            byte[] hash = Sha256Hash.of(data).getBytes();

            byte[] sign = AccountStorage.get().getEcKey().sign(hash);

            Message message = new Message();
            message.setTime(block.getHead().getTime());
            message.setType(MessageType.BLOCK_ARRIVED.getType());//对于接收方来说,是区块到来,因为此消息有明确的接收方
            message.setContent(data);
            message.setSigner(account.getPublicKey());
            message.setSignContent(sign);

            MessageChannel mc = new MessageChannel();
            mc.setMessage(message);
            mc.setFromIp(ConnectionManager.get().selfIp);
            //log.info("完成本次打包,打包区块hash:{},区块高度:{},区块时间:{}", Hex.encode(hash), height, packageTime);
            MessageQueue.get().addMessage(mc);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取挖矿奖励
     *
     * @param packageTime
     * @param height
     * @return
     */
    private byte[] getCoinBase(int packageTime, int height) throws Exception {
        if (height == 1) {//创世块不挖矿
            return null;
        }
        Message message = new Message();
        BigDecimal baseCoin = new BigDecimal(MiningRule.getBaseCoin(height));//奖励给矿机的钱
        BigDecimal depositCoin = new BigDecimal(MiningRule.getDepositCoin(height));//其他储蓄的钱所分得的利息

        int item = 1;
        //添加coinbase
        Transaction coinBase = new Transaction();
        coinBase.setTranType(TranType.COIN_BASE.getType());
        coinBase.setTranNumber(chainStateStorage.newTranNumber());//设置交易号
        List<TransactionOut> outs = new ArrayList<>();
        List<String>  miningAddress = CacheManager.get().get(new String(Constant.MINING_ADDRESS));
        if (miningAddress == null) {
            return null;
        }
        int size = miningAddress.size();
        int idx = height % size;//当前获得收益的区块
        String sn = miningAddress.get(idx);


        TransactionOut mining = new TransactionOut();//矿机自身获得
        mining.setAddress(Utils.deShowAddress(sn));
        mining.setAmount(baseCoin.doubleValue());
        mining.setStatus(OutStatus.ENABLE.getType());
        mining.setItem(item++);
        mining.setTime(packageTime);
        mining.setCoinBaseType(CoinBaseType.MINING.getType());
        outs.add(mining);
        log.info("挖矿奖励给地址:{},高度:{},金额:{}", sn, height, mining.getAmount());

        List<DepositAccount> deposits = chainStateStorage.getDeposits(sn);
        if (deposits.size() == 0) {//当前没有储蓄帐户,则挖出来的币直接奖励给矿机
            TransactionOut tout = new TransactionOut();
            tout.setAddress(Utils.deShowAddress(sn));
            tout.setAmount(depositCoin.doubleValue());
            tout.setStatus(OutStatus.ENABLE.getType());
            tout.setTime(packageTime);
            tout.setItem(item++);
            tout.setCoinBaseType(CoinBaseType.DEPOSITION.getType());
            outs.add(tout);
            log.info("挖矿奖励给储蓄地址:{},高度:{},金额:{}", sn, height,depositCoin.doubleValue());
        } else {//有储蓄者
            BigDecimal totalAmount = calTotalAmount(deposits);
            for (DepositAccount deposit : deposits) {
                TransactionOut tout = new TransactionOut();
                tout.setAddress(deposit.getAddress());
                double gain = (new BigDecimal(deposit.getAmount()).divide(totalAmount).multiply(depositCoin)).doubleValue();
                tout.setAmount(gain);
                tout.setStatus(OutStatus.ENABLE.getType());
                tout.setTime(packageTime);
                tout.setItem(item++);
                tout.setCoinBaseType(CoinBaseType.DEPOSITION.getType());
                outs.add(tout);
                log.info("挖矿奖励给储蓄地址:{},高度:{},金额:{}", Utils.showAddress(deposit.getAddress()),height, gain);
            }
        }

        coinBase.setOuts(outs);
        byte[] cbase = SerializationUtil.serializer(coinBase);
        message.setContent(cbase);
        message.setType(MessageType.TRANSACTION.getType());
        message.setTime(DateUtil.getTimeSecond());
        byte[] hash = Sha256Hash.of(cbase).getBytes();
        byte[] sign = AccountStorage.get().getEcKey().sign(hash);
        message.setSigner(AccountStorage.get().getAccount().getPublicKey());
        message.setSignContent(sign);
        return SerializationUtil.serializer(message);
    }

    private BigDecimal calTotalAmount(List<DepositAccount> deposits) {
        BigDecimal total = new BigDecimal(0);
        for (DepositAccount deposit : deposits) {
            total = total.add(new BigDecimal(deposit.getAmount()));
        }
        return total;
    }

//    public static void main(String[] args) {
//        BigDecimal divide = new BigDecimal(10).divide(new BigDecimal(3), 8, BigDecimal.ROUND_HALF_UP);
//        System.out.println(divide.doubleValue());
//    }

}
