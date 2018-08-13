package org.talust.consensus;

import lombok.extern.slf4j.Slf4j;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.model.*;
import org.talust.common.tools.*;
import org.talust.core.core.NetworkParams;
import org.talust.core.data.DataContainer;
import org.talust.core.model.Account;
import org.talust.core.model.Block;
import org.talust.core.model.BlockHeader;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.storage.AccountStorage;
import org.talust.core.storage.BlockStorage;
import org.talust.core.storage.ChainStateStorage;
import org.talust.core.transaction.Transaction;
import org.talust.network.netty.ConnectionManager;
import org.talust.network.netty.queue.MessageQueue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 打包工具
 */
@Slf4j
public class PackBlockTool {
    private DataContainer dataContainer = DataContainer.get();
    private BlockStorage blockStorage = BlockStorage.get();
    private NetworkParams networkParams = MainNetworkParams.get();
    private CacheManager cu = CacheManager.get();


    //打包
    public void  pack(int packageTime) {
        try {
            Account account = AccountStorage.get().getAccount();
            //批量获取需要打包的数据
            List<Transaction> transactionList = dataContainer.getBatchRecord();
            if (transactionList == null) {
                transactionList = new ArrayList<>();
            }
            long height = cu.getCurrentBlockHeight();
            height++;
            Transaction coinBase = getCoinBase(packageTime, height);
            if (coinBase != null) {
                //加入挖矿奖励,挖矿交易生成
                transactionList.add(coinBase);
            }
            //本地最新区块
            BlockHeader bestBlockHeader = blockStorage.getBestBlockHeader().getBlockHeader();
            //获取我的时段开始时间
            Block block = new Block(networkParams);
            long currentHeight = bestBlockHeader.getHeight() + 1;
            block.setHeight(currentHeight);
            block.setPreHash(bestBlockHeader.getHash());
            block.setTime(packageTime);
            block.setVersion(networkParams.getProtocolVersionNum(NetworkParams.ProtocolVersion.CURRENT));
            block.setTxs(transactionList);
            block.setTxCount(transactionList.size());
            block.setMerkleHash(block.buildMerkleHash());
            block.sign(account);
            block.verify();
            block.verifyScript();



            byte[] data = SerializationUtil.serializer(block);
            byte[] sign = account.getEcKey().sign(Sha256Hash.of(data)).encodeToDER();

            Message message = new Message();
            message.setTime(packageTime);
            //对于接收方来说,是区块到来,因为此消息有明确的接收方
            message.setType(MessageType.BLOCK_ARRIVED.getType());
            message.setContent(data);
            message.setSigner(account.getEcKey().getPubKey());
            message.setSignContent(sign);

            MessageChannel mc = new MessageChannel();
            mc.setMessage(message);
            mc.setFromIp(ConnectionManager.get().selfIp);
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
    private Transaction getCoinBase(int packageTime, long height) throws Exception {
        //创世块不挖矿
//        if (height == 1) {
//            return null;
//        }
//        Message message = new Message();
//        //奖励给矿机的钱
//        BigDecimal baseCoin = new BigDecimal(MiningRule.getBaseCoin(height));
//        //其他储蓄的钱所分得的利息
//        BigDecimal depositCoin = new BigDecimal(MiningRule.getDepositCoin(height));
//
//        int item = 1;
//        //添加coinbase
//        Transaction coinBase = new Transaction();
//        coinBase.setTranType(TranType.COIN_BASE.getType());
//        //设置交易号
//        coinBase.setTranNumber(chainStateStorage.newTranNumber() );
//        List<TransactionOut> outs = new ArrayList<>();
//        List<String>  miningAddress = CacheManager.get().get(new String(Constant.MINING_ADDRESS));
//        if (miningAddress == null) {
//            return null;
//        }
//        int size = miningAddress.size();
//        //当前获得收益的区块
//        int idx = height % size;
//        String sn = miningAddress.get(idx);
//
//        //矿机自身获得
//        TransactionOut mining = new TransactionOut();
//        mining.setAddress(StringUtils.hexStringToBytes(sn));
//        mining.setAmount(baseCoin.longValue());
//        mining.setEnableHeight(packageTime);
//        outs.add(mining);
//        log.info("挖矿奖励给地址:{},高度:{},金额:{}", sn, height, mining.getAmount());
//
//        List<DepositAccount> deposits = chainStateStorage.getDeposits(sn);
//        //当前没有储蓄帐户,则挖出来的币直接奖励给矿机
//        if (deposits.size() == 0) {
//            TransactionOut tout = new TransactionOut();
//            tout.setAddress(StringUtils.hexStringToBytes(sn));
//            tout.setAmount(depositCoin.longValue());
//            outs.add(tout);
//            log.info("挖矿奖励给储蓄地址:{},高度:{},金额:{}", sn, height,depositCoin.doubleValue());
//        } else {//有储蓄者
//            BigDecimal totalAmount = calTotalAmount(deposits);
//            for (DepositAccount deposit : deposits) {
//                TransactionOut tout = new TransactionOut();
//                tout.setAddress(deposit.getAddress());
//                long gain = (new BigDecimal(deposit.getAmount()).divide(totalAmount).multiply(depositCoin)).longValue();
//                tout.setAmount(gain);
//                outs.add(tout);
//                log.info("挖矿奖励给储蓄地址:{},高度:{},金额:{}", Utils.showAddress(deposit.getAddress()),height, gain);
//            }
//        }
//
//        coinBase.setOuts(outs);
//        byte[] cbase = SerializationUtil.serializer(coinBase);
//        message.setContent(cbase);
//        message.setType(MessageType.TRANSACTION.getType());
//        message.setTime(DateUtil.getTimeSecond());
//        byte[] hash = Sha256Hash.of(cbase).getBytes();
//        byte[] sign = AccountStorage.get().getEcKey().sign(hash);
//        message.setSigner(AccountStorage.get().getAccount().getPublicKey());
//        message.setSignContent(sign);
        return null;
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
