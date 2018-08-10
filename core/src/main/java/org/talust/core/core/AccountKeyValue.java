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

import java.io.UnsupportedEncodingException;

public class AccountKeyValue extends KeyValue {
	
	public final static AccountKeyValue NAME = new AccountKeyValue("name", "名称");
	public final static AccountKeyValue LOGO = new AccountKeyValue("logo", "图标");

	public AccountKeyValue(String code, String name) {
		this.code = code;
		this.name = name;
	}
	
	public AccountKeyValue(String code, String name, byte[] value) {
		this.code = code;
		this.name = name;
		this.value = value;
	}
	
	public AccountKeyValue(String code, String name, String value) {
		this.code = code;
		this.name = name;
		try {
			this.value = value.getBytes(CHARSET);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public AccountKeyValue(byte[] content) {
		super(content);
	}

}
