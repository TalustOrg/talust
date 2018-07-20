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
        ECKey ecKey = new ECKey();
       String  prk ="03f9ec5465755067f872d29ba6e9aa72fed73d5d2198e9e4a521bfa81b576b67c9";
       String psd = "Talust.2018";
        BigInteger bigInteger =  new BigInteger("zqtW1pM6/hkfdzw+s5zdg5ikO44Ytcj3FCjm12MMVVw6uvPzaqagdyPdboujYv1s".getBytes());
        byte[] sign =  ecKey.sign(Sha256Hash.of(AESEncrypt.encrypt(prk.getBytes(), psd)).getBytes(),bigInteger);
        System.out.println("publickey : "+ Base64.getEncoder().encodeToString(AESEncrypt.encrypt(prk.getBytes(), psd)));
        System.out.println("sign : "+Base64.getEncoder().encodeToString(sign));


        Sha256Hash hash = Sha256Hash.of(AESEncrypt.encrypt(prk.getBytes(), psd));
        boolean verify = ECKey.verify(hash.getBytes(),
                AESEncrypt.encrypt(prk.getBytes(), psd),
                "0300c65cdd05d851108c58ff7b572214109acc887abc765768cbf875ee93459c93".getBytes());
        System.out.println(verify);
    }
}
