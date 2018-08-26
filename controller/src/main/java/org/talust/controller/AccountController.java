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

import com.alibaba.fastjson.JSONObject;
import org.talust.ResponseMessage;
import org.talust.common.exception.AccountFileNotExistException;
import org.talust.common.exception.EncryptedExistException;
import org.talust.common.exception.ErrorPasswordException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.talust.common.tools.ArithUtils;
import org.talust.core.core.NetworkParams;
import org.talust.core.model.Address;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.storage.AccountStorage;
import org.talust.core.storage.TransactionStorage;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@Api("帐户相关的Api")
public class AccountController {

    @ApiOperation(value = "登录帐户", notes = "帐户信息已经存在的情况下,登录")
    @PostMapping(value = "login")
    ResponseMessage login() {
       List<String> addrs =new ArrayList<>();
        try {
//            AccountStorage.get().walletLogin();
        } catch (Exception e) {
            if (e instanceof ErrorPasswordException) {
                return ResponseMessage.error("登录的账户和密码不匹配,请确认后输入!");
            } else if (e instanceof EncryptedExistException) {
                return ResponseMessage.error("已经存在此前的账户信息与当前登录帐户不匹配!");
            } else if (e instanceof AccountFileNotExistException) {
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
        System.out.println(accPassword);
        String address = "";
        try {
//            address =   AccountStorage.get().createAccount(accPassword);
        } catch (Exception e) {
            if (e instanceof ErrorPasswordException) {
                return ResponseMessage.error("登录的账户和密码不匹配,请确认后输入!");
            } else if (e instanceof EncryptedExistException) {
                return ResponseMessage.error("已经存在此前的账户信息与当前登录帐户不匹配!");
            } else if (e instanceof AccountFileNotExistException) {
                return ResponseMessage.error("当前系统还未有用户,请先创建!");
            }
        }
        JSONObject json = new JSONObject();
        json.put("success", true);
        return ResponseMessage.ok(address);
    }

    @ApiOperation(value = "查询拥有的代币", notes = "查询拥有的代币")
    @PostMapping(value = "getCoins")
    JSONObject getCoins(@RequestParam String  address) {
        Address   addr = Address.fromBase58(MainNetworkParams.get(),address);
       long  value =  TransactionStorage.get().getBalanceAndUnconfirmedBalance(addr.getHash160())[0].value;
       long  lockValue =  TransactionStorage.get().getBalanceAndUnconfirmedBalance(addr.getHash160())[1].value;
       JSONObject   jsonObject = new JSONObject();
       jsonObject.put("value",ArithUtils.div(value+"" , "100000000",8));
       jsonObject.put("lockValue",ArithUtils.div(lockValue+"" , "100000000",8));
        return jsonObject  ;
    }


}
