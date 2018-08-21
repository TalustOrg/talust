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
package org.talust.service.impl;

import com.alibaba.fastjson.JSONObject;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.talust.client.validator.TransactionValidator;
import org.talust.common.crypto.AESEncrypt;
import org.talust.common.crypto.Base58;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.crypto.Utils;
import org.talust.common.model.Coin;
import org.talust.common.model.Message;
import org.talust.common.model.MessageType;
import org.talust.common.tools.ArithUtils;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.DateUtil;
import org.talust.common.tools.SerializationUtil;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;
import org.talust.core.core.SynBlock;
import org.talust.core.data.TransactionCache;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.script.ScriptBuilder;
import org.talust.core.server.NtpTimeService;
import org.talust.core.storage.AccountStorage;
import org.talust.core.storage.TransactionStorage;
import org.talust.core.transaction.LocalTransactionSigner;
import org.talust.core.transaction.Transaction;
import org.talust.core.transaction.TransactionInput;
import org.talust.core.transaction.TransactionOutput;
import org.talust.network.model.MyChannel;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.ConnectionManager;
import org.talust.service.TransferAccountService;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Axe-Liu
 * @date 2018/8/1.
 */
@Service
@Slf4j
public class TransferAccountServiceImpl implements TransferAccountService {
    private final static Lock locker = new ReentrantLock();

    private TransactionStorage transactionStorage = TransactionStorage.get();

