package edu.icewiz.timny;

import edu.icewiz.crdt.CrdtDoc;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.LoadException;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;

import java.io.IOException;

public class EditingPageController {
    private Scene landingPageScene;
    private LandingPageController landingPageController;
    CrdtDoc doc = new CrdtDoc();
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
        logArea.setEditable(false);
        editingText.requestFocus();
        editingText.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println(myName);
//            System.out.printf("new %s\n", newValue);
//            System.out.printf("old %s\n", oldValue);
            if(oldValue.equals(newValue) || (lastReceivedMessage != null && lastReceivedMessage.equals(newValue)))return;
            int lef = 0;
            while(lef < oldValue.length() && lef < newValue.length() && oldValue.charAt(lef) == newValue.charAt(lef))lef++;
            int rig = 0;
            while(oldValue.length() - rig - 1 >= lef && newValue.length() - rig - 1 >= lef &&
                    oldValue.charAt(oldValue.length() - rig - 1) == newValue.charAt(newValue.length() - rig - 1))rig++;
//            System.out.printf("lef: %d, rig: %d\n", lef, rig);
//            StringBuffer tmp = new StringBuffer(oldValue);
//            tmp.delete(lef,oldValue.length()-rig);
//            tmp.insert(lef,newValue.substring(lef,newValue.length()-rig));
//            System.out.printf("tmp: %s, old: %s, new: %s, doc: %s\n", tmp, oldValue, newValue, doc);
            if(editingClient != null){
//                editingClient.send(WebSocketMessage.serializeFromString(2, newValue));
                for(int i = lef; i < oldValue.length() - rig; ++i){
                    editingClient.send(WebSocketMessage.serializeFromItem(4,doc.localDelete(myName, lef)));
                }
                for(int i = newValue.length() - rig - 1; i >= lef; --i){
                    editingClient.send(WebSocketMessage.serializeFromItem(3, doc.localInsert(myName,lef,newValue.substring(i,i+1))));
                }
            }else if(editingServer != null){
//                editingServer.broadcast(WebSocketMessage.serializeFromString(2, newValue));
                for(int i = lef; i < oldValue.length() - rig; ++i){
                    editingServer.broadcast(WebSocketMessage.serializeFromItem(4,doc.localDelete(myName, lef)));
                }
                for(int i = newValue.length() - rig - 1; i >= lef; --i){
                    editingServer.broadcast(WebSocketMessage.serializeFromItem(3, doc.localInsert(myName,lef,newValue.substring(i,i+1))));
                }
            }
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
        editingServer.setCrdtDoc(doc);
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
        editingClient.setCrdtDoc(doc);
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
