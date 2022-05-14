package edu.icewiz.crdt;

public class ItemID {
    String agent;
    int seq;
    ItemID(String agent, int seq) {
        this.agent = agent;
        this.seq = seq;
    }
}
