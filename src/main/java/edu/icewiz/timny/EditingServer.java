package edu.icewiz.timny;

import edu.icewiz.crdt.CrdtDoc;
import edu.icewiz.crdt.CrdtItem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.fxmisc.richtext.CodeArea;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.extensions.permessage_deflate.PerMessageDeflateExtension;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class EditingServer extends WebSocketServer {
    HashMap<WebSocket,String> ConnectionInfo = new HashMap<WebSocket,String>();
    private EditingPageController editingPageController;
    private CrdtDoc doc;
    private String myName = "Alice";
    @FXML
    private TextArea logArea;
    @FXML
    private CodeArea codeArea;
    private static final Draft perMessageDeflateDraft = new Draft_6455(
            new PerMessageDeflateExtension());
    public EditingServer(int port) {
        super(new InetSocketAddress(port), Collections.singletonList(perMessageDeflateDraft));
    }
    public EditingServer(InetSocketAddress address) {
        super(address,Collections.singletonList(perMessageDeflateDraft));
    }
    public EditingServer(int port, Draft_6455 draft) {
        super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
    }
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send(WebSocketMessage.serializeFromString(0, "Welcome to the server from " + myName + "!"));
    }
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String info = ConnectionInfo.get(conn) + " has left the room!";
//        broadcast(WebSocketMessage.serializeFromString(0, info));
        logArea.appendText(info);
        logArea.positionCaret(logArea.getLength());
    }
    @Override
    public void onMessage(WebSocket conn, String message){
        //Do not need this function
        //But it is here to fulfill interface requirement
    }
    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        WebSocketMessage operation = new WebSocketMessage(message);

        if(operation.type == 0){
            logArea.appendText(ConnectionInfo.get(conn) + ": " + operation.detail + "!\n");
            logArea.positionCaret(logArea.getLength());
        }else if(operation.type == 1){
            ConnectionInfo.put(conn, operation.detail);
            logArea.appendText(operation.detail + " join the server" + "!\n");
            logArea.positionCaret(logArea.getLength());
        }else{
            if (operation.item == null || operation.item.id == null) return;
            if (operation.type == 2) doc.addInsertOperationToWaitList(operation.item);
            if (operation.type == 3) doc.addDeleteOperationToWaitList(operation.item);
            Runnable update = () -> codeArea.replaceText(doc.toString());
            Platform.runLater(update);
            broadcastExclude(conn, message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null) {
            logArea.appendText("Server encountered an error: " + ex + "!\n");
            logArea.positionCaret(logArea.getLength());
        }
    }

    @Override
    public void onStart() {
//        System.out.println("Server started!");
        logArea.appendText("Server started on port: " + getPort() + "!\n");
        logArea.positionCaret(logArea.getLength());
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

    private void broadcastExclude(WebSocket conn, ByteBuffer message){
        for(WebSocket client : ConnectionInfo.keySet()){
            if(client.equals(conn))continue;
            client.send(message);
        }
    }
    public void setLogArea(TextArea logArea){
        this.logArea = logArea;
    }
    public void setCodeArea(CodeArea codeArea){
        this.codeArea = codeArea;
    }
    public void setName(String name){
        if(name != null)myName = name;
    }
    public void Shutdown() throws InterruptedException{
        stop(1000);
    }
    public void setEditingPageController(EditingPageController editingPageController){
        this.editingPageController = editingPageController;
    }
    public void setCrdtDoc(CrdtDoc doc){
        this.doc = doc;
    }
}
