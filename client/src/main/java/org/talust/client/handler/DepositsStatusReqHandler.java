package org.talust.client.handler;/*
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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.talust.common.model.*;
import org.talust.common.tools.SerializationUtil;
import org.talust.core.model.Address;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.storage.ChainStateStorage;
import org.talust.network.MessageHandler;
import org.talust.network.netty.ConnectionManager;
import org.talust.network.netty.queue.MessageQueue;

import java.util.Collection;

@Slf4j
public class DepositsStatusReqHandler implements MessageHandler {
    private MessageQueue mq = MessageQueue.get();
    private ChainStateStorage chainStateStorage = ChainStateStorage.get();
    @Override
    public boolean handle(MessageChannel messageChannel) {
        Collection<SuperNode> superNodes = ConnectionManager.get().getSuperNodes();
        JSONObject dataList =  new JSONObject();
        for(SuperNode superNode : superNodes){
            Address address =  Address.fromBase58(MainNetworkParams.get(),superNode.getAddress());
            Deposits deposits =chainStateStorage.getDeposits(address.getHash160());
            dataList.put(superNode.getAddress(),deposits);
        }
        byte[] serializer = SerializationUtil.serializer(dataList);
        Message msg = new Message();
        msg.setType(MessageType.NODES_RESP.getType());
        msg.setContent(serializer);
        msg.setMsgCount(messageChannel.getMessage().getMsgCount());
        MessageChannel mc = new MessageChannel();
        mc.setMessage(msg);
        mc.setToIp(messageChannel.getFromIp());
        mq.addMessage(mc);
        log.info("向远端ip:{} 返回所请求的所有储蓄状态}", messageChannel.getFromIp());
        return true;
    }
}
