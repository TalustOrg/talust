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

package org.talust.block.data;

import com.alibaba.fastjson.JSONObject;
import org.talust.account.Account;
import org.talust.account.AccountType;
import org.talust.block.model.TranType;
import org.talust.block.model.Transaction;
import org.talust.common.crypto.Hex;
import org.talust.common.model.Message;
import org.talust.common.model.MessageType;
import org.talust.common.tools.*;
import org.talust.common.crypto.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Genesis {
    private DataContainer dataContainer = DataContainer.get();

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
        Account rootAcc = new Account();
        rootAcc.setAccType(AccountType.ROOT.getType());
        rootAcc.setPublicKey(Hex.decode( CacheManager.get().get("ROOT_PK")));
        rootAcc.setAddress(Utils.getAddress(rootAcc.getPublicKey()));
        rootAcc.setParentPub(Hex.decode( CacheManager.get().get("ROOT_PK")));
        rootAcc.setParentSign(StringUtils.hexStringToBytes( CacheManager.get().get("ROOT_SIGN")));

        Transaction transaction = new Transaction();
        transaction.setTranType(TranType.ACCOUNT.getType());//设定为账户下发类型
        transaction.setDatas(SerializationUtil.serializer(rootAcc));//存储账户下发的具体数据

        Message message = new Message();
        message.setContent(SerializationUtil.serializer(transaction));
        message.setType(MessageType.TRANSACTION.getType());
        message.setTime(DateUtil.getTimeSecond());

        dataContainer.addRecord(SerializationUtil.serializer(message));
    }

    /**
     * 加入talust帐户
     */
    private void addTalustAccount() {
        Account rootAcc = new Account();
        rootAcc.setAccType(AccountType.TALUST.getType());
        rootAcc.setPublicKey(Hex.decode( CacheManager.get().get("TALUST_PK")));
        rootAcc.setAddress(Utils.getAddress(rootAcc.getPublicKey()));
        rootAcc.setParentPub(Hex.decode(CacheManager.get().get("ROOT_PK")));
        rootAcc.setParentSign(StringUtils.hexStringToBytes(CacheManager.get().get("TALUST_SIGN")));

        Transaction transaction = new Transaction();
        //设定为账户下发类型
        transaction.setTranType(TranType.ACCOUNT.getType());
        //存储账户下发的具体数据
        transaction.setDatas(SerializationUtil.serializer(rootAcc));

        Message message = new Message();
        message.setContent(SerializationUtil.serializer(transaction));
        message.setType(MessageType.TRANSACTION.getType());
        message.setTime(DateUtil.getTimeSecond());

        dataContainer.addRecord(SerializationUtil.serializer(message));
    }

    public static void main(String[] args) throws Exception{
//        Account rootAcc = new Account();
//        rootAcc.setAccType(AccountType.ROOT.getType());
//        rootAcc.setPublicKey(Hex.decode("02e1dd5dacdde736b4b0f7a9111198a86533f97a1f6b0ef9d265492e80c97bd2e4"));
//        rootAcc.setAddress(Utils.getAddress(rootAcc.getPublicKey()));
//
//        Transaction transaction = new Transaction();
//        transaction.setTranType(TranType.ACCOUNT.getType());//设定为账户下发类型
//        transaction.setDatas(SerializationUtil.serializer(rootAcc));//存储账户下发的具体数据
//
//        Sha256Hash hash = Sha256Hash.of(SerializationUtil.serializer(transaction));
//        String encode = Hex.encode(hash.getBytes());
//        System.out.println(encode);



//        Account talustAccount = new Account();
//        talustAccount.setAccType(AccountType.TALUST.getType());
//        talustAccount.setPublicKey(Hex.decode("02fd5cb45128a463bf204fa8ce3be33acee533788c8b4dd45e246656c45517b596"));
//        talustAccount.setAddress(Utils.getAddress(talustAccount.getPublicKey()));
//
//        Transaction transaction = new Transaction();
//        transaction.setTranType(TranType.ACCOUNT.getType());//设定为账户下发类型
//        transaction.setDatas(SerializationUtil.serializer(talustAccount));//存储账户下发的具体数据
//
//        byte[] hash = Sha256Hash.of(SerializationUtil.serializer(transaction)).getBytes();
//        String encode = Hex.encode(hash);
//        System.out.println(encode);



//        Account talustAccount = new Account();
//        talustAccount.setAccType(AccountType.MINING.getType());
//        talustAccount.setPublicKey(Hex.decode("032b7587a05552e0c6b9dd7e34eed680ec02a87483dccf85c4dd589d078b35a628"));
//        talustAccount.setAddress(Utils.getAddress(talustAccount.getPublicKey()));
//
//        Transaction transaction = new Transaction();
//        transaction.setTranType(TranType.ACCOUNT.getType());//设定为账户下发类型
//        transaction.setDatas(SerializationUtil.serializer(talustAccount));//存储账户下发的具体数据
//        byte[] hash = Sha256Hash.of(SerializationUtil.serializer(transaction)).getBytes();
//        String encode = Hex.encode(hash);
//        System.out.println(encode);



    }

}
