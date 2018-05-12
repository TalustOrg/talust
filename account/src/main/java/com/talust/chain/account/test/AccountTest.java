package com.talust.chain.account.test;

import com.talust.chain.common.crypto.Base58;
import com.talust.chain.common.crypto.ECKey;
import com.talust.chain.common.crypto.Hex;
import com.talust.chain.common.crypto.Utils;

import java.math.BigInteger;

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
        ECKey key = ECKey.fromPrivate(new BigInteger(Hex.decode("009afd62c8b3e6818e275385ba24195c453de3d2ad24fcee35d88072ab61ae1d7b")));
        String publicKeyAsHex = key.getPublicKeyAsHex();
        System.out.println(publicKeyAsHex);

        byte[] kk = Utils.getAddress(key.getPubKey());
        String s = Utils.showAddress(kk);
        System.out.println(s);

//        byte[] bytes = Utils.sha256hash160(key.getPubKey());
//        byte xor = getXor(bytes);
//        byte[] base58bytes = new byte[23];
//        System.arraycopy(bytes, 0, base58bytes, 0, bytes.length);
//        base58bytes[bytes.length] = xor;
//        base58bytes[21] = xor;
//        base58bytes[22] = xor;
//
//        String addr = Base58.encode(base58bytes);
//        System.out.println(addr);


//        ECKey key = ECKey.fromPrivate(new BigInteger(Hex.decode("009afd62c8b3e6818e275385ba24195c453de3d2ad24fcee35d88072ab61ae1d7b")));
//        ECKey rootKey = ECKey.fromPrivate(new BigInteger(Hex.decode("371e83844fea712340bb19f70f3fba0e126d58317e1a3b6e13ddc7d4fc4b3ba7")));
//        Sha256Hash hash = Sha256Hash.of(key.getPubKey());
//        ECKey.ECDSASignature sign = rootKey.sign(hash);
//        byte[] sign_bytes = sign.encodeToDER();
//        System.out.println(sign_bytes.length);
//        boolean verify = ECKey.verify(hash.getBytes(), sign_bytes, rootKey.getPubKey());
//        System.out.println(verify);


//        ECKey ecKey = new ECKey();
//        System.out.println(ecKey.getPublicKeyAsHex());
//        System.out.println(ecKey.getPrivateKeyAsHex());
//        String encode = Base58.encode(Utils.sha256hash160(ecKey.getPubKey()));
//        System.out.println(encode);
        //System.out.println(Utils.getAddress(ecKey.getPubKey()));

    }

}
