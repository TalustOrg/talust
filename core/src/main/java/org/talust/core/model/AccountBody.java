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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talust.common.crypto.VarInt;
import org.talust.common.exception.ContentErrorExcetption;
import org.talust.core.core.AccountKeyValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 账户主体
 */
public class AccountBody {
	
	private static Logger log = LoggerFactory.getLogger(AccountBody.class);
	
	private List<AccountKeyValue> contents;
	
	public AccountBody(byte[] content) throws ContentErrorExcetption {
			parse(content);
	}
	
	public AccountBody(List<AccountKeyValue> contents) {
		this.contents = contents;
	}
	
	public AccountBody(AccountKeyValue[] contents) {
		this.contents = Arrays.asList(contents);
	}
	
	public static AccountBody empty() {
		AccountBody ab = null;
		try {
			ab = new AccountBody(new byte[0]);
		}catch (Exception e){
			System.out.print("never happen");
		}
		return ab;
	}
	
	public final byte[] serialize() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			if(contents != null) {
				for (AccountKeyValue keyValuePair : contents) {
					byte[] keyValue = keyValuePair.toByte();
					bos.write(new VarInt(keyValue.length).encode());
					bos.write(keyValue);
				}
			}
			return bos.toByteArray();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				bos.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			} finally {
			}
		}
		return new byte[0];
	}
	

	public void parse(byte[] content) throws ContentErrorExcetption{
		if(content == null || content.length == 0) {
			return;
		}
		int cursor = 0;
		contents = new ArrayList<AccountKeyValue>();
		while(true) {
			VarInt varint = new VarInt(content, cursor);
	        cursor += varint.getOriginalSizeInBytes();
	        
	        AccountKeyValue keyValuePair = new AccountKeyValue(Arrays.copyOfRange(content, cursor, cursor + (int)varint.value));
	        contents.add(keyValuePair);
	        
	        cursor += varint.value;
	        if(cursor >= content.length) {
	        	break;
	        }
		}
	}

	public List<AccountKeyValue> getContents() {
		return contents;
	}

	public void setContents(List<AccountKeyValue> contents) {
		this.contents = contents;
	}
	
	public String getName() {
		for (AccountKeyValue keyValuePair : contents) {
			if(keyValuePair.getCode().equals(AccountKeyValue.NAME.getCode())) {
				return keyValuePair.getValueToString();
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "AccountBody [contents=" + contents + "]";
	}
}
