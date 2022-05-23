package edu.icewiz.timny;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainApp extends Application{
    private LandingPageController landingPageController;
    private EditingPageController editingPageController;
    @Override
    public void stop(){
        editingPageController.shutdownServerOrClient();
    }
    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader landingPageLoader = new FXMLLoader(getClass().getResource("fxml/landingPage.fxml"));
        Parent landingPagePane = landingPageLoader.load();
        Scene landingPageScene = new Scene(landingPagePane);

        FXMLLoader editingPageLoader = new FXMLLoader(getClass().getResource("fxml/editingPage.fxml"));
        Parent editingPagePane = editingPageLoader.load();
        Scene editingPageScene = new Scene(editingPagePane);

        landingPageController = landingPageLoader.getController();
        editingPageController = editingPageLoader.getController();

        landingPageController.setEditingPageScene(editingPageScene);
        landingPageController.setEditingPageController(editingPageController);
        editingPageController.setLandingPageScene(landingPageScene);
        editingPageController.setLandingPageController(landingPageController);

        stage.setTitle("Timny");
        stage.setScene(landingPageScene);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                stop();
            }
        });
        stage.show();
    }

    public static void main(String[] args) {
        Platform.runLater(() -> {
            try {
                new MainApp().start(new Stage());
            }catch (Exception e){}
        });
        Platform.runLater(() -> {
            try {
                new MainApp().start(new Stage());
            }catch (Exception e){}
        });
        Platform.runLater(() -> {
            try {
                new MainApp().start(new Stage());
            }catch (Exception e){}
        });
    }
}
