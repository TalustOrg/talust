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
import org.talust.common.tools.SerializationUtil;
import org.talust.core.model.Block;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.storage.BlockStore;
import org.talust.network.MessageHandler;
import org.talust.core.storage.BlockStorage;
import sun.applet.Main;

import java.io.IOException;

@Slf4j//其他节点广播出来的区块数据
public class BlockArrivedHandler implements MessageHandler {
    private BlockStorage blockStorage = BlockStorage.get();

    @Override
    public boolean handle(MessageChannel messageChannel) {
        log.info("接收到远端ip:{} 发送过来的区块数据", messageChannel.getFromIp());
        try {
            return saveBlock(messageChannel);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 保存区块
     *
     * @param messageChannel
     * @return
     */
    public boolean saveBlock(MessageChannel messageChannel) {
        byte[] blockBytes = messageChannel.getMessage().getContent();
        BlockStore blockStore = SerializationUtil.deserializer(blockBytes, BlockStore.class);
        try {
            //最值该节点的最新高度
            long old = MainNetworkParams.get().getBestHeight();
            long now = blockStorage.saveBlock(blockStore);
            if(old<now){
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }
}
