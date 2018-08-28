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

package org.talust.core.core;

import lombok.extern.slf4j.Slf4j;
import org.talust.common.model.Message;
import org.talust.common.model.MessageChannel;
import org.talust.common.model.MessageType;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.SerializationUtil;
import org.talust.common.tools.ThreadPool;
import org.talust.core.model.Block;
import org.talust.core.network.MainNetworkParams;
import org.talust.core.storage.AccountStorage;
import org.talust.core.storage.BlockStorage;
import org.talust.core.storage.BlockStore;
import org.talust.network.MessageHandler;
import org.talust.network.MessageValidator;
import org.talust.network.model.MyChannel;
import org.talust.network.netty.ChannelContain;
import org.talust.network.netty.ConnectionManager;
import org.talust.network.netty.SynRequest;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public class SynBlock {
    private final int THREAD_POOL_SIZE = 20;
    private ThreadPoolExecutor threadPool = ThreadPool.get().threadPool;

    private static SynBlock instance = new SynBlock();

    private SynBlock() {
    }

    public static SynBlock get() {
        return instance;
    }

    private AccountStorage accountStorage = AccountStorage.get();
    private MessageHandler blockArrivedHandler;//区块到来处理器
    private MessageValidator blockArrivedValidator;//区块到来校验器
    private AtomicBoolean syning = new AtomicBoolean(false);//是否正在同步

    //开始进行数据块的同步
    public void startSynBlock() {
        if (syning.get()) {//如果当前正在同步区块,则直接返回
            return;
        }
        syning.set(true);//设置为正在同步
        try {
            synBlock();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        syning.set(false);

    }

    private void synBlock() {
        log.info("进行数据块的同步...");

        long selfBlockHeight = MainNetworkParams.get().getBestBlockHeight();
        List<Future<MessageChannel>> results = new ArrayList<>();
        Collection<MyChannel> allChannel = ChannelContain.get().getMyChannels();
        log.info("当前区块高度为:{},本节点连接的远端节点数为:{}", selfBlockHeight, allChannel.size());
        for (final MyChannel channel : allChannel) {
            Future<MessageChannel> submit = threadPool.submit(() -> {
                Message nodeMessage = new Message();
                nodeMessage.setType(MessageType.HEIGHT_REQ.getType());
                log.info("向远端ip:{}请求当前网络的区块高度...", channel.getRemoteIp());
                MessageChannel message = SynRequest.get().synReq(nodeMessage, channel.getRemoteIp());
                if (message != null) {
                    log.info("远端ip:{}返回当前区块高度:{}", channel.getRemoteIp(), new String(message.getMessage().getContent()));
                } else {
                    log.info("-------------------------------------------");
                }
                return message;
            });
            results.add(submit);
        }
        //最高区块高度
        int maxBlockHeight = -1;
        //用于存储每个通道的区块高度
        Map<String, Integer> channelBlockHeight = new HashMap<>();
        for (Future<MessageChannel> result : results) {
            MessageChannel nodeMessage;
            try {
                nodeMessage = result.get();
                if (nodeMessage != null) {
                    String toChannel = nodeMessage.getFromIp();
                    int bh = Integer.parseInt(new String(nodeMessage.getMessage().getContent()));
                    channelBlockHeight.put(toChannel, bh);
                    if (bh > maxBlockHeight) {
                        maxBlockHeight = bh;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("当前节点高度:{},网络最新区块高度:{}", selfBlockHeight, maxBlockHeight);
        if (selfBlockHeight >= maxBlockHeight) {
            return;
        }
        downBlock(selfBlockHeight, maxBlockHeight, channelBlockHeight);
    }

    /**
     * 从其他节点下载区块
     *
     * @param selfBlockHeight
     * @param maxBlockHeight
     * @param channelBlockHeight
     */
    private void downBlock(long selfBlockHeight, int maxBlockHeight, Map<String, Integer> channelBlockHeight) {
        List<String> ac = new ArrayList<>();//用于存储每个远端ip
        Collection<MyChannel> allChannel = ChannelContain.get().getMyChannels();
        for (MyChannel channel : allChannel) {
            ac.add(channel.getRemoteIp());
        }

        boolean haveGenesis = true;
        if (selfBlockHeight == 0) {
            haveGenesis = false;
        }
        Random rand = new Random();
        long needBlock = maxBlockHeight - selfBlockHeight;//需要下载的块数
        long times = needBlock / THREAD_POOL_SIZE;//区块需要下载的轮数,每一轮下载的区块数与线程池最大数一样,分批下载区块
        long mod = needBlock % THREAD_POOL_SIZE;
        if (mod != 0) {
            times++;
        }
        for (int time = 0; time < times; time++) {
            //循环每一轮
            List<Future<MessageChannel>> results = new ArrayList<>();
            long start = selfBlockHeight + time * THREAD_POOL_SIZE + 1;//开始下载的区块数
            long end = start + THREAD_POOL_SIZE;
            if (end > maxBlockHeight) {
                end = maxBlockHeight;
            }
            long needBlockCount = end - start;//需要下载的区块数
            List<BlockStore> blocks = new ArrayList<>();
            Map<Long, MessageChannel> mapHeightData = new HashMap<>();
            while (true) {//始终要保证每一轮下载完成该下的任务
                if (blocks.size() >= needBlockCount) {
                    break;
                }
                for (long idx = start; idx <= end; idx++) {//依次去取当前节点需要的每一个块,idx表示的是要取哪个块
                    boolean needGain = true;//当前块需要下载
                    for (BlockStore block : blocks) {
                        long height = block.getBlock().getHeight();
                        if (height == idx) {
                            needGain = false;//说明不需要下载该块,因为已经下载下来了
                            break;
                        }
                    }
                    if (needGain) {
                        int selectChannel = rand.nextInt(ac.size());//所选中的块所在的channel进行获取块,选中的channel是随机选择的
                        String scId = ac.get(selectChannel);
                        while (true) {
                            log.info("当前请求的通道id:{},整个通道数量情况:{}", scId, channelBlockHeight);
                            Integer bh = channelBlockHeight.get(scId);//选中的通道拥有的块的高度
                            if (bh < idx) {//选中的通道拥有的块高度不满足要求所取的块高度,则将此通道从备选通道中移除
                                ac.remove(scId);
                                selectChannel = rand.nextInt(ac.size());
                                scId = ac.get(selectChannel);
                            } else {
                                break;
                            }
                        }
                        final String selectIp = scId;
                        final long selectBlockHeight = idx;
                        if (!haveGenesis) {
                            Future<MessageChannel> genesis = threadPool.submit(() -> {
                                Message nodeMessage = new Message();
                                nodeMessage.setType(MessageType.BLOCK_REQ.getType());
                                nodeMessage.setContent(Long.toString(0).getBytes());//所请求的块的高度
                                log.info("向网络节点:{} 请求区块高度为:{}的区块...", selectIp, 0);
                                MessageChannel message = SynRequest.get().synReq(nodeMessage, selectIp);
                                return message;
                            });
                            results.add(genesis);
                            haveGenesis = true;
                        }
                        Future<MessageChannel> submit = threadPool.submit(() -> {
                            Message nodeMessage = new Message();
                            nodeMessage.setType(MessageType.BLOCK_REQ.getType());
                            nodeMessage.setContent(Long.toString(selectBlockHeight).getBytes());//所请求的块的高度
                            log.info("向网络节点:{} 请求区块高度为:{}的区块...", selectIp, selectBlockHeight);
                            MessageChannel message = SynRequest.get().synReq(nodeMessage, selectIp);
                            return message;
                        });
                        results.add(submit);
                    }
                }
                for (Future<MessageChannel> result : results) {
                    try {
                        MessageChannel message = result.get();
                        if (message != null) {
                            byte[] content = message.getMessage().getContent();
                            BlockStore blockStore = SerializationUtil.deserializer(content, BlockStore.class);//远端返回来的区块
                            blocks.add(blockStore);
                            mapHeightData.put(blockStore.getBlock().getBlockHeader().getHeight(), message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            TreeMap<Long, BlockStore> map = new TreeMap<Long, BlockStore>(new Comparator<Long>() {
                @Override
                public int compare(Long o1, Long o2) {
                    return o1.compareTo(o2);
                }
            });
            for (BlockStore blockStore : blocks) {
                map.put(blockStore.getBlock().getHeight(), blockStore);
            }
            log.info("从其他网络节点下载下来的区块数为:{}", blocks.size());
            for (BlockStore block : map.values()) {
                log.info("经过排序后的区块高度为:{}", block.getBlock().getBlockHeader().getHeight());
                MessageChannel messageChannel = mapHeightData.get(block.getBlock().getBlockHeader().getHeight());
                if (messageChannel != null) {
                    try {
                        if (blockArrivedValidator.check(messageChannel)) {
                            blockArrivedHandler.handle(messageChannel);
                        }
                    } catch (Exception e) {
                        log.info("区块高度{}的区块数据异常 :{}", block.getBlock().getBlockHeader().getHeight(), e.getMessage());
                        synBlock();
                    }
                } else {
                    log.error("未获取到区块高度:{} 对应的数据内容...", block.getBlock().getBlockHeader().getHeight());
                }

            }
        }
        synBlock();
    }

    public MessageHandler getBlockArrivedHandler() {
        return blockArrivedHandler;
    }

    public void setBlockArrivedHandler(MessageHandler blockArrivedHandler) {
        this.blockArrivedHandler = blockArrivedHandler;
    }

    public MessageValidator getBlockArrivedValidator() {
        return blockArrivedValidator;
    }

    public void setBlockArrivedValidator(MessageValidator blockArrivedValidator) {
        this.blockArrivedValidator = blockArrivedValidator;
    }

    public AtomicBoolean getSyning() {
        return syning;
    }

    public void setSyning(AtomicBoolean syning) {
        this.syning = syning;
    }
}
