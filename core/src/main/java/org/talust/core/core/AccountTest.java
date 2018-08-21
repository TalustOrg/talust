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
            System.out.println("address rootAccount Base58:"+rootAddr.getBase58());
            System.out.println("address rootAccount decode check en Base58:"+Base58.decodeChecked(rootAddr.getBase58()));
            System.out.println("address rootAccount hash160:"+rootAddr.getHash160());
            System.out.println("address rootAccount hash:"+rootAddr.getHash());
            System.out.println("data rootAccount: "+Base58.encode(rootAccount.serialize()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
