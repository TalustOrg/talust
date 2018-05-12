package com.talust.chain.network;

import com.talust.chain.common.tools.Constant;
import com.talust.chain.network.netty.ConnectionManager;
import com.talust.chain.network.netty.server.NodeServer;
import lombok.extern.slf4j.Slf4j;

//节点控制台
@Slf4j
public class NodeConsole {

    public static void main(String[] args) {
        NodeConsole nc = new NodeConsole();
        nc.start();
    }

    public void start() {
        log.info("绑定本地端口...");
        bindNodeServer();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        log.info("初始化底层连接...");
        ConnectionManager.get().init();
    }

    //绑定节点服务,即启动本地服务,本地可能是外网,也可能是内网,如果是内网,用处不大,但不影响
    private void bindNodeServer() {
        new Thread(() -> {
            NodeServer ns = new NodeServer();
            try {
                ns.bind(Constant.PORT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }
}
