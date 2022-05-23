module timny {
    requires javafx.controls;
    requires javafx.fxml;
    requires Java.WebSocket;
    requires java.desktop;
    requires java.datatransfer;
    requires java.logging;
    requires org.fxmisc.richtext;
    requires reactfx;

    opens edu.icewiz.timny to javafx.fxml;
    exports edu.icewiz.timny;
}