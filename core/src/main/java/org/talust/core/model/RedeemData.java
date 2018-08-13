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



import org.talust.common.crypto.ECKey;
import org.talust.common.crypto.Utils;
import org.talust.core.script.Script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 交易赎回信息，包含赎回脚步和接收的公匙，以及使用该笔交易的私匙
 */
public class RedeemData {
	//赎回脚本
    public final Script redeemScript;
    
    public final List<ECKey> keys;

    private RedeemData(List<ECKey> keys, Script redeemScript) {
        this.redeemScript = redeemScript;
        List<ECKey> sortedKeys = new ArrayList<ECKey>(keys);
        this.keys = sortedKeys;
    }

    public static RedeemData of(List<ECKey> keys, Script redeemScript) {
        return new RedeemData(keys, redeemScript);
    }

    public static RedeemData of(ECKey key, Script program) {
        Utils.checkNotNull(program.isSentToAddress() || program.isSentToRawPubKey());
        return key != null ? new RedeemData(Collections.singletonList(key), program) : null;
    }

    /**
     * Returns the first key that has private bytes
     */
    public ECKey getFullKey() {
        for (ECKey key : keys)
            if (key.hasPrivKey())
                return key;
        return null;
    }
}
