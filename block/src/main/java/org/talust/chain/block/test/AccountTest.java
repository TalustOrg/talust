package org.talust.chain.block.test;


import org.talust.chain.account.Account;
import org.talust.chain.account.AccountType;
import org.talust.chain.common.crypto.Base58;
import org.talust.chain.common.crypto.Hex;
import org.talust.chain.storage.AccountStorage;

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
