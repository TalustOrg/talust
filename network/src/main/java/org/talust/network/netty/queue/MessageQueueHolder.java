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

package org.talust.network.netty.queue;

import lombok.extern.slf4j.Slf4j;
import org.talust.common.crypto.ECKey;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.model.MessageType;
import org.talust.common.tools.StringUtils;
import org.talust.common.tools.ThreadPool;
import org.talust.network.MessageHandler;
import org.talust.network.MessageValidator;
import org.talust.network.model.MyChannel;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.ConnectionManager;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 消息队列处理器,针对底层通讯层的处理器
 */
@Slf4j
public class MessageQueueHolder {
    private static MessageQueueHolder instance = new MessageQueueHolder();

    private MessageQueueHolder() {
    }

    public static MessageQueueHolder get() {
        return instance;
    }

    protected ConnectionManager cm = ConnectionManager.get();
    private MessageQueue mq = MessageQueue.get();
    //构造一个线程池,此线程池用于通讯层收发消息所用
    private ThreadPoolExecutor threadPool = ThreadPool.get().threadPool;

    public void start() {
        ExecutorService executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        executorService.execute(() -> {
            MessageQueue messageQueue = MessageQueue.get();
            while (true) {//不断地从消息队列中取出消息进行处理
                try {
                    MessageChannel message = messageQueue.takeMessage();
                    Integer type = message.getMessage().getType();
                    List<MessageHandler> messageHandler = mapHandlers.get(type);
                    if (messageHandler != null) {
                        MessageValidator validator = mapValidators.get(type);
                        threadPool.execute(new PoolTask(message, validator, messageHandler));
                    }
                } catch (Throwable e) {
                    log.error("消息处理错误:", e);
                }
            }
        });
    }

    /**
     * 类型处理器
     */
    private Map<Integer, List<MessageHandler>> mapHandlers = new HashMap<>();
    /**
     * 类型校验器
     */
    private Map<Integer, MessageValidator> mapValidators = new HashMap<>();

    /**
     * 添加消息处理器
     *
     * @param messageType
     * @param handler
     */
    public synchronized void addHandler(MessageType messageType, MessageHandler handler) {
        List<MessageHandler> message1Handlers = mapHandlers.get(messageType.getType());
        if (message1Handlers == null) {
            message1Handlers = new ArrayList<>();
            mapHandlers.put(messageType.getType(), message1Handlers);
        }
        message1Handlers.add(handler);
    }

    /**
     * 设置校验器
     *
     * @param messageType
     * @param validator
     */
    public synchronized void setValidator(MessageType messageType, MessageValidator validator) {
        mapValidators.put(messageType.getType(), validator);
    }


    /**
     * 检测签名
     *
     * @return
     */
    public boolean checkSign(Message message) {
        Sha256Hash hash = Sha256Hash.of(message.getContent());
        byte[] signer = message.getSigner();
        byte[] signContent = message.getSignContent();
        return ECKey.verify(hash.getBytes(), signContent, signer);
    }

    /**
     * 广播消息,向本节点所连接的所有节点广播消息
     * 这里的广播并非是指直接在底层就发送数据,而是多次复制,放入消息队列中
     * 通过消息队列分发消息,以此达到多线程处理的目的
     *
     * @param message
     */
    public void broadMessage(MessageChannel message) {
        String fromChannel = message.getFromIp();
        String selfIp = cm.getSelfIp();
        //分别向其他各个节点发送消息
        Collection<MyChannel> allChannel = ChannelContain.get().getMyChannels();
        log.info("向网络节点广播消息,当前本节点所连接的网络节点数为:{},消息类型:{}", allChannel.size(), message.getMessage().getType());
        for (MyChannel channel : allChannel) {//主要是考虑为了提升效率
            if (!selfIp.equals(channel.getRemoteIp())) {
                if (StringUtils.isNotEmpty(fromChannel) && fromChannel.equals(channel.getRemoteIp())) {
                    //不需要给发送过来的通道再发回去
                    continue;
                }
                log.info("------------向ip:{} 广播消息:{}", channel.getRemoteIp(), message.getMessage().getType());
                MessageChannel mc = new MessageChannel();
                mc.setMessage(message.getMessage());
                mc.setToIp(channel.getRemoteIp());
                mc.setFromIp(selfIp);
                mq.addMessage(mc);
            }
        }
    }

}
