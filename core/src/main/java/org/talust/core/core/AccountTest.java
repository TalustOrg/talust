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
import org.talust.core.model.Coin;
import org.talust.core.network.MainNetworkParams;

import java.io.IOException;

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
        ECKey key = new ECKey();
        Address address = Address.fromP2PKHash(networkParams, networkParams.getSystemAccountVersion(), Utils.sha256hash160(key.getPubKey(false)));
        address.setBalance(Coin.ZERO);
        address.setUnconfirmedBalance(Coin.ZERO);
        Account account = new Account(networkParams);
        account.setPriSeed(key.getPrivKeyBytes());
        account.setAccountType(address.getVersion());
        account.setAddress(address);
        account.setMgPubkeys(new byte[][]{key.getPubKey(true)});
        try {
            account.setSigns(account.signAccount(account.getEcKey(),null));
            account.signAccount(key, null);
            System.out.println("address haxString:"+address.getHashAsHex());
            System.out.println("data: "+Base58.encode(account.serialize()));
        } catch (IOException e) {
            e.printStackTrace();
        }


        //root account create Test
        ECKey key2 = new ECKey();
        Address address2 = Address.fromP2PKHash(networkParams, networkParams.getSystemAccountVersion(), Utils.sha256hash160(key2.getPubKey(false)));
        address2.setBalance(Coin.ZERO);
        address2.setUnconfirmedBalance(Coin.ZERO);
        Account account2 = new Account(networkParams);
        account2.setPriSeed(key2.getPrivKeyBytes());
        account2.setAccountType(address2.getVersion());
        account2.setAddress(address2);
        account2.setMgPubkeys(new byte[][]{key2.getPubKey(true)});
        try {
            account2.setSigns(account2.signAccount(account2.getEcKey(), account.getEcKey()));
            System.out.println("address2 haxString:"+address2.getHashAsHex());
            System.out.println("data2: "+Base58.encode(account2.serialize()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
