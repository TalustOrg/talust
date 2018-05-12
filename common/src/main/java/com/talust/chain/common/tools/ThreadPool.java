package com.talust.chain.common.tools;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 整个系统用到的线程池
 */
public class ThreadPool {
    private static ThreadPool instance = new ThreadPool();

    private ThreadPool() {
        int coreSize = 2 * Runtime.getRuntime().availableProcessors() + 1;
        threadPool = new ThreadPoolExecutor(coreSize, Integer.MAX_VALUE, 5,
                TimeUnit.SECONDS, new SynchronousQueue(), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public static ThreadPool get() {
        return instance;
    }

    public ThreadPoolExecutor threadPool;

}
