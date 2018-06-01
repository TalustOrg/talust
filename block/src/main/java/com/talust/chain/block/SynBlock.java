package com.talust.chain.block;

import com.talust.chain.block.model.Block;
import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.common.tools.CacheManager;
import com.talust.chain.common.tools.SerializationUtil;
import com.talust.chain.common.tools.ThreadPool;
import com.talust.chain.common.model.Message;
import com.talust.chain.common.model.MessageType;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.MessageValidator;
import com.talust.chain.network.model.MyChannel;
import com.talust.chain.network.netty.ChannelContain;
import com.talust.chain.network.netty.SynRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

//同步块
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
        try {
            //TODO
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int selfBlockHeight = CacheManager.get().getCurrentBlockHeight();
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

        int maxBlockHeight = -1;//最高区块高度
        Map<String, Integer> channelBlockHeight = new HashMap<>();//用于存储每个通道的区块高度
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
        if (selfBlockHeight >= maxBlockHeight-1) {
            return;
        }
        downBlock(selfBlockHeight, maxBlockHeight-1, channelBlockHeight);
    }

    /**
     * 从其他节点下载区块
     *
     * @param selfBlockHeight
     * @param maxBlockHeight
     * @param channelBlockHeight
     */
    private void downBlock(int selfBlockHeight, int maxBlockHeight, Map<String, Integer> channelBlockHeight) {
        List<String> ac = new ArrayList<>();//用于存储每个远端ip
        Collection<MyChannel> allChannel = ChannelContain.get().getMyChannels();
        for (MyChannel channel : allChannel) {
            ac.add(channel.getRemoteIp());
        }

        Random rand = new Random();
        int needBlock = maxBlockHeight - selfBlockHeight;//需要下载的块数
        int times = needBlock / THREAD_POOL_SIZE;//区块需要下载的轮数,每一轮下载的区块数与线程池最大数一样,分批下载区块
        int mod = needBlock % THREAD_POOL_SIZE;
        if (mod != 0) {
            times++;
        }
        for (int time = 0; time < times; time++) {//循环每一轮
            List<Future<MessageChannel>> results = new ArrayList<>();
            int start = selfBlockHeight + time * THREAD_POOL_SIZE + 1;//开始下载的区块数
            int end = start + THREAD_POOL_SIZE;
            if (end > maxBlockHeight + 1) {
                end = maxBlockHeight + 1;
            }
            int needBlockCount = end - start;//需要下载的区块数
            List<Block> blocks = new ArrayList<>();
            Map<Integer, MessageChannel> mapHeightData = new HashMap<>();
            while (true) {//始终要保证每一轮下载完成该下的任务
                if (blocks.size() >= needBlockCount) {
                    break;
                }
                for (int idx = start; idx < end; idx++) {//依次去取当前节点需要的每一个块,idx表示的是要取哪个块
                    boolean needGain = true;//当前块需要下载
                    for (Block block : blocks) {
                        int height = block.getHead().getHeight();
                        if (height == idx) {
                            needGain = false;//说明不需要下载该块,因为已经下载下来了
                            break;
                        }
                    }
                    if (needGain) {
                        int selectChannel = rand.nextInt(ac.size());//所选中的块所在的channel进行获取块,选中的channel是随机选择的
                        String scId = ac.get(selectChannel);
                        while (true) {
                            //log.info("当前请求的通道id:{},整个通道数量情况:{}", scId, channelBlockHeight);
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
                        final int selectBlockHeight = idx;
                        Future<MessageChannel> submit = threadPool.submit(() -> {
                            Message nodeMessage = new Message();
                            nodeMessage.setType(MessageType.BLOCK_REQ.getType());
                            nodeMessage.setContent(Integer.toString(selectBlockHeight).getBytes());//所请求的块的高度
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
                            Block block = SerializationUtil.deserializer(content, Block.class);//远端返回来的区块
                            blocks.add(block);
                            mapHeightData.put(block.getHead().getHeight(), message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            Collections.sort(blocks, (Block o1, Block o2) -> {//对本次返回的区块进行排序
                int i = o1.getHead().getHeight() - o2.getHead().getHeight();
                if (i == 0) {
                    return 0;
                }
                return i;
            });
            log.info("从其他网络节点下载下来的区块数为:{}", blocks.size());
            for (Block block : blocks) {
                try {
                    log.info("经过排序后的区块高度为:{}", block.getHead().getHeight());
                    MessageChannel messageChannel = mapHeightData.get(block.getHead().getHeight());
                    if (messageChannel != null) {
                        if (blockArrivedValidator.check(messageChannel)) {
                            blockArrivedHandler.handle(messageChannel);
                        }
                    } else {
                        log.error("未获取到区块高度:{} 对应的数据内容...", block.getHead().getHeight());
                    }
                } catch (Throwable e) {//本次下载的一批区块,其中有区块有问题
                    //@TODO 需要将区块有问题的下载节点加入黑名单,本处暂时忽略
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
}
