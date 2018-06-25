package org.talust.chain.common.tools;

import java.util.Date;

public class CacheData<T> {
    CacheData(T t, int expire) {
        this.data = t;
        this.expire = expire <= 0 ? 0 : expire * 1000;
        this.saveTime = System.currentTimeMillis() + this.expire;
    }

    private T data;
    private long saveTime; // 存活时间
    private long expire;   // 过期时间 小于等于0标识永久存活

    public T getData() {
        return data;
    }

    public long getExpire() {
        return expire;
    }

    public long getSaveTime() {
        return saveTime;
    }
}
