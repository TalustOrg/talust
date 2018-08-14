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

import org.talust.common.crypto.Base58;
import org.talust.common.crypto.ECKey;
import org.talust.common.crypto.Utils;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.common.model.Coin;
import org.talust.core.network.MainNetworkParams;

import java.io.IOException;
import java.security.AccessControlContext;

public class AccountTest {


    NetworkParams networkParams = MainNetworkParams.get();
    public static void main(String[] args) {
        AccountTest at = new AccountTest();
        at.start();
    }

    protected byte getXor(byte[] body) {

        byte xor = 0x00;
        for (int i = 0; i < body.length; i++) {
            xor ^= body[i];
        }

        return xor;
    }

    private void start(){

        //non Root account create Test

        ECKey rootKey = new ECKey();
        Address rootAddr = Address.fromP2PKHash(networkParams, networkParams.getSystemAccountVersion(), Utils.sha256hash160(rootKey.getPubKey(false)));
        rootAddr.setBalance(Coin.ZERO);
        rootAddr.setUnconfirmedBalance(Coin.ZERO);
        Account rootAccount = new Account(networkParams);
        rootAccount.setPriSeed(rootKey.getPrivKeyBytes());
        rootAccount.setAccountType(rootAddr.getVersion());
        rootAccount.setAddress(rootAddr);
        rootAccount.setMgPubkeys(new byte[][]{rootKey.getPubKey(true)});
        try {
           // rootAccount.setSigns(rootAccount.signAccount(rootAccount.getEcKey(),null));
            rootAccount.signAccount(rootKey, null);
            System.out.println("address rootAccount haxString:"+rootAddr.getHashAsHex());
            System.out.println("data rootAccount: "+Base58.encode(rootAccount.serialize()));
        } catch (IOException e) {
            e.printStackTrace();
        }


        //root account create Test
        ECKey talustKey = new ECKey();
        Address talustAddr = Address.fromP2PKHash(networkParams, networkParams.getSystemAccountVersion(), Utils.sha256hash160(talustKey.getPubKey(false)));
        talustAddr.setBalance(Coin.ZERO);
        talustAddr.setUnconfirmedBalance(Coin.ZERO);
        Account talustAccount = new Account(networkParams);
        talustAccount.setPriSeed(talustKey.getPrivKeyBytes());
        talustAccount.setAccountType(talustAddr.getVersion());
        talustAccount.setAddress(talustAddr);
        talustAccount.setMgPubkeys(new byte[][]{talustKey.getPubKey(true)});
        try {
           // talustAccount.setSigns(talustAccount.signAccount(talustAccount.getEcKey(), rootAccount.getEcKey()));
            talustAccount.signAccount(talustKey, rootKey);
            System.out.println("address talustAccount haxString:"+talustAddr.getHashAsHex());
            System.out.println("data talustAccount: "+Base58.encode(talustAccount.serialize()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Account accountTest = Account.parse(rootAccount.serialize(),networkParams);
            Account accountTest2 = Account.parse(talustAccount.serialize(),networkParams);
            accountTest.verify();
            accountTest2.verify();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