    private TransactionValidator transactionValidator =new  TransactionValidator();
    private NetworkParams network = MainNetworkParams.get();
    @Override
    public boolean decryptAccount(String password, Account account) {
        if(!validPassword(password)) {
            return false;
        }
        if(account == null){
            return false;
        }
        try {
           AESEncrypt.decrypt( account.getPriSeed(), password);
        } catch (org.spongycastle.crypto.InvalidCipherTextException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public Account getAccountByAddress(String address) {
        Base58.decodeChecked(address);
        return AccountStorage.get().getAccountByAddress(address);
    }

    @Override
    public JSONObject transfer(String toAddress, String money, String address, String password) {
        JSONObject resp =  new JSONObject();
        Utils.checkNotNull(toAddress);
        Collection<MyChannel> connects =ChannelContain.get().getMyChannels();
        if(connects.size()<=0){
            resp.put("retCode","1");
            resp.put("message","当前网络不可用，请稍后再尝试");
            return resp;
        }
        long height = CacheManager.get().getCurrentBlockHeight();
        long localbestheighttime = CacheManager.get().getCurrentBlockTime();
        if(height==0){
            if(SynBlock.get().getSyning().get()){
                resp.put("retCode","1");
                resp.put("message","正在同步区块中，请稍后再尝试");
                return resp;
            }else{
                ConnectionManager.get().init();
                resp.put("retCode","1");
                resp.put("message","当前网络不可用，正在重试网络和数据修复，请稍后再尝试");
                return resp;
            }
        }
        if(NtpTimeService.currentTimeMillis()-localbestheighttime>60){
            if(SynBlock.get().getSyning().get()) {
                resp.put("retCode","1");
                resp.put("message","正在同步区块中，请稍后再尝试");
                return resp;
            }else {
                ConnectionManager.get().init();
                resp.put("retCode","1");
                resp.put("message","当前网络不可用，正在重试网络和数据修复，请稍后再尝试");
                return resp;
            }
        }
        locker.lock();
        try{
            if(money.compareTo("0") <= 0) {
                resp.put("retCode", "1");
                resp.put("message", "发送的金额需大于0");
                return resp;
            }
            Account account = this.getAccountByAddress(address);
            if (null == account) {
                resp.put("retCode", "1");
                resp.put("message", "出账账户不存在");
                return resp;
            }
            if(account.getAddress().getBase58().equals(toAddress)){
                resp.put("retCode", "1");
                resp.put("message", "不能给自己转账");
                return resp;
            }
            if (account.isEncrypted()) {
                if (StringUtil.isNullOrEmpty(password)) {
                    resp.put("retCode", "1");
                    resp.put("message", "输入钱包密码进行转账");
                    return resp;
                } else {
                    boolean pswCorrect = this.decryptAccount(password, account);
                    if (!pswCorrect) {
                        resp.put("retCode", "1");
                        resp.put("message", "账户密码不正确");
                        return resp;
                    }
                }
            }
            //当前余额可用余额
            long balance = account.getAddress().getBalance().value;
            if(ArithUtils.compareStr(balance+"",money) < 0) {
                resp.put("retCode", "1");
                resp.put("message", "余额不足");
                return resp;
            }
            Transaction tx = new Transaction(MainNetworkParams.get());
            tx.setVersion(Definition.VERSION);
            tx.setType(Definition.TYPE_PAY);
            Coin totalInputCoin = Coin.ZERO;
            //根据交易金额获取当前交易地址下所有的未花费交易
            Coin pay = Coin.parseCoin(money);
            List<TransactionOutput> fromOutputs = selectNotSpentTransaction(pay, account.getAddress());
            TransactionInput input = new TransactionInput();
            for (TransactionOutput output : fromOutputs) {
                input.addFrom(output);
                totalInputCoin = totalInputCoin.add(Coin.valueOf(output.getValue()));
            }
            //创建一个输入的空签名
            if(account.getAccountType() == network.getSystemAccountVersion()) {
                //普通账户的签名
                input.setScriptSig(ScriptBuilder.createInputScript(null, account.getEcKey()));
            } else {
                //认证账户的签名
                input.setScriptSig(ScriptBuilder.createCertAccountInputScript(null, account.getAccountTransaction().getHash().getBytes(), account.getAddress().getHash160()));
            }
            tx.addInput(input);

            //交易输出
            tx.addOutput(pay, Address.fromBase58(network,toAddress));
            //是否找零
            if(totalInputCoin.compareTo(pay) > 0) {
                tx.addOutput(totalInputCoin.subtract(pay), account.getAddress());
            }

            //签名交易
            final LocalTransactionSigner signer = new LocalTransactionSigner();
            try {
                if(account.getAccountType() == network.getSystemAccountVersion()) {
                    //普通账户的签名
                    signer.signInputs(tx, account.getEcKey());
                } else {
                    //认证账户的签名
                    signer.signCertAccountInputs(tx, account.getTrEckeys(), account.getAccountTransaction().getHash().getBytes(), account.getAddress().getHash160());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            //验证交易是否合法
            assert transactionValidator.checkTransaction(tx,null);
            //加入内存池，因为广播的Inv消息出去，其它对等体会回应getDatas获取交易详情，会从本机内存取出来发送
            boolean success = TransactionCache.getInstace().add(tx);

            Message message = new Message();
            byte[] data = SerializationUtil.serializer(tx);
            byte[] sign = account.getEcKey().sign(Sha256Hash.of(data)).encodeToDER();
            message.setContent(data);
            message.setSigner(account.getEcKey().getPubKey());
            message.setSignContent(sign);
            message.setType(MessageType.TRANSACTION.getType());
            message.setTime(DateUtil.getTimeSecond());
            //广播交易
            ConnectionManager.get().TXMessageSend(message);
        }catch (Exception e ){
        }finally {
            locker.unlock();
        }
        //验证本账户金额与交易金额是否正常
        return resp;
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

    public List<TransactionOutput> selectNotSpentTransaction(Coin amount, Address address) {

        //获取到所有未花费的交易
        List<TransactionOutput> outputs = transactionStorage.getNotSpentTransactionOutputs(address.getHash160());

        //选择结果存放列表
        List<TransactionOutput> thisOutputs = new ArrayList<TransactionOutput>();

        if(outputs == null || outputs.size() == 0) {
            return thisOutputs;
        }

        //遍历选择，原则是尽量少的数据，也就是笔数最少

        //小于amount的集合
        List<TransactionOutput> lessThanList = new ArrayList<TransactionOutput>();
        //大于amount的集合
        List<TransactionOutput> moreThanList = new ArrayList<TransactionOutput>();

        for (TransactionOutput transactionOutput : outputs) {
            if(transactionOutput.getValue() == amount.value) {
                //如果刚好相等，则立即返回
                thisOutputs.add(transactionOutput);
                return thisOutputs;
            } else if(transactionOutput.getValue() > amount.value) {
                //加入大于集合
                moreThanList.add(transactionOutput);
            } else {
                //加入小于于集合
                lessThanList.add(transactionOutput);
            }
        }

//        if(Configure.TRANSFER_PREFERRED == 2) {
//            //优先使用零钱
//            transferPreferredWithSmallChange(amount, lessThanList, moreThanList, thisOutputs);
//        } else {
            //以交易数据小优先，该种机制尽量选择一笔输入，默认方式
            transferPreferredWithLessNumber(amount, lessThanList, moreThanList, thisOutputs);
//        }
        //依然按照交易时间排序
        if(thisOutputs.size() > 0) {
            Collections.sort(thisOutputs, new Comparator<TransactionOutput>() {
                @Override
                public int compare(TransactionOutput o1, TransactionOutput o2) {
                    long v1 = o1.getParent().getTime();
                    long v2 = o2.getParent().getTime();
                    if(v1 == v2) {
                        return 0;
                    } else if(v1 > v2) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });
        }
        return thisOutputs;
    }

//    /*
//     * 交易选择 -- 优先使用零钱
//     */
//    private void transferPreferredWithSmallChange(Coin amount, List<TransactionOutput> lessThanList,
//                                                  List<TransactionOutput> moreThanList, List<TransactionOutput> thisOutputs) {
//        if(lessThanList.size() > 0) {
//            //计算所有零钱，是否足够
//            Coin lessTotal = Coin.ZERO;
//            for (TransactionOutput transactionOutput : lessThanList) {
//                lessTotal = lessTotal.add(Coin.valueOf(transactionOutput.getValue()));
//            }
//
//            if(lessTotal.isLessThan(amount)) {
//                //不够，那么必定有大的
//                selectOneOutput(moreThanList, thisOutputs);
//            } else {
//                //选择零钱
//                selectSmallChange(amount, lessThanList, thisOutputs);
//            }
//        } else {
//            //没有比本次交易最大的未输出交易
//            selectOneOutput(moreThanList, thisOutputs);
//        }
//    }

    /*
     * 交易选择 -- 优先使用零钱
     */
    private void transferPreferredWithSmallChangeMulUser(Coin amount, HashMap<String, List<TransactionOutput>> lessThanList,
                                                         HashMap<String, List<TransactionOutput>> moreThanList, HashMap<String, List<TransactionOutput>> thisOutputs) {
        if(lessThanList.size() > 0) {
            //计算所有零钱，是否足够
            Coin lessTotal = Coin.ZERO;
            Iterator<String> lessit= lessThanList.keySet().iterator();
            while (lessit.hasNext()){
                String address = lessit.next();
                List<TransactionOutput> userLessThanlist = lessThanList.get(address);
                for (TransactionOutput transactionOutput : userLessThanlist) {
                    lessTotal = lessTotal.add(Coin.valueOf(transactionOutput.getValue()));
                }
            }

            if(lessTotal.isLessThan(amount)) {
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
        if(moreThanList.size() > 0) {
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
    private void transferPreferredWithLessNumberMulUser(Coin amount, HashMap<String,List<TransactionOutput>> lessThanList,  HashMap<String,List<TransactionOutput>> moreThanList,  HashMap<String,List<TransactionOutput>> outputs) {
        if(moreThanList.size() > 0) {
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
        if(moreThanList == null || moreThanList.size() == 0) {
            return;
        }
        Collections.sort(moreThanList, new Comparator<TransactionOutput>() {
            @Override
            public int compare(TransactionOutput o1, TransactionOutput o2) {
                long v1 = o1.getValue();
                long v2 = o2.getValue();
                if(v1 == v2) {
                    return 0;
                } else if(v1 > v2) {
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
    private void selectOneOutputMulUser(HashMap<String,List<TransactionOutput>> moreThanList, HashMap<String,List<TransactionOutput>> outputs) {
        if(moreThanList == null || moreThanList.size() == 0) {
            return;
        }
        Iterator<String> moreit = moreThanList.keySet().iterator();
        while (moreit.hasNext()) {
            String address = moreit.next();
            List<TransactionOutput> userMoreThanList = moreThanList.get(address);
            if(userMoreThanList.size()==0) {
                continue;
            }else {
                TransactionOutput out = userMoreThanList.get(0);
                List<TransactionOutput> oneList = new ArrayList<TransactionOutput>();
                oneList.add(out);
                outputs.put(address,oneList);
                return;
            }
        }
    }

    /*
     * 选择零钱，原则是尽量少的找钱，尽量少的使用输出笔数
     */
    private void selectSmallChange(Coin amount, List<TransactionOutput> lessThanList, List<TransactionOutput> outputs) {
        if(lessThanList == null || lessThanList.size() == 0) {
            return;
        }
        //排序
        Collections.sort(lessThanList, new Comparator<TransactionOutput>() {
            @Override
            public int compare(TransactionOutput o1, TransactionOutput o2) {
                long v1 = o1.getValue();
                long v2 = o2.getValue();
                if(v1 == v2) {
                    return 0;
                } else if(v1 > v2) {
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
            if(total.isGreaterThan(amount)) {
                //判断是否可以移除最小的几笔交易
                List<TransactionOutput> removeList = new ArrayList<TransactionOutput>();
                for (TransactionOutput to : outputs) {
                    total = total.subtract(Coin.valueOf(to.getValue()));
                    if(total.isGreaterThan(amount)) {
                        removeList.add(to);
                    } else {
                        break;
                    }
                }
                if(removeList.size() > 0) {
                    outputs.removeAll(removeList);
                }
                break;
            }
        }
    }

    /*
     * 选择零钱，原则先后顺序
     */
    private void selectSmallChangeMulUser(Coin amount, HashMap<String,List<TransactionOutput>> lessThanList, HashMap<String,List<TransactionOutput>> outputs) {
        if(lessThanList == null || lessThanList.size() == 0) {
            return;
        }
        //已选择的金额
        Coin total = Coin.ZERO;

        Iterator<String> lessit = lessThanList.keySet().iterator();
        while (lessit.hasNext()) {
            String address = lessit.next();
            List<TransactionOutput> userLessThanList=lessThanList.get(address);
            List<TransactionOutput> userOutputList= new ArrayList<TransactionOutput>();
            //从小到大选择
            for (TransactionOutput transactionOutput : userLessThanList) {
                userOutputList.add(transactionOutput);
                total = total.add(Coin.valueOf(transactionOutput.getValue()));
                if (total.isGreaterThan(amount)) {
                    break;
                }
            }
            outputs.put(address,userOutputList);
        }
    }

}
