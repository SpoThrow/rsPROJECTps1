package com.rsps.interfacemaker.view;

import com.rsps.interfacemaker.generator.CodeGenerator;
import com.rsps.interfacemaker.model.InterfaceProject;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.AnchorPane;

public class CodePreviewPanel extends VBox {
    private InterfaceProject project;
    private TextArea codeArea;
    private TextArea guideArea;
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
        AnchorPane javaTabContent = new AnchorPane(codeArea);
        AnchorPane.setTopAnchor(codeArea, 0.0);
        AnchorPane.setBottomAnchor(codeArea, 0.0);
        AnchorPane.setLeftAnchor(codeArea, 0.0);
        AnchorPane.setRightAnchor(codeArea, 0.0);
        javaTab.setContent(javaTabContent);
        javaTab.setClosable(false);

        // Implementation guide tab
        Tab guideTab = new Tab("Implementation Guide");
        guideArea = new TextArea();
        guideArea.setEditable(false);
        guideArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");
        AnchorPane guideTabContent = new AnchorPane(guideArea);
        AnchorPane.setTopAnchor(guideArea, 0.0);
        AnchorPane.setBottomAnchor(guideArea, 0.0);
        AnchorPane.setLeftAnchor(guideArea, 0.0);
        AnchorPane.setRightAnchor(guideArea, 0.0);
        guideTab.setContent(guideTabContent);
        guideTab.setClosable(false);

        tabPane.getTabs().addAll(javaTab, guideTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        getChildren().add(tabPane);

        updatePreview();
    }

    public void updatePreview() {
        CodeGenerator generator = new CodeGenerator(project);
        codeArea.setText(generator.generateInterfaceMethod());
        guideArea.setText(generator.generateImplementationGuide());
    }

    public void setProject(InterfaceProject project) {
        this.project = project;
        updatePreview();
    }
}
