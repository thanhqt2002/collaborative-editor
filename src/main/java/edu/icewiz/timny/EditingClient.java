package edu.icewiz.timny;

import java.net.URI;
import java.net.URISyntaxException;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.permessage_deflate.PerMessageDeflateExtension;
import org.java_websocket.handshake.ServerHandshake;

public class EditingClient extends WebSocketClient {
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
    }

    @Override
    public void onMessage(String message) {
        logArea.appendText("Received message: " + message + "\n");
        logArea.positionCaret(logArea.getLength());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
    }

    @Override
    public void onError(Exception ex) {
    }

    public void setLogArea(TextArea logArea){
        this.logArea = logArea;
    }
}
