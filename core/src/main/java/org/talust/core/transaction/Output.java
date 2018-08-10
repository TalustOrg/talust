package org.talust.core.transaction;


import org.talust.core.script.Script;

import java.io.IOException;
import java.io.OutputStream;

public interface Output {

	/**
	 * 序列化交易输出
	 * @param stream
	 * @throws IOException 
	 */
	public void serialize(OutputStream stream) throws IOException;
	
	public byte[] getScriptBytes();
	
	public void setScriptBytes(byte[] scriptBytes);
	
	void setScript(Script scriptSig);
	
	Script getScript();
	
	long getValue();
	
	long getLockTime();
}
