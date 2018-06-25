package org.talust.chain.common.tools;

import java.util.concurrent.atomic.AtomicLong;

public class MessageCount {
    static {
        msgCount = new AtomicLong(0);
    }
    public static final AtomicLong msgCount;//消息计数器,主要用于发送出去的消息进行计数,每次累加1,系统重启之后,再重0开启

}
