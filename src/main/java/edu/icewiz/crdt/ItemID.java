package edu.icewiz.crdt;

import java.io.*;

public class ItemID implements Serializable {
    @Serial
    private static final long serialVersionUID = 61338028833508L;
    String agent;
    int seq;
    ItemID(String agent, int seq) {
        this.agent = agent;
        this.seq = seq;
    }
}
