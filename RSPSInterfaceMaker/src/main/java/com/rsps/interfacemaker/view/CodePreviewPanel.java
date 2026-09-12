package com.rsps.interfacemaker.view;

import com.rsps.interfacemaker.generator.CodeGenerator;
import com.rsps.interfacemaker.model.InterfaceProject;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class CodePreviewPanel extends VBox {
    private InterfaceProject project;
    private TextArea codeArea;
    private TabPane tabPane;

    public CodePreviewPanel(InterfaceProject project) {
        this.project = project;
        initializeUI();
    }

    private void initializeUI() {
        setSpacing(5);
        setStyle("-fx-padding: 5;");

        Label label = new Label("Live Code Preview");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        getChildren().add(label);

        tabPane = new TabPane();

        // Java method tab
        Tab javaTab = new Tab("Java Method");
        codeArea = new TextArea();
        codeArea.setEditable(false);
        codeArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");
        javaTab.setContent(codeArea);
        javaTab.setClosable(false);

        // Implementation guide tab
        Tab guideTab = new Tab("Implementation Guide");
        TextArea guideArea = new TextArea();
        guideArea.setEditable(false);
        guideArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");
        guideTab.setContent(guideArea);
        guideTab.setClosable(false);

        tabPane.getTabs().addAll(javaTab, guideTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        getChildren().add(tabPane);
        setPrefHeight(200);

        updatePreview();
    }

    public void updatePreview() {
        CodeGenerator generator = new CodeGenerator(project);
        codeArea.setText(generator.generateInterfaceMethod());
        
        TextArea guideArea = (TextArea) tabPane.getTabs().get(1).getContent();
        guideArea.setText(generator.generateImplementationGuide());
    }

    public void setProject(InterfaceProject project) {
        this.project = project;
        updatePreview();
    }
}
