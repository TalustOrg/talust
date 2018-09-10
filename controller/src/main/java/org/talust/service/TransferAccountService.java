package org.talust.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.talust.common.model.Deposits;
import org.talust.core.model.Account;

/**
 * @author Axe-Liu
 * @date 2018/8/1.
 */
public interface TransferAccountService {
    boolean decryptAccount(String password , Account account);
    Account getAccountByAddress(String address);
    JSONObject transfer(String toAddress, String money, String address, String password);
    Deposits getDeposits(byte[] hash160);
    JSONArray getAllDeposits();
    JSONObject consensusJoin(String nodeAddress, String money, String address, String password);
    JSONObject consensusLeave(String nodeAddress, String address, String password);
    JSONObject searchAllTransfer(String address);
    JSONObject searchAllCoinBaseTransfer(String address,String date);
    JSONObject searchAddressConsensusStatus(String address);
}
