package com.talust.chain.consensus.handler;

import com.talust.chain.common.model.Message;
import com.talust.chain.common.model.MessageChannel;
import com.talust.chain.common.model.MessageType;
import com.talust.chain.common.model.SuperNode;
import com.talust.chain.consensus.Conference;
import com.talust.chain.network.MessageHandler;
import com.talust.chain.network.netty.queue.MessageQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j //请求切换新的共识网络中的master节点
public class NewMasterReqHandler implements MessageHandler {

    @Override
    public boolean handle(MessageChannel message) {
        MessageChannel mc = new MessageChannel();
        Message sendMessage = new Message();
        sendMessage.setType(MessageType.NEW_MASTER_RESP.getType());
        sendMessage.setMsgCount(message.getMessage().getMsgCount());
        mc.setMessage(sendMessage);
        String fromChannel = message.getFromIp();
        mc.setToIp(fromChannel);


        String newMasterIp = new String(message.getMessage().getContent());
        boolean checkNewMaster = Conference.get().checkNewMaster(newMasterIp);
        log.info("接收到请求当前共识网络更新master的请求,当前master的ip为:{},希望更新的masterip为:{}", Conference.get().getMaster(), newMasterIp);
        if (checkNewMaster) {
            sendMessage.setContent("ok".getBytes());
        } else {
            sendMessage.setType(MessageType.ERROR_MESSAGE.getType());
            sendMessage.setContent("当前节点不认同更新的新master节点.".getBytes());
        }
        MessageQueue.get().addMessage(mc);
        return true;
    }

}
