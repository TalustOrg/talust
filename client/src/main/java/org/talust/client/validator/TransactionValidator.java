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

package org.talust.client.validator;

import lombok.extern.slf4j.Slf4j;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.exception.VerificationException;
import org.talust.common.model.*;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;
import org.talust.core.data.DataContainer;
import org.talust.core.data.TransactionCache;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.script.Script;
import org.talust.core.server.NtpTimeService;
import org.talust.core.storage.TransactionStore;
import org.talust.core.transaction.*;
import org.talust.network.MessageValidator;
import org.talust.network.netty.queue.MessageQueueHolder;
import org.talust.core.storage.BlockStorage;
import org.talust.core.storage.ChainStateStorage;
import org.talust.common.tools.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 交易数据校验,是指的一条一条的交易数据
 */
@Slf4j
public class TransactionValidator implements MessageValidator {
    private MessageQueueHolder mqHolder = MessageQueueHolder.get();
    private NetworkParams network = MainNetworkParams.get();
    private BlockStorage blockStorage = BlockStorage.get();
    private TransactionCache transactionCache = TransactionCache.getInstace();
    private ChainStateStorage chainStateStorage =  ChainStateStorage.get();
    private double nearZero = 0.0000000000001;

    @Override
    public boolean check(MessageChannel message) {//对每一条交易数据进行验证
        Message msg = message.getMessage();
        if (msg.getSigner() != null && msg.getSigner().length > 0) {
            boolean signOk = mqHolder.checkSign(msg);
            if (!signOk) {//签名不正确
                return false;
            }
        }
        Transaction transaction = SerializationUtil.deserializer(msg.getContent(), Transaction.class);
        if(checkTransaction(transaction,null)){
            return true;
        }
        return false;
    }

