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
package org.talust.common.tools;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点黑名单,整个网络中有可能会有些节点是恶意节点
 * 只要从某个节点获取的数据校验失败超过三次,则认为是
 * 恶意节点
 */
@Slf4j
public class NodeBlacklist {
    private static NodeBlacklist instance = new NodeBlacklist();

    private NodeBlacklist() {
    }

    public static NodeBlacklist get() {
        return instance;
    }

    private Map<String, Integer> ipError = new ConcurrentHashMap<>();

    /**
     * 新增一次节点错误机会
     *
     * @param nodeIp
     */
    public void addErrorOne(String nodeIp) {
        Integer number = ipError.get(nodeIp);
        if (number == null) {
            number = 0;
        }
        ipError.put(nodeIp, ++number);
    }

    /**
     * 检测某节点ip是否是恶意节点
     *
     * @param nodeIp
     * @return
     */
    public boolean isErrorNode(String nodeIp) {
        Integer number = ipError.get(nodeIp);
        if (number != null) {
            if (number.intValue() > 2) {//说明当前节点是超级节点
                return true;
            }
        }
        return false;
    }

    /**
     * 清除恶意节点,当从某个节点收到的数据经过验证是正确的,则认为当前节点
     * 不是恶意节点
     *
     * @param nodeIp
     */
    public void clearErrorNode(String nodeIp) {
        ipError.remove(nodeIp);
    }


}
