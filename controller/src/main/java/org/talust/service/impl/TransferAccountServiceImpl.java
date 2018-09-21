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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.talust.client.validator.TransactionValidator;
import org.talust.common.crypto.*;
import org.talust.common.exception.KeyCrypterException;
import org.talust.common.model.*;
import org.talust.common.tools.*;
import org.talust.core.core.Definition;
import org.talust.core.core.ECKey;
import org.talust.core.core.NetworkParams;
import org.talust.core.core.SynBlock;
import org.talust.core.data.TransactionCache;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.script.Script;
import org.talust.core.script.ScriptBuilder;
import org.talust.core.server.NtpTimeService;
import org.talust.core.storage.*;
import org.talust.core.transaction.*;
import org.talust.model.TxMine;
import org.talust.network.model.MyChannel;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.ConnectionManager;
import org.talust.service.TransferAccountService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private ChainStateStorage chainStateStorage = ChainStateStorage.get();

    private TransactionCreator transactionCreator = new TransactionCreator();

    private TransactionValidator transactionValidator = new TransactionValidator();
    private NetworkParams network = MainNetworkParams.get();

    @Override
    public boolean decryptAccount(String password, Account account) {
        if (!validPassword(password)) {
            return false;
        }
        if (account == null) {
            return false;
        }
        try {
            ECKey keys = account.getEcKey();
            if(keys.getEncryptedPrivateKey()==null){
                EncryptedData encryptedData = new EncryptedData(account.getPriSeed());
                keys.setEncryptedPrivateKey(encryptedData);
            }
            keys = keys.decrypt(password);
            if(null==keys){
                return false;
            }
        } catch (KeyCrypterException e) {
            return false;
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
        JSONObject resp = new JSONObject();
        Utils.checkNotNull(toAddress);
        Collection<MyChannel> connects = ChannelContain.get().getMyChannels();
        if (connects.size() <= 0) {
            resp.put("retCode", "1");
            resp.put("msg", "当前网络不可用，请稍后再尝试");
            return resp;
        }
        long height = MainNetworkParams.get().getBestBlockHeight();
        long localbestheighttime = BlockStorage.get().getBestBlockHeader().getBlockHeader().getTime();
        if (height == 0) {
            if (SynBlock.get().getSyning().get()) {
                resp.put("retCode", "1");
                resp.put("msg", "正在同步区块中，请稍后再尝试");
                return resp;
            } else {
                ConnectionManager.get().init();
                resp.put("retCode", "1");
                resp.put("msg", "当前网络不可用，正在重试网络和数据修复，请稍后再尝试");
                return resp;
            }
        }
        long now = NtpTimeService.currentTimeSeconds();
        if (now - localbestheighttime > 6) {
            if (SynBlock.get().getSyning().get()) {
                resp.put("retCode", "1");
                resp.put("msg", "正在同步区块中，请稍后再尝试");
                return resp;
            } else {
                ConnectionManager.get().init();
                resp.put("retCode", "1");
                resp.put("msg", "当前网络不可用，正在重试网络和数据修复，请稍后再尝试");
                return resp;
            }
        }
        locker.lock();
        try {
            if (money.compareTo("0") <= 0) {
                resp.put("retCode", "1");
                resp.put("msg", "发送的金额需大于0");
                return resp;
            }
            Account account = this.getAccountByAddress(address);
            if (null == account) {
                resp.put("retCode", "1");
                resp.put("msg", "出账账户不存在");
                return resp;
            }
            if (account.getAddress().getBase58().equals(toAddress)) {
                resp.put("retCode", "1");
                resp.put("msg", "不能给自己转账");
                return resp;
            }
            if (account.isEncrypted()) {
                if (StringUtil.isNullOrEmpty(password)) {
                    resp.put("retCode", "1");
                    resp.put("msg", "输入钱包密码进行转账");
                    return resp;
                } else {
                    boolean pswCorrect = this.decryptAccount(password, account);
                    if (!pswCorrect) {
                        resp.put("retCode", "1");
                        resp.put("msg", "账户密码不正确");
                        return resp;
                    }
                }
            }
            //当前余额可用余额
            long balance = account.getAddress().getBalance().value;
            if (ArithUtils.compareStr(balance + "", ArithUtils.mul(money, "100000000", 0)) < 0) {
                resp.put("retCode", "1");
                resp.put("msg", "余额不足");
                return resp;
            }
            Transaction tx = new Transaction(MainNetworkParams.get());
            tx.setVersion(Definition.VERSION);
            tx.setType(Definition.TYPE_PAY);
            Coin totalInputCoin = Coin.ZERO;
            //根据交易金额获取当前交易地址下所有的未花费交易
            Coin pay = Coin.COIN.multiply((long) Double.parseDouble(money));
            List<TransactionOutput> fromOutputs = transactionCreator.selectNotSpentTransaction(pay, account.getAddress());
            TransactionInput input = new TransactionInput();
            for (TransactionOutput output : fromOutputs) {
                input.addFrom(output);
                totalInputCoin = totalInputCoin.add(Coin.valueOf(output.getValue()));
            }
            //创建一个输入的空签名
            if (account.getAccountType() == network.getSystemAccountVersion()) {
                //普通账户的签名
                input.setScriptSig(ScriptBuilder.createInputScript(null, account.getEcKey()));
            } else {
                //认证账户的签名
                input.setScriptSig(ScriptBuilder.createCertAccountInputScript(null, account.getAccountTransaction().getHash().getBytes(), account.getAddress().getHash160()));
            }
            tx.addInput(input);

            //交易输出
            tx.addOutput(pay, Address.fromBase58(network, toAddress));
            //是否找零
            if (totalInputCoin.compareTo(pay) > 0) {
                tx.addOutput(totalInputCoin.subtract(pay), account.getAddress());
            }

            //签名交易
            final LocalTransactionSigner signer = new LocalTransactionSigner();
            try {
                if (account.getAccountType() == network.getSystemAccountVersion()) {
                    //普通账户的签名
                    signer.signInputs(tx, account.getEcKey());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            //验证交易是否合法
            if (transactionValidator.checkTransaction(tx, null)) {
                //加入内存池，因为广播的Inv消息出去，其它对等体会回应getDatas获取交易详情，会从本机内存取出来发送
                boolean success = TransactionCache.getInstace().add(tx);
                Message message = new Message();
                byte[] data = SerializationUtil.serializer(tx);
                message.setContent(data);
                message.setType(MessageType.TRANSACTION.getType());
                message.setTime(NtpTimeService.currentTimeSeconds());
                //广播交易
                ConnectionManager.get().TXMessageSend(message);
                resp.put("retCode", "0");
                resp.put("msg", "交易已上送");
            }
        } catch (Exception e) {
        } finally {
            locker.unlock();
        }
        //验证本账户金额与交易金额是否正常
        return resp;
    }

    /**
     * 校验密码难度
     *
     * @param password
     * @return boolean
     */
    public static boolean validPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return false;
        }
        if (password.length() < 6) {
            return false;
        }
        if (password.matches("(.*)[a-zA-z](.*)") && password.matches("(.*)\\d+(.*)")) {
            return true;
        } else {
            return false;
        }
    }



    public Deposits getDeposits(byte[] hash160) {
        return chainStateStorage.getDeposits(hash160);
    }

    @Override
    public JSONArray getAllDeposits() {
        Collection<SuperNode> superNodes = ConnectionManager.get().getSuperNodes();
        List<SuperNode> superNodeList = new ArrayList<>();
        superNodeList.addAll(superNodes);
        Collections.sort(superNodeList, new Comparator<SuperNode>() {
            @Override
            public int compare(SuperNode o1, SuperNode o2) {
                int o1Id =Integer.parseInt( o1.getId());
                int o2Id = Integer.parseInt( o2.getId());
                return o1Id-o2Id;
            }
        });
        JSONArray dataList = new JSONArray();
        for (SuperNode superNode : superNodeList) {
            JSONObject data = new JSONObject();
            data.put("address", superNode.getAddress());
            Address address = Address.fromBase58(network, superNode.getAddress());
            Deposits deposits = getDeposits(address.getHash160());
            List<DepositAccount> depositAccountList = deposits.getDepositAccounts();
            if (null != depositAccountList && depositAccountList.size() > 0) {
                data.put("size", deposits.getDepositAccounts().size());
                Coin totalCoin = Coin.ZERO;
                Coin minCoin = Coin.ZERO;
                Coin maxCoin = Coin.ZERO;
                for (DepositAccount depositAccount : depositAccountList) {
                    Coin coin = depositAccount.getAmount();
                    if (minCoin.isGreaterThan(coin) || minCoin.isZero()) {
                        minCoin = coin;
                    }
                    if (maxCoin.isLessThan(coin)) {
                        maxCoin = coin;
                    }
                    totalCoin = totalCoin.add(coin);
                }

                data.put("totalCoin", ArithUtils.div(totalCoin.getValue() + "", "100000000", 8));
                data.put("minCoin", ArithUtils.div(minCoin.getValue() + "", "100000000", 8));
                data.put("maxCoin", ArithUtils.div(maxCoin.getValue() + "", "100000000", 8));
            } else {
                data.put("size", 0);
                data.put("totalCoin", 0);
                data.put("minCoin", 0);
                data.put("maxCoin", 0);
            }
            data.put("id", superNode.getId());
            dataList.add(data);
        }
        return dataList;
    }

    @Override
    public JSONObject consensusJoin(String nodeAddress, String money, String address, String password) {
        JSONObject resp = new JSONObject();
        Utils.checkNotNull(nodeAddress);
        Collection<MyChannel> connects = ChannelContain.get().getMyChannels();
        if (connects.size() <= 0) {
            resp.put("retCode", "1");
            resp.put("msg", "当前网络不可用，请稍后再尝试");
            return resp;
        }
        long height = MainNetworkParams.get().getBestBlockHeight();
        long localbestheighttime = BlockStorage.get().getBestBlockHeader().getBlockHeader().getTime();
        if (height == 0) {
            if (SynBlock.get().getSyning().get()) {
                resp.put("retCode", "1");
                resp.put("msg", "正在同步区块中，请稍后再尝试");
                return resp;
            } else {
                ConnectionManager.get().init();
                resp.put("retCode", "1");
                resp.put("msg", "当前网络不可用，正在重试网络和数据修复，请稍后再尝试");
                return resp;
            }
        }
        long now = NtpTimeService.currentTimeSeconds();
        if (now - localbestheighttime > 6) {
            if (SynBlock.get().getSyning().get()) {
                resp.put("retCode", "1");
                resp.put("msg", "正在同步区块中，请稍后再尝试");
                return resp;
            } else {
                ConnectionManager.get().init();
                resp.put("retCode", "1");
                resp.put("msg", "当前网络不可用，正在重试网络和数据修复，请稍后再尝试");
                return resp;
            }
        }
        locker.lock();
        try {
            if (money.compareTo("10000") < 0) {
                resp.put("retCode", "1");
                resp.put("msg", "发送的金额需大于10000");
                return resp;
            }
            Account account = this.getAccountByAddress(address);
            if (null == account) {
                resp.put("retCode", "1");
                resp.put("msg", "出账账户不存在");
                return resp;
            }
            if (account.isEncrypted()) {
                if (StringUtil.isNullOrEmpty(password)) {
                    resp.put("retCode", "1");
                    resp.put("msg", "输入钱包密码进行交易");
                    return resp;
                } else {
                    boolean pswCorrect = this.decryptAccount(password, account);
                    if (!pswCorrect) {
                        resp.put("retCode", "1");
                        resp.put("msg", "账户密码不正确");
                        return resp;
                    }
                }
            }
            //当前余额可用余额
            long balance = account.getAddress().getBalance().value;
            if (ArithUtils.compareStr(balance + "", ArithUtils.mul(money, "100000000", 0)) < 0) {
                resp.put("retCode", "1");
                resp.put("msg", "余额不足");
                return resp;
            }
            Address nodeAddr = Address.fromBase58(network, nodeAddress);
            Deposits deposits = getDeposits(nodeAddr.getHash160());
            List<DepositAccount> depositAccountList = deposits.getDepositAccounts();
            if (null == depositAccountList || depositAccountList.size() < 100) {
                Transaction tx = transactionCreator.createRegConsensus(money, account, nodeAddr.getHash160());
                verifyAndSendMsg(account, tx);
            } else {
                DepositAccount depositAccount = checkAmtLowest(money, depositAccountList);
                if (null != depositAccount) {
                    //大于最低的账户的金额则
                    Transaction regTx = transactionCreator.createRegConsensus(money, account, nodeAddr.getHash160());
                    verifyAndSendMsg(account, regTx);
                    Transaction remTx = transactionCreator.createRemConsensus(depositAccount, null,nodeAddr.getHash160());
                    verifyAndSendMsg(account, remTx);
                } else {
                    resp.put("retCode", "1");
                    resp.put("msg", "当前节点共识已满，且参与共识金额小于当前最低的已参与共识金额");
                    return resp;
                }
            }

            resp.put("retCode", "0");
            resp.put("msg", "交易已上送");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            locker.unlock();
        }
        //验证本账户金额与交易金额是否正常
        return resp;
    }

    @Override
    public JSONObject consensusLeave(String nodeAddress, String address, String password) {
        JSONObject resp = new JSONObject();
        Utils.checkNotNull(nodeAddress);
        Collection<MyChannel> connects = ChannelContain.get().getMyChannels();
        if (connects.size() <= 0) {
            resp.put("retCode", "1");
            resp.put("msg", "当前网络不可用，请稍后再尝试");
            return resp;
        }
        long height = MainNetworkParams.get().getBestBlockHeight();
        long localbestheighttime = BlockStorage.get().getBestBlockHeader().getBlockHeader().getTime();
        if (height == 0) {
            if (SynBlock.get().getSyning().get()) {
                resp.put("retCode", "1");
                resp.put("msg", "正在同步区块中，请稍后再尝试");
                return resp;
            } else {
                ConnectionManager.get().init();
                resp.put("retCode", "1");
                resp.put("msg", "当前网络不可用，正在重试网络和数据修复，请稍后再尝试");
                return resp;
            }
        }
        long now = NtpTimeService.currentTimeSeconds();
        if (now - localbestheighttime > 6) {
            if (SynBlock.get().getSyning().get()) {
                resp.put("retCode", "1");
                resp.put("msg", "正在同步区块中，请稍后再尝试");
                return resp;
            } else {
                ConnectionManager.get().init();
                resp.put("retCode", "1");
                resp.put("msg", "当前网络不可用，正在重试网络和数据修复，请稍后再尝试");
                return resp;
            }
        }
        locker.lock();
        try {
            Account account = this.getAccountByAddress(address);
            if (null == account) {
                resp.put("retCode", "1");
                resp.put("msg", "出账账户不存在");
                return resp;
            }
            if (account.isEncrypted()) {
                if (StringUtil.isNullOrEmpty(password)) {
                    resp.put("retCode", "1");
                    resp.put("msg", "输入钱包密码进行交易");
                    return resp;
                } else {
                    boolean pswCorrect = this.decryptAccount(password, account);
                    if (!pswCorrect) {
                        resp.put("retCode", "1");
                        resp.put("msg", "账户密码不正确");
                        return resp;
                    }
                }
            }
            Address nodeAddr = Address.fromBase58(network, nodeAddress);
            Deposits deposits = getDeposits(nodeAddr.getHash160());
            String hash160 = Base58.encode(account.getAddress().getHash160());
            List<DepositAccount> depositAccountList = deposits.getDepositAccounts();
            for (DepositAccount depositAccount : depositAccountList) {
                String test = Base58.encode(depositAccount.getAddress());
                boolean is = hash160.equals(test);
                if (is) {
                    Transaction remTx = transactionCreator.createRemConsensus(depositAccount, account,nodeAddr.getHash160());
                    verifyAndSendMsg(account, remTx);
                    resp.put("retCode", "0");
                    resp.put("msg", "交易已上送");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            locker.unlock();
        }
        //验证本账户金额与交易金额是否正常
        return resp;
    }

    @Override
    public JSONObject searchAllTransfer(String address) {
        Address addr = Address.fromBase58(MainNetworkParams.get(), address);
        Map<byte[], List<TransactionStore>> txMaps = new HashMap<>();
        List<TransactionStore> txList = transactionStorage.getMyTxList();
        List<TransactionInput> allInput = new ArrayList<>();
        List<TransactionOutput> allOutput = new ArrayList<>();
        for (TransactionStore transactionStore : txList) {
            Transaction tx = transactionStore.getTransaction();
            if (tx.getType() != Definition.TYPE_COINBASE) {
                List<TransactionInput> inputs = tx.getInputs();
                List<TransactionOutput> outputs = tx.getOutputs();
                allInput.addAll(BlockStorage.get().findTxInputsIsMine(inputs, addr.getHash160()));
                allOutput.addAll(BlockStorage.get().findTxOutputIsMine(outputs, addr.getHash160(), transactionStore.getTransaction().getType()));
            }
        }
        List<TxMine> txMines = new ArrayList<>();
        for (TransactionInput input : allInput) {
            List<TransactionOutput> transactionOutputList = input.getParent().getOutputs();
            for (TransactionOutput transactionOutput : transactionOutputList) {
                if (!Arrays.equals(transactionOutput.getScript().getChunks().get(2).data, addr.getHash160())) {
                    String toAddress = Address.fromP2PKHash(MainNetworkParams.get(), MainNetworkParams.get().getSystemAccountVersion(), transactionOutput.getScript().getChunks().get(2).data).getBase58();
                    TxMine txMine = new TxMine();
                    txMine.setType(input.getParent().getType());
                    txMine.setAsCode(1);
                    txMine.setTime(input.getParent().getTime());
                    txMine.setMoney(transactionOutput.getValue() / 100000000);
                    txMine.setAddress(toAddress);
                    txMines.add(txMine);
                }else if(input.getParent().getType()==Definition.TYPE_REG_CONSENSUS||input.getParent().getType()==Definition.TYPE_REM_CONSENSUS){
                    TxMine txMine = new TxMine();
                    txMine.setType(input.getParent().getType());
                    txMine.setAsCode(1);
                    txMine.setTime(input.getParent().getTime());
                    txMine.setMoney(transactionOutput.getValue() / 100000000);
                    txMine.setAddress(address);
                    txMines.add(txMine);
                }
            }
        }
        for (TransactionOutput output : allOutput) {
            Sha256Hash fromId =  output.getParent().getInputs().get(0).getFroms().get(0).getParent().getHash();
                //对上一交易的引用以及索引值
                TransactionStore txStore = BlockStorage.get().getTransaction(fromId.getBytes());
                if (txStore != null) {
                    Script script = txStore.getTransaction().getOutput(0).getScript();
                    String toAddress = Address.fromP2PKHash(MainNetworkParams.get(), MainNetworkParams.get().getSystemAccountVersion(), script.getChunks().get(2).data).getBase58();
                    TxMine txMine = new TxMine();
                    txMine.setType(output.getParent().getType());
                    txMine.setAsCode(0);
                    txMine.setMoney(output.getValue() / 100000000);
                    txMine.setTime(output.getParent().getTime());
                    txMine.setAddress(toAddress);
                    txMines.add(txMine);
                }
        }
        Collections.sort(txMines, new Comparator<TxMine>() {
            @Override
            public int compare(TxMine o1, TxMine o2) {
                if (o1.getTime() < o2.getTime()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        JSONObject resp = new JSONObject();
        resp.put(address, txMines);
        return resp;
    }

    @Override
    public JSONObject searchAllCoinBaseTransfer(String address, String date) {
        try {
            Address addr = Address.fromBase58(MainNetworkParams.get(), address);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = simpleDateFormat.parse(date);
            startDate.setHours(00);
            long start = startDate.getTime() / 1000;
            Date endDate = startDate;
            endDate.setHours(24);
            long end = endDate.getTime() / 1000;
            Map<byte[], List<TransactionStore>> txMaps = new HashMap<>();
            List<TransactionStore> txList = transactionStorage.getMyTxList();
            List<TransactionOutput> allOutput = new ArrayList<>();
            for (TransactionStore transactionStore : txList) {
                Transaction tx = transactionStore.getTransaction();
                if (start < tx.getTime() && end > tx.getTime()) {
                    if (tx.getType() == Definition.TYPE_COINBASE) {
                        List<TransactionOutput> outputs = tx.getOutputs();
                        allOutput.addAll(BlockStorage.get().findTxOutputIsMine(outputs, addr.getHash160(), transactionStore.getTransaction().getType()));
                    }
                }
            }
            List<TxMine> txMines = new ArrayList<>();
            for (TransactionOutput output : allOutput) {
                TxMine txMine = new TxMine();
                txMine.setType(Definition.TYPE_COINBASE);
                txMine.setAsCode(0);
                txMine.setMoney(output.getValue() / 100000000);
                txMine.setTime(output.getParent().getTime());
                txMines.add(txMine);
            }
            Collections.sort(txMines, new Comparator<TxMine>() {
                @Override
                public int compare(TxMine o1, TxMine o2) {
                    if (o1.getTime() < o2.getTime()) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });
            JSONObject resp = new JSONObject();
            resp.put(addr.getBase58(), txMines);
            return resp;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    public DepositAccount checkAmtLowest(String amt, List<DepositAccount> depositAccounts) {
        long money = Long.parseLong(ArithUtils.mul(amt, "100000000", 0));
        DepositAccount lowest = null;
        for (DepositAccount depositAccount : depositAccounts) {
            if (money > depositAccount.getAmount().value) {
                if (null == lowest) {
                    lowest = depositAccount;
                } else if (lowest.getAmount().value > depositAccount.getAmount().value) {
                    lowest = depositAccount;
                }
            }
        }
        return lowest;
    }

    public void verifyAndSendMsg(Account account, Transaction tx) {
        //验证交易是否合法
        assert transactionValidator.checkTransaction(tx, null);
        //加入内存池，因为广播的Inv消息出去，其它对等体会回应getDatas获取交易详情，会从本机内存取出来发送
        boolean success = TransactionCache.getInstace().add(tx);
        Message message = new Message();
        byte[] data = SerializationUtil.serializer(tx);
        message.setContent(data);
        message.setType(MessageType.TRANSACTION.getType());
        message.setTime(NtpTimeService.currentTimeSeconds());
        //广播交易
        ConnectionManager.get().TXMessageSend(message);
    }

    @Override
    public JSONObject searchAddressConsensusStatus(String address) {
        Collection<SuperNode> superNodes = ConnectionManager.get().getSuperNodes();
        Address addr = Address.fromBase58(MainNetworkParams.get(), address);
        JSONObject resp = new JSONObject();
        for (SuperNode superNode : superNodes) {
            JSONObject data = new JSONObject();
            Address superNodeAddress = Address.fromBase58(network, superNode.getAddress());
            Deposits deposits = getDeposits(superNodeAddress.getHash160());
            DepositAccount joinDepos = null;
            List<DepositAccount> depositAccountList = deposits.getDepositAccounts();
            if (null != depositAccountList && depositAccountList.size() > 0) {
                Coin totalCoin = Coin.ZERO;
                for (DepositAccount depositAccount : depositAccountList) {
                    totalCoin = totalCoin.add(depositAccount.getAmount());
                    if (Arrays.equals(depositAccount.getAddress(), addr.getHash160())) {
                        joinDepos = depositAccount;
                    }
                }
                if (joinDepos != null) {
                    int rate = 1;
                    for (DepositAccount depositAccount : depositAccountList) {
                        if (joinDepos.getAmount().isLessThan(depositAccount.getAmount())) {
                            rate++;
                        }
                    }
                    data.put("rate", rate);
                    List<Sha256Hash> txHashs = joinDepos.getTxHash();
                    if (txHashs != null) {
                        long lastTime = 0;
                        for (Sha256Hash txHash : txHashs) {
                            TransactionStore tx = transactionStorage.getTransaction(txHash);
                            if (lastTime == 0 || lastTime < tx.getTransaction().getTime()) {
                                lastTime = tx.getTransaction().getTime();
                            }
                        }
                        data.put("time", lastTime);
                    }
                    data.put("totalCoin", ArithUtils.div(totalCoin.value + "", "100000000", 8));
                }
            }
            if (data != null && data.size() > 0) {
                data.put("address", superNode.getAddress());
                resp.put(superNode.getId(), data);
            }
        }
        return resp;
    }
}
