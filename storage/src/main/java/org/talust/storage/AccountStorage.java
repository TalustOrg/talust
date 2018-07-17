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

package org.talust.storage;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.talust.account.Account;
import org.talust.common.crypto.AESEncrypt;
import org.talust.common.crypto.ECKey;
import org.talust.common.crypto.Utils;
import org.talust.common.exception.AccountFileEmptyException;
import org.talust.common.exception.AccountFileNotExistException;
import org.talust.common.exception.ErrorPasswordException;
import org.talust.common.tools.Configure;
import org.talust.common.tools.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
    private List<Account> accounts = new ArrayList<>();
    private Account account ;

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
     * @throws Exception
     */
    public  String  createAccount(String accPs,int accType) throws Exception{
        Account account = new Account();
        ecKey = new ECKey();
        JSONObject fileJson =  new JSONObject();
        if(null==accPs){
            accPs="";
            fileJson.put("Crypto","false");
        }else{
            fileJson.put("Crypto","true");
        }
        account.setAccPwd(accPs);
        if(accType == 0){
            account.setAccType(7);
            fileJson.put("t",7);
        }else{
            account.setAccType(accType);
            fileJson.put("t",accType);
        }
        account.setPublicKey(ecKey.getPubKey());
        byte[] privKeyBytes = ecKey.getPrivKeyBytes();
        byte[] encryptPkb = AESEncrypt.encrypt(privKeyBytes, accPs);
        byte[] address = Utils.getAddress(ecKey.getPubKey());
        account.setPrivateKey(encryptPkb);
        account.setAddress(address);
        fileJson.put("version",getVersion() );
        fileJson.put("privateKey",encryptPkb);
        fileJson.put("address", Utils.showAddress(address));
        String  accPath = filePath + File.separator +Utils.showAddress(account.getAddress());
        log.info("Account save path :{}", accPath);
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
            }
        }
        accounts.add(account);
        return Utils.showAddress(address);
    }


    public  String  createAccount(String accPs) throws Exception {
        return createAccount(accPs,0);
    }

    public String getVersion() throws IOException {
        return Account.class.getProtectionDomain().getCodeSource().getLocation().getFile().split("account")[1].split(".jar")[0].substring(1);
    }
    /**
     * walletLogin
     * @throws Exception
     */
    public String walletLogin() throws Exception {
        List<String> list = getAllFile(filePath,true);
        String addrs = "";
        for(String path : list ){
            File file = new File(path);
            if (file.exists()) {
                String content = FileUtil.fileToTxt(file);
                if (content != null && content.length() > 0) {//说明已经有账户信息
                    try {
                        JSONObject fileJson = JSONObject.parseObject(content);
                        addrs= addrs + fileJson.get("address") +",";
                        //TODO less amount total
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
        return addrs;
    }
    /**
     * accountLogin
     */
    public Account accountLogin(String filePath , String accPassword) throws Exception {
        File file =  new File(filePath);
        if(file.exists()){
            String content = FileUtil.fileToTxt(file);
            if (content != null && content.length() > 0) {
                try {
                    JSONObject fileJson = JSONObject.parseObject(content);
                    if(null==accPassword){
                        accPassword="";
                    }
                    byte[] decrypt = AESEncrypt.decrypt( fileJson.getBytes("privateKey"), accPassword);
                    account = new Account();
                    account.setPublicKey(fileJson.getBytes("privateKey"));
                    account.setPrivateKey(decrypt);
                    account.setAccType(fileJson.getInteger("t"));
                    account.setAddress(fileJson.getBytes("address"));
                    account.setAccPwd(accPassword);
                    //解密成功,将密钥对信息放入ecKey对象中
                    ecKey = ECKey.fromPrivate(new BigInteger(decrypt));
                    //TODO less amount total
                    //TODO  find this addr  in accountStorage , if this aaddr is exist and  the blockHeght is the highest , so this value is the real amount
                    //TODO  if we can't find this addr in accountStorage , we need to  reload the blocks , and get all addr to reload  this addr's amount and save into accountStorage
                } catch (Exception e) {
                    throw new ErrorPasswordException();
                }
            } else {
                throw new AccountFileEmptyException();
            }
        }
        return account;
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


    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }
}
