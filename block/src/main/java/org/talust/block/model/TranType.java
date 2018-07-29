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


package org.talust.block.model;

/**
 * 交易类型
 */
public enum TranType {
    //挖矿所得
    COIN_BASE(1),
    //转账
    TRANSFER(2),
    //储蓄
    DEPOSIT(3),
    //账户下发
    ACCOUNT(4),
    //业务数据
    BUSINESS(5),
    //冻结交易，按时间或高度释放
    FROZEN(6);


    private int type;

    /**
     * @param type
     */
    TranType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static TranType getMessageType(int type) {
        for (TranType b : TranType.values()) {
            if (b.getType() == type) {
                return b;
            }
        }
        return null;
    }

}
