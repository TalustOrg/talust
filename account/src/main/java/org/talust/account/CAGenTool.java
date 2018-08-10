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

package org.talust.account;

import org.talust.common.crypto.ECKey;
import org.talust.common.crypto.Sha256Hash;

import java.math.BigInteger;

//生成CA证书相关类
public class CAGenTool {

    public static void main(String[] args) {
        ECKey ecKey = new ECKey();
        String privateKeyAsHex = ecKey.getPrivateKeyAsHex();
        String publicKeyAsHex = ecKey.getPublicKeyAsHex();
        System.out.println(privateKeyAsHex);
        System.out.println(publicKeyAsHex);


    }

    //签名公钥,用户父key签名自身公钥,以体现继承关系
//    public static byte[] signPub(ECKey parentKey,ECKey selfKey){
//        return parentKey.sign(selfKey.getPubKey());
//    }


    //根据密码生成ECKey对象
    private static ECKey genECKeyByPassword(String password) {
        BigInteger seedPriv = new BigInteger(1, Sha256Hash.hash(password.getBytes()));
        ECKey ecKey = ECKey.fromPrivate(seedPriv);
        return ecKey;
    }


    /**
     * 根据种子私匙，和密码，生成对应的帐户管理私匙或者交易私匙
     */
    public final static BigInteger genPrivKey(byte[] priSeed, byte[] pw) {
        byte[] privSeedSha256 = Sha256Hash.hash(priSeed);
        //取种子私匙的sha256 + pw的sha256，然后相加的结果再sha256为帐户管理的私匙
        byte[] pwSha256 = Sha256Hash.hash(pw);
        //把privSeedSha256 与 pwPwSha256 混合加密
        byte[] pwPriBytes = new byte[privSeedSha256.length + pwSha256.length];
        for (int i = 0; i < pwPriBytes.length; i += 2) {
            int index = i / 2;
            pwPriBytes[index] = privSeedSha256[index];
            pwPriBytes[index + 1] = pwSha256[index];
        }
        //生成账户管理的私匙
        return new BigInteger(1, Sha256Hash.hash(pwPriBytes));
    }

}
