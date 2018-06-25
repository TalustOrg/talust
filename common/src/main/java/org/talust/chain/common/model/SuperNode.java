package org.talust.chain.common.model;

/**
 * 超级节点信息
 */
public class SuperNode {
    //ip地址
    private String ip;
    //超级节点编号
    private int code;
    //超级节点的帐户地址
    private String address;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
