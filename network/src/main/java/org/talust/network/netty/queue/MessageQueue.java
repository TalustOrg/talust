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
import org.talust.common.model.MessageChannel;
import org.talust.common.tools.DateUtil;

import java.util.concurrent.LinkedTransferQueue;

/**
 * 接收消息队列,即从远端发送过来的数据将存储于此
 */
@Slf4j
public class MessageQueue {
    private static MessageQueue instance = new MessageQueue();

    private MessageQueue() {
    }

    public static MessageQueue get() {
        return instance;
    }

    private LinkedTransferQueue<MessageChannel> queue = new LinkedTransferQueue<>();

    public void addMessage(MessageChannel message) {
        if (message.getMessage() != null) {
            message.getMessage().setTime(System.currentTimeMillis()/1000);
        }
        this.queue.add(message);
    }

    public MessageChannel takeMessage() throws Exception {
        return this.queue.take();
    }
}
