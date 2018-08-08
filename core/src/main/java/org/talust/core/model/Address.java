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
package org.talust.core.model;



import org.talust.common.crypto.Base58;
import org.talust.common.crypto.Hex;
import org.talust.common.crypto.Sha256Hash;
import org.talust.common.crypto.Utils;
import org.talust.common.exception.AddressFormatException;
import org.talust.common.exception.VerificationException;
import org.talust.common.exception.WrongNetworkException;

import java.util.Arrays;

/**
 * 系统帐户的地址
 */
public class Address {
	
	//base58的长度
	public static final int HASH_LENGTH = 25;
	//address 的 RIPEMD160 长度
    public static final int LENGTH = 20;
    //版本
    protected  int version;
    //内容
    protected byte[] bytes;
    //最新余额
    protected Coin balance;
    //等待中的余额
    protected Coin unconfirmedBalance;
    
    /**
     * 根据hash160创建
     * @param hash160
     */
    public Address(byte[] hash160) {
        this.bytes = hash160;
    }

    /**
     * 根据版本、hash160创建
     * @param hash160
     */
    public Address( int version, byte[] hash160) throws WrongNetworkException {
        Utils.checkState(hash160.length == LENGTH, "地址的hash160不正确，必须是20位");
        this.version = version;
        this.bytes = hash160;
    }

	/**
     * 根据base58创建
     * @param address
     * @throws AddressFormatException
     * @throws WrongNetworkException
     */
    public Address( String address) throws AddressFormatException{
    	byte[] versionAndDataBytes = Base58.decodeChecked(address);
        byte versionByte = versionAndDataBytes[0];
        version = versionByte & 0xFF;
        bytes = new byte[LENGTH];
        System.arraycopy(versionAndDataBytes, 1, bytes, 0, LENGTH);
    }
    
    /**
     * 根据hash160创建地址
     * @param version
     * @return hash160
     */
    public static Address fromP2PKHash(int version, byte[] hash160) {
        try {
            return new Address(version, hash160);
        } catch (WrongNetworkException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }
    
    /**
     * 根据hash内容创建地址
     * @param hashs
     * @return Address
     * @throws AddressFormatException
     */
    public static Address fromHashs(byte[] hashs) throws AddressFormatException {
    	
    	if(hashs == null || hashs.length != HASH_LENGTH) {
    		throw new AddressFormatException();
    	}
    	
    	int version = hashs[0] & 0XFF;
    	byte[] content = new byte[LENGTH];
        System.arraycopy(hashs, 1, content, 0, LENGTH);
    	
        byte[] sign = new byte[4];
    	System.arraycopy(hashs, 21, sign, 0, 4);
    	
    	Address address = new Address(version, content);
    	address.checkSign(sign);
    	
    	return address;
    }
    
    /**
     * 根据base58创建地址
     * @param address
     * @return Address
     * @throws AddressFormatException
     */
    public static Address fromBase58( String address) throws AddressFormatException {
    	return new Address( address);
    }
    /**
     * 获取包含版本和效验码的地址内容
     * @return byte[]
     */
    public byte[] getHash() {
    	//地址一共25字节
        byte[] versionAndHash160 = new byte[21];
        //加上版本号
        versionAndHash160[0] = (byte) version;
        //加上20字节的hash160
        System.arraycopy(bytes, 0, versionAndHash160, 1, bytes.length);
        //加上4位的效验码
        byte[] checkSin = getCheckSin(versionAndHash160);
        byte[] base58bytes = new byte[25];
        System.arraycopy(versionAndHash160, 0, base58bytes, 0, versionAndHash160.length);
        System.arraycopy(checkSin, 0, base58bytes, versionAndHash160.length, checkSin.length);
        return base58bytes;
    }
    
    /**
     * 获取包含版本和效验码的地址16进制编码
     * @return String
     */
    public String getHashAsHex() {
        return Hex.encode(getHash());
    }

    /**
     * 获取地址20字节的hash160
     * @return byte[]
     */
    public byte[] getHash160() {
    	return Utils.checkNotNull(bytes);
    }
    
    /**
     * 获取地址20字节的hash160 16进制编码
     * @return String
     */
    public String getHash160AsHex() {
        return Hex.encode(getHash160());
    }
    
    public String getBase58() {
    	return Base58.encode(getHash());
    }
    


    /**
     * This implementation narrows the return type to <code>Address</code>.
     */
    public Address clone() throws CloneNotSupportedException {
        return (Address) super.clone();
    }

    /*
     * 获取4位的效验码
     */
    protected byte[] getCheckSin(byte[] versionAndHash160) {
		byte[] checkSin = new byte[4];
		System.arraycopy(Sha256Hash.hashTwice(versionAndHash160), 0, checkSin, 0, 4);
		return checkSin;
	}
    
    protected void checkSign(byte[] sign) throws VerificationException {
    	//地址一共25字节
        byte[] versionAndHash160 = new byte[21];
        //加上版本号
        versionAndHash160[0] = (byte) version;
        //加上20字节的hash160
        System.arraycopy(bytes, 0, versionAndHash160, 1, bytes.length);
        
    	byte[] checkSin = new byte[4];
		System.arraycopy(Sha256Hash.hashTwice(versionAndHash160), 0, checkSin, 0, 4);
		
		if(!Arrays.equals(checkSin, sign)) {
			throw new VerificationException("地址校验失败");
		}
    }
    
    public int getVersion() {
		return version;
	}
    
    public Coin getBalance() {
		return balance;
	}

	public void setBalance(Coin balance) {
		this.balance = balance;
	}

	public Coin getUnconfirmedBalance() {
		return unconfirmedBalance;
	}

	public void setUnconfirmedBalance(Coin unconfirmedBalance) {
		this.unconfirmedBalance = unconfirmedBalance;
	}

}