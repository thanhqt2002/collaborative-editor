package edu.icewiz.timny;

import edu.icewiz.crdt.CrdtDoc;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.concurrent.Task;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;


import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

public class EditingPageController {
    private Scene landingPageScene;
    private LandingPageController landingPageController;
    CrdtDoc doc = new CrdtDoc();
    private String myName;
    @FXML TextArea logArea;
    @FXML
    private CodeArea codeArea;
    @FXML
    private Button disconnectButton;
    @FXML
    private Label peerNumber;
    EditingServer editingServer;
    EditingClient editingClient;
    String lastReceivedMessage;
    private ExecutorService executor;
    ReadWriteLock lock = new ReentrantReadWriteLock();
    Lock writeLock = lock.writeLock();
    Lock readLock = lock.readLock();

    private static final String[] KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );


    @FXML
    void disconnectServer(ActionEvent event) {
        codeArea.setEditable(false);
        disconnectButton.setDisable(true);
        if(editingServer != null){
            logArea.appendText("Server shutdown, disconnecting all peers\n");
            logArea.positionCaret(logArea.getLength());
        }
        shutdownServerOrClient();
    }
    @FXML
    void initialize(){
        Platform.setImplicitExit(false);

        logArea.setEditable(false);
        executor = Executors.newSingleThreadExecutor();
        codeArea.setStyle("-fx-font-family: monospace; -fx-font-size: 15pt;");
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        Subscription cleanupWhenDone = codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(200))
                .retainLatestUntilLater(executor)
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(codeArea.multiPlainChanges())
                .filterMap(t -> {
                    if(t.isSuccess()) {
                        return Optional.of(t.get());
                    } else {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                })
                .subscribe(this::applyHighlighting);
        codeArea.textProperty().addListener((observable, oldValue, newValue) -> {
//            try {
//                writeLock.lock();
                if(oldValue.equals(newValue) || newValue.equals(doc.toString()))return;
                int lef = 0;
                while(lef < oldValue.length() && lef < newValue.length() && oldValue.charAt(lef) == newValue.charAt(lef))lef++;
                int rig = 0;
                while(oldValue.length() - rig - 1 >= lef && newValue.length() - rig - 1 >= lef &&
                        oldValue.charAt(oldValue.length() - rig - 1) == newValue.charAt(newValue.length() - rig - 1))rig++;
//                System.out.printf("lef: %d, rig: %d\n", lef, rig);
//                StringBuffer tmp = new StringBuffer(oldValue);
//                tmp.delete(lef,oldValue.length()-rig);
//                tmp.insert(lef,newValue.substring(lef,newValue.length()-rig));
                if (editingClient != null) {
                    for (int i = lef; i < oldValue.length() - rig; ++i) {
                        editingClient.send(WebSocketMessage.serializeFromItem(3, doc.localDelete(myName, lef)));
                    }
                    for (int i = newValue.length() - rig - 1; i >= lef; --i) {
                        editingClient.send(WebSocketMessage.serializeFromItem(2, doc.localInsert(myName, lef, newValue.substring(i, i + 1))));
                    }
                } else if (editingServer != null) {
                    for (int i = lef; i < oldValue.length() - rig; ++i) {
                        editingServer.broadcast(WebSocketMessage.serializeFromItem(3, doc.localDelete(myName, lef)));
                    }
                    for (int i = newValue.length() - rig - 1; i >= lef; --i) {
                        editingServer.broadcast(WebSocketMessage.serializeFromItem(2, doc.localInsert(myName, lef, newValue.substring(i, i + 1))));
                    }
                }
//                System.out.printf("tmp: %s, old: %s, new: %s, doc: %s\n", tmp, oldValue, newValue, doc);
//                assert(tmp.equals(doc.toString()));
//            }finally {
//                writeLock.unlock();
//            }
        });
    }

    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                return computeHighlighting(text);
            }
        };
        executor.execute(task);
        return task;
    }

    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        codeArea.setStyleSpans(0, highlighting);
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("PAREN") != null ? "paren" :
                                    matcher.group("BRACE") != null ? "brace" :
                                            matcher.group("BRACKET") != null ? "bracket" :
                                                    matcher.group("SEMICOLON") != null ? "semicolon" :
                                                            matcher.group("STRING") != null ? "string" :
                                                                    matcher.group("COMMENT") != null ? "comment" :
                                                                            null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
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
        editingServer.setCodeArea(codeArea);
        editingServer.setEditingPageController(this);
        editingServer.setCrdtDoc(doc);
//        editingServer.setLock(lock,writeLock,readLock);
        editingServer.setPeerNumber(peerNumber);
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
        editingClient.setCodeArea(codeArea);
        editingClient.setEditingPageController(this);
        editingClient.setCrdtDoc(doc);
        peerNumber.setVisible(false);
//        editingClient.setLock(lock,writeLock,readLock);
        editingClient.connect();
    }

    public void fromStringToEditingServer(String text, String portString){
        openServer(portString);
        codeArea.replaceText(text);
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
    public void setButtonName(String buttonName){
        disconnectButton.setText(buttonName);
    }
}
