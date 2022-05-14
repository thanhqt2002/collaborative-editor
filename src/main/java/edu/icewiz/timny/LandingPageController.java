package edu.icewiz.timny;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.stage.Stage;


import java.io.*;

public class LandingPageController {

    private Scene editingPageScene;
    @FXML
    private Button connectButton;

    @FXML
    private TextField linkTextArea;

    @FXML
    private Button newButton;

    @FXML
    private Button openButton;

    @FXML
    void initialize(){
        linkTextArea.setText("9990");
    }

    @FXML
    void connectPort(ActionEvent event) {

    }

    @FXML
    void newFile(ActionEvent event) {

    }

    @FXML
    void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File fileToLoad = fileChooser.showOpenDialog(null);
//        if(fileToLoad != null){
//            loadFileToTextArea(fileToLoad);
//        }
        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        stage.setScene(editingPageScene);
    }

    public void setEditingPageScene(Scene scene){
        editingPageScene = scene;
    }
}
