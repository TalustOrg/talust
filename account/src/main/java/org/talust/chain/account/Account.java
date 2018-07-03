package org.talust.chain.account;

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
    @Tag(7)//帐号,用于用户使用客户端时生成密钥对有用
    private String account;
    @Tag(8)//帐号密码,用于用户使用客户端时生成密钥对有用
    private String accPwd;
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

    public String getAccPwd() {
        return accPwd;
    }

    public void setAccPwd(String accPwd) {
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
