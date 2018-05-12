package com.talust.chain.common.model;

public enum MessageType {
    HEARTBEAT_REQ(1), //心跳请求
    HEARTBEAT_RESP(2),//心跳响应
    NODES_REQ(3), //获取节点请求
    NODES_RESP(4),//获取节点响应
    BLOCK_ARRIVED(5),//块数据到来
    HEIGHT_REQ(7),//区块高度请求
    HEIGHT_RESP(8),//区块高度响应
    BLOCK_REQ(9),//获取区块请求
    BLOCK_RESP(10),//获取区块响应
    NODE_JOIN(11),//节点加入
    NODE_EXIT(12),//节点退出
    ERROR_MESSAGE(13),//错误消息处理,一般针对请求响应模型才会有,遇到此种情况,直接向异步请求端投递即可
    TRANSACTION(14),//接收到交易数据
    MASTER_REQ(17),//master请求
    MASTER_RESP(18),//master响应
    END(10000)//结束,主要是为了开发时新增消息类型方便/
    ;


    private int type;

    /**
     * @param type
     */
    MessageType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static MessageType getMessageType(int type) {
        for (MessageType b : MessageType.values()) {
            if (b.getType() == type) {
                return b;
            }
        }
        return null;
    }

}
