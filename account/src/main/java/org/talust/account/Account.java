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
package org.talust.account;

import io.protostuff.Tag;

//用户账号信息
public class Account {
    @Tag(1)//账户类型,0表示为根账号
    private Integer accType;
    @Tag(2)//用户的公钥
    private byte[] publicKey;
    @Tag(3)//账户的地址,通过公钥处理后生成的
    private byte[] address;
    @Tag(4)//父证书签名内容,主要是对当前帐户的公钥进行的签名
    private byte[] parentSign;
    @Tag(5)//父证书公钥
    private byte[] parentPub;
    @Tag(6)//帐户状态,默认可用
    private int status = AccountStatus.ENABLE.getType();
    @Tag(7)//别名
    private String account;
    @Tag(8)//帐号密码,用于用户使用客户端时生成密钥对有用
    private boolean accPwd;
    @Tag(9)//密钥对的私钥,一般情况下,不要保存到该字段中
    private byte[] privateKey;
    @Tag(10)//帐户金额数
    private double amount;


    public Integer getAccType() {
        return accType;
    }

    public void setAccType(Integer accType) {
        this.accType = accType;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public byte[] getParentSign() {
        return parentSign;
    }

    public void setParentSign(byte[] parentSign) {
        this.parentSign = parentSign;
    }

    public byte[] getParentPub() {
        return parentPub;
    }

    public void setParentPub(byte[] parentPub) {
        this.parentPub = parentPub;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public boolean isAccPwd() {
        return accPwd;
    }

    public void setAccPwd(boolean accPwd) {
        this.accPwd = accPwd;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

}
