package org.talust.chain.controller;

import com.alibaba.fastjson.JSONObject;
import org.talust.chain.ResponseMessage;
import org.talust.chain.account.Account;
import org.talust.chain.client.BlockChainServer;
import org.talust.chain.common.crypto.Utils;
import org.talust.chain.common.exception.AccountFileEmptyException;
import org.talust.chain.common.exception.EncryptedExistException;
import org.talust.chain.common.exception.ErrorPasswordException;
import org.talust.chain.storage.AccountStorage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@Api("帐户相关的Api")
public class AccountController {

    @ApiOperation(value = "登录帐户", notes = "帐户信息已经存在的情况下,登录")
    @PostMapping(value = "login")
    ResponseMessage login() {
        String addrs ="";
        try {
            addrs = AccountStorage.get().walletLogin();
            BlockChainServer.get().start();
        } catch (Exception e) {
            if (e instanceof ErrorPasswordException) {
                return ResponseMessage.error("登录的账户和密码不匹配,请确认后输入!");
            } else if (e instanceof EncryptedExistException) {
                return ResponseMessage.error("已经存在此前的账户信息与当前登录帐户不匹配!");
            } else if (e instanceof AccountFileEmptyException) {
                return ResponseMessage.error("当前系统还未有用户,请先创建!");
            }
            e.printStackTrace();
        }
        JSONObject json = new JSONObject();
        json.put("success", true);
        json.put("addrs", addrs);
        return ResponseMessage.ok(addrs);
    }

    @ApiOperation(value = "创建帐户", notes = "新创建帐户")
    @PostMapping(value = "register")
    ResponseMessage register(@RequestParam String  accPassword) {
        String address = "";
        try {
           address =   AccountStorage.get().createAccount(accPassword,0);
            BlockChainServer.get().start();
        } catch (Exception e) {
            if (e instanceof ErrorPasswordException) {
                return ResponseMessage.error("登录的账户和密码不匹配,请确认后输入!");
            } else if (e instanceof EncryptedExistException) {
                return ResponseMessage.error("已经存在此前的账户信息与当前登录帐户不匹配!");
            } else if (e instanceof AccountFileEmptyException) {
                return ResponseMessage.error("当前系统还未有用户,请先创建!");
            }
        }
        JSONObject json = new JSONObject();
        json.put("success", true);
        return ResponseMessage.ok(address);
    }

    @ApiOperation(value = "查看地址", notes = "查看当前登录用户的地址信息")
    @GetMapping(value = "showAddr", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseMessage showAddr() {
        List<Account> usrAccs = AccountStorage.get().getAccounts();
        if (usrAccs != null) {
            String addrs = "";
            for(Account account: usrAccs){
                addrs = addrs+Utils.showAddress(account.getAddress())+",";
            }
            return ResponseMessage.ok(addrs);
        }
        return ResponseMessage.error("当前无登录用户");
    }


}
