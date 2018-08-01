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

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.talust.account.Account;
import org.talust.service.TransferAccountService;

/**
 * @author Axe-Liu
 * @date 2018/8/1.
 */
@Service("transferAccountService")
public class TransferAccountServiceImpl implements TransferAccountService {
    @Override
    public boolean decryptAccount(String password, Account account) {
        if(!validPassword(password)) {
            return false;
        }
        if(account == null){
            return false;
        }
        return false;
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
