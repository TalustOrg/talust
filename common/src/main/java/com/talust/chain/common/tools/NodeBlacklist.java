package com.talust.chain.common.tools;

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
