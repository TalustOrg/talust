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

package org.talust.core.network;

import java.net.InetSocketAddress;

/**
 * 种子
 */
public class Seed {

	public static final int SEED_CONNECT_WAIT = 0;
	public static final int SEED_CONNECT_ING = 1;
	public static final int SEED_CONNECT_SUCCESS = 2;
	public static final int SEED_CONNECT_FAIL = 3;
	public static final int SEED_CONNECT_CLOSE = 4;
	
	private InetSocketAddress address;
	private int staus;			//状态，0待连接，1连接中，2连接成功，3连接失败，4断开连接
	private long lastTime;		//最后连接时间
	private boolean retry;		//连接失败是否重试
	private int retryInterval;	//失败的节点，默认60秒重试
	private int failCount;		//失败了多少次
	
	public Seed(InetSocketAddress address) {
		this(address, false, 60);
	}
	
	public Seed(InetSocketAddress address,  boolean retry, int retryInterval) {
		super();
		this.address = address;
		this.retry = retry;
		this.retryInterval = retryInterval;
	}

	public InetSocketAddress getAddress() {
		return address;
	}
	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}
	public int getStaus() {
		return staus;
	}
	public void setStaus(int staus) {
		this.staus = staus;
	}
	public long getLastTime() {
		return lastTime;
	}
	public void setLastTime(long lastTime) {
		this.lastTime = lastTime;
	}
	public boolean isRetry() {
		return retry;
	}
	public void setRetry(boolean retry) {
		this.retry = retry;
	}
	public int getRetryInterval() {
		return retryInterval;
	}
	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
	}
	public int getFailCount() {
		return failCount;
	}
	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}

	@Override
	public String toString() {
		return "Seed [address=" + address + ", staus=" + staus + ", lastTime=" + lastTime + ", retry=" + retry
				+ ", retryInterval=" + retryInterval + "]";
	}
}
