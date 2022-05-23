package edu.icewiz.timny;

import edu.icewiz.crdt.CrdtItem;

import java.io.*;
import java.nio.ByteBuffer;

public class WebSocketCrdtItem implements Serializable{
    @Serial
    private static final long serialVersionUID = 61248029351309L;
    int type;
    CrdtItem detail;
    WebSocketCrdtItem(int type, CrdtItem detail){
        this.type = type;
        this.detail = detail;
    }
    WebSocketCrdtItem(ByteBuffer serializedData){
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(serializedData.array());
            ObjectInputStream oInputStream = new ObjectInputStream(bis);
            WebSocketCrdtItem deserializedObject = (WebSocketCrdtItem) oInputStream.readObject();
            oInputStream.close();
            type = deserializedObject.type;
            detail = deserializedObject.detail;
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static ByteBuffer serializeFromItem(int type, CrdtItem data){
        ByteBuffer result = null;
        try {
            WebSocketCrdtItem message = new WebSocketCrdtItem(type, data);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bos);
            os.writeObject(message);
            result = ByteBuffer.wrap(bos.toByteArray());
            os.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
}
