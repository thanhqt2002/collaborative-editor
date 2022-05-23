package edu.icewiz.timny;

import edu.icewiz.crdt.CrdtDoc;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.LoadException;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;
import org.reactfx.collection.ListModification;

public class EditingPageController {
    private Scene landingPageScene;
    private LandingPageController landingPageController;
    CrdtDoc doc = new CrdtDoc();
    private String myName;
    @FXML TextArea logArea;
    @FXML
    private CodeArea codeArea;
    EditingServer editingServer;
    EditingClient editingClient;
    String lastReceivedMessage;
    private ExecutorService executor;

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
    void SaveText(ActionEvent event) {

    }

    @FXML
    void disconnectServer(ActionEvent event) {

    }
    private static final String sampleCode = String.join("\n", new String[] {
            "package com.example;",
            "",
            "import java.util.*;",
            "",
            "public class Foo extends Bar implements Baz {",
            "",
            "    /*",
            "     * multi-line comment",
            "     */",
            "    public static void main(String[] args) {",
            "        // single-line comment",
            "        for(String arg: args) {",
            "            if(arg.length() != 0)",
            "                System.out.println(arg);",
            "            else",
            "                System.err.println(\"Warning: empty string as argument\");",
            "        }",
            "    }",
            "",
            "}"
    });
    @FXML
    void initialize(){
        logArea.setEditable(false);
        executor = Executors.newSingleThreadExecutor();
        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getVisibleParagraphs().addModificationObserver
                (
                        new VisibleParagraphStyler<>(codeArea, this::computeHighlighting)
                );

        // auto-indent: insert previous line's indents on enter
        final Pattern whiteSpace = Pattern.compile("^\\s+");
        codeArea.addEventHandler(KeyEvent.KEY_PRESSED, KE ->
        {
            if (KE.getCode() == KeyCode.ENTER) {
                int caretPosition = codeArea.getCaretPosition();
                int currentParagraph = codeArea.getCurrentParagraph();
                Matcher m0 = whiteSpace.matcher(codeArea.getParagraph(currentParagraph - 1).getSegments().get(0));
                if (m0.find()) Platform.runLater(() -> codeArea.insertText(caretPosition, m0.group()));
            }
        });
        codeArea.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println(myName);
            System.out.printf("new %s\n", newValue);
            System.out.printf("old %s\n", oldValue);
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

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
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

    private class VisibleParagraphStyler<PS, SEG, S> implements Consumer<ListModification<? extends Paragraph<PS, SEG, S>>>
    {
        private final GenericStyledArea<PS, SEG, S> area;
        private final Function<String,StyleSpans<S>> computeStyles;
        private int prevParagraph, prevTextLength;

        public VisibleParagraphStyler( GenericStyledArea<PS, SEG, S> area, Function<String,StyleSpans<S>> computeStyles )
        {
            this.computeStyles = computeStyles;
            this.area = area;
        }

        @Override
        public void accept( ListModification<? extends Paragraph<PS, SEG, S>> lm )
        {
            if ( lm.getAddedSize() > 0 )
            {
                int paragraph = Math.min( area.firstVisibleParToAllParIndex() + lm.getFrom(), area.getParagraphs().size()-1 );
                String text = area.getText( paragraph, 0, paragraph, area.getParagraphLength( paragraph ) );

                if ( paragraph != prevParagraph || text.length() != prevTextLength )
                {
                    int startPos = area.getAbsolutePosition( paragraph, 0 );
                    Platform.runLater( () -> area.setStyleSpans( startPos, computeStyles.apply( text ) ) );
                    prevTextLength = text.length();
                    prevParagraph = paragraph;
                }
            }
        }
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
}
