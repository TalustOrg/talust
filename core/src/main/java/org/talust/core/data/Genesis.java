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

package org.talust.core.data;

import lombok.extern.slf4j.Slf4j;
import org.talust.account.AccountType;
import org.talust.common.crypto.Base58;
import org.talust.common.crypto.Hex;
import org.talust.common.crypto.Utils;
import org.talust.common.model.Message;
import org.talust.common.model.MessageType;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.DateUtil;
import org.talust.common.tools.SerializationUtil;
import org.talust.common.tools.StringUtils;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;
import org.talust.core.data.DataContainer;
import org.talust.core.model.Account;
import org.talust.core.model.Block;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.transaction.Transaction;

import java.io.IOException;

@Slf4j
public class Genesis {
    private DataContainer dataContainer = DataContainer.get();
    private NetworkParams networkParams = MainNetworkParams.get();

    /**
     * 生成创世块所需要的内容
     * 目前考虑的创世块中,只含有根证书信息,其他信息都是从此开始的,根账户下发需要交易双方
     */
    public void genGenesisContent() {
        log.info("创建创世块内容,本处初始化了一些基础帐户信息...");
        addRootAccount();
        addTalustAccount();
    }

    /**
     * 加入根帐户
     */
    private void addRootAccount() {
        try {
            byte[] parseData = Base58.decode(CacheManager.get().get("ROOT_DATA"));
            Account root = Account.parse(parseData, networkParams);
            root.verify();
            Transaction transaction = new Transaction(networkParams);
            //设定为账户下发类型
            transaction.setType(Definition.TYPE_ACCOUNT);
            transaction.setData(parseData);
            //存储账户下发的具体数据
            dataContainer.addRecord(transaction);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加入talust帐户
     */
    private void addTalustAccount() {
        try {
            byte[] parseData = Base58.decode(CacheManager.get().get("TALUST_DATA"));
            Account talust = Account.parse(parseData, networkParams);
            talust.verify();
            Transaction transaction = new Transaction(networkParams);
            //设定为账户下发类型
            transaction.setType(Definition.TYPE_ACCOUNT);
            transaction.setData(parseData);
            //存储账户下发的具体数据
            dataContainer.addRecord(transaction);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
