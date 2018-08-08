package org.talust.service.impl;/*
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

import com.alibaba.fastjson.JSONObject;
import io.netty.util.internal.StringUtil;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.talust.account.Account;
import org.talust.client.SynBlock;
import org.talust.common.crypto.AESEncrypt;
import org.talust.common.crypto.Base58;
import org.talust.common.crypto.Utils;
import org.talust.common.tools.ArithUtils;
import org.talust.common.tools.CacheManager;
import org.talust.core.transaction.TranType;
import org.talust.core.transaction.Transaction;
import org.talust.network.model.MyChannel;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.ConnectionManager;
import org.talust.core.server.NtpTimeService;
import org.talust.service.TransferAccountService;
import org.talust.storage.AccountStorage;

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Axe-Liu
 * @date 2018/8/1.
 */
@Service
public class TransferAccountServiceImpl implements TransferAccountService {
    private final static Lock locker = new ReentrantLock();
    @Override
    public boolean decryptAccount(String password, Account account) {
        if(!validPassword(password)) {
            return false;
        }
        if(account == null){
            return false;
        }
        try {
           AESEncrypt.decrypt( account.getPrivateKey(), password);
        } catch (InvalidCipherTextException e) {
            return false;
        }
        return true;
    }

    @Override
    public Account getAccountByAddress(String address) {
        return AccountStorage.get().getAccountByAddress(address);
    }

    @Override
    public JSONObject transfer(String toAddress, String money, String address, String password) {
        JSONObject resp =  new JSONObject();
        Utils.checkNotNull(toAddress);
        Collection<MyChannel> connects =ChannelContain.get().getMyChannels();
        if(connects.size()<=0){
            resp.put("retCode","1");
            resp.put("message","当前网络不可用，请稍后再尝试");
            return resp;
        }
        int height = CacheManager.get().getCurrentBlockHeight();
        int localbestheighttime = CacheManager.get().getCurrentBlockTime();
        if(height==0){
            if(SynBlock.get().getSyning().get()){
                resp.put("retCode","1");
                resp.put("message","正在同步区块中，请稍后再尝试");
                return resp;
            }else{
                ConnectionManager.get().init();
                resp.put("retCode","1");
                resp.put("message","当前网络不可用，正在重试网络和数据修复，请稍后再尝试");
                return resp;
            }
        }
        if(NtpTimeService.currentTimeMillis()-localbestheighttime>60){
            if(SynBlock.get().getSyning().get()) {
                resp.put("retCode","1");
                resp.put("message","正在同步区块中，请稍后再尝试");
                return resp;
            }else {
                ConnectionManager.get().init();
                resp.put("retCode","1");
                resp.put("message","当前网络不可用，正在重试网络和数据修复，请稍后再尝试");
                return resp;
            }
        }
        locker.lock();
        try{
            if(money.compareTo("0") <= 0) {
                resp.put("retCode", "1");
                resp.put("message", "发送的金额需大于0");
                return resp;
            }
            Account account = this.getAccountByAddress(address);
            if (null == account) {
                resp.put("retCode", "1");
                resp.put("message", "出账账户不存在");
                return resp;
            }
            if(account.getAddress().equals(Base58.decode(toAddress))){
                resp.put("retCode", "1");
                resp.put("message", "不能给自己转账");
                return resp;
            }
            if (account.isAccPwd()) {
                if (StringUtil.isNullOrEmpty(password)) {
                    resp.put("retCode", "1");
                    resp.put("message", "输入钱包密码进行转账");
                    return resp;
                } else {
                    boolean pswCorrect = this.decryptAccount(password, account);
                    if (!pswCorrect) {
                        resp.put("retCode", "1");
                        resp.put("message", "账户密码不正确");
                        return resp;
                    }
                }
            }
            //byte[] myAddress = account.getAddress();
            //当前余额可用余额
            String balance = account.getAmount();
            if(ArithUtils.compareStr(balance,money) < 0) {
                resp.put("retCode", "1");
                resp.put("message", "余额不足");
                return resp;
            }
            Transaction transaction = new Transaction();
            transaction.setTranType(TranType.TRANSFER.getType());
            //根据交易金额获取当前交易地址下所有的未花费交易
        }catch (Exception e ){

        }finally {
            locker.unlock();
        }
        //验证本账户金额与交易金额是否正常
        return resp;
    }

    /**
     * 校验密码难度
     * @param password
     * @return boolean
     */
    public static boolean validPassword(String password) {
        if(StringUtils.isEmpty(password)){
            return false;
        }
        if(password.length() < 6){
            return false;
        }
        if(password.matches("(.*)[a-zA-z](.*)") && password.matches("(.*)\\d+(.*)")){
            return true;
        } else {
            return false;
        }
    }
}
