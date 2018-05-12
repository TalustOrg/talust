package com.talust.chain.block.test;


import com.talust.chain.account.Account;
import com.talust.chain.account.AccountType;
import com.talust.chain.storage.AccountStorage;
import com.talust.chain.common.crypto.Base58;
import com.talust.chain.common.crypto.Hex;

//对一些帐户进行初始化
public class AccountTest {

    public static void main(String[] args) throws Exception{
        AccountStorage accountStorage = AccountStorage.get();
        accountStorage.init();
        Account account = new Account();
        account.setAccount("root");
        account.setAccPwd("rootpwd");
        account.setAccType(AccountType.USER.getType());

//        boolean hadAccount = accountStorage.hadAccount();
//        if(hadAccount){
//            System.out.println("已经存在帐户信息...");
//        }else{
//            accountStorage.createAccount(account);
//        }

        accountStorage.login(account);
        System.out.println("public:"+ Hex.encode(account.getAddress()));
        System.out.println("address:"+ Base58.encode(account.getAddress()));




    }

}
