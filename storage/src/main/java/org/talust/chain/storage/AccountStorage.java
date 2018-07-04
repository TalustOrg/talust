package org.talust.chain.storage;

import com.alibaba.fastjson.JSONObject;
import org.talust.chain.account.Account;
import org.talust.chain.common.crypto.*;
import org.talust.chain.common.exception.AccountFileEmptyException;
import org.talust.chain.common.exception.AccountFileNotExistException;
import org.talust.chain.common.exception.ErrorPasswordException;
import org.talust.chain.common.tools.Configure;
import org.talust.chain.common.tools.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.math.BigInteger;
import java.util.*;

@Slf4j //帐户存储服务器
public class AccountStorage {
    private static AccountStorage instance = new AccountStorage();

    private AccountStorage() {
    }

    public static AccountStorage get() {
        return instance;
    }

    private ECKey ecKey;
    private String filePath = Configure.DATA_ACCOUNT;
    private Account account;

    /**
     * 初始化帐户存储的路径
     *
     * @throws IOException
     */
    public void init() throws IOException {
        File fp = new File(filePath);
        if (!fp.exists()) {
            fp.mkdirs();
        }
        log.info("帐户信息存储路径文件:{}", filePath);
    }

    /**
     * 创建新帐户,会产生一对密钥对,以即会生成一个地址。
     * 缺少无密码账户创建。
     * @throws Exception
     */
    public  void createAccount(String accPs) throws Exception{
        Account account = new Account();
        account.setAccPwd(accPs);
        ecKey = new ECKey();
        account.setPublicKey(ecKey.getPubKey());
        byte[] privKeyBytes = ecKey.getPrivKeyBytes();
        byte[] encryptPkb = AESEncrypt.encrypt(privKeyBytes, accPs);
        account.setPrivateKey(encryptPkb);
        account.setAddress(Utils.getAddress(ecKey.getPubKey()));
        JSONObject fileJson =  new JSONObject();
        fileJson.put("version",getVersion());
        fileJson.put("privateKey",encryptPkb);
        fileJson.put("address", Utils.showAddress(account.getAddress()));
        String  accPath = filePath + File.separator +Utils.showAddress(account.getAddress());
        log.info("帐户保存路径为:{}", accPath);
        File accFile = new File(accPath);
        if (!accFile.exists()) {
            accFile.createNewFile();
        }
        FileOutputStream  fos= new FileOutputStream(accFile);
        fos.write(fileJson.toJSONString().getBytes());
        fos.flush();
        if (fos!=null) {
            try {
                fos.close();
            } catch (IOException e) {
                System.err.println("文件流关闭失败");
            }
        }
        this.account = account;
    }

    public String getVersion() throws IOException {
        return Account.class.getProtectionDomain().getCodeSource().getLocation().getFile().split("account")[1].split(".jar")[0].substring(1);
    }
    /**
     * 默认登录
     *
     * @param acc
     * @throws Exception
     */
    public void login(Account acc) throws Exception {
        List<String> list = getAllFile(filePath,true);
        File file = new File(list.get(0));
        if (file.exists()) {
            String bytes = FileUtil.fileToTxt(file.getPath());
            if (bytes != null && bytes.length() > 0) {//说明已经有账户信息
                try {
                    JSONObject fileJson = JSONObject.parseObject(bytes);
                    byte[] decrypt = AESEncrypt.decrypt( fileJson.getBytes("privateKey"), acc.getAccPwd());//解密
                    //解密成功,将密钥对信息放入ecKey对象中
                    ecKey = ECKey.fromPrivate(new BigInteger(decrypt));
                    acc.setAddress(Utils.deShowAddress((String) fileJson.get("address")));
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
    /**
     * 导入文件登录
     *
     * @param acc
     * @throws Exception
     */

    /**
     * 获取路径下的所有文件/文件夹
     */
    public static List<String> getAllFile(String directoryPath,boolean isAddDirectory) {
        List<String> list = new ArrayList<String>();
        File baseFile = new File(directoryPath);
        if (baseFile.isFile() || !baseFile.exists()) {
            return list;
        }
        File[] files = baseFile.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if(isAddDirectory){
                    list.add(file.getAbsolutePath());
                }
                list.addAll(getAllFile(file.getAbsolutePath(),isAddDirectory));
            } else {
                list.add(file.getAbsolutePath());
            }
        }
        return list;
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
