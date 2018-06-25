package org.talust.chain.storage;

import org.talust.chain.account.Account;
import org.talust.chain.common.exception.AccountFileEmptyException;
import org.talust.chain.common.exception.AccountFileNotExistException;
import org.talust.chain.common.exception.EncryptedExistException;
import org.talust.chain.common.exception.ErrorPasswordException;
import org.talust.chain.common.tools.Configure;
import org.talust.chain.common.tools.FileUtil;
import org.talust.chain.common.tools.SerializationUtil;
import org.talust.chain.common.crypto.AESEncrypt;
import org.talust.chain.common.crypto.ECKey;
import org.talust.chain.common.crypto.Utils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.math.BigInteger;

@Slf4j //帐户存储服务器
public class AccountStorage {
    private static AccountStorage instance = new AccountStorage();

    private AccountStorage() {
    }

    public static AccountStorage get() {
        return instance;
    }

    private ECKey ecKey;
    private String accPath;
    private Account account;

    /**
     * 初始化帐户存储的路径
     *
     * @throws IOException
     */
    public void init() throws IOException {
        String dataDir = Configure.DATA_ACCOUNT;
//        String dataDir = "/app";//为了测试方便,先写死
//        String dataDir = "D:/account";//为了测试方便,先写死
        String filePath = dataDir;// Configure.DATA_ACCOUNT;
        File fp = new File(filePath);
        if (!fp.exists()) {
            fp.mkdirs();
        }

        filePath = filePath + File.separator + "account.dat";
        File accFile = new File(filePath);
        if (!accFile.exists()) {
            accFile.createNewFile();
        }
        this.accPath = filePath;
        log.info("帐户信息存储路径文件:{}", filePath);
    }

    /**
     * 判断当前是否已经存在帐户了,返回为真则表示此前已经存在一个帐户信息,是否是删除还是登录由用户自行决定
     *
     * @return
     */
    public boolean hadAccount() {
        File accFile = new File(accPath);
        if (accFile.exists()) {
            byte[] bytes = FileUtil.fileToBytes(accPath);
            if (bytes != null && bytes.length > 0) {//说明已经有账户信息
                return true;
            }
        }
        return false;
    }

    /**
     * 创建新帐户,会产生一对密钥对,以即会生成一个地址
     *
     * @param account
     * @throws Exception
     */
    public void createAccount(Account account) throws Exception {
        log.info("帐户保存路径为:{}", accPath);
        File accFile = new File(accPath);
        if (!accFile.exists()) {
            throw new AccountFileNotExistException();
        }
        //只要是创建,就会破坏性写入,之前的帐户信息将会被
        ecKey = new ECKey();
        String accPwd = account.getAccPwd();
        account.setAccPwd(null);
        account.setPublicKey(ecKey.getPubKey());
        byte[] privKeyBytes = ecKey.getPrivKeyBytes();
        byte[] encryptPkb = AESEncrypt.encrypt(privKeyBytes, accPwd);
        account.setPrivateKey(encryptPkb);
        account.setAddress(Utils.getAddress(ecKey.getPubKey()));
        byte[] serializer = SerializationUtil.serializer(account);
        FileUtil.bytesToFile(serializer, accPath);
        this.account = account;
    }

    /**
     * 登录
     *
     * @param acc
     * @throws Exception
     */
    public void login(Account acc) throws Exception {
        File file = new File(accPath);
        if (file.exists()) {
            byte[] bytes = FileUtil.fileToBytes(accPath);
            if (bytes != null && bytes.length > 0) {//说明已经有账户信息
                try {
                    account = SerializationUtil.deserializer(bytes, Account.class);
                    if (acc.getAccount().equals(account.getAccount())) {
                        byte[] decrypt = AESEncrypt.decrypt(account.getPrivateKey(), acc.getAccPwd());//解密
                        //解密成功,将密钥对信息放入ecKey对象中
                        ecKey = ECKey.fromPrivate(new BigInteger(decrypt));
                        acc.setAddress(account.getAddress());
                        account.setPrivateKey(null);
                    } else {
                        throw new EncryptedExistException();
                    }
                } catch (Exception e) {
                    throw new ErrorPasswordException();
                }
            } else {//有帐户文件,无帐户信息
                throw new AccountFileEmptyException();
            }
        } else {//帐户文件不存在,一般不可能有此种情况
            throw new AccountFileNotExistException();
        }
    }

    public ECKey getEcKey() {
        return ecKey;
    }


    public void setAccount(Account account) {
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }
}
