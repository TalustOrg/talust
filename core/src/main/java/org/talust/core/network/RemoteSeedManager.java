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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talust.common.tools.Constant;
import org.talust.core.server.NtpTimeService;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

/**
 * 远程种子节点管理
 */
public class RemoteSeedManager implements SeedManager {
	
	private final static Logger log = LoggerFactory.getLogger(RemoteSeedManager.class);
	
	private final static Set<String> SEED_DOMAINS = new HashSet<String>(); 
	
	private List<Seed> list = new ArrayList<Seed>();
	
	private volatile boolean hasInit;
	
	public RemoteSeedManager() {
	}
	
	@Override
	public List<Seed> getSeedList(int maxConnectionCount) {
		//如果没有初始化，则先初始化
		if(!hasInit) {
			init();
		}
				
		List<Seed> newList = new ArrayList<Seed>();
		List<Seed> removeList = new ArrayList<Seed>();
		//排除连接失败且不重试的
		for (Seed seed : list) {
			if(seed.getStaus() == Seed.SEED_CONNECT_WAIT ||
				(seed.getStaus() == Seed.SEED_CONNECT_FAIL && seed.isRetry() &&
					(NtpTimeService.currentTimeMillis() > seed.getLastTime() + seed.getRetryInterval()))) {
				newList.add(seed);
				seed.setStaus(Seed.SEED_CONNECT_ING);
			} else if(seed.getStaus() == Seed.SEED_CONNECT_FAIL && !seed.isRetry()) {
				removeList.add(seed);
			}
		}
		list.removeAll(removeList);
		return newList;
	}

	@Override
	public boolean hasMore() {
		//如果没有初始化，则先初始化
		if(!hasInit) {
			init();
		}
		for (Seed seed : list) {
			if(seed.getStaus() == Seed.SEED_CONNECT_WAIT ||
				(seed.getStaus() == Seed.SEED_CONNECT_FAIL && seed.isRetry() &&
					(NtpTimeService.currentTimeMillis() > seed.getLastTime() + seed.getRetryInterval()))) {
				return true;
			}
		}
		return false;
	}

	public boolean add(Seed node) {
		return list.add(node);
	}
	
	/**
	 * 初始化种子节点
	 */
	private synchronized void init() {
		try {
			if(hasInit) {
				return;
			}
			if(log.isDebugEnabled()) {
				log.debug("初始化种子节点");
			}
			Set<String> myIps = getIps();
			for (String seedDomain : SEED_DOMAINS) {
				try {
					InetAddress[] response = InetAddress.getAllByName(seedDomain);
					for (InetAddress inetAddress : response) {
						//排除自己
						if(myIps.contains(inetAddress.getHostAddress())) {
							continue;
						}
						//若连接失败，则重试，暂定1分钟
						Seed seed = new Seed(new InetSocketAddress(inetAddress, Constant.PORT), true, 1 * 60000);
						add(seed);
					}
				} catch (Exception e) {
					log.error("种子域名{}获取出错 {}", seedDomain, e.getMessage());
				}
			}
			
			if(log.isDebugEnabled()) {
				log.debug("种子节点初始化完成");
			}
			
			hasInit = true;
		} catch (Exception e) {
			log.error("种子节点获取出错 {}", e.getMessage());
		}
	}

	/**
	 * 添加一个dns种子
	 * @param domain
	 * @return boolean
	 */
	@Override
	public boolean addDnsSeed(String domain) {
		return SEED_DOMAINS.add(domain);
	}
	
	/**
	 * 重置种子节点
	 */
	public void reset() {
		hasInit = false;
		list = new ArrayList<Seed>();
	}

	@Override
	public List<Seed> getAllSeeds(){
		return list;
	}


	/**
	 * 多IP处理，可以得到最终ip
	 * @return Set<String>
	 */
	public static Set<String> getIps() {
		//返回本机所有的IP地址 ，包括了内网和外网的IP4地址
		Set<String> ips = new HashSet<String>();
		try {
			Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
			InetAddress ip = null;
			while (netInterfaces.hasMoreElements()) {
				NetworkInterface ni = netInterfaces.nextElement();
				Enumeration<InetAddress> address = ni.getInetAddresses();
				while (address.hasMoreElements()) {
					ip = address.nextElement();
					if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
							&& ip.getHostAddress().indexOf(":") == -1) {// 外网IP
						ips.add(ip.getHostAddress());
						break;
					} else if (ip.isSiteLocalAddress()
							&& !ip.isLoopbackAddress()
							&& ip.getHostAddress().indexOf(":") == -1) {// 内网IP
						ips.add(ip.getHostAddress());
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return ips;
	}
}
