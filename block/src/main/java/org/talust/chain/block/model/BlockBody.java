package org.talust.chain.block.model;

import io.protostuff.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * 区块体
 */
public class BlockBody {
//    @Tag(1)
//    private int size;//记录的是每个块中存储的业务数据记录条数
    @Tag(1)//每一个byte[]存储的是业务数据,具体的业务数据内容,由业务自行定义,底层区块不解析业务数据,只做与区块有关的内容,这个data其实就是Record的pb序列化的内容
    private List<byte[]> data = new ArrayList<>();

//    public int getSize() {
//        return size;
//    }
//
//    public void setSize(int size) {
//        this.size = size;
//    }

    public List<byte[]> getData() {
        return data;
    }

    public void setData(List<byte[]> data) {
        this.data = data;
    }
}
