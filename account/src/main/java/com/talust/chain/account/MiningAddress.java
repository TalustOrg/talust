package com.talust.chain.account;

import io.protostuff.Tag;

import java.util.List;

/**
 * 挖矿收益地址
 */
public class MiningAddress {
    @Tag(1)
    private List<String> address;

    public List<String> getAddress() {
        return address;
    }

    public void setAddress(List<String> address) {
        this.address = address;
    }
}
