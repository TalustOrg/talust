package org.talust.service;

import com.alibaba.fastjson.JSONObject;

/**
 * @author Axe-Liu
 * @date 2018/8/1.
 */
public interface TransferAccountService {
//    boolean decryptAccount(String password , Account account);
//    Account getAccountByAddress(String address);
    JSONObject transfer(String toAddress, String money, String address, String password);
}
