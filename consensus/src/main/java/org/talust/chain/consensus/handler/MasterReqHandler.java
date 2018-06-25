package org.talust.chain.consensus.handler;

import org.talust.chain.common.model.Message;
import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.model.MessageType;
import org.talust.chain.common.model.SuperNode;
import org.talust.chain.consensus.Conference;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.netty.queue.MessageQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j //请求当前共识网络中的master节点
public class MasterReqHandler implements MessageHandler {

    @Override
    public boolean handle(MessageChannel message) {
        MessageChannel mc = new MessageChannel();
        Message sendMessage = new Message();
        sendMessage.setType(MessageType.MASTER_RESP.getType());
        sendMessage.setMsgCount(message.getMessage().getMsgCount());
        mc.setMessage(sendMessage);
        String fromChannel = message.getFromIp();
        mc.setToIp(fromChannel);
        SuperNode master = Conference.get().getMaster();
        log.info("接收到请求当前共识网络的master的请求,当前master的ip为:{}", master.getIp());
        if (master != null) {
            sendMessage.setContent(master.getIp().getBytes());
        } else {
            sendMessage.setType(MessageType.ERROR_MESSAGE.getType());
            sendMessage.setContent("未找到当前会议的master".getBytes());
        }
        MessageQueue.get().addMessage(mc);
        return true;
    }

}
