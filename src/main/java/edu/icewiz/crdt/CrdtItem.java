package edu.icewiz.crdt;

public class CrdtItem {
    String value;
    ItemID id;
    ItemID originLeft;
    ItemID originRight;
    boolean isDeleted;
    CrdtItem(String value, ItemID id, ItemID originLeft, ItemID originRight, boolean isDeleted){
        this.value = value;
        this.id = id;
        this.originLeft = originLeft;
        this.originRight = originRight;
        this.isDeleted = isDeleted;
    }

}
