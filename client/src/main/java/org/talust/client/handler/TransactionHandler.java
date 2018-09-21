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
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.Configure;
import org.talust.common.tools.SerializationUtil;
import org.talust.consensus.Conference;
import org.talust.core.core.Definition;
import org.talust.core.data.DataContainer;
import org.talust.core.data.TransactionCache;
import org.talust.core.transaction.Transaction;
import org.talust.network.MessageHandler;
import org.talust.core.storage.BlockStorage;
import org.talust.core.storage.ChainStateStorage;
import org.talust.network.netty.ConnectionManager;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 交易数据处理
 */
@Slf4j
public class TransactionHandler implements MessageHandler {
    @Override
    public boolean handle(MessageChannel message) {
        Transaction transaction = SerializationUtil.deserializer(message.getMessage().getContent(), Transaction.class);
        log.info("接收到节点IP：{}的漫游交易传输，交易类型{}", message.getFromIp(), transaction.getType());
        boolean checkRepeat = CacheManager.get().checkRepeat(("tx_hash:" + transaction.getHash().toString()), Configure.BLOCK_GEN_TIME);
        if (!checkRepeat) {
            if (!ConnectionManager.get().getMasterIp().equals(ConnectionManager.get().getSelfIp())) {
                ConnectionManager.get().TXMessageSend(message.getMessage());
            }else{
                DataContainer.get().addRecord(transaction);
            }
        }else{
            log.info("接收到节点IP：{}的漫游交易传输已经接收过。", message.getFromIp());
        }
        return checkRepeat;
    }

}
