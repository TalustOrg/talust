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
package org.talust.core.core;

import org.talust.account.AccountType;
import org.talust.common.crypto.Base58;
import org.talust.common.crypto.Utils;
import org.talust.common.model.Coin;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.storage.AccountStorage;

import java.io.IOException;
import java.math.BigInteger;

public class AccountTest {


    NetworkParams networkParams = MainNetworkParams.get();
    public static void main(String[] args) {
        //解封下面一段代码进行 按需要的账户生成
      //  makeAccountFile(21);

        //解封下面两段代码进行root talust 账号生成
       AccountTest at = new AccountTest();
        at.gen();


    }

    protected byte getXor(byte[] body) {

        byte xor = 0x00;
        for (int i = 0; i < body.length; i++) {
            xor ^= body[i];
        }

        return xor;
    }
    //部分参数名称需要自己改动
    private void  gen(){
        //non Root account create Test
        ECKey rootKey = new ECKey();
        Address rootAddr = Address.fromP2PKHash(networkParams, networkParams.getSystemAccountVersion(), Utils.sha256hash160(rootKey.getPubKey(false)));
        rootAddr.setBalance(Coin.ZERO);
        rootAddr.setUnconfirmedBalance(Coin.ZERO);
        byte[] prikeySeedRoot = rootKey.getPubKey(false);
        Account rootAccount = new Account(networkParams);
        rootAccount.setPriSeed(prikeySeedRoot);
        rootAccount.setAccountType(rootAddr.getVersion());
        rootAccount.setAddress(rootAddr);
        rootAccount.setSupervisor(rootAccount.getAddress().getHash160() );
        rootAccount.setlevel(AccountType.ROOT.getType());
        String mgPw = "TALUST";//管理密码
        String trPw="TALUST";//交易密码
        //生成账户管理的私匙
        BigInteger mgPri1 = AccountTool.genPrivKey1(prikeySeedRoot, mgPw.getBytes());
        //生成交易的私匙
        BigInteger trPri1 = AccountTool.genPrivKey1(prikeySeedRoot, trPw.getBytes());

        BigInteger mgPri2 = AccountTool.genPrivKey2(prikeySeedRoot, mgPw.getBytes());
        ECKey mgkey1 = ECKey.fromPrivate(mgPri1);
        ECKey mgkey2 = ECKey.fromPrivate(mgPri2);

        ECKey trkey1 = ECKey.fromPrivate(trPri1);

        rootAccount.setMgPubkeys(new byte[][] {mgkey1.getPubKey(true), mgkey2.getPubKey(true)});	//存储帐户管理公匙
        rootAccount.setTrPubkeys(new byte[][] {trkey1.getPubKey(true)});//存储交易公匙
        try {
            rootAccount.setSigns(rootAccount.signAccount(rootAccount.getEcKey(),null));
            rootAccount.signAccount(rootKey, null);
            System.out.println("address rootAccount haxString:"+Base58.encode(rootAddr.getHash160()));
            System.out.println("address rootAccount privateKey:"+rootKey.getPrivKey());
            System.out.println("data rootAccount: "+Base58.encode(rootAccount.serialize()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //root account create Test
        ECKey talustKey = new ECKey();
        Address talustAddr = Address.fromP2PKHash(networkParams, networkParams.getSystemAccountVersion(), Utils.sha256hash160(talustKey.getPubKey(false)));
        talustAddr.setBalance(Coin.ZERO);
        talustAddr.setUnconfirmedBalance(Coin.ZERO);
        byte[] prikeySeedTalust = talustKey.getPubKey(false);
        Account talustAccount = new Account(networkParams);
        talustAccount.setPriSeed(prikeySeedTalust);
        talustAccount.setAccountType(talustAddr.getVersion());
        talustAccount.setAddress(talustAddr);
        talustAccount.setSupervisor(rootAccount.getAddress().getHash160() );
        talustAccount.setlevel(AccountType.TALUST.getType());
        String mgPwT = "TALUST";//管理密码
        String trPwT="TALUST";//交易密码
        //生成账户管理的私匙
        BigInteger mgPri1T = AccountTool.genPrivKey1(prikeySeedTalust, mgPwT.getBytes());
        //生成交易的私匙
        BigInteger trPri1T = AccountTool.genPrivKey1(prikeySeedTalust, trPwT.getBytes());

        BigInteger mgPri2T = AccountTool.genPrivKey2(prikeySeedTalust, mgPwT.getBytes());
        ECKey mgkey1T = ECKey.fromPrivate(mgPri1T);
        ECKey mgkey2T = ECKey.fromPrivate(mgPri2T);

        ECKey trkey1T = ECKey.fromPrivate(trPri1T);

        talustAccount.setMgPubkeys(new byte[][] {mgkey1T.getPubKey(true), mgkey2T.getPubKey(true)});	//存储帐户管理公匙
        talustAccount.setTrPubkeys(new byte[][] {trkey1T.getPubKey(true)});//存储交易公匙
        try {
            talustAccount.setSigns(talustAccount.signAccount(talustAccount.getEcKey(), rootAccount.getEcKey()));
            talustAccount.signAccount(talustKey, rootKey);
            System.out.println("address talustAccount haxString:"+Base58.encode(talustAddr.getHash160() ));
            System.out.println("address talustAccount privateKey:"+talustKey.getPrivKey());
            System.out.println("data talustAccount: "+Base58.encode(talustAccount.serialize()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void makeAccountFile(int num){
        for(int i = 0 ; i <num; i++){
            try {
                AccountStorage.get().createAccount();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