    /**
     * 交易验证器，验证交易的输入输出是否合法
     *
     * @param tx  待验证的交易
     * @param txs 当输入引用找不到时，就在这个列表里面查找（当同一个区块包含多个交易链时需要用到）
     * @return ValidatorResult<TransactionValidatorResult>
     */
    public boolean checkTransaction(Transaction tx, List<Transaction> txs) {


        tx.verify();
        //验证交易的合法性
        if (tx instanceof BaseCommonlyTransaction) {
            ((BaseCommonlyTransaction) tx).verifyScript();
        }

        //交易的txid不能和区块里面的交易重复
        TransactionStore verifyTX = blockStorage.getTransaction(tx.getHash().getBytes());
        if (verifyTX != null) {
            return false;
        }
        //如果是转帐交易
        //TODO 以下代码请使用状态模式重构
        if (tx.isPaymentTransaction() && tx.getType() != Definition.TYPE_COINBASE) {
            //验证交易的输入来源，是否已花费的交易，同时验证金额
            Coin txInputFee = Coin.ZERO;
            Coin txOutputFee = Coin.ZERO;

            //验证本次交易的输入
            List<TransactionInput> inputs = tx.getInputs();
            //交易引用的输入，赎回脚本必须一致
            byte[] scriptBytes = null;
            int i = 0;
            for (TransactionInput input : inputs) {
                scriptBytes = null;
                List<TransactionOutput> outputs = input.getFroms();
                if (outputs == null || outputs.size() == 0) {
                    throw new VerificationException("交易没有引用输入");
                }
                for (TransactionOutput output : outputs) {
                    //对上一交易的引用以及索引值
                    Transaction fromTx = output.getParent();
                    if (fromTx == null) {
                        throw new VerificationException("交易没有正确的输入引用");
                    }
                    Sha256Hash fromId = fromTx.getHash();
                    int index = output.getIndex();

                    //如果引用已经是完整的交易，则不查询
                    if (fromTx.getOutputs() == null || fromTx.getOutputs().isEmpty()) {
                        //需要设置引用的完整交易
                        //查询内存池里是否有该交易
                        Transaction preTransaction = transactionCache.get(fromId);
                        //内存池里面没有，那么是否在传入的列表里面
                        if (preTransaction == null && txs != null && txs.size() > 0) {
                            for (Transaction transaction : txs) {
                                if (transaction.getHash().equals(fromId)) {
                                    preTransaction = transaction;
                                    break;
                                }
                            }
                        }
                        if (preTransaction == null) {
                            //内存池和传入的列表都没有，那么去存储里面找
                            TransactionStore preTransactionStore = blockStorage.getTransaction(fromId.getBytes());
                            if (preTransactionStore == null) {
                                return false;
                            }
                            preTransaction = preTransactionStore.getTransaction();
                        }
                        output.setParent(preTransaction);
                        output.setScript(preTransaction.getOutput(index).getScript());
                        fromTx = preTransaction;
                    }

                    //验证引用的交易是否可用
                    if (fromTx.getLockTime() < 0l ||
                            (fromTx.getLockTime() > Definition.LOCKTIME_THRESHOLD && fromTx.getLockTime() > NtpTimeService.currentTimeSeconds())
                            || (fromTx.getLockTime() < Definition.LOCKTIME_THRESHOLD && fromTx.getLockTime() > network.getBestHeight())) {
                        throw new VerificationException("引用了不可用的交易");
                    }
                    //验证引用的交易输出是否可用
                    long lockTime = output.getLockTime();
                    if (lockTime < 0l || (lockTime > Definition.LOCKTIME_THRESHOLD && lockTime > NtpTimeService.currentTimeSeconds())
                            || (lockTime < Definition.LOCKTIME_THRESHOLD && lockTime > network.getBestHeight())) {
                        throw new VerificationException("引用了不可用的交易输出");
                    }

                    TransactionOutput preOutput = fromTx.getOutput(index);
                    txInputFee = txInputFee.add(Coin.valueOf(preOutput.getValue()));
                    output.setValue(preOutput.getValue());
                    //验证交易赎回脚本必须一致
                    if (scriptBytes == null) {
                        scriptBytes = preOutput.getScriptBytes();
                    } else if (!Arrays.equals(scriptBytes, preOutput.getScriptBytes())) {
                        throw new VerificationException("错误的输入格式，不同的交易赎回脚本不能合并");
                    }

                    //验证交易不能双花
                    byte[] statusKey = output.getKey();
                    byte[] state = chainStateStorage.getBytes(statusKey);
                    if ((state == null || Arrays.equals(state, new byte[]{1})) && txs != null && !txs.isEmpty()) {

                    } else if (Arrays.equals(state, new byte[]{2})) {
                        //已经花费了
                        return false;
                    }
                }
                Script verifyScript = new Script(scriptBytes);
                if (verifyScript.isConsensusOutputScript()) {
                    //共识保证金引用脚本，则验证
                    //因为共识保证金，除了本人会操作，还会有其它共识人操作
                    //并且不一定是转到自己的账户，所以必须对输入输出都做严格的规范
                    if (!(tx.getType() == Definition.TYPE_REM_CONSENSUS || tx.getType() == Definition.TYPE_VIOLATION)) {
                        throw new VerificationException("不合法的交易引用");
                    }
                    //输入必须只有一个
                    if (inputs.size() != 1 || inputs.get(0).getFroms().size() != 1) {
                        return false;
                    }
                    //输出必须只有一个，切必须按照指定的类型输出到相应的账户
                    if (tx.getOutputs().size() != 1) {
                        return false;
                    }
                    TransactionOutput ouput = tx.getOutputs().get(0);
                    //验证保证金的数量
                    if (ouput.getValue() != inputs.get(0).getFroms().get(0).getValue()) {
                        return false;
                    }
                    Script outputScript = ouput.getScript();
                    //必须输出到地址
                    if (!outputScript.isSentToAddress()) {
                        return false;
                    }
                    //必须输出到指定的账户
                    //自己的账户
                    byte[] selfAccount = verifyScript.getChunks().get(0).data;
                    //输出账户
                    byte[] ouputAccount = outputScript.getChunks().get(2).data;
                } else {
                    input.getScriptSig().run(tx, i, verifyScript);
                }
                i++;
            }
            //验证本次交易的输出
            List<TransactionOutput> outputs = tx.getOutputs();
            for (Output output : outputs) {
                Coin outputCoin = Coin.valueOf(output.getValue());
                //输出金额不能为负数
                if (outputCoin.isLessThan(Coin.ZERO)) {
                    return false;
                }
                if (outputCoin.isGreaterThan(Configure.MAX_OUTPUT_COIN)) {
                    return false;
                }
                txOutputFee = txOutputFee.add(outputCoin);
            }
            //验证不能给自己转账
            boolean isLock = false;
            if (tx.getType() == Definition.TYPE_PAY) {
                Script inputScript = new Script(scriptBytes);
                byte[] sender = inputScript.getChunks().get(2).data;
                TransactionOutput output = outputs.get(0);
                byte[] receiver = output.getScript().getChunks().get(2).data;
                if (Arrays.equals(sender, receiver)) {
                    //不能给自己转账，因为毫无意义，一种情况除外
                    //锁仓的时候，除外，但是锁仓需要大于24小时，并金额大于100
                    Coin value = Coin.valueOf(output.getValue());
                    long lockTime = output.getLockTime();

                    //发送的金额必须大于100
                    if (value.compareTo(Coin.COIN.multiply(100)) < 0) {
                        return false;
                    }
                    //锁仓的时间必须大于24小时
                    if (lockTime - tx.getTime() < 24 * 60 * 60) {
                        return false;
                    }
                    isLock = true;
                }
            }

            //输出金额不能大于输入金额
            if (txOutputFee.isGreaterThan(txInputFee)) {
                return false;
            }


        }
        return true;
    }
}
