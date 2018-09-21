package org.talust.consensus;

import lombok.extern.slf4j.Slf4j;
import org.talust.common.crypto.Base58;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.model.*;
import org.talust.common.model.Message;
import org.talust.common.tools.*;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;
import org.talust.core.data.ConsensusCalculationUtil;
import org.talust.core.data.DataContainer;
import org.talust.core.model.*;
import org.talust.common.model.DepositAccount;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.script.ScriptBuilder;
import org.talust.core.server.NtpTimeService;
import org.talust.core.storage.*;
import org.talust.core.transaction.Transaction;
import org.talust.core.transaction.TransactionCreator;
import org.talust.core.transaction.TransactionInput;
import org.talust.network.netty.ConnectionManager;
import org.talust.network.netty.queue.MessageQueue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 打包工具
 */
@Slf4j
public class PackBlockTool {
    private DataContainer dataContainer = DataContainer.get();
    private BlockStorage blockStorage = BlockStorage.get();
    private NetworkParams networkParams = MainNetworkParams.get();
    private  TransactionCreator transactionCreator = new TransactionCreator();
    //打包
    public void  pack(long packageTime) {
        try {
            Account account = AccountStorage.get().getAccount();
            //批量获取需要打包的数据
            List<Transaction> transactionList = new ArrayList<>();
            long height = blockStorage.getBestBlockHeader().getBlockHeader().getHeight();
            height++;
            Transaction coinBase = getCoinBase(packageTime, height);
            networkParams.setBestHeight(height);
            if (coinBase != null) {
                //加入挖矿奖励,挖矿交易生成
                transactionList.add(coinBase);
            }

            //TODO  针对所有的交易进行验证， 中心验证共识交易是否出现极限情况。
            /**
             * 迭代缓存交易，取出所有的共识交易，按节点形成LIST，而后根据节点地址进行判定，是否满足全部放入的情况，
             * 如果剩余位置满足则不对交易数据进行操作，如果剩余位置不满足则根据加入金额大小进行判定
             * 整体list中踢出加入金额最小的那个。
             */
            log.info("共识交易验证开始：{}",NtpTimeService.currentTimeMillis());
            List<Transaction> consensusTx = new ArrayList<>();
            for(Transaction transaction : dataContainer.getValidatorRecord()){
                if(transaction.getType()==Definition.TYPE_REG_CONSENSUS||transaction.getType()==Definition.TYPE_REM_CONSENSUS){
                    consensusTx.add(transaction);
                }
            }
            List<TxValidator> txValidators = ChainStateStorage.get().checkConsensus(consensusTx);
            if(null!=txValidators){
                for(TxValidator txValidator :txValidators){
                    if(null!=txValidator.getTransaction()){
                        dataContainer.removeRecord(txValidator.getTransaction());
                    }else if(null!=txValidator.getDepositAccount()){
                        Transaction transaction =transactionCreator.createRemConsensus(txValidator.getDepositAccount(),null,txValidator.getNodeAddress());
                        dataContainer.addRecord(transaction);
                    }
                }
            }
            log.info("共识交易验证结束：{}",NtpTimeService.currentTimeMillis());
            List<Transaction> dataContain = dataContainer.getBatchRecord();
            dataContain.sort(new Comparator<Transaction>() {
                @Override
                public int compare(Transaction o1, Transaction o2) {
                    return o2.getType()-o1.getType();
                }
            });
            transactionList.addAll(dataContain );
            //本地最新区块
            BlockHeader BlockHeader = blockStorage.getBestBlockHeader().getBlockHeader();
            //获取我的时段开始时间
            Block block = new Block(networkParams);
            long currentHeight = BlockHeader.getHeight() + 1;
            block.setHeight(currentHeight);
            block.setPreHash(BlockHeader.getHash());
            block.setTime(packageTime);
            block.setVersion(networkParams.getProtocolVersionNum(NetworkParams.ProtocolVersion.CURRENT));
            block.setTxs(transactionList);
            block.setTxCount(transactionList.size());
            block.setMerkleHash(block.buildMerkleHash());
            block.sign(account);
            block.verify();
            block.verifyScript();
            BlockStore blockStore = new BlockStore(networkParams , block);
            byte[] data = SerializationUtil.serializer(blockStore);
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
    private Transaction getCoinBase(long packageTime, long height) throws Exception {
        //添加共识奖励交易
        Transaction coinBase = new Transaction(networkParams);
        coinBase.setVersion(Definition.VERSION);
        coinBase.setType(Definition.TYPE_COINBASE);
        TransactionInput input = new TransactionInput();
        coinBase.addInput(input);
        input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a coinBase tx".getBytes()));
        Coin consensusRreward = ConsensusCalculationUtil.calculatConsensusReward(height);
        Coin minerRreward = ConsensusCalculationUtil.calculatMinerReward(height);
        List<String>  miningAddress = CacheManager.get().get(new String(Constant.MINING_ADDRESS));
        if (miningAddress == null) {
            return null;
        }
        int size = miningAddress.size();
        //当前获得收益的区块
        int idx = (int) (height % size);
        //矿机自身获得
        Address mingAddr = Address.fromBase58(networkParams,miningAddress.get(idx));
        byte[] sn = mingAddr.getHash160();
        coinBase.addOutput(minerRreward,new Address(networkParams,sn));
        log.info("挖矿奖励给地址:{},高度:{},金额:{}",mingAddr.getBase58(), height, ArithUtils.div( minerRreward.value+"","100000000",8));

        List<DepositAccount> deposits = ChainStateStorage.get().getDeposits(sn).getDepositAccounts();
        //当前没有储蓄帐户,则挖出来的币直接奖励给矿机
        if (null==deposits||deposits.size() == 0) {
            coinBase.addOutput(consensusRreward,new Address(networkParams,sn));
            log.info("挖矿奖励给矿机地址:{},高度:{},金额:{}", mingAddr.getBase58(), height,ArithUtils.div( consensusRreward.value+"","100000000",8));
        } else {//有储蓄者
            Coin totalAmount = calTotalAmount(deposits);
            log.info("储蓄金额总和为：{}",totalAmount.value);
            for (DepositAccount deposit : deposits) {
                Coin per =  Coin.parseCoin(caculatPer(consensusRreward.value,deposit.getAmount().value,totalAmount.value));
                coinBase.addOutput(per,new Address(networkParams,deposit.getAddress()));
                log.info("挖矿奖励给储蓄地址:{},高度:{},金额:{}", Base58.encode(deposit.getAddress()),height, ArithUtils.div(per.value+"","100000000",8));
            }
        }
        coinBase.verify();
        return coinBase;
    }

    private Coin calTotalAmount(List<DepositAccount> deposits) {
        Coin total =  Coin.ZERO;
        for (DepositAccount deposit : deposits) {
            total = total.add(deposit.getAmount());
        }
        return total;
    }

    private String caculatPer(long reAll , long have , long total){
        String mul = ArithUtils.mul(reAll,have,0);
        mul = ArithUtils.div(mul,"100000000",8);
        String res = ArithUtils.div(mul,total,0);
        return res;
    }
}
