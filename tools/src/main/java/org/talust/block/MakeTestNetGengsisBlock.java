package org.talust.block;

import org.talust.common.crypto.ECKey;
import org.talust.common.crypto.Hex;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.model.Coin;

import org.talust.core.core.AccountTool;
import org.talust.core.core.Definition;
import org.talust.core.core.NetworkParams;

import org.talust.core.model.Address;
import org.talust.core.model.Block;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.script.ScriptBuilder;
import org.talust.core.storage.BlockStorage;
import org.talust.core.storage.BlockStore;
import org.talust.core.transaction.Transaction;
import org.talust.core.transaction.TransactionInput;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


/**
 * 制作测试网络的创世块
 */
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
        gengsisBlock.setTime(1478070769l);

        //交易列表
        List<Transaction> txs = new ArrayList<Transaction>();

        //产出货币总量
        Transaction coinBaseTx = new Transaction(network);
        coinBaseTx.setVersion(Definition.VERSION);
        coinBaseTx.setType(Definition.TYPE_COINBASE);

        TransactionInput input = new TransactionInput();
        coinBaseTx.addInput(input);
        input.setScriptSig(ScriptBuilder.createCoinbaseInputScript("this a gengsis tx".getBytes()));

        //创世账户
        ECKey key = ECKey.fromPrivate(new BigInteger("67354228887878139695633819126625517515785554606767523849461500912225575561110"));

        Address address = AccountTool.newAddress(network, key);

        System.out.println("==========================");
        System.out.println(address.getBase58());
        System.out.println("==========================");

        coinBaseTx.addOutput(Coin.MAX, address);
        coinBaseTx.verify();

        txs.add(coinBaseTx);


//		//注册创世管理帐户
//		CertAccountRegisterTransaction certTx = new CertAccountRegisterTransaction(network, Hex.decode("0b01000000000000000000000098ba9559d02ae15f34b0209a87377f1e59c501730100022103ebe369f63421457abbca40b3295a3980db5488ee34d56ebe8d488f1d5d301f8321022700a96f3fd3b0d082abfd8bd758502f2e7e881eeaa5c69662c8eac7ade6d4330221028b3106d4cac5218388d2249503abab63c9b5de10525d13299d0423ab6f455a402103ca686fce8b25c1dd648e5dcbed9a8c95d8d5ec28baaefdbd7af2117fc22a6286c9c1200000000000000000000000000000000000000000000000000000000000000000c3140000000000000000000000000000000000000000874630440220296e7127545692d5580fc89aa84060374fb733facb91709fc2d8591b746e4baf022040cf22ff7ca342528890d867050473d861f3266d17119e705c68b950c0ffea4e4730450221008075c85feeee35d99e83a2678919904ff0b15283c017531fd5900f47efb65a47022056419266e203ec18e12fd3d77d8c2b68477f91a5f14ffee38b6e7878968a5621ac"));
//
//		certTx.verfify();
//		certTx.verfifyScript();
//
//		txs.add(certTx);

        gengsisBlock.setTxs(txs);

        gengsisBlock.setTxCount(txs.size());

        Sha256Hash merkleHash = gengsisBlock.buildMerkleHash();

        BlockStore blockStore = new BlockStore(network, gengsisBlock);

//		Assert.assertTrue(Arrays.equals(gengsisBlock.baseSerialize(), blockStore.baseSerialize()));

        BlockStore gengsisBlockTemp = new BlockStore(network, blockStore.baseSerialize());

        Sha256Hash merkleHashTemp = gengsisBlockTemp.getBlock().buildMerkleHash();

        System.out.println("merkle hash temp: " + merkleHashTemp);
        blockStorage.saveBlock(gengsisBlockTemp);
    }
}
