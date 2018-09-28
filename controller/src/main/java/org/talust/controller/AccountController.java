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

package org.talust.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.talust.common.crypto.EncryptedData;
import org.talust.common.tools.ArithUtils;
import org.talust.common.tools.Configure;
import org.talust.common.tools.FileUtil;
import org.talust.core.core.ECKey;
import org.talust.core.core.SynBlock;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.server.NtpTimeService;
import org.talust.core.storage.AccountStorage;
import org.talust.core.storage.BlockStorage;
import org.talust.core.storage.TransactionStorage;

import java.io.*;
import java.util.Collection;

@RestController
@RequestMapping("/api/user")
@Api("帐户相关的Api")
@Slf4j
public class AccountController {

    @ApiOperation(value = "查询单一地址拥有的代币", notes = "查询拥有的代币")
    @PostMapping(value = "getCoins")
    JSONObject getCoins(@RequestParam String address) {
        JSONObject jsonObject = new JSONObject();
        Address addr = Address.fromBase58(MainNetworkParams.get(), address);
        if (AccountStorage.get().reloadCoin()) {
            long value = TransactionStorage.get().getBalanceAndUnconfirmedBalance(addr.getHash160())[0].value;
            long lockValue = TransactionStorage.get().getBalanceAndUnconfirmedBalance(addr.getHash160())[1].value;
            jsonObject.put("value", ArithUtils.div(value + "", "100000000", 8));
            jsonObject.put("lockValue", ArithUtils.div(lockValue + "", "100000000", 8));
        }
        return jsonObject;
    }


    @ApiOperation(value = "账户加密", notes = "账户加密")
    @PostMapping(value = "encryptWallet")
    JSONObject encryptWallet(@RequestParam String address, @RequestParam String password) {
        return AccountStorage.get().encryptWallet(password, address);
    }

    @ApiOperation(value = "查询全部地址拥有的代币", notes = "查询拥有的代币")
    @PostMapping(value = "getAllCoins")
    JSONObject getAllCoins() {
        JSONObject jsonObject = new JSONObject();
        long localbestheighttime= 0l;
        try{
            localbestheighttime   = BlockStorage.get().getBestBlockHeader().getBlockHeader().getTime();
        }catch (Exception e){
            jsonObject.put("msgCode", "E00001");
            return jsonObject;
        }

        long now = NtpTimeService.currentTimeSeconds();
        boolean syncheck =  true ;
        if (now - localbestheighttime > Configure.BLOCK_GEN_TIME) {
            syncheck =false;
        }
        Collection<Account> accountList =  AccountStorage.get().getAccountMap().values();
        if (null != accountList) {
            JSONArray accounts = new JSONArray();
            for (Account account : accountList) {
                JSONObject data = new JSONObject();
                if(syncheck){
                    if (AccountStorage.get().reloadCoin()) {
                        data.put("value", ArithUtils.div(account.getAddress().getBalance() + "", "100000000", 8));
                        data.put("lockValue", ArithUtils.div(account.getAddress().getUnconfirmedBalance() + "", "100000000", 8));
                    }
                }else{
                    data.put("value", "0");
                    data.put("lockValue", "0");
                }
                data.put("address",account.getAddress().getBase58());
                accounts.add(data);
            }
            jsonObject.put("data",accounts);
        } else {
            jsonObject.put("msgCode", "E00001");
        }
        return jsonObject;
    }

    @ApiOperation(value = "导入账户", notes = "导入账户")
    @PostMapping(value = "importAccountFile")
    JSONObject importAccountFile(@RequestParam String path) {
        JSONObject resp = new JSONObject();
        //验证文件是否存在
        if (null == path) {
            resp.put("retCode", "1");
            resp.put("msgCode", "E00002");
            return resp;
        }
        File file = new File(path);
        if (!file.exists()) {
            resp.put("retCode", "1");
            resp.put("msgCode", "E00003");
            return resp;
        }
        try {
            String content = FileUtil.fileToTxt(file);
            JSONObject fileJson = JSONObject.parseObject(content);
            Account account = Account.parse(fileJson.getBytes("data"), 0, MainNetworkParams.get());
            try {
                if(fileJson.getBoolean("isEncrypted")){
                    EncryptedData encryptedData =new EncryptedData(fileJson.getBytes("vector"),fileJson.getBytes("privateKey"));
                    ECKey ecKey = ECKey.fromEncrypted(encryptedData,fileJson.getBytes("publicKey"));
                    account.setEcKey(ecKey);
                }else{
                    account.resetKey();
                }
                if(!AccountStorage.get().importAccountFile(account,fileJson)){
                    resp.put("retCode", "1");
                    resp.put("msgCode", "E00005");
                    return resp;
                }
            } catch (Exception e) {
                log.warn("导入{}时出错", account.getAddress().getBase58(), e);
                resp.put("retCode", "1");
                resp.put("msgCode", "E00005");
                return resp;
            }
        } catch (IOException e) {
            resp.put("retCode", "1");
            resp.put("msgCode", "E00004");
            return resp;
        }catch (Exception e){
            resp.put("retCode", "1");
            resp.put("msgCode", "E00003");
            return resp;
        }
        resp.put("retCode", "0");
        resp.put("msgCode", "S00005");
        return resp;
    }


    @ApiOperation(value = "导出账户", notes = "导出账户")
    @PostMapping(value = "outPutAccountFile")
    JSONObject outPutAccountFile(@RequestParam String path, @RequestParam String address) {
        JSONObject resp = new JSONObject();
        try {
            File file = new File(path+ File.separator + address);
            if (!file.exists()) {
                file.createNewFile();
            }
            String accPath = Configure.DATA_ACCOUNT + File.separator + address;
            File oldFile = new File(accPath);
            String content = FileUtil.fileToTxt(oldFile);
            FileOutputStream fos = new FileOutputStream(file);
            JSONObject fileJson = JSONObject.parseObject(content);
            fos.write(fileJson.toJSONString().getBytes());
            fos.flush();
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        } catch (IOException e) {
            resp.put("retCode", "1");
            resp.put("msgCode", "E00006");
        }
        resp.put("retCode", "0");
        resp.put("msgCode", "S00006");
        return resp;
    }

    @ApiOperation(value = "移除账户", notes = "移除账户")
    @PostMapping(value = "removeAccount")
    JSONObject removeAccount(@RequestParam String address) {
        JSONObject resp = new JSONObject();
        if (null == address) {
            resp.put("retCode", "1");
            resp.put("msgCode", "E00007");
        }
        resp = AccountStorage.get().removeAccount(address);
        return resp;
    }

    @ApiOperation(value = "查询同步状态", notes = "查询同步状态")
    @PostMapping(value = "searchSyncStatus")
    JSONObject searchSyncStatus(){
        boolean isSync =  SynBlock.get().isSync();
        long maxHeight = SynBlock.get().getMaxHeight();
        long nowHeight = MainNetworkParams.get().getBestHeight();
        JSONObject resp =  new JSONObject();
        resp.put("syncStatus",isSync);
        resp.put("nowHeight",nowHeight);
        if(maxHeight<nowHeight){
            maxHeight = nowHeight;
        }
        resp.put("maxHeight",maxHeight);
        return resp;
    }
}
