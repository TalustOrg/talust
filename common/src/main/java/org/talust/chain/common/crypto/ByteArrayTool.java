package org.talust.chain.common.crypto;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**

 */
@Slf4j
public class ByteArrayTool {

	private ByteArrayOutputStream array;
	
	public ByteArrayTool() {
		array = new ByteArrayOutputStream();
	}
	
	public ByteArrayTool(int size) {
		array = new ByteArrayOutputStream(size);
	}
	
	public ByteArrayTool(byte[] bytes) {
		array = new ByteArrayOutputStream(bytes.length);
		try {
			array.write(bytes);
		} catch (IOException e) {
			log.error("",e);
		}
	}
	
	public void append(byte b) {
		try {
			array.write(new byte[]{b});
		} catch (IOException e) {
			log.error("",e);
		}
	}
	
	public void append(int i) {
		append((byte)i);
	}
	
	public void append(byte[] bytes) {
		try {
			array.write(bytes);
		} catch (IOException e) {
			log.error("",e);
		}
	}
	
	public void append(long val) {
		try {
			Utils.uint32ToByteStreamLE(val, array);
		} catch (IOException e) {
			log.error("",e);
		}
	}
	
	public void append64(long val) {
		try {
			Utils.int64ToByteStreamLE(val, array);
		} catch (IOException e) {
			log.error("",e);
		}
	}
	
	public byte[] toArray() {
		try {
			return array.toByteArray();
		} finally {
			try {
				array.close();
			} catch (IOException e) {
				log.error("",e);
			}
		}
	}
	
	public void close() {
		try {
			array.close();
		} catch (IOException e) {
			log.error("",e);
		}
	}
}
