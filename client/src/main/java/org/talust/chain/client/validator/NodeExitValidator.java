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

package org.talust.chain.client.validator;

import org.talust.chain.common.model.MessageChannel;
import org.talust.chain.common.tools.CacheManager;
import org.talust.chain.common.tools.Configure;
import org.talust.chain.network.MessageValidator;


/**
 * 节点退出时的验证
 */
public class NodeExitValidator implements MessageValidator {

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
