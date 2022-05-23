package edu.icewiz.timny;

import edu.icewiz.crdt.CrdtItem;

import java.io.*;
import java.nio.ByteBuffer;

//class InsertOperationMessage implements Serializable {
//
//
//

public class WebSocketMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 51258019831509L;
    int type;
    String detail;
    CrdtItem item;
    WebSocketMessage(int type, String detail){
        this.type = type;
        this.detail = detail;
        this.item = null;
    }
    WebSocketMessage(int type, CrdtItem item){
        this.type = type;
        this.item = item;
        this.detail = null;
    }
    WebSocketMessage(ByteBuffer serializedData){
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(serializedData.array());
            ObjectInputStream oInputStream = new ObjectInputStream(bis);
            WebSocketMessage deserializedObject = (WebSocketMessage) oInputStream.readObject();
            oInputStream.close();
            type = deserializedObject.type;
            detail = deserializedObject.detail;
            item = deserializedObject.item;
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static ByteBuffer serializeFromString(int type, String data){
        ByteBuffer result = null;
        try {
            WebSocketMessage message = new WebSocketMessage(type, data);
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
    public static ByteBuffer serializeFromItem(int type, CrdtItem data){
        ByteBuffer result = null;
        try {
            WebSocketMessage message = new WebSocketMessage(type, data);
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