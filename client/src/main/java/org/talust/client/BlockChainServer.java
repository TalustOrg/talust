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

package org.talust.client;

import lombok.extern.slf4j.Slf4j;
import org.talust.account.MiningAddress;
import org.talust.block.SynBlock;
import org.talust.block.model.Block;
import org.talust.client.handler.*;
import org.talust.client.validator.BlockArrivedValidator;
import org.talust.client.validator.NodeExitValidator;
import org.talust.client.validator.NodeJoinValidator;
import org.talust.client.validator.TransactionValidator;
import org.talust.common.model.MessageType;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.Constant;
import org.talust.common.tools.SerializationUtil;
import org.talust.consensus.ConsensusService;
import org.talust.consensus.handler.MasterReqHandler;
import org.talust.consensus.handler.MasterRespHandler;
import org.talust.consensus.handler.NewMasterReqHandler;
import org.talust.consensus.handler.NewMasterRespHandler;
import org.talust.network.MessageHandler;
import org.talust.network.MessageValidator;
import org.talust.network.NodeConsole;
import org.talust.network.model.MyChannel;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.PeersManager;
import org.talust.network.netty.queue.MessageQueueHolder;
import org.talust.storage.AccountStorage;
import org.talust.storage.BlockStorage;
import org.talust.storage.ChainStateStorage;
import org.talust.storage.TransactionStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class BlockChainServer {
    private BlockStorage blockStorage = BlockStorage.get();
    private AccountStorage accountStorage = AccountStorage.get();
    private ChainStateStorage chainStateStorage = ChainStateStorage.get();
    private TransactionStorage transactionStorage = TransactionStorage.get();
    private MessageQueueHolder queueHolder = MessageQueueHolder.get();

    private static BlockChainServer instance = new BlockChainServer();

    private BlockChainServer() {
    }

    public static BlockChainServer get() {
        return instance;
    }

    public void start() throws Exception {
        log.info("初始化消息处理器...");
        initValidators();
        initHandlers();
        MessageQueueHolder.get().start();

        NodeConsole nc = new NodeConsole();
        nc.start();//启动网络的收发

        Collection<MyChannel> allChannel = ChannelContain.get().getMyChannels();
        log.info("当前节点所连接的节点数:{}", allChannel.size());
        if (allChannel != null && allChannel.size() > 0) {//说明有其他的节点
            SynBlock.get().setBlockArrivedValidator(new BlockArrivedValidator());
            SynBlock.get().setBlockArrivedHandler(new BlockArrivedHandler());
            SynBlock.get().startSynBlock();//同步区块
        }

        //启动共识
        ConsensusService.get().start();

    }

    private void initValidators() {
        addValidator(MessageType.BLOCK_ARRIVED, new BlockArrivedValidator());
        addValidator(MessageType.NODE_EXIT, new NodeExitValidator());
        addValidator(MessageType.NODE_JOIN, new NodeJoinValidator());
        addValidator(MessageType.TRANSACTION, new TransactionValidator());
    }

    /**
     * 初始化消息处理器
     */
    private void initHandlers() {
        addHandler(MessageType.NODES_REQ, new NetNodesReqHandler());
        addHandler(MessageType.NODES_RESP, new NetNodesRespHandler());
        addHandler(MessageType.BLOCK_ARRIVED, new BlockArrivedBroadHandler());
        addHandler(MessageType.BLOCK_ARRIVED, new BlockArrivedHandler());
        addHandler(MessageType.HEIGHT_REQ, new BlockHeightReqHandler());
        addHandler(MessageType.HEIGHT_RESP, new BlockHeightRespHandler());
        addHandler(MessageType.BLOCK_REQ, new BlockDataReqHandler());
        addHandler(MessageType.BLOCK_RESP, new BlockDataRespHandler());
        addHandler(MessageType.ERROR_MESSAGE, new ErrorMessageHandler());
        addHandler(MessageType.NODE_JOIN, new NodeJoinHandler());
        addHandler(MessageType.NODE_JOIN_RESP, new NodeJoinRespHandler());
        addHandler(MessageType.NODE_EXIT, new NodeExitHandler());
        addHandler(MessageType.TRANSACTION, new TransactionHandler());
        addHandler(MessageType.MASTER_REQ, new MasterReqHandler());
        addHandler(MessageType.MASTER_RESP, new MasterRespHandler());
        addHandler(MessageType.NEW_MASTER_REQ, new NewMasterReqHandler());
        addHandler(MessageType.NEW_MASTER_RESP, new NewMasterRespHandler());
    }

    public void addValidator(MessageType messageType, MessageValidator validator) {
        queueHolder.setValidator(messageType, validator);
    }

    public void addHandler(MessageType messageType, MessageHandler handler) {
        queueHolder.addHandler(messageType, handler);
    }


    /**
     * 初始化存储
     */
    public void initStorage() throws Exception {
        log.info("初始化存储...");
        blockStorage.init();
        chainStateStorage.init();
        transactionStorage.init();
        accountStorage.init();
        PeersManager.get().initPeers();

        log.info("初始化缓存...");
             byte[] nowBlockHash = blockStorage.get(Constant.NOW_BLOCK_HASH);

        if (nowBlockHash != null) {
            byte[] nowBlock = blockStorage.get(nowBlockHash);
            byte[] addrBytes = blockStorage.get(Constant.MINING_ADDRESS);
            List<String>  miningAddress = new ArrayList<>();
            if(addrBytes!=null){
                MiningAddress miningAddres = SerializationUtil.deserializer(addrBytes, MiningAddress.class);
                if (miningAddres != null) {
                    miningAddress = miningAddres.getAddress();
                }
                CacheManager.get().put(new String(Constant.MINING_ADDRESS), miningAddress);
            }
            if (nowBlock != null) {
                Block block = SerializationUtil.deserializer(nowBlock, Block.class);
                //将区块的最新打包时间写入缓存
                CacheManager.get().setCurrentBlockTime(block.getHead().getTime());
                CacheManager.get().setCurrentBlockHeight(block.getHead().getHeight());
                CacheManager.get().setCurrentBlockHash(nowBlockHash);
            }
        }
        BlockChainServer.get().start();
    }
}
