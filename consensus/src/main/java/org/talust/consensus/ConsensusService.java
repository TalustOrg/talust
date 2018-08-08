package org.talust.consensus;

import lombok.extern.slf4j.Slf4j;
import org.talust.account.Genesis;
import org.talust.common.model.SuperNode;
import org.talust.common.tools.CacheManager;
import org.talust.common.tools.Configure;
import org.talust.common.tools.DateUtil;
import org.talust.network.netty.ConnectionManager;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j //共识服务
public class ConsensusService {
    private static ConsensusService instance = new ConsensusService();

    private ConsensusService() {
    }

    public static ConsensusService get() {
        return instance;
    }
    private CacheManager cu = CacheManager.get();
    private Conference conference = Conference.get();
    private  ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1);
    private PackBlockTool packBlockTool = new PackBlockTool();
    private AtomicBoolean genRunning = new AtomicBoolean(false);
    private int checkSecond;//检测区块是否正常的时长

    public void start() {
        boolean superNode = ConnectionManager.get().superNode;
        //如果当前节点是超级节点,则启动共识机制
        if (superNode) {
            log.info("当前节点是超级节点,启动参与共识....");
            checkSecond = Configure.BLOCK_GEN_TIME / 3;
            genBlock();
            SuperNode master = conference.reqNetMaster();
            if (master != null) {
                log.info("获取会议master ip:{}", master.getIp());
                if (master.getIp().equals(ConnectionManager.get().getSelfIp())) {
                    startGenBlock();
                }
            }
        }
    }

    private void genBlock() {
        int delay = 0;
        //当前最新区块生成时间
        int currentBlockTime = CacheManager.get().getCurrentBlockTime();
        if (currentBlockTime > 0) {
            int timeSecond = DateUtil.getTimeSecond();
            delay = Configure.BLOCK_GEN_TIME - timeSecond + currentBlockTime;
            if (delay < 0) {
                delay = 0;
            }
        }

        service.scheduleAtFixedRate(() -> {
            if (genRunning.get()) {
                byte[] currentBlockHash = CacheManager.get().getCurrentBlockHash();
                if (ConnectionManager.get().genesisIp && currentBlockHash == null) {
                    log.info("当前节点创建创世块...");
                    log.info("当前缓存中没有最新块的hash值");
                    try {
                        genGenesis();//当前节点是创世块ip
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    log.info("打包ip:{}", ConnectionManager.get().selfIp);
                    int packageTime = DateUtil.getTimeSecond();
                    packBlockTool.pack(packageTime);//打包
                }
            }

        }, delay, Configure.BLOCK_GEN_TIME, TimeUnit.SECONDS);
        log.info("启动定时任务生成区块,延时:{}...", delay);


        new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(checkSecond);
                } catch (InterruptedException e) {
                }
                try {
                    //检测master是否正常,通过块判断
                    int nowSecond = DateUtil.getTimeSecond();
                    int ct = cu.getCurrentBlockTime();
                    if (ct > 0) {
                        if ((nowSecond - ct) >= (Configure.BLOCK_GEN_TIME + checkSecond)) {//未收到区块响应
                            conference.changeMaster();
                        }
                    }
                } catch (Throwable e) {
                    log.error("error:",e);
                }
            }
        }).start();
    }

    /**
     * 开始生成块
     */
    public void startGenBlock() {
        genRunning.set(true);
    }

    /**
     * 停止生成块
     */
    public void stopGenBlock() {
        genRunning.set(false);
    }

    //生成创世块内容,直接将其放入数据窗口中,不需要进行校验
    private void genGenesis() {
        Genesis genesis = new Genesis();
        genesis.genGenesisContent();
        int packageTime = DateUtil.getTimeSecond();
        packBlockTool.pack(packageTime);//打包
    }

}
