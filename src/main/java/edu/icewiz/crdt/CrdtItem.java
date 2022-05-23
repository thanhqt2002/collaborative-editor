package edu.icewiz.crdt;

import java.io.*;

public class CrdtItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 61228022833708L;
    String value;
    public ItemID id;
    ItemID originLeft;
    ItemID originRight;
    public boolean isDeleted;
    CrdtItem(String value, ItemID id, ItemID originLeft, ItemID originRight, boolean isDeleted){
        this.value = value;
        this.id = id;
        this.originLeft = originLeft;
        this.originRight = originRight;
        this.isDeleted = isDeleted;
    }

}
