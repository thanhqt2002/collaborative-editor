package edu.icewiz.timny;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application{
    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader landingPageLoader = new FXMLLoader(getClass().getResource("fxml/landingPage.fxml"));
        Parent landingPagePane = landingPageLoader.load();
        Scene landingPageScene = new Scene(landingPagePane);

        FXMLLoader editingPageLoader = new FXMLLoader(getClass().getResource("fxml/editingPage.fxml"));
        Parent editingPagePane = editingPageLoader.load();
        Scene editingPageScene = new Scene(editingPagePane);

        LandingPageController landingPageController = landingPageLoader.getController();
        EditingPageController editingPageController = editingPageLoader.getController();

        landingPageController.setEditingPageScene(editingPageScene);
        editingPageController.setLandingPageScene(landingPageScene);

        stage.setTitle("Timny");
        stage.setScene(landingPageScene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
