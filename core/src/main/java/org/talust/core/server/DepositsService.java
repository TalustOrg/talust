package org.talust.core.server;/*
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

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.talust.common.model.Deposits;
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.model.MessageType;
import org.talust.common.tools.SerializationUtil;
import org.talust.core.storage.ChainStateStorage;
import org.talust.network.model.MyChannel;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.SynRequest;

import java.util.Collection;
import java.util.Map;

@Slf4j
public class DepositsService {
    private static DepositsService instance = new DepositsService();

    private DepositsService() {
        this.chainStateStorage =  ChainStateStorage.get();
    }

    public static DepositsService get() {
        return instance;
    }

    private ChainStateStorage chainStateStorage  ;
    /**
     * 更新本地的储蓄账户缓存
     */
    public void updateDeposits() {
        Collection<MyChannel> myChannels= ChannelContain.get().getMyChannels();
        if(null!=myChannels &&myChannels.size()>0){
            String ip = myChannels.iterator().next().getRemoteIp();
            String selfIp = myChannels.iterator().next().getLocalIp();
            try {
                Message message = new Message();
                message.setType(MessageType.DEPOSITS_STATUS_REQ.getType());
                message.setContent(selfIp.getBytes());
                log.info("向地址{}发送当前节点ip:{}的更新储蓄账户请求",ip, selfIp);
                MessageChannel messageChannel = SynRequest.get().synReq(message, ip);
                if (messageChannel != null) {
                    JSONObject  depositsList = SerializationUtil.deserializer(messageChannel.getMessage().getContent(), JSONObject.class);
                    for (Object map : depositsList.entrySet()) {
                        Deposits deposits = (Deposits) ((Map.Entry) map).getValue();
                        String address =(String)((Map.Entry) map).getKey();
                        chainStateStorage.forceUpdateDeposits(address,deposits);
                    }
                }else{
                    log.info("请求储蓄集合，节点ip:{}，请求失败", ip);
                }
            } catch (Exception e) {
                log.info("请求储蓄集合，节点ip:{}，请求失败", ip);
            }
        }
    }
}
