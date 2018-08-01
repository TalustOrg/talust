package org.talust.service;

import org.talust.account.Account;

/**
 * @author Axe-Liu
 * @date 2018/8/1.
 */
public interface TransferAccountService {
    boolean decryptAccount(String password , Account account);
}
