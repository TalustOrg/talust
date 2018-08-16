package org.talust.core.transaction;


import org.talust.common.crypto.ECKey;

public interface TransactionSigner {

    boolean isReady();

    byte[] serialize();

    void deserialize(byte[] data);

    boolean signInputs(Transaction tx, ECKey key);
    boolean signOneInputs(Transaction tx, ECKey key, int inputIndex);

}
