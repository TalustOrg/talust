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

package org.talust.client.validator;

import lombok.extern.slf4j.Slf4j;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.exception.VerificationException;
import org.talust.common.model.Coin;
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.Configure;
import org.talust.common.tools.SerializationUtil;
import org.talust.core.core.ByteHash;
import org.talust.core.core.Definition;
import org.talust.core.core.SynBlock;
import org.talust.core.model.Block;
import org.talust.core.model.BlockHeader;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.server.NtpTimeService;
import org.talust.core.storage.BlockHeaderStore;
import org.talust.core.storage.BlockStore;
import org.talust.core.transaction.Transaction;
import org.talust.core.transaction.TransactionInput;
import org.talust.core.transaction.TransactionOutput;
import org.talust.network.MessageValidator;
import org.talust.core.storage.BlockStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * 块数据接收到
 */
@Slf4j
public class BlockArrivedValidator implements MessageValidator {
    private BlockStorage storageService = BlockStorage.get();
    private TransactionValidator transactionValidator = new TransactionValidator();

    @Override
    public boolean check(MessageChannel messageChannel) {
        boolean result = false;
        BlockStore blockStore = SerializationUtil.deserializer(messageChannel.getMessage().getContent(), BlockStore.class);
        Block block = blockStore.getBlock();
        long height = block.getHeight();
        long nowHeight = MainNetworkParams.get().getBestBlockHeight();
        log.info("准备验证存储区块时间：{},区块高度：{},本地区块高度：{}",NtpTimeService.currentTimeSeconds(),height,nowHeight);
        if((height-nowHeight)==1||height==0){
            boolean checkRepeat = CacheManager.get().checkRepeat(("block_height:" + height), Configure.BLOCK_GEN_TIME);
            if (checkRepeat) {//说明本节点接收到过同样的消息,则直接将该消息扔掉
                log.info("区块高度：{}的区块已经被接收过，直接抛弃",height);
                return false;
            }
            Sha256Hash prevBlock = block.getPreHash();//前一区块hash
            byte[] preBlockBytes = storageService.get(prevBlock.getBytes());
            if (preBlockBytes != null) {
                BlockHeader blockHeader = new BlockHeader(MainNetworkParams.get(), preBlockBytes);
                long preHeight = blockHeader.getHeight();
                if ((height - preHeight) == 1) {
                    result = true;
                }
            }else if(block.getHeight()==0){
                return true;
            }
            if (result) {//继续校验区块里面的每一条数据
                block.verify();
                block.verifyScript();
                if (verifyBlock(block)) {
                    List<Transaction> data = block.getTxs();
                    for (Transaction datum : data) {
                        MessageChannel nm = new MessageChannel();
                        Message msg = new Message();
                        msg.setContent(SerializationUtil.serializer(datum));
                        nm.setMessage(msg);
                        boolean check = transactionValidator.check(nm);
                        if (!check) {
                            log.info("区块中的交易信息校验失败...");
                            result = false;
                            break;
                        }
                    }
                }
            }
        }else{
            log.info("接受到的区块高度不一致！");
            SynBlock.get().startSynBlock();
            return false;
        }
        return result;
    }


    public boolean verifyBlock(Block block) {
        try {
            if (!block.verify()) {
                return false;
            }
        } catch (VerificationException e) {
            return false;
        }
        //验证区块签名
        try {
            block.verifyScript();
        } catch (Exception e) {
            return false;
        }

        //验证区块的交易是否正确
        if (block.getTxCount() != block.getTxs().size()) {
            return false;
        }
        //每个区块只能包含一个coinbase交易，并且只能是第一个
        boolean coinbase = false;

        List<Transaction> txs = block.getTxs();
        for (Transaction tx : txs) {

            boolean result = transactionValidator.checkTransaction(tx, txs);

            if (!result) {
                throw new VerificationException("交易内容验证失败");
            }
            //区块的第一个交易必然是coinbase交易，除第一个之外的任何交易都不应是coinbase交易，否则出错
            if (!coinbase) {
                if (tx.getType() != Definition.TYPE_COINBASE) {
                    throw new VerificationException("the block first tx is not coinbase tx");
                }
                coinbase = true;
                continue;
            } else if (tx.getType() == Definition.TYPE_COINBASE) {
                throw new VerificationException("the block too much coinbase tx");
            }
        }

        //验证本区块的双花
        List<ByteHash> outputIndexHashArray = new ArrayList<ByteHash>();
        for (Transaction t : txs) {
            List<TransactionInput> inputsTemp = t.getInputs();
            if (inputsTemp == null || inputsTemp.size() == 0) {
                continue;
            }
            for (TransactionInput in : t.getInputs()) {
                List<TransactionOutput> fromsTemp = in.getFroms();
                if (fromsTemp == null || fromsTemp.size() == 0) {
                    continue;
                }
                for (TransactionOutput fromTemp : fromsTemp) {
                    byte[] statusKey = fromTemp.getKey();

                    ByteHash byteHash = new ByteHash(statusKey);
                    if (outputIndexHashArray.contains(byteHash)) {
                        log.warn("存在双花交易");
                        return false;
                    } else {
                        outputIndexHashArray.add(byteHash);
                    }
                }
            }
        }

        //获取区块的最新高度
        BlockHeaderStore bestBlockHeader = storageService.getBestBlockHeader();
        //必需衔接
        if (!block.getPreHash().equals(bestBlockHeader.getBlockHeader().getHash()) ||
                block.getHeight() != bestBlockHeader.getBlockHeader().getHeight() + 1) {
            log.warn("block info warn newblock {}, localblock {}", block.getHeight(), bestBlockHeader.getBlockHeader().getHeight());
            return false;
        }
        return true;
    }


}
