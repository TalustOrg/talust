package org.talust.chain.client.handler;

import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.tools.Configure;
import org.talust.chain.common.tools.Constant;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.netty.ConnectionManager;
import com.talust.chain.network.netty.ChannelContain;
import com.talust.chain.network.netty.client.NodeClient;
import com.talust.chain.network.netty.queue.MessageQueueHolder;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j //节点加入到网络
public class NodeJoinHandler implements MessageHandler {
    private int connSize = Configure.MAX_CONNECT_TO_COUNT;//节点允许主动连接其他节点数
    private MessageQueueHolder mqHolder = MessageQueueHolder.get();

    @Override
    public boolean handle(MessageChannel message) {
        String ip = new String(message.getMessage().getContent());
        log.info("接收到节点ip:{} 加入网络的消息...", ip);
        mqHolder.broadMessage(message);
        ChannelContain.get().addNodeIp(ip);
        int activeConnectionCount = ChannelContain.get().getActiveConnectionCount();
        if (activeConnectionCount < connSize) {//说明当前连接数过小,则再次启动连接
            if (!ConnectionManager.get().isSelfIp(ip)) {
                log.info("当前节点要求主动连接其他节点的总结点数为:{},当前已经主动连接数为:{},将会连接ip:{}", connSize, activeConnectionCount, ip);
                try {
                    NodeClient tmpnc = new NodeClient();
                    Channel connect = tmpnc.connect(ip, Constant.PORT);
                    ChannelContain.get().addChannel(connect, false);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        }
        return true;
    }
}
