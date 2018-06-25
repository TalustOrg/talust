package org.talust.chain.network.netty;

import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.tools.SyncFuture;
import org.talust.chain.common.model.Message;
import org.talust.chain.common.tools.MessageCount;
import com.talust.chain.network.netty.queue.MessageQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 异步请求处理
 */
@Slf4j
public class SynRequest {

    private static SynRequest instance = new SynRequest();

    private SynRequest() {
    }

    public static SynRequest get() {
        return instance;
    }

    private MessageQueue mq = MessageQueue.get();

    //用于存储异步请求时,某个消息ID,对应的异步回调
    private Map<Long, SyncFuture<MessageChannel>> synMap = new ConcurrentHashMap<>();

    /**
     * 添加异步请求
     *
     * @param message 节点消息
     * @param toIp    需要通过该通道进行发送数据
     */
    public MessageChannel synReq(Message message, String toIp) throws Exception {
        SyncFuture<MessageChannel> sync = new SyncFuture<>();
        long mc = MessageCount.msgCount.addAndGet(1);
        if (mc > 65530) {
            MessageCount.msgCount.set(0);
        }
        synMap.put(mc, sync);
        message.setMsgCount(mc);
        MessageChannel mch = new MessageChannel();
        mch.setMessage(message);
        mch.setToIp(toIp);
        mq.addMessage(mch);
        MessageChannel retMsg = sync.get(3, TimeUnit.SECONDS);
        synMap.remove(mc);
        return retMsg;//返回的要么为空,要么有值
    }

    //异步响应
    public void synResp(MessageChannel message) {
        SyncFuture<MessageChannel> syncFuture = synMap.remove(message.getMessage().getMsgCount());
        if (syncFuture != null) {
            syncFuture.setResponse(message);
        }
    }

}
