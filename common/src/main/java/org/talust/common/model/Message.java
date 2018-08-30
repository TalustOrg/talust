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

package org.talust.common.model;

import io.protostuff.Tag;

/**
 * 平台消息的封装
 */
public class Message {
    @Tag(1)//消息类型
    private Integer type;
    @Tag(2)//消息内容
    private byte[] content;
    @Tag(3)//内容的签名者,指的是密钥对中的公钥
    private byte[] signer;
    @Tag(4)//内容的签名者所签的内容,一般指是对内容的hash值进行的签名
    private byte[] signContent;
    @Tag(5)//消息发送时间戳
    private Long time;
    @Tag(6)//消息计数器,由一个AtomicLong不断累加,主要针对请求响应模型的,通过此来定位请求点
    private Long msgCount;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }


    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Long getMsgCount() {
        return msgCount;
    }

    public void setMsgCount(Long msgCount) {
        this.msgCount = msgCount;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public byte[] getSigner() {
        return signer;
    }

    public void setSigner(byte[] signer) {
        this.signer = signer;
    }

    public byte[] getSignContent() {
        return signContent;
    }

    public void setSignContent(byte[] signContent) {
        this.signContent = signContent;
    }

}

