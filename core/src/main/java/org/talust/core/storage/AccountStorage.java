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

package org.talust.core.storage;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.spongycastle.asn1.cms.ecc.ECCCMSSharedInfo;
import org.talust.common.crypto.*;
import org.talust.common.tools.Configure;
import org.talust.common.tools.FileUtil;
import org.talust.common.tools.StringUtils;
import org.talust.core.core.NetworkParams;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.core.model.Coin;
import org.talust.core.network.MainNetworkParams;
import org.talust.storage.BaseStoreProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Slf4j //帐户存储服务
public class AccountStorage {

    private Account account;
    private List<Account> accountList = new ArrayList<Account>();
    private MainNetworkParams network = MainNetworkParams.get();
    private static AccountStorage instance = new AccountStorage();

    private AccountStorage() {
        init(Configure.DATA_ACCOUNT);
    }

    public static AccountStorage get() {
        return instance;
    }

    /**
     * 初始化帐户存储的路径
     *
     * @throws IOException
     */
    public void init(String filePath) {
        File fp = new File(filePath);
        if (!fp.exists()) {
            fp.mkdirs();
        }
        log.info("帐户信息存储路径文件:{}", filePath);
    }

    /**
     * 创建新帐户,会产生一对密钥对,以即会生成一个地址。
     *
     * @throws Exception
     */
    public Address createAccount() throws Exception {
        ECKey key = new ECKey();
        Address address = Address.fromP2PKHash(network, network.getSystemAccountVersion(), Utils.sha256hash160(key.getPubKey(false)));
        address.setBalance(Coin.ZERO);
        address.setUnconfirmedBalance(Coin.ZERO);
        account = new Account(network);
        account.setPriSeed(key.getPrivKeyBytes());
        account.setAccountType(address.getVersion());
        account.setAddress(address);
        account.setMgPubkeys(new byte[][]{key.getPubKey(true)});
        account.signAccount(key, null);
        String accPath = Configure.DATA_ACCOUNT + File.separator + account.getAddress().getBase58();
        log.info("Account save path :{}", accPath);
        File accFile = new File(accPath);
        if (!accFile.exists()) {
            accFile.createNewFile();
        }
        byte[] data = account.serialize();
        JSONObject fileJson = new JSONObject();
        fileJson.put("data", data);
        fileJson.put("address", account.getAddress().getBase58());
        fileJson.put("privateKey", account.getPriSeed());
        fileJson.put("isEncrypted", account.isEncrypted());
        FileOutputStream fos = new FileOutputStream(accFile);
        fos.write(fileJson.toJSONString().getBytes());
        fos.flush();
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
            }
        }
        account.setEcKey(key);
        accountList.add(account);
        return address;
    }

    /**
     * walletLogin
     *
     * @throws Exception
     */
    public void nomorlNodeLogin() {
        List<String> list = getAllFile(Configure.DATA_ACCOUNT, true);
        if (list == null || list.size() == 0) {
            try {
                createAccount();
            } catch (Exception e) {
                log.warn("创建账户时出错！");
            }
        } else {
            try{
                for (String path : list) {
                    File file = new File(path);
                    if (file.exists()) {
                        String content = FileUtil.fileToTxt(file);
                        //说明已经有账户信息
                        if (content != null && content.length() > 0) {
                            JSONObject fileJson = JSONObject.parseObject(content);
                            account = Account.parse(fileJson.getBytes("data"), network);
                            try {
                                account.verify();
                                accountList.add(account);
                            } catch (Exception e) {
                                log.warn("默认登陆{}时出错", account.getAddress().getBase58(), e);
                            }
                        }
                    }
                }
            }catch (Exception e){
                log.warn("账户文件路径读取错误");
            }
        }

    }
//
//    /**
//     * 导入文件登录
//     *
//     * @throws Exception
//     */
//    public Account accountLogin(String filePath, String accPassword) throws ErrorPasswordException, AccountFileEmptyException {
//        File file = new File(filePath);
//        if (file.exists()) {
//            String content = null;
//            try {
//                content = FileUtil.fileToTxt(file);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            if (content != null && content.length() > 0) {
//                JSONObject fileJson = JSONObject.parseObject(content);
//                if (null == accPassword) {
//                    accPassword = "";
//                }
//                byte[] decrypt = new byte[0];
//                try {
//                    decrypt = AESEncrypt.decrypt(Hex.decode(fileJson.getString("privateKey")), accPassword);
//                } catch (InvalidCipherTextException e) {
//                    throw new ErrorPasswordException();
//                }
//                //解密成功,将密钥对信息放入ecKey对象中
//                ecKey = ECKey.fromPrivate(new BigInteger(decrypt));
//                account = new Account();
//                account.setPublicKey(ecKey.getPubKey());
//                account.setPrivateKey(decrypt);
//                account.setAccType(fileJson.getInteger("t"));
//                String fileAddress = fileJson.getString("address");
//                byte[] address = new byte[0];
//                try {
//                    address = Base58.decodeChecked(Base58.encode(StringUtils.hexStringToBytes(fileAddress)));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                if (Utils.getAddress(ecKey.getPubKey()).equals(address)) {
//                    account.setAddress(StringUtils.hexStringToBytes(fileAddress));
//                }
//                if (null == accPassword || "".equals(accPassword)) {
//                    account.setAccPwd(false);
//                } else {
//                    account.setAccPwd(true);
//                }
//
//            } else {
//                throw new AccountFileEmptyException("error");
//            }
//        }
//        return account;
//    }
//

    /**
     * 根据PK 登录
     */
    public void superNodeLogin() {
        try {
            List<String> list = getAllFile(Configure.DATA_ACCOUNT, true);
            if (list == null || list.size() == 0) {
                createAccount();
            } else {
                File file = new File(list.get(0));
                String content = FileUtil.fileToTxt(file);
                JSONObject fileJson = JSONObject.parseObject(content);
                account = Account.parse(fileJson.getBytes("data"), 0, network);
                try {
                    account.verify();
                    accountList.add(account);
                } catch (Exception e) {
                    log.warn("默认登陆{}时出错", account.getAddress().getBase58(), e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取路径下的所有文件/文件夹
     */
    public static List<String> getAllFile(String directoryPath, boolean isAddDirectory) {
        List<String> list = new ArrayList<String>();
        File baseFile = new File(directoryPath);
        if (baseFile.isFile() || !baseFile.exists()) {
            return list;
        }
        File[] files = baseFile.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (isAddDirectory) {
                    list.add(file.getAbsolutePath());
                }
                list.addAll(getAllFile(file.getAbsolutePath(), isAddDirectory));
            } else {
                list.add(file.getAbsolutePath());
            }
        }
        return list;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public List<Account> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<Account> accountList) {
        this.accountList = accountList;
    }

    //
//    /**
//     * @return
//     */
//
//    public Account getAccountByAddress(String address) {
//        for (Account account : accounts) {
//            if (Utils.showAddress(account.getAddress()).equals(address)) {
//                return account;
//            }
//        }
//        return null;
//    }
//
//    public ECKey getEcKey() {
//        return ecKey;
//    }
//
//
//    public List<Account> getAccounts() {
//        return accounts;
//    }
//
//    public void setAccounts(List<Account> accounts) {
//        this.accounts = accounts;
//    }
}
