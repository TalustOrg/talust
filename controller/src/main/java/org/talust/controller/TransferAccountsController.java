package org.talust.controller;/*
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.talust.ResponseMessage;
import org.talust.account.Account;
import org.talust.common.crypto.Base58;
import org.talust.common.crypto.Utils;
import org.talust.common.tools.ArithUtils;
import org.talust.service.TransferAccountService;
import org.talust.storage.AccountStorage;

import java.util.List;

/**
 * @author Axe-Liu
 * @date 2018/7/31.
 */
@RestController
@RequestMapping("/api/transfer")
@Api("转账相关API")
public class TransferAccountsController {
    @Autowired
    private TransferAccountService transferAccountService;

    @ApiOperation(value = "发起转账", notes = "帐户信息已经存在的情况下,转账")
    @PostMapping(value = "tansfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    JSONObject tansfer(@RequestParam String toAddress, @RequestParam String money, @RequestParam String address, @RequestParam String password) {
        JSONObject resp = new JSONObject();
        if (StringUtil.isNullOrEmpty(toAddress) || StringUtil.isNullOrEmpty(money)) {
            resp.put("retCode", "1");
            resp.put("message", "核心参数缺失");
            return resp;
        }
        try {
            money  = ArithUtils.mul(money, "1", 8);
        } catch (Exception e) {
            resp.put("retCode", "1");
            resp.put("message", "金额不正确");
            return resp;
        }
        Account account = transferAccountService.getAccountByAddress(address);
        if (null == account) {
            resp.put("retCode", "1");
            resp.put("message", "出账账户不存在");
            return resp;
        }
        try {
            Base58.decodeChecked(toAddress);
        } catch (Exception e) {
            resp.put("retCode", "1");
            resp.put("message", "目标账户验证失败");
            return resp;
        }
        if (account.isAccPwd()) {
            if (StringUtil.isNullOrEmpty(password)) {
                resp.put("retCode", "1");
                resp.put("message", "输入钱包密码进行转账");
                return resp;
            } else {
                boolean pswCorrect = transferAccountService.decryptAccount(password, account);
                if (!pswCorrect) {
                    resp.put("retCode", "1");
                    resp.put("message", "账户密码不正确");
                    return resp;
                }
            }
        }
        JSONObject isOk = transferAccountService.transfer(toAddress,money,address,password);

        return null;
    }

    public TransferAccountService getTransferAccountService() {
        return transferAccountService;
    }

    public void setTransferAccountService(TransferAccountService transferAccountService) {
        this.transferAccountService = transferAccountService;
    }
}
