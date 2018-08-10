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


import org.talust.common.crypto.ECKey;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.crypto.Utils;
import org.talust.core.model.Address;

import java.math.BigInteger;

/**
 * 帐户工具
 */
public final class AccountTool {

	/**
	 * 生成一个新的私匙/公匙对
	 * @return ECKey
	 */
	public final static ECKey newPriKey() {
		return new ECKey();
	}
	
	/**
	 * 生成一个新的地址
	 * @param network
	 * @return Address
	 */
	public final static Address newAddress(NetworkParams network) {
		return newAddress(network, network.getSystemAccountVersion());
	}
	
	public final static Address newAddress(NetworkParams network, ECKey key) {
		return newAddress(network, network.getSystemAccountVersion(), key);
	}
	
	public final static Address newAddress(NetworkParams network, int version) {
		ECKey key = newPriKey();
		return Address.fromP2PKHash(network, version, Utils.sha256hash160(key.getPubKey(false)));
	}
	
	public final static Address newAddress(NetworkParams network, int version, ECKey key) {
		return Address.fromP2PKHash(network, version, Utils.sha256hash160(key.getPubKey(false)));
	}
	
	public final static Address newAddressFromPrikey(NetworkParams network, int version, BigInteger pri) {
		ECKey key = ECKey.fromPrivate(pri);
		return Address.fromP2PKHash(network, version, Utils.sha256hash160(key.getPubKey(false)));
	}
	
	public final static Address newAddressFromKey(NetworkParams network, ECKey key) {
		return Address.fromP2PKHash(network, network.getSystemAccountVersion(), Utils.sha256hash160(key.getPubKey(false)));
	}
	
	public final static Address newAddressFromKey(NetworkParams network, int version, ECKey key) {
		return Address.fromP2PKHash(network, version, Utils.sha256hash160(key.getPubKey(false)));
	}
	
	/**
	 * 根据种子私匙，和密码，生成对应的帐户管理私匙或者交易私匙
	 */
	public final static BigInteger genPrivKey1(byte[] priSeed, byte[] pw) {
		byte[] privSeedSha256 = Sha256Hash.hash(priSeed);
		//取种子私匙的sha256 + pw的sha256，然后相加的结果再sha256为帐户管理的私匙
		byte[] pwSha256 = Sha256Hash.hash(pw);
		//把privSeedSha256 与 pwPwSha256 混合加密
		byte[] pwPriBytes = new byte[privSeedSha256.length + pwSha256.length];
		for (int i = 0; i < pwPriBytes.length; i+=2) {
			int index = i / 2;
			pwPriBytes[index] = privSeedSha256[index];
			pwPriBytes[index+1] = pwSha256[index];
		}
		//生成账户管理的私匙
		return new BigInteger(1, Sha256Hash.hash(pwPriBytes));
	}
	
	/**
	 * 根据种子私匙，和密码，生成对应的帐户管理私匙或者交易私匙
	 */
	public final static BigInteger genPrivKey2(byte[] priSeed, byte[] pw) {
		byte[] privSeedSha256 = Sha256Hash.hash(priSeed);
		//取种子私匙的sha256 + pw的sha256，然后相加的结果再sha256为帐户管理的私匙
		byte[] pwSha256 = Sha256Hash.hash(pw);
		//把privSeedSha256 与 pwPwSha256 混合加密
		byte[] pwPriBytes = new byte[privSeedSha256.length + pwSha256.length];
		for (int i = 0; i < pwPriBytes.length; i+=2) {
			int index = i / 2;
			pwPriBytes[index] = pwSha256[index];
			pwPriBytes[index+1] = privSeedSha256[index];
		}
		//生成账户管理的私匙
		return new BigInteger(1, Sha256Hash.hash(pwPriBytes));
	}
}
