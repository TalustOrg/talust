package org.talust.chain.account;

import org.talust.chain.common.crypto.ECKey;
import org.talust.chain.common.crypto.Sha256Hash;

import java.math.BigInteger;

//生成CA证书相关类
public class CAGenTool {

    public static void main(String[] args) {
//        ECKey rootKey = genRootCa();
//        ECKey genesisKey = genGenesisCa();
//        signGenesisCa(rootKey,genesisKey);


        ECKey ecKey = new ECKey();
        String privateKeyAsHex = ecKey.getPrivateKeyAsHex();
        String publicKeyAsHex = ecKey.getPublicKeyAsHex();
        System.out.println(privateKeyAsHex);
        System.out.println(publicKeyAsHex);


    }

    //签名公钥,用户父key签名自身公钥,以体现继承关系
    public static byte[] signPub(ECKey parentKey,ECKey selfKey){
        return parentKey.sign(selfKey.getPubKey());
    }

//
//    //生成根证书
//    private static ECKey genRootCa() {
//        //设置两重密码主要是为了安全
//        String password1 = "root.password1";//根用户密码1
//        String password2 = "root.password2";//根用户密码2
//        ECKey ecKey1 = genECKeyByPassword(password1);
//        ECKey ecKey2 = genECKeyByPassword(password2);
////        BigInteger privKey = genPrivKey(ecKey1.getPubKey(false), ecKey2.getPubKey(false));
////        ECKey ecKey = ECKey.fromPrivate(privKey);
//        //String publicKeyAsHex = ecKey.getPublicKeyAsHex();
//        //System.out.println(publicKeyAsHex);
//        //03fecab6625b15d40d3245a17fd1a367ed1c17511e7f665b6a951d5b470deb200e   //这是根证书的公钥
//        return ecKey;
//    }

//    //生成创世块公钥
//    private static ECKey genGenesisCa() {
//        String password1 = "genesis.password1";//创世块帐户密码1
//        String password2 = "genesis.password2";//创世块帐户密码2
//        ECKey ecKey1 = genECKeyByPassword(password1);
//        ECKey ecKey2 = genECKeyByPassword(password2);
//        BigInteger privKey = genPrivKey(ecKey1.getPubKey(false), ecKey2.getPubKey(false));
//        ECKey ecKey = ECKey.fromPrivate(privKey);
//        String publicKeyAsHex = ecKey.getPublicKeyAsHex();
//        System.out.println(publicKeyAsHex);
//        //029736b6e388cb44f436a5531474c294eced77c368ea2ed5638a97665c0ecf279c   //这是创世块帐户的公钥
//        return ecKey;
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
