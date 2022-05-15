module timny {
    requires javafx.controls;
    requires javafx.fxml;
    requires Java.WebSocket;
    requires java.desktop;
    requires java.datatransfer;
    requires java.logging;

    opens edu.icewiz.timny to javafx.fxml;
    exports edu.icewiz.timny;
}