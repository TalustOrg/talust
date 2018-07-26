package org.talust.account.test;/*
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

import org.spongycastle.crypto.InvalidCipherTextException;
import org.talust.common.crypto.*;

import java.math.BigInteger;
import java.util.Base64;

public class CreateMiningAccount {

    public static void main(String[]  arg ) throws InvalidCipherTextException {
        ECKey ecKey = ECKey.fromPublicOnly(Hex.decode("6d686158f2930e25035c92edced7c2e436d441141d22c0bdd8af7e70c73c8909"));
//        byte[] sign =  ecKey.sign(Sha256Hash.of(AESEncrypt.encrypt(prk.getBytes(), psd)).getBytes(),bigInteger);
//        System.out.println("publickey : "+ Base64.getEncoder().encodeToString(AESEncrypt.encrypt(prk.getBytes(), psd)));
//        System.out.println("sign : "+Base64.getEncoder().encodeToString(sign));
    }
}
