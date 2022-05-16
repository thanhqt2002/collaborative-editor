package edu.icewiz.timny;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.extensions.permessage_deflate.PerMessageDeflateExtension;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;

public class EditingServer extends WebSocketServer {
    @FXML
    private TextArea logArea;
    private static final Draft perMessageDeflateDraft = new Draft_6455(
            new PerMessageDeflateExtension());
    public EditingServer(int port) {
        super(new InetSocketAddress(port), Collections.singletonList(perMessageDeflateDraft));
    }
    public EditingServer(InetSocketAddress address) {
        super(address);
    }
    public EditingServer(int port, Draft_6455 draft) {
        super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
    }
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send("Welcome to the server!"); //This method sends a message to the new client
        broadcast("new connection: " + handshake
                .getResourceDescriptor()); //This method sends a message to all clients connected
        System.out.println(
                conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
    }
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        broadcast(conn + " has left the room!");
        System.out.println(conn + " has left the room!");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        broadcast(message);
        System.out.println(conn + ": " + message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        broadcast(message.array());
        System.out.println(conn + ": " + message);
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

    public void setLogArea(TextArea logArea){
        this.logArea = logArea;
    }

}
