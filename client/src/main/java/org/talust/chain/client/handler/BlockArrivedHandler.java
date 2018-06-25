package org.talust.chain.client.handler;

import org.talust.chain.block.data.DataContainer;
import org.talust.chain.block.model.Block;
import org.talust.chain.common.crypto.Hex;
import org.talust.chain.common.crypto.Sha256Hash;
import org.talust.chain.common.model.Message;
import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.tools.CacheManager;
import org.talust.chain.common.tools.Constant;
import org.talust.chain.common.tools.SerializationUtil;
import org.talust.chain.consensus.Conference;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.storage.BlockStorage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j//其他节点广播出来的区块数据
public class BlockArrivedHandler implements MessageHandler {
    private BlockStorage blockStorage = BlockStorage.get();
    //    private MessageQueueHolder mqHolder = MessageQueueHolder.get();
    private CacheManager cu = CacheManager.get();
    private TransactionHandler transactionHandler = new TransactionHandler();

    @Override
    public boolean handle(MessageChannel messageChannel) {
        log.info("接收到远端ip:{} 发送过来的区块数据", messageChannel.getFromIp());
//        mqHolder.broadMessage(messageChannel);
        //log.info("区块数据验证没有问题...");
        try {
            saveBlock(messageChannel);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 保存区块
     *
     * @param messageChannel
     * @return
     */
    public byte[] saveBlock(MessageChannel messageChannel) {
        byte[] blockBytes = messageChannel.getMessage().getContent();
        Block block = SerializationUtil.deserializer(blockBytes, Block.class);
        byte[] hash = Sha256Hash.of(blockBytes).getBytes();
        blockStorage.put(hash, blockBytes);//存储区块
        blockStorage.put(Constant.NOW_BLOCK_HASH, hash);//存储最新区块的hash值
        byte[] bh = (Constant.BH_PRIX + block.getHead().getHeight()).getBytes();
        blockStorage.put(bh, hash);//key为区块高度,value为区块的hash值
        //将区块的最新打包时间写入缓存
        cu.setCurrentBlockHeight(block.getHead().getHeight());
        cu.setCurrentBlockTime(block.getHead().getTime());
        cu.setCurrentBlockHash(hash);
        if(Conference.get().getMaster()!=null){
            cu.setCurrentBlockGenIp(Conference.get().getMaster().getIp());//设置收到的块时最新的master节点ip
        }
        log.info("成功存储区块数据,当前hash:{},height:{},time:{}", Hex.encode(hash), block.getHead().getHeight(), block.getHead().getTime());

        List<byte[]> data = block.getBody().getData();
        for (byte[] datum : data) {
            MessageChannel nm = new MessageChannel();
            Message msg = SerializationUtil.deserializer(datum, Message.class);
            nm.setMessage(msg);
            transactionHandler.handle(nm);
            DataContainer.get().removeRecord(datum);
        }
        //@TODO 区块存储时,需要将里面的数据解析出来进行一些索引的存储,比如交易信息这些
        return hash;
    }
}
