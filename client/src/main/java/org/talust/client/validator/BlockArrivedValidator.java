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
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.Configure;
import org.talust.common.tools.SerializationUtil;
import org.talust.core.core.SynBlock;
import org.talust.core.model.Block;
import org.talust.core.transaction.Transaction;
import org.talust.network.MessageValidator;
import org.talust.core.storage.BlockStorage;

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
        Block block = SerializationUtil.deserializer(messageChannel.getMessage().getContent(), Block.class);
        long height = block.getHeight();
        boolean checkRepeat = CacheManager.get().checkRepeat(("block_height:" + height), Configure.BLOCK_GEN_TIME);
        if (checkRepeat) {//说明本节点接收到过同样的消息,则直接将该消息扔掉
            return false;
        }
        if (height > 1) {
            Sha256Hash prevBlock = block.getPreHash();//前一区块hash
            byte[] preBlockBytes = storageService.get(prevBlock.getBytes());
            if (preBlockBytes != null) {
                Block preBlock = SerializationUtil.deserializer(preBlockBytes, Block.class);
                long preHeight = preBlock.getHeight();
                long nowHeight = block.getHeight();
                if ((nowHeight - preHeight) == 1) {
                    result = true;
                }
            } else {
                log.info("本区块同步有问题,未存储所接收到的区块的前一区块的数据,应该启用同步区块程序,当前区块高度:{}", height);
                SynBlock.get().startSynBlock();
            }
        } else {
            result = true;
        }
        if (result) {//继续校验区块里面的每一条数据
            block.verify();
            block.verifyScript();
            List<Transaction> data = block.getTxs();
            for (Transaction datum : data) {
                MessageChannel nm = new MessageChannel();
                Message msg = SerializationUtil.deserializer(SerializationUtil.serializer(datum), Message.class);
                nm.setMessage(msg);
                boolean check = transactionValidator.check(nm);
                if (!check) {
                    log.info("区块中的交易信息校验失败...");
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
}
