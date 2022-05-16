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
    @FXML TextArea logArea;
    @FXML
    private TextArea editingText;
    EditingServer editingServer;
    EditingClient editingClient;
    @FXML
    void SaveText(ActionEvent event) {

    }

    @FXML
    void disconnectServer(ActionEvent event) {

    }
    @FXML
    void initialize(){
        logArea.setEditable(false);
    }

    void openServer(String portString) {
        //The default port is set to 9990
        int port = 9990;
        try{
            port = Integer.parseInt(portString);
        }catch (Exception e){

        }
        editingServer = new EditingServer(port);
        editingServer.setLogArea(logArea);
        editingServer.start();
    }

    void connectServer(String portString){
        int port = 9990;
        try{
            port = Integer.parseInt(portString);
            editingClient = new EditingClient(port);
        }catch (Exception e){

        }
        editingClient.setLogArea(logArea);
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

}
