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

import org.talust.block.MakeTestNetGengsisBlock;
import org.talust.client.handler.*;
import org.talust.client.validator.BlockArrivedValidator;
import org.talust.client.validator.NodeExitValidator;
import org.talust.client.validator.TransactionValidator;
import org.talust.common.model.MessageType;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.DateUtil;
import org.talust.consensus.ConsensusService;
import org.talust.consensus.handler.MasterReqHandler;
import org.talust.consensus.handler.MasterRespHandler;
import org.talust.consensus.handler.NewMasterReqHandler;
import org.talust.consensus.handler.NewMasterRespHandler;
import org.talust.core.core.SynBlock;
import org.talust.core.server.NtpTimeService;
import org.talust.core.storage.TransactionStorage;
import org.talust.network.MessageHandler;
import org.talust.network.MessageValidator;
import org.talust.network.NodeConsole;
import org.talust.network.model.MyChannel;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.ConnectionManager;
import org.talust.network.netty.PeersManager;
import org.talust.network.netty.queue.MessageQueueHolder;
import org.talust.core.storage.AccountStorage;

import java.util.Collection;

@Slf4j
public class BlockChainServer {
    private AccountStorage accountStorage = AccountStorage.get();
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
        CacheManager.get().put("net_best_time",0);
        MessageQueueHolder.get().start();
        NodeConsole nc = new NodeConsole();
        nc.start();//启动网络的收发
        if(ConnectionManager.get().superNode){
            accountStorage.superNodeLogin();
            ConnectionManager.get().superNodeJoin();
        }else{
            accountStorage.nomorlNodeLogin();
            ConnectionManager.get().normalNodeJoin();
        }
        log.info("开启网络检查");
        ConnectionManager.get().startNetCheck();
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
        NtpTimeService.get().start();
        PeersManager.get().initPeers();
        AccountStorage.get();
        TransactionStorage.get().init();
        BlockChainServer.get().start();
    }
}
