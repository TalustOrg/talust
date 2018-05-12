package com.talust.chain.consensus.verification;

import lombok.extern.slf4j.Slf4j;

@Slf4j //代币持有量校验
public class AmountHoldVerification implements QualificationVerification {
    @Override
    public boolean haveQualification(String account) {
        return false;
    }
}
