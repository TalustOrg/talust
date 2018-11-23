package org.talust.core.transaction;/*
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

import lombok.extern.slf4j.Slf4j;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.model.Coin;
import org.talust.common.model.DepositAccount;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.script.ScriptBuilder;
import org.talust.core.storage.ChainStateStorage;
import org.talust.core.storage.TransactionStorage;

import java.util.*;

@Slf4j
public class TransactionCreator {
    private TransactionStorage transactionStorage = TransactionStorage.get();
    private ChainStateStorage chainStateStorage = ChainStateStorage.get();
    private NetworkParams network = MainNetworkParams.get();

    public Transaction createRegConsensus(String money, Account account, byte[] nodeHash160) {
        //根据交易金额获取当前交易地址下所有的未花费交易

         Transaction tx = new Transaction(MainNetworkParams.get());
        tx.setVersion(Definition.VERSION);
        tx.setType(Definition.TYPE_REG_CONSENSUS);
        Coin totalInputCoin = Coin.ZERO;
        Coin pay = Coin.COIN.multiply((long) Double.parseDouble(money));
        List<TransactionOutput> fromOutputs = selectNotSpentTransaction(pay, account.getAddress());
        TransactionInput input = new TransactionInput();
        for (TransactionOutput output : fromOutputs) {
            input.addFrom(output);
            totalInputCoin = totalInputCoin.add(Coin.valueOf(output.getValue()));
        }
        //创建一个输入的空签名
        if (account.getAccountType() == network.getSystemAccountVersion()||account.getAccountType() == network.getMainAccountVersion()) {
            //普通账户的签名
            input.setScriptSig(ScriptBuilder.createInputScript(null, account.getEcKey(),nodeHash160));
        } else {
            //认证账户的签名
            input.setScriptSig(ScriptBuilder.createCertAccountInputScript(null, account.getAccountTransaction().getHash().getBytes(), account.getAddress().getHash160()));
        }
        tx.addInput(input);

        //交易输出
        tx.addOutput(pay, Definition.LOCKTIME_THRESHOLD - 1, account.getAddress());
        //是否找零(
        if (totalInputCoin.compareTo(pay) > 0) {
            tx.addOutput(totalInputCoin.subtract(pay), account.getAddress());
        }
        //签名交易
        final LocalTransactionSigner signer = new LocalTransactionSigner();
        try {
            if (account.getAccountType() == network.getSystemAccountVersion()||account.getAccountType() == network.getMainAccountVersion()) {
                //普通账户的签名
                signer.signInputs(tx, account.getEcKey());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return tx;
    }


    public Transaction createRemConsensus(DepositAccount depositAccount, Account account, byte[] nodeAddress) {
        Transaction remTx = new Transaction(MainNetworkParams.get());
        remTx.setVersion(Definition.VERSION);
        remTx.setType(Definition.TYPE_REM_CONSENSUS);
        Coin totalInputCoin = Coin.ZERO;
         List<Sha256Hash> txhashs = depositAccount.getTxHash();

        TransactionInput input = new TransactionInput();
        for (Sha256Hash txhash : txhashs) {
            Transaction tx = transactionStorage.getTransaction(txhash).getTransaction();
            input.addFrom(tx.getOutput(0));
            totalInputCoin = totalInputCoin.add(Coin.valueOf(tx.getOutput(0).getValue()));
        }
        input.setScriptSig(ScriptBuilder.createInputScript(null, account.getEcKey(),nodeAddress));
        remTx.addInput(input);
        if (null == account) {
            Address address = new Address(MainNetworkParams.get(), depositAccount.getAddress());
            remTx.addOutput(totalInputCoin, address);
        } else {
            remTx.addOutput(totalInputCoin, account.getAddress());
        }
        return remTx;
    }


    public List<TransactionOutput> selectNotSpentTransaction(Coin amount, Address address) {

        //获取到所有未花费的交易
        List<TransactionOutput> outputs = transactionStorage.getNotSpentTransactionOutputs(address.getHash160());

        //选择结果存放列表
        List<TransactionOutput> thisOutputs = new ArrayList<TransactionOutput>();

        if (outputs == null || outputs.size() == 0) {
            return thisOutputs;
        }

        //遍历选择，原则是尽量少的数据，也就是笔数最少

        //小于amount的集合
        List<TransactionOutput> lessThanList = new ArrayList<TransactionOutput>();
        //大于amount的集合
        List<TransactionOutput> moreThanList = new ArrayList<TransactionOutput>();

        for (TransactionOutput transactionOutput : outputs) {
            if (transactionOutput.getValue() == amount.value) {
                //如果刚好相等，则立即返回
                thisOutputs.add(transactionOutput);
                return thisOutputs;
            } else if (transactionOutput.getValue() > amount.value) {
                //加入大于集合
                moreThanList.add(transactionOutput);
            } else {
                //加入小于于集合
                lessThanList.add(transactionOutput);
            }
        }
        transferPreferredWithLessNumber(amount, lessThanList, moreThanList, thisOutputs);
        //依然按照交易时间排序
        if (thisOutputs.size() > 0) {
            Collections.sort(thisOutputs, new Comparator<TransactionOutput>() {
                @Override
                public int compare(TransactionOutput o1, TransactionOutput o2) {
                    long v1 = o1.getParent().getTime();
                    long v2 = o2.getParent().getTime();
                    if (v1 == v2) {
                        return 0;
                    } else if (v1 > v2) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });
        }
        return thisOutputs;
    }

    /*
     * 交易选择 -- 优先使用零钱
     */
    private void transferPreferredWithSmallChangeMulUser(Coin amount, HashMap<String, List<TransactionOutput>> lessThanList,
                                                         HashMap<String, List<TransactionOutput>> moreThanList, HashMap<String, List<TransactionOutput>> thisOutputs) {
        if (lessThanList.size() > 0) {
            //计算所有零钱，是否足够
            Coin lessTotal = Coin.ZERO;
            Iterator<String> lessit = lessThanList.keySet().iterator();
            while (lessit.hasNext()) {
                String address = lessit.next();
                List<TransactionOutput> userLessThanlist = lessThanList.get(address);
                for (TransactionOutput transactionOutput : userLessThanlist) {
                    lessTotal = lessTotal.add(Coin.valueOf(transactionOutput.getValue()));
                }
            }

            if (lessTotal.isLessThan(amount)) {
                //不够，那么必定有大的
                selectOneOutputMulUser(moreThanList, thisOutputs);
            } else {
                //选择零钱
                selectSmallChangeMulUser(amount, lessThanList, thisOutputs);
            }
        } else {
            //没有比本次交易最大的未输出交易
            selectOneOutputMulUser(moreThanList, thisOutputs);
        }
    }

    /*
     * 交易选择 -- 以交易数据小优先，该种机制尽量选择一笔输入
     */
    private void transferPreferredWithLessNumber(Coin amount, List<TransactionOutput> lessThanList, List<TransactionOutput> moreThanList, List<TransactionOutput> outputs) {
        if (moreThanList.size() > 0) {
            //有比本次交易大的未输出交易，直接使用其中最小的一个
            selectOneOutput(moreThanList, outputs);
        } else {
            //没有比本次交易最大的未输出交易
            selectSmallChange(amount, lessThanList, outputs);
        }
    }

    /*
     * 交易选择 -- 以交易数据小优先，该种机制尽量选择一笔输入
     */
    private void transferPreferredWithLessNumberMulUser(Coin amount, HashMap<String, List<TransactionOutput>> lessThanList, HashMap<String, List<TransactionOutput>> moreThanList, HashMap<String, List<TransactionOutput>> outputs) {
        if (moreThanList.size() > 0) {
            //有比本次交易大的未输出交易，直接使用其中最小的一个
            selectOneOutputMulUser(moreThanList, outputs);
        } else {
            //没有比本次交易最大的未输出交易
            selectSmallChangeMulUser(amount, lessThanList, outputs);
        }
    }

    /*
     * 选择列表里面金额最小的一笔作为输出
     */
    private void selectOneOutput(List<TransactionOutput> moreThanList, List<TransactionOutput> outputs) {
        if (moreThanList == null || moreThanList.size() == 0) {
            return;
        }
        Collections.sort(moreThanList, new Comparator<TransactionOutput>() {
            @Override
            public int compare(TransactionOutput o1, TransactionOutput o2) {
                long v1 = o1.getValue();
                long v2 = o2.getValue();
                if (v1 == v2) {
                    return 0;
                } else if (v1 > v2) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        outputs.add(moreThanList.get(0));
    }

    /*
     * 出现的第一笔为输出
     */
    private void selectOneOutputMulUser(HashMap<String, List<TransactionOutput>> moreThanList, HashMap<String, List<TransactionOutput>> outputs) {
        if (moreThanList == null || moreThanList.size() == 0) {
            return;
        }
        Iterator<String> moreit = moreThanList.keySet().iterator();
        while (moreit.hasNext()) {
            String address = moreit.next();
            List<TransactionOutput> userMoreThanList = moreThanList.get(address);
            if (userMoreThanList.size() == 0) {
                continue;
            } else {
                TransactionOutput out = userMoreThanList.get(0);
                List<TransactionOutput> oneList = new ArrayList<TransactionOutput>();
                oneList.add(out);
                outputs.put(address, oneList);
                return;
            }
        }
    }

    /*
     * 选择零钱，原则是尽量少的找钱，尽量少的使用输出笔数
     */
    private void selectSmallChange(Coin amount, List<TransactionOutput> lessThanList, List<TransactionOutput> outputs) {
        if (lessThanList == null || lessThanList.size() == 0) {
            return;
        }
        //排序
        Collections.sort(lessThanList, new Comparator<TransactionOutput>() {
            @Override
            public int compare(TransactionOutput o1, TransactionOutput o2) {
                long v1 = o1.getValue();
                long v2 = o2.getValue();
                if (v1 == v2) {
                    return 0;
                } else if (v1 > v2) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        //已选择的金额
        Coin total = Coin.ZERO;
        //从小到大选择
        for (TransactionOutput transactionOutput : lessThanList) {
            outputs.add(transactionOutput);
            total = total.add(Coin.valueOf(transactionOutput.getValue()));
            if (total.isGreaterThan(amount)) {
                //判断是否可以移除最小的几笔交易
                List<TransactionOutput> removeList = new ArrayList<TransactionOutput>();
                for (TransactionOutput to : outputs) {
                    total = total.subtract(Coin.valueOf(to.getValue()));
                    if (total.isGreaterThan(amount)) {
                        removeList.add(to);
                    } else {
                        break;
                    }
                }
                if (removeList.size() > 0) {
                    outputs.removeAll(removeList);
                }
                break;
            }
        }
    }

    /*
     * 选择零钱，原则先后顺序
     */
    private void selectSmallChangeMulUser(Coin amount, HashMap<String, List<TransactionOutput>> lessThanList, HashMap<String, List<TransactionOutput>> outputs) {
        if (lessThanList == null || lessThanList.size() == 0) {
            return;
        }
        //已选择的金额
        Coin total = Coin.ZERO;

        Iterator<String> lessit = lessThanList.keySet().iterator();
        while (lessit.hasNext()) {
            String address = lessit.next();
            List<TransactionOutput> userLessThanList = lessThanList.get(address);
            List<TransactionOutput> userOutputList = new ArrayList<TransactionOutput>();
            //从小到大选择
            for (TransactionOutput transactionOutput : userLessThanList) {
                userOutputList.add(transactionOutput);
                total = total.add(Coin.valueOf(transactionOutput.getValue()));
                if (total.isGreaterThan(amount)) {
                    break;
                }
            }
            outputs.put(address, userOutputList);
        }
    }

}
