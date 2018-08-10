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
package org.talust.account.test;

import org.talust.common.crypto.*;
import org.talust.common.tools.StringUtils;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;

public class AccountTest {

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
        ECKey key = ECKey.fromPrivate(new BigInteger(Hex.decode("00c9f079f45601872d6d2ae9425c2a1245202db3f19d2d6c6f0ae1bde9618f2f30")));
//        ECKey ecKey = new ECKey();
//        System.out.println("public key:"+Hex.encode(ecKey.getPubKey()));
//        System.out.println("private key :"+ecKey.getPrivateKeyAsHex());
//        Sha256Hash hash = Sha256Hash.of(ecKey.getPubKey());
//        byte[] sign_bytes = key.sign(hash.getBytes());
//        System.out.println("sign : "+ StringUtils.bytesToHexString(sign_bytes));
//        byte[] address = Utils.getAddress(ecKey.getPubKey());
//        String addr = StringUtils.bytesToHexString(address);
//        System.out.println("address haxString:"+addr);
//        System.out.println("address haxString to  bytes and Base58:"+Base58.encode(StringUtils.hexStringToBytes(addr)));
//        System.out.println("address Base58:"+Base58.encode(address));
//        boolean verify = ECKey.verify(hash.getBytes(), sign_bytes, key.getPubKey());
//        System.out.println(verify);

        //root account create Test
//        ECKey ecKey = new ECKey();
//        System.out.println("public key:"+Hex.encode(ecKey.getPubKey()));
//        System.out.println("private key :"+ecKey.getPrivateKeyAsHex());
//        Sha256Hash hash = Sha256Hash.of(ecKey.getPubKey());
//        byte[] sign_bytes = ecKey.sign(hash.getBytes());
//        System.out.println("sign : "+ StringUtils.bytesToHexString(sign_bytes));
//        byte[] actualChecksum = Arrays.copyOfRange(Sha256Hash.hashTwice(Utils.getAddress(ecKey.getPubKey())), 0, 4);
//        byte[] address = Utils.addBytes(Utils.getAddress(ecKey.getPubKey()),actualChecksum);
//        String addr = StringUtils.bytesToHexString(address);
//        System.out.println("address haxString:"+addr);
//        System.out.println("address haxString to  bytes and Base58:"+Base58.encode(StringUtils.hexStringToBytes(addr)));
//        System.out.println("address Base58:"+Base58.encode(address));
//        boolean verify = ECKey.verify(hash.getBytes(), sign_bytes, ecKey.getPubKey());
//        System.out.println(verify);
    }

}
