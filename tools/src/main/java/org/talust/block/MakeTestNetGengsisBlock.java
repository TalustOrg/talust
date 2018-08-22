package org.talust.block;

import lombok.extern.slf4j.Slf4j;
import org.talust.common.crypto.ECKey;
import org.talust.common.crypto.Hex;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.model.Coin;

import org.talust.common.tools.Configure;
import org.talust.common.tools.DateUtil;
import org.talust.core.core.AccountTool;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;

import org.talust.core.data.ConsensusCalculationUtil;
import org.talust.core.model.Account;
import org.talust.core.model.Address;
import org.talust.core.model.Block;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.script.ScriptBuilder;
import org.talust.core.server.NtpTimeService;
import org.talust.core.storage.BlockStorage;
import org.talust.core.storage.BlockStore;
import org.talust.core.transaction.LocalTransactionSigner;
import org.talust.core.transaction.Transaction;
import org.talust.core.transaction.TransactionInput;
import org.talust.core.transaction.TransactionOutput;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


/**
 * 制作测试网络的创世块
 */
@Slf4j
public class MakeTestNetGengsisBlock {
    private static NetworkParams network = MainNetworkParams.get();
    private static BlockStorage blockStorage = BlockStorage.get();

    public static void main(String[] args) throws Exception {
        makeTestNetGengsisBlock();
    }

    private static void makeTestNetGengsisBlock() throws Exception {

        Block gengsisBlock = new Block(network);

        gengsisBlock.setPreHash(Sha256Hash.wrap(Hex.decode("0000000000000000000000000000000000000000000000000000000000000000")));
        gengsisBlock.setHeight(0);
        gengsisBlock.setTime(NtpTimeService.currentTimeSeconds());

        //交易列表
        List<Transaction> txs = new ArrayList<Transaction>();

        //产出货币总量
        Transaction coinBaseTx = new Transaction(network);
        coinBaseTx.setVersion(Definition.VERSION);
        coinBaseTx.setType(Definition.TYPE_COINBASE);

        TransactionInput input = new TransactionInput();
        coinBaseTx.addInput(input);
        input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a gengsis tx".getBytes()));

        //货币存放账户
        ECKey key = ECKey.fromPrivate(new BigInteger("114346884908037705206586672597018220174539506952607328716648835904805261540068"));
        Address address = AccountTool.newAddress(network, key);

//			Address address = Address.fromBase58(network, "toMahRViJBfKJ49QzYymKVb6JqNCLxTPN4");

        System.out.println("==========================");
        System.out.println(address.getBase58());
        System.out.println("==========================");

        coinBaseTx.addOutput(Coin.MAX.subtract(ConsensusCalculationUtil.TOTAL_REWARD), 0, address);
        coinBaseTx.verify();

        txs.add(coinBaseTx);

        //货币存放账户
        ECKey key1 = ECKey.fromPrivate(new BigInteger("2997229700204357452671714648402150025921621779744031016766434714937335169460"));
        Address address1 = AccountTool.newAddress(network, key1);

        //把永久锁定的转入一个账号内
        Transaction tx = new Transaction(network);
        tx.setVersion(Definition.VERSION);
        tx.setType(Definition.TYPE_PAY);
        tx.setLockTime(0l);
        input = new TransactionInput((TransactionOutput)coinBaseTx.getOutput(0));
        tx.addInput(input);

        //冻结数量
        Coin freeze = Coin.MAX.div(10);
        tx.addOutput(freeze, DateUtil.convertStringToDate("2019-08-01 00:00:00").getTime(), address1);
        tx.addOutput(Coin.valueOf(coinBaseTx.getOutput(0).getValue()).subtract(freeze), 0, address);

        //创建一个输入的空签名
        input.setScriptSig(ScriptBuilder.createInputScript(null, key));
        //签名交易
        final LocalTransactionSigner signer = new LocalTransactionSigner();
        signer.signInputs(tx, key);
        tx.verify();
        txs.add(tx);
        gengsisBlock.setTxs(txs);
        gengsisBlock.setTxCount(txs.size());

        Sha256Hash merkleHash = gengsisBlock.buildMerkleHash();
        System.out.println("the merkle hash is: "+ merkleHash);



        address = AccountTool.newAddress(network, key);

        System.out.println("==========================");
        System.out.println(address.getBase58());
        System.out.println("==========================");

        Account account = new Account(network);
        account.setAddress(address);
        account.setEcKey(key);
        account.setMgPubkeys(new byte[][]{ key.getPubKey()});

        gengsisBlock.sign(account);
        gengsisBlock.verifyScript();

        System.out.println("the block hash is: "+ Hex.encode(gengsisBlock.getHash().getBytes()));

        System.out.println(Hex.encode(gengsisBlock.baseSerialize()));


        Block block = new Block(network, gengsisBlock.baseSerialize());
        block.buildMerkleHash();
        BlockStore blockStore = new BlockStore(network,block);
        log.info("genesis : "+ Hex.encode(blockStore.serializeHeaderToBytes()));
        blockStorage.saveBlock(blockStore);
    }
}
