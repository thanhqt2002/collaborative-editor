package edu.icewiz.timny;

import java.io.*;
import java.nio.ByteBuffer;

//class InsertOperationMessage implements Serializable {
//    @Serial
//    private static final long serialVersionUID = 61238028833508L;
//
//}
//

public class WebSocketMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 51258019831509L;
    int type;
    String detail;
    WebSocketMessage(int type, String detail){
        this.type = type;
        this.detail = detail;
    }
    WebSocketMessage(ByteBuffer serializedData){
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(serializedData.array());
            ObjectInputStream oInputStream = new ObjectInputStream(bis);
            WebSocketMessage deserializedObject = (WebSocketMessage) oInputStream.readObject();
            oInputStream.close();
            type = deserializedObject.type;
            detail = deserializedObject.detail;
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
}
