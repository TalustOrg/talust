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
import org.springframework.util.StringUtils;
import org.talust.common.crypto.*;
import org.talust.common.tools.Configure;
import org.talust.common.tools.FileUtil;
import org.talust.core.core.ECKey;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.common.model.Coin;
import org.talust.core.network.MainNetworkParams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

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
        fileJson.put("address",account.getAddress().getBase58());
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
                                TransactionStorage.get().addAddress(account.getAddress().getHash160());
                            } catch (Exception e) {
                                log.warn("默认登陆{}时出错", account.getAddress().getBase58(), e);
                            }
                        }
                    }
                }
                //加载账户信息
                List<byte[]> hash160s = getAccountHash160s();
                //或许重新加载账户相关的交易记录
                maybeReLoadTransaction(hash160s);
                loadBalanceFromChainstateAndUnconfirmedTransaction(hash160s);
            }catch (Exception e){
                log.warn("账户文件路径读取错误");
            }
        }
    }

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
                    TransactionStorage.get().addAddress(account.getAddress().getHash160());
                } catch (Exception e) {
                    log.warn("默认登陆{}时出错", account.getAddress().getBase58(), e);
                }
            }
            //加载账户信息
            List<byte[]> hash160s = getAccountHash160s();
            //或许重新加载账户相关的交易记录
            maybeReLoadTransaction(hash160s);
            loadBalanceFromChainstateAndUnconfirmedTransaction(hash160s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public JSONObject encryptWallet(String password,String address) {
        JSONObject resp = new JSONObject();
        //密码位数和难度检测
        if(!validPassword(password)) {
            log.info("输入的密码需6位或以上，且包含字母和数字");
            resp.put("retCode",1);
            resp.put("msg","输入的密码需6位或以上，且包含字母和数字");
            return resp;
        }

        int successCount = 0; //成功个数
        Account account = null;
        if(address!=null){
            account = getAccount(address);
            if(account == null){
                log.info("账户"+address+"不存在");
                resp.put("retCode",1);
                resp.put("msg","账户"+address+"不存在");
                return resp;
            }
        }else {
            account = getDefaultAccount();
        }

        if(account.isEncrypted()) {
            log.info("账户"+address+"已经加密");
            resp.put("retCode",1);
            resp.put("msg","账户"+address+"已经加密");
            return resp;
        }
        ECKey eckey = account.getEcKey();
        try {
            ECKey newKey = eckey.encrypt(password);
            account.setEcKey(newKey);
            account.setPriSeed(newKey.getEncryptedPrivateKey().getEncryptedBytes());

            //重新签名
            account.signAccount(eckey, null);

            //回写到钱包文件
            File accountFile = new File(Configure.DATA_ACCOUNT, account.getAddress().getBase58());

            FileOutputStream fos = new FileOutputStream(accountFile);
            try {

                byte[] data = account.serialize();
                JSONObject fileJson = new JSONObject();
                fileJson.put("data", data);
                fileJson.put("address",account.getAddress().getBase58());
                fileJson.put("privateKey", account.getPriSeed());
                fileJson.put("isEncrypted", account.isEncrypted());
                fos.write(fileJson.toJSONString().getBytes());
                fos.flush();
                successCount++;
            } finally {
                fos.close();
            }
        } catch (Exception e) {
            log.error("加密 {} 失败: {}", account.getAddress().getBase58(), e.getMessage(), e);
            resp.put("retCode",1);
            resp.put("msg","账户"+address+"加密失败");
            return resp;
        }
        String message = "成功加密"+account.getAddress().getBase58();
        resp.put("retCode",0);
        resp.put("msg",message);
        return resp;
    }
    /**
     * 通过地址获取账户
     * @param address
     * @return Account
     */
    public Account getAccount(String address) {
        for (Account account : accountList) {
            if(account.getAddress().getBase58().equals(address)) {
                return account;
            }
        }
        return null;
    }
    /**
     * 获取默认账户
     * @return Account
     */
    public Account getDefaultAccount() {
        if(accountList == null || accountList.size() == 0) {
            return null;
        }
        return accountList.get(0);
    }

    /**
     * 校验密码难度
     * @param password
     * @return boolean
     */
    public static boolean validPassword(String password) {
        if(StringUtils.isEmpty(password)){
            return false;
        }
        if(password.length() < 6){
            return false;
        }
        if(password.matches("(.*)[a-zA-z](.*)") && password.matches("(.*)\\d+(.*)")){
            return true;
        } else {
            return false;
        }
    }

    public boolean reloadCoin(){
        List<byte[]> hash160s = getAccountHash160s();
        boolean ending =  false;
       if( TransactionStorage.get().reloadTransaction(hash160s)){
           ending=   loadBalanceFromChainstateAndUnconfirmedTransaction(hash160s);
       }
       return ending;
    }

    //是否重新加载账户交易
    private void maybeReLoadTransaction(List<byte[]> hash160s) {
        //判断上次加载的和本次的账户是否完全一致
        List<byte[]> hash160sStore = TransactionStorage.get().getAddresses();

        //如果个数一样，则判断是否完全相同
        if(hash160s.size() == hash160sStore.size()) {
            Comparator<byte[]> comparator = new Comparator<byte[]>() {
                @Override
                public int compare(byte[] o1, byte[] o2) {
                    return Hex.encode(o1).compareTo(Hex.encode(o2));
                }
            };
            Collections.sort(hash160s, comparator);
            Collections.sort(hash160sStore, comparator);
            boolean fullSame = true;
            for (int i = 0; i < hash160s.size(); i++) {
                if(!Arrays.equals(hash160sStore.get(i), hash160s.get(i))) {
                    fullSame = false;
                    break;
                }
            }
            if(fullSame) {
                return;
            }
        }
        TransactionStorage.get().reloadTransaction(hash160s);
    }

    //获取账户对应的has160
    public List<byte[]> getAccountHash160s() {
        CopyOnWriteArrayList<byte[]> hash160s = new CopyOnWriteArrayList<byte[]>();
        for (Account account : accountList) {
            Address address = account.getAddress();
            byte[] hash160 = address.getHash160();

            hash160s.add(hash160);
        }
        return hash160s;
    }

    /*
     * 从状态链（未花费的地址集合）和未确认的交易加载余额
     */
    public boolean loadBalanceFromChainstateAndUnconfirmedTransaction(List<byte[]> hash160s) {

        try {
            for (Account account : accountList) {
                Address address = account.getAddress();
                loadAddressBalance(address);
            }
        }catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return true;
    }

    //加载单个地址的余额信息
    private void loadAddressBalance(Address address) {
        //查询可用余额和等待中的余额
        Coin[] balances = TransactionStorage.get().getBalanceAndUnconfirmedBalance(address.getHash160());
        address.setBalance(balances[0]);
        address.setUnconfirmedBalance(balances[1]);
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


    /**
     * @return
     */

    public Account getAccountByAddress(String address) {
        for (Account account : accountList) {
            if (account.getAddress().getBase58().equals(address)) {
                return account;
            }
        }
        return null;
    }
    public Account getAccountByAddress(byte[] address) {
        for (Account account : accountList) {
            if (account.getAddress().getHash160().equals(address)) {
                return account;
            }
        }
        return null;
    }
}
