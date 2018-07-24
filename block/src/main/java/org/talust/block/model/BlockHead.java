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

package org.talust.block.model;

import io.protostuff.Tag;

/**
 * 区块头
 */
public class BlockHead {
    @Tag(1)
    private long magic = PlatKey.MAGIC;//魔数
    @Tag(2)//第多少块,即区块高度
    private int height;
    @Tag(3)
    private int version = PlatKey.VERSION;//版本
    @Tag(4)
    private byte[] prevBlock;//前一区块hash
    @Tag(5)
    private byte[] bodyHash;//包体hash
    @Tag(6)
    private Integer time;//打包的时间戳
    @Tag(7)
    private int packNodeCode;//打包的节点的编号


    public long getMagic() {
        return magic;
    }

    public void setMagic(long magic) {
        this.magic = magic;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public byte[] getPrevBlock() {
        return prevBlock;
    }

    public void setPrevBlock(byte[] prevBlock) {
        this.prevBlock = prevBlock;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public byte[] getBodyHash() {
        return bodyHash;
    }

    public void setBodyHash(byte[] bodyHash) {
        this.bodyHash = bodyHash;
    }

    public int getPackNodeCode() {
        return packNodeCode;
    }

    public void setPackNodeCode(int packNodeCode) {
        this.packNodeCode = packNodeCode;
    }
}
