package org.talust.core.storage;


import org.talust.common.exception.ProtocolException;
import org.talust.core.core.NetworkParams;
import org.talust.core.model.Message;

public abstract class Store extends Message {
	
	protected byte[] key;
	
	public Store() {
	}
	
	protected Store(NetworkParams network) {
        this.network = network;
        serializer = network.getDefaultSerializer();
    }
    
    protected Store(NetworkParams network, byte[] payload, int offset) throws ProtocolException {
        this(network, payload, offset, network.getProtocolVersionNum(NetworkParams.ProtocolVersion.CURRENT));
    }

    protected Store(NetworkParams network, byte[] payload, int offset, int protocolVersion) throws ProtocolException {
        super(network, payload, offset, protocolVersion, network.getDefaultSerializer(), UNKNOWN_LENGTH);
    }
	
	public byte[] getKey() {
		return key;
	}

	public void setKey(byte[] key) {
		this.key = key;
	}
	
}
