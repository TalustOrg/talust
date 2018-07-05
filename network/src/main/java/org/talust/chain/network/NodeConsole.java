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

package org.talust.chain.network;

import lombok.extern.slf4j.Slf4j;
import org.talust.chain.common.tools.Constant;
import org.talust.chain.network.netty.ConnectionManager;
import org.talust.chain.network.netty.server.NodeServer;

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
