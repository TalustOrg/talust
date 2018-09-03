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
package org.talust.core.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talust.core.core.ECKey;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.crypto.Utils;
import org.talust.common.exception.VerificationException;
import org.talust.core.core.Definition;
import org.talust.core.model.Account;
import org.talust.core.model.RedeemData;
import org.talust.core.script.Script;
import org.talust.core.script.ScriptBuilder;
import org.talust.core.script.ScriptChunk;

import java.util.List;

public class LocalTransactionSigner implements TransactionSigner {
	
    private static final Logger log = LoggerFactory.getLogger(LocalTransactionSigner.class);

    @Override
    public boolean isReady() {
        return true;
    }

    /**
     * 普通账户的签名
     */
    @Override
    public boolean signInputs(Transaction tx, ECKey key) {
        int numInputs = tx.getInputs().size();
        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = (TransactionInput) tx.getInput(i);
            if (txIn.getFroms() == null || txIn.getFroms().size() == 0) {
                log.warn("缺少上次交易的引用,index:{}", i);
                continue;
            }

            RedeemData redeemData = txIn.getFroms().get(0).getRedeemData(key);

            if ((key = redeemData.getFullKey()) == null) {
                log.warn("No local key found for input {}", i);
                continue;
            }

            Script inputScript = txIn.getScriptSig();
            Script redeemScript = redeemData.redeemScript;
            
            byte[] script = redeemScript.getProgram();
            try {
                TransactionSignature signature = tx.calculateSignature(i, key, script, Transaction.SigHash.ALL);
                int sigIndex = 0;
                inputScript = redeemScript.getScriptSigWithSignature(inputScript, signature.encode(), sigIndex);
                txIn.setScriptSig(inputScript);
            } catch (ECKey.MissingPrivateKeyException e) {
                log.warn("No private key in keypair for input {}", i);
            }

        }
        return true;
    }

    /**
     * 普通账户的签名
     */
    @Override
    public boolean signOneInputs(Transaction tx, ECKey key,int inputIndex) {
        int numInputs = tx.getInputs().size();
        if(numInputs<inputIndex+1){
            log.warn("交易输入index越界");
            return false;
        }

        TransactionInput txIn = (TransactionInput) tx.getInput(inputIndex);
        if (txIn.getFroms() == null || txIn.getFroms().size() == 0) {
            log.warn("缺少上次交易的引用,index:{}", inputIndex);
            return false;
        }

        RedeemData redeemData = txIn.getFroms().get(0).getRedeemData(key);

        if ((key = redeemData.getFullKey()) == null) {
            log.warn("No local key found for input {}", inputIndex);
            return false;
        }

        Script inputScript = txIn.getScriptSig();
        Script redeemScript = redeemData.redeemScript;

        byte[] script = redeemScript.getProgram();
        try {
            TransactionSignature signature = tx.calculateSignature(inputIndex, key, script, Transaction.SigHash.ALL);
            int sigIndex = 0;
            inputScript = redeemScript.getScriptSigWithSignature(inputScript, signature.encode(), sigIndex);
            txIn.setScriptSig(inputScript);
        } catch (ECKey.MissingPrivateKeyException e) {
            log.warn("No private key in keypair for input {}", inputIndex);
        }


        return true;
    }

    /**
     * 认证账户的签名
     * @param tx
     * @param eckeys
     * @param txid
     * @param hash160
     */
	public boolean signCertAccountInputs(Transaction tx, ECKey[] eckeys, byte[] txid, byte[] hash160) {
		int numInputs = tx.getInputs().size();
		
        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = (TransactionInput) tx.getInput(i);
            if (txIn.getFroms() == null || txIn.getFroms().size() == 0) {
                log.warn("缺少上次交易的引用,index:{}", i);
                continue;
            }

            Script inputScript = txIn.getFromScriptSig();
            
            List<ScriptChunk> chunks = inputScript.getChunks();
            
            Utils.checkState(chunks.size() == 5);
            Utils.checkState(eckeys.length == 1);
            
            byte[][] signs = new byte[1][];
            for (int j = 0; j < eckeys.length; j++) {
	            TransactionSignature signature = tx.calculateSignature(i, eckeys[j], inputScript.getProgram(), Transaction.SigHash.ALL);
	            signs[j] = signature.encode();
			}
            txIn.setScriptSig(ScriptBuilder.createCertAccountInputScript(signs, txid, hash160));
        }
        return true;
	}

	@Override
	public byte[] serialize() {
		return null;
	}

	@Override
	public void deserialize(byte[] data) {
		
	}

	
	/**
     * 账户对指定内容签名
	 * 如果账户已加密的情况，则需要先解密账户
     * 如果是认证账户，默认使用交易的签名，如需使用账户管理签名，则调用sign(account, TransactionDefinition.TX_VERIFY_MG)
     * @param account
     * @param hash
     * @return byte[][]
     */
	public static byte[][] signHash(Account account, Sha256Hash hash) {
		return signHash(account, hash, Definition.TX_VERIFY_TR);
	}
	
	/**
	 * 账户对指定内容签名
	 * 如果账户已加密的情况，则需要先解密账户
	 * @param account
     * @param hash
	 * @param type TransactionDefinition.TX_VERIFY_MG利用管理私钥签名，TransactionDefinition.TX_VERIFY_TR利用交易私钥签名
	 */
	public static byte[][] signHash(Account account, Sha256Hash hash, int type) {
		
		if(account.isCertAccount()) {
			//认证账户
			if(account.getAccountTransaction() == null) {
				throw new VerificationException("签名失败，认证账户没有对应的信息交易");
			}
			
			ECKey[] keys = null;
			if(type == Definition.TX_VERIFY_MG) {
				keys = account.getMgEckeys();
			} else {
				keys = account.getTrEckeys();
			}
			
			if(keys == null) {
				throw new VerificationException("账户没有解密？");
			}
			
			ECKey.ECDSASignature ecSign = keys[0].sign(hash);
			byte[] sign1 = ecSign.encodeToDER();
			
			ecSign = keys[1].sign(hash);
			byte[] sign2 = ecSign.encodeToDER();
			
			return new byte[][] {sign1, sign2};
		} else {
			//普通账户
			ECKey key = account.getEcKey();
			ECKey.ECDSASignature ecSign = key.sign(hash);
			byte[] sign = ecSign.encodeToDER();

			return new byte[][] {sign};
		}
		
	}
}
