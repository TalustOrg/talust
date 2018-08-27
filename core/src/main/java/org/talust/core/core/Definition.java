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

package org.talust.core.core;

import org.talust.core.model.Message;
import org.talust.core.transaction.RegConsensusTransaction;
import org.talust.core.transaction.RemConsensusTransaction;
import org.talust.core.transaction.Transaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 协议定义
 */
public final class Definition {


	/**
     * TALUST 核心程序版本
     */
    public static final String TALUST_VERSION = "1.05";

    /**
     * 版本完整信息
     */
    public static final String LIBRARY_SUBVER = "talust core v" + TALUST_VERSION + "";
    
	public static final long VERSION = 1;
	
	/**
	 * 区块最大限制
	 */
	public static final int MAX_BLOCK_SIZE = 2 * 1024 * 1024;

	public static final int MIN_BLOCK_SIZE = 512*1024;
	
	/** lockTime 小于该值的代表区块高度，大于该值的代表时间戳（毫秒） **/
	public static final long LOCKTIME_THRESHOLD = 500000000L;

//	/** 转账最低手续费,0.1个talc */
//	public static final Coin MIN_PAY_FEE = Coin.COIN.divide(10);

	public static final int TYPE_ACCOUNT = 0;					//账户下发
	public static final int TYPE_COINBASE = 1;					//coinbase交易
	public static final int TYPE_PAY = 2;						//普通支付交易
	public static final int TYPE_REG_CONSENSUS = 3;				//注册成为共识节点
	public static final int TYPE_REM_CONSENSUS = 4;				//注销共识节点


	
	/** 认证账户注册 **/
	public static final int TYPE_CERT_ACCOUNT_REGISTER = 11;
	/** 认证账户修改信息 **/
	public static final int TYPE_CERT_ACCOUNT_UPDATE = 12;


	public static final int TYPE_CERT_ACCOUNT_REVOKE = 15;
	

	public static final int TX_VERIFY_MG = 1;				//脚本认证，账户管理类
	public static final int TX_VERIFY_TR = 2;				//脚本认证，交易类
	

	


	/**
	 * 判断传入的交易是否跟代币有关
	 * @param type
	 * @return boolean
	 */
	public static boolean isPaymentTransaction(int type) {
		return type == TYPE_COINBASE || type == TYPE_PAY || type == TYPE_REG_CONSENSUS
				|| type == TYPE_REM_CONSENSUS  ;
	}
	
	//交易关联
	public static final Map<Integer, Class<? extends Message>> TRANSACTION_RELATION = new HashMap<Integer, Class<? extends Message>>();
	//消息命令关联
	public static final Map<Class<? extends Message>, String> MESSAGE_COMMANDS = new HashMap<Class<? extends Message>, String>();
	//命令消息关联
	public static final Map<String, Class<? extends Message>> COMMANDS_MESSAGE = new HashMap<String, Class<? extends Message>>();
	//交易命令
	public static final Set<String> TRANSACTION_COMMANDS = new HashSet<>();

	static {
		MESSAGE_COMMANDS.put(Transaction.class, "tx_0");
		//===========================-分割线=============================//
		TRANSACTION_COMMANDS.add("tx_0");
		TRANSACTION_COMMANDS.add("tx_" + TYPE_CERT_ACCOUNT_REGISTER);
		TRANSACTION_COMMANDS.add("tx_" + TYPE_CERT_ACCOUNT_UPDATE);
		TRANSACTION_COMMANDS.add("tx_" + TYPE_CERT_ACCOUNT_REVOKE);
		TRANSACTION_COMMANDS.add("tx_" + TYPE_REG_CONSENSUS);
		TRANSACTION_COMMANDS.add("tx_" + TYPE_REM_CONSENSUS);

    	//===========================-分割线=============================//
    	
    	TRANSACTION_RELATION.put(TYPE_COINBASE, Transaction.class);
		TRANSACTION_RELATION.put(TYPE_PAY, Transaction.class);
		TRANSACTION_RELATION.put(TYPE_ACCOUNT, Transaction.class);
		TRANSACTION_RELATION.put(TYPE_REG_CONSENSUS, Transaction.class);
		TRANSACTION_RELATION.put(TYPE_REM_CONSENSUS, Transaction.class);

		//===========================-分割线=============================//
    	
    	for (Map.Entry<Class<? extends Message>, String> entry : MESSAGE_COMMANDS.entrySet()) {
			COMMANDS_MESSAGE.put(entry.getValue(), entry.getKey());
		}
    }
}
