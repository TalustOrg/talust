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

package org.talust.core.model;

//交易输出状态
public enum OutStatus {

    DISABLE(0), //不可用
    ENABLE(1),//可用
    LOCKED(2), //锁定状态,一般指这部分钱用于挖矿所用,解锁后变成可用状态
    END(10000)//结束,主要是为了开发时新增消息类型方便/
    ;


    private int type;

    /**
     * @param type
     */
    OutStatus(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static OutStatus getMessageType(int type) {
        for (OutStatus b : OutStatus.values()) {
            if (b.getType() == type) {
                return b;
            }
        }
        return null;
    }

}
