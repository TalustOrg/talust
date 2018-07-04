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

package org.talust.chain.account;

//账户类型
public enum AccountType {

    ROOT(1), //根帐户
    TALUST(2),//平台运营类型账户
    MINING(3),//挖矿的帐户,由平台运营帐户创建
    VERIFY(4),//平台运营管理员帐户
    ADMIN(5),//业务方即企业方管理员
    HR(6),//企业方HR
    USER(7)//普通用户
    ;


    private int type;

    /**
     * @param type
     */
    AccountType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static AccountType getAccountType(int type) {
        for (AccountType b : AccountType.values()) {
            if (b.getType() == type) {
                return b;
            }
        }
        return null;
    }

}
