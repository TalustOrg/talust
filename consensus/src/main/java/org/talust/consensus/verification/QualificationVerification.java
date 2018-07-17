package org.talust.consensus.verification;

//资格校验,用于校验某帐户是否具有进入共识的权限
public interface QualificationVerification {

    /**
     * 判断帐户是否有进行共识的权限
     * @param account
     * @return
     */
    boolean haveQualification(String account);

}
