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

@RestController
@RequestMapping("/api/user")
@Api("帐户相关的Api")
public class AccountController {

    @ApiOperation(value = "登录帐户", notes = "帐户信息已经存在的情况下,登录")
    @PostMapping(value = "login", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseMessage login(@RequestBody Account account) {
        Account usrAcc = AccountStorage.get().getAccount();
        if (usrAcc != null) {//说明用户已经登录了
            return ResponseMessage.error("当前系统已启用!");
        }

        try {
            AccountStorage.get().login(account);
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
        return ResponseMessage.ok("登录成功,服务已启动!");
    }

    @ApiOperation(value = "创建帐户", notes = "新创建帐户")
    @PostMapping(value = "register", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseMessage register(@RequestBody Account account) {
        Account usrAcc = AccountStorage.get().getAccount();
        if (usrAcc != null) {//说明用户已经登录了
            return ResponseMessage.error("当前系统已启用!");
        }

        try {
            AccountStorage.get().createAccount(account);
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
        return ResponseMessage.ok("创建成功,服务已启动!");
    }

    @ApiOperation(value = "查看地址", notes = "查看当前登录用户的地址信息")
    @GetMapping(value = "showAddr", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseMessage showAddr() {
        Account usrAcc = AccountStorage.get().getAccount();
        if (usrAcc != null) {//说明用户已经登录了
            return ResponseMessage.ok(Utils.showAddress(usrAcc.getAddress()));
        }
        return ResponseMessage.error("当前无登录用户");
    }


}
