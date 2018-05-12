package com.talust.chain.block.model;

import com.talust.chain.common.crypto.Sha256Hash;

import java.math.BigInteger;

/**
 * 平台相关的写死的一些账户的publickey
 */
public class PlatKey {

    //用于进行账号签名的根root的公钥,所有扔到链上的数据都必须得保证是该公钥对应的私钥签名了的
    public static final String ACCOUNT_ROOT_PUB = "025069e79f4e10a874867501b01386b4d16863310cb163d2a27864e7058a41e259";
    public static final String GENESIS_BLOCK = "iamgenesisblock";
    public static final int VERSION = 1;
    public static final long MAGIC = 66668888;//魔法数

    public interface MSG_TYPE {
        int GENESIS_CA = 1;//创世块中的CA证书
    }


    public static void main(String[] args) {
//        byte[] randSeed = getRandomSeed();
//        String rootpwd = "rootpassword";//根账号密码
//        BigInteger bigInteger = genPrivKey1(randSeed, rootpwd.getBytes());
//        ECKey ecKey = ECKey.fromPrivate(bigInteger);
//        byte[] pubKey = ecKey.getPubKey(true);
//        String encode = Hex.encode(pubKey);
//        byte[] decode = Hex.decode(encode);
//        System.out.println(decode);


//        byte[] content = "hello".getBytes();
//        ECKey ecKey = new ECKey();
//        Sha256Hash hash = Sha256Hash.of(content);
//        ECKey.ECDSASignature sign = ecKey.sign(hash);
//        byte[] bytes = sign.encodeToDER();
//
//        byte[] pubKey = ecKey.getPubKey(true);
//        ECKey ecKey1 = ECKey.fromPublicOnly(pubKey);
//
//        boolean verify = ecKey1.verify(hash.getBytes(), bytes);
//        System.out.println(verify);


    }

//    //生成一个随机的种子,每个账号在注册时,一方面会生成用户相关的公私钥对,同时还生成一个随机种子,以加强安全性
//    public static byte[] getRandomSeed() {
//        //本处使用写死密码的方式生成一个固定的公私钥对,主要是为了测试方便,实际应该是随机的
//        String mgPw = "randompwd";
//        BigInteger seedPriv = new BigInteger(1, Sha256Hash.hash(mgPw.getBytes()));
//        ECKey ecKey = ECKey.fromPrivate(seedPriv);
//        return ecKey.getPubKey(false);
//    }

    /**
     * 根据种子私匙，和密码，生成对应的帐户管理私匙或者交易私匙
     */
    public final static BigInteger genPrivKey1(byte[] priSeed, byte[] pw) {
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
