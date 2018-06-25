package org.talust.chain.client.validator;

import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.tools.CacheManager;
import org.talust.chain.common.tools.Configure;
import org.talust.chain.network.MessageValidator;


/**
 * 节点加入时的验证
 */
public class NodeJoinValidator implements MessageValidator {

    @Override
    public boolean check(MessageChannel messageChannel) {
        String ip = new String(messageChannel.getMessage().getContent());
        String time = messageChannel.getMessage().getTime().toString();
        String identifier = ip + time;
        boolean checkRepeat = CacheManager.get().checkRepeat(identifier, Configure.BLOCK_GEN_TIME);
        if (!checkRepeat) {//消息无重复
            return true;
        }
        return false;
    }
}
