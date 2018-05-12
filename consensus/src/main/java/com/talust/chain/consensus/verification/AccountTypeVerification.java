package com.talust.chain.consensus.verification;

import lombok.extern.slf4j.Slf4j;

@Slf4j//帐户类型校验,特定的帐户类型才有打包的权限
public class AccountTypeVerification implements QualificationVerification{
    @Override
    public boolean haveQualification(String account) {
        return false;
    }
}
