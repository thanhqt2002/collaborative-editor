package edu.icewiz.timny;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.icewiz.crdt.CrdtDoc;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.fxmisc.richtext.CodeArea;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.permessage_deflate.PerMessageDeflateExtension;
import org.java_websocket.handshake.ServerHandshake;

public class EditingClient extends WebSocketClient {
    private String myName = "Bob";
    private EditingPageController editingPageController;
    private CrdtDoc doc;
    @FXML
    private CodeArea codeArea;
    @FXML
    private TextArea logArea;
    private static final Draft perMessageDeflateDraft = new Draft_6455(
            new PerMessageDeflateExtension());
    public EditingClient(int port) throws URISyntaxException{
        super(new URI("ws://localhost:" + port), perMessageDeflateDraft);
    }
    public EditingClient(URI address) {
        super(address,perMessageDeflateDraft);
    }
    public EditingClient(int port, Draft_6455 draft) throws URISyntaxException{
        super(new URI("ws://localhost:" + port), draft);
    }
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logArea.appendText("Established connection on " + getURI() + "!\n");
        logArea.positionCaret(logArea.getLength());
        send(WebSocketMessage.serializeFromString(1, myName));
        send(WebSocketMessage.serializeFromString(0, "Hello Server from " + myName));
    }
    @Override
    public void onMessage(String message){
        //Do not need this function
        //But it is here to fulfill interface requirement
    }
    @Override
    public void onMessage(ByteBuffer message) {
        WebSocketMessage operation = new WebSocketMessage(message);
        if(operation.type == 0){
            logArea.appendText("Received message: " + operation.detail + "!\n");
            logArea.positionCaret(logArea.getLength());
        }else {
            if(operation.type == 5){
                Runnable update = () -> codeArea.replaceText(doc.toString());
                Platform.runLater(update);
                return;
            }
            if (operation.item == null || operation.item.id == null) return;
            if (operation.type == 2 || operation.type == 4) doc.addInsertOperationToWaitList(operation.item);
            if (operation.type == 3) doc.addDeleteOperationToWaitList(operation.item);
            if(operation.type != 4) {
                Runnable update = () -> codeArea.replaceText(doc.toString());
                Platform.runLater(update);
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logArea.appendText("You are disconnected from the room\n");
        logArea.positionCaret(logArea.getLength());
        codeArea.setEditable(false);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        logArea.appendText("Client encountered an error: " + ex + "!\n");
        logArea.positionCaret(logArea.getLength());
    }

    public void setLogArea(TextArea logArea){
        this.logArea = logArea;
    }
    public void setCodeArea(CodeArea codeArea){
        this.codeArea = codeArea;
    }

    public void setName(String name){
        if(name != null)this.myName = name;
    }
    public void setEditingPageController(EditingPageController editingPageController){
        this.editingPageController = editingPageController;
    }
    public void setCrdtDoc(CrdtDoc doc){
        this.doc = doc;
    }
}
