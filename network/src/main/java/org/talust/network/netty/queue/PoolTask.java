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
import org.talust.network.MessageHandler;
import org.talust.network.MessageValidator;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.ConnectionManager;

import java.io.Serializable;
import java.util.List;

/**
 * 线程池任务
 */
@Slf4j
public class PoolTask implements Runnable, Serializable {
    private static final long serialVersionUID = 0;
    //保存任务所需要的数据
    private MessageChannel message;
    private MessageValidator validator;
    private List<MessageHandler> handlers;

    public PoolTask(MessageChannel works, MessageValidator validator, List<MessageHandler> handler) {
        this.message = works;
        this.validator = validator;
        this.handlers = handler;
    }

    @Override
    public void run() {
        try {
            String toIP = message.getToIp();
            if (toIP != null && toIP.length() > 0) {
                if (null != message.getChannelId()) {
                    ChannelContain.get().sendMessageByChannelId(toIP, message.getMessage(), message.getChannelId());
                } else {
                    ChannelContain.get().sendMessage(toIP, message.getMessage());
                }
            } else {
                try {
                    boolean check = true;//
                    if (validator != null) {
                        check = validator.check(message);
                    }
                    if (check) {
                        for (MessageHandler handler : handlers) {
                            handler.handle(message);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
            log.error("消息处理错误:", e);
        }
        message = null;
        validator = null;
        handlers = null;
    }
}
