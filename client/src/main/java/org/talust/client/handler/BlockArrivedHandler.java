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
import org.talust.block.data.DataContainer;
import org.talust.block.model.Block;
import org.talust.common.crypto.Hex;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.Constant;
import org.talust.common.tools.SerializationUtil;
import org.talust.consensus.Conference;
import org.talust.network.MessageHandler;
import org.talust.storage.BlockStorage;

import java.util.List;

@Slf4j//其他节点广播出来的区块数据
public class BlockArrivedHandler implements MessageHandler {
    private BlockStorage blockStorage = BlockStorage.get();
    private CacheManager cu = CacheManager.get();
    private TransactionHandler transactionHandler = new TransactionHandler();

    @Override
    public boolean handle(MessageChannel messageChannel) {
        log.info("接收到远端ip:{} 发送过来的区块数据", messageChannel.getFromIp());
        try {
            saveBlock(messageChannel);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 保存区块
     *
     * @param messageChannel
     * @return
     */
    public byte[] saveBlock(MessageChannel messageChannel) {
        byte[] blockBytes = messageChannel.getMessage().getContent();
        Block block = SerializationUtil.deserializer(blockBytes, Block.class);
        byte[] hash = Sha256Hash.of(blockBytes).getBytes();
        blockStorage.put(hash, blockBytes);
        blockStorage.put(Constant.NOW_BLOCK_HASH, hash);
        byte[] bh = (Constant.BH_PRIX + block.getHead().getHeight()).getBytes();
        blockStorage.put(bh, hash);

        cu.setCurrentBlockHeight(block.getHead().getHeight());
        cu.setCurrentBlockTime(block.getHead().getTime());
        cu.setCurrentBlockHash(hash);
        if(Conference.get().getMaster()!=null){
            cu.setCurrentBlockGenIp(Conference.get().getMaster().getIp());
        }
        log.info("成功存储区块数据,当前hash:{},height:{},time:{}", Hex.encode(hash), block.getHead().getHeight(), block.getHead().getTime());

        List<byte[]> data = block.getBody().getData();
        for (byte[] datum : data) {
            MessageChannel nm = new MessageChannel();
            Message msg = SerializationUtil.deserializer(datum, Message.class);
            nm.setMessage(msg);
            transactionHandler.handle(nm);
            DataContainer.get().removeRecord(datum);
        }
        //@TODO 区块存储时,需要将里面的数据解析出来进行一些索引的存储,比如交易信息这些
        return hash;
    }
}
