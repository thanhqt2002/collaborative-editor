package edu.icewiz.timny;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.LoadException;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;

import java.io.IOException;

public class EditingPageController {
    private Scene landingPageScene;
    private LandingPageController landingPageController;
    private String myName;
    @FXML TextArea logArea;
    @FXML
    private TextArea editingText;
    EditingServer editingServer;
    EditingClient editingClient;
    String lastReceivedMessage;
    @FXML
    void SaveText(ActionEvent event) {

    }

    @FXML
    void disconnectServer(ActionEvent event) {

    }
    @FXML
    void initialize(){
        lastReceivedMessage = "";
        logArea.setEditable(false);
        editingText.requestFocus();
        editingText.textProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue.equals(newValue) || lastReceivedMessage.equals(newValue))return;
//            editingText.setEditable(false);
//            System.out.println("editingText changed from " + oldValue + " to " + newValue);
            if(editingClient != null){
                editingClient.send(WebSocketMessage.serializeFromString(2, newValue));
            }else if(editingServer != null){
                editingServer.broadcast(WebSocketMessage.serializeFromString(2, newValue));
            }
//            editingText.setEditable(true);
        });
    }

    void openServer(String portString) {
        //The default port is set to 8887
        int port = 8887;
        try{
            port = Integer.parseInt(portString);
        }catch (Exception e){
            e.printStackTrace();
        }
        editingServer = new EditingServer(port);
        editingServer.setName(myName);
        editingServer.setLogArea(logArea);
        editingServer.setEditingText(editingText);
        editingServer.setEditingPageController(this);
        editingServer.start();
    }

    void connectServer(String portString){
        //The default port is set to 8887
        int port = 8887;
        try{
            port = Integer.parseInt(portString);
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
            editingClient = new EditingClient(port);
        }catch (Exception e){
            e.printStackTrace();
        }
        editingClient.setName(myName);
        editingClient.setLogArea(logArea);
        editingClient.setEditingText(editingText);
        editingClient.setEditingPageController(this);
        editingClient.connect();
    }

    public void fromStringToEditingServer(String text, String portString){
        openServer(portString);
        editingText.setText(text);
    }

    public void setLandingPageScene(Scene scene){
        landingPageScene = scene;
    }

    public void setLandingPageController(LandingPageController landingPageController){
        this.landingPageController = landingPageController;
    }
    public void shutdownServerOrClient() {
        try {
            if (editingClient != null) {
                editingClient.close();
                System.out.println("Shut down client");
            }
            if (editingServer != null){
                editingServer.Shutdown();
                System.out.println("Shut down server");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void setMyName(String myName){
        if(myName != null) this.myName = myName;
    }
}
