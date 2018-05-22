package com.talust.chain.consensus;

/**
 * 选举状态
 */
public enum VoteStatus {

    NOT_NEED(0, "勿需选举"),
    LOOKING(1, "正在选举");

    private String note;
    private int type;

    VoteStatus(int type, String note) {
        this.type = type;
        this.note = note;
    }

    public static String getNote(int type) {
        for (VoteStatus c : VoteStatus.values()) {
            if (c.getType() == type) {
                return c.getNote();
            }
        }
        return null;
    }

    public static VoteStatus getDataStatus(int type) {
        for (VoteStatus c : VoteStatus.values()) {
            if (c.getType() == type) {
                return c;
            }
        }
        return null;
    }

    public String getNote() {
        return note;
    }

    public int getType() {
        return type;
    }
}
