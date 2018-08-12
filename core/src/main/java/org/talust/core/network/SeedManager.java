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

import java.util.List;

/**
 * 种子节点管理
 */
public interface SeedManager {

	/**
	 * 添加一个种子节点
	 * @param seed
	 * @return boolean
	 */
	boolean add(Seed seed);
	
	/**
	 * 添加一个dns种子
	 * @param domain
	 * @return boolean
	 */
	boolean addDnsSeed(String domain);
	
	/**
	 * 获取一个可用的种子节点列表
	 * @param maxConnectionCount 最大数量
	 * @return List<Seed>
	 */
	List<Seed> getSeedList(int maxConnectionCount);

	/**
	 * 获取所有的节点，不管可不可用
	 * @return
	 */
	List<Seed> getAllSeeds();

	/**
	 * 是否有更多的可用节点
	 * @return boolean
	 */
	boolean hasMore();
	
	/**
	 * 重置种子节点
	 */
	void reset();
}
