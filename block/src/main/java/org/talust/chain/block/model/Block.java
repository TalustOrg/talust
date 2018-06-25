package org.talust.chain.block.model;

import io.protostuff.Tag;

/**
 * 区块
 */
public class Block {
    @Tag(1)
    private BlockHead head;
    @Tag(2)
    private BlockBody body;

    public BlockHead getHead() {
        return head;
    }

    public void setHead(BlockHead head) {
        this.head = head;
    }

    public BlockBody getBody() {
        return body;
    }

    public void setBody(BlockBody body) {
        this.body = body;
    }
}
