package org.talust.chain.network.model;

import io.protostuff.Tag;

import java.util.ArrayList;
import java.util.List;

//存储所有节点
public class AllNodes {
    @Tag(1)
    private List<String> nodes = new ArrayList<>();

    public List<String> getNodes() {
        return nodes;
    }

}
