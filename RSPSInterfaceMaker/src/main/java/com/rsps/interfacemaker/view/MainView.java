package com.rsps.interfacemaker.view;

import com.rsps.interfacemaker.model.InterfaceComponent;
import com.rsps.interfacemaker.model.InterfaceProject;
import com.rsps.interfacemaker.model.SpriteComponent;
import com.rsps.interfacemaker.model.ButtonComponent;
import com.rsps.interfacemaker.model.TextComponent;
import com.rsps.interfacemaker.model.ComponentType;
import com.rsps.interfacemaker.generator.CodeGenerator;
import com.rsps.interfacemaker.util.ProjectSerializer;
import com.rsps.interfacemaker.util.Templates;
import com.rsps.interfacemaker.util.Validator;
import com.rsps.interfacemaker.util.CacheReader;
import com.rsps.interfacemaker.util.ZipExporter;
import com.rsps.interfacemaker.util.SpriteLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainView extends BorderPane {
    private InterfaceProject project;
    private InterfaceCanvas canvas;
    private ComponentListView componentListView;
    private SpriteLibraryPanel spriteLibraryPanel;
    private PropertyPanel propertyPanel;
    private CodePreviewPanel codePreviewPanel;
    private Stage primaryStage;
    private String cachePath = "";

    public MainView(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.project = new InterfaceProject();
        initializeUI();
    }

    private void initializeUI() {
        // Top toolbar
        ToolBar toolBar = createToolBar();
        setTop(toolBar);

        // Main horizontal split: Left panel vs Right area
        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.setDividerPositions(0.15); // Left panel 15%, Right area 85%

        // Left: Component list + Sprite Library (tabbed)
        TabPane leftTabPane = new TabPane();
        
        // Components tab
        componentListView = new ComponentListView(project);
        componentListView.setOnComponentSelectedWithComponent(this::onComponentSelectedFromList);
        ScrollPane leftScroll = new ScrollPane(componentListView);
        leftScroll.setFitToWidth(true);
        leftScroll.setFitToHeight(true);
        Tab componentsTab = new Tab("Components", leftScroll);
        componentsTab.setClosable(false);
        
        // Sprite Library tab
        spriteLibraryPanel = new SpriteLibraryPanel();
        spriteLibraryPanel.setOnSpriteAssigned(this::onSpriteAssigned);
        spriteLibraryPanel.setSelectedComponent(null);
        ScrollPane spriteScroll = new ScrollPane(spriteLibraryPanel);
        spriteScroll.setFitToWidth(true);
        spriteScroll.setFitToHeight(true);
        Tab spriteLibraryTab = new Tab("Sprite Library", spriteScroll);
        spriteLibraryTab.setClosable(false);
        
        leftTabPane.getTabs().addAll(componentsTab, spriteLibraryTab);
        leftTabPane.setMinWidth(150);
        leftTabPane.setMaxWidth(250);

        // Right area: Canvas vs Right sidebar
        SplitPane rightSplitPane = new SplitPane();
        rightSplitPane.setDividerPositions(0.75); // Canvas 75%, Right sidebar 25%

        // Center: Canvas (main focus)
        canvas = new InterfaceCanvas(project);
        canvas.setOnComponentSelected(this::onComponentSelected);
        canvas.setOnComponentsDuplicated(this::onComponentsDuplicated);
        ScrollPane centerScroll = new ScrollPane(canvas);
        centerScroll.setFitToWidth(true);
        centerScroll.setFitToHeight(true);
        centerScroll.setStyle("-fx-background: #1a1a1a;");

        // Right sidebar: Properties vs Code Preview
        SplitPane sidebarSplitPane = new SplitPane();
        sidebarSplitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        sidebarSplitPane.setDividerPositions(0.6); // Properties 60%, Code Preview 40%

        // Right: Property panel
        propertyPanel = new PropertyPanel(project);
        propertyPanel.setStage(primaryStage);
        propertyPanel.setOnComponentModified(this::onComponentModified);
        ScrollPane rightScroll = new ScrollPane(propertyPanel);
        rightScroll.setFitToWidth(true);
        rightScroll.setFitToHeight(true);
        rightScroll.setMinWidth(200);
        rightScroll.setMaxWidth(300);

        // Far right: Code preview
        codePreviewPanel = new CodePreviewPanel(project);
        ScrollPane codeScroll = new ScrollPane(codePreviewPanel);
        codeScroll.setFitToWidth(true);
        codeScroll.setFitToHeight(true);
        codeScroll.setMinWidth(200);
        codeScroll.setMaxWidth(300);

        sidebarSplitPane.getItems().addAll(rightScroll, codeScroll);
        rightSplitPane.getItems().addAll(centerScroll, sidebarSplitPane);
        mainSplitPane.getItems().addAll(leftTabPane, rightSplitPane);
        setCenter(mainSplitPane);

        // Bottom status bar
        Label statusLabel = new Label("Ready");
        setBottom(statusLabel);
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem newProject = new MenuItem("New Project");
        newProject.setOnAction(e -> newProject());
        MenuItem openProject = new MenuItem("Open Project");
        openProject.setOnAction(e -> openProject());
        MenuItem saveProject = new MenuItem("Save Project");
        saveProject.setOnAction(e -> saveProject());
        MenuItem validateProject = new MenuItem("Validate Project");
        validateProject.setOnAction(e -> validateProject());
        MenuItem exportCode = new MenuItem("Export Code");
        exportCode.setOnAction(e -> exportCode());
        MenuItem exportZip = new MenuItem("Export as ZIP Package");
        exportZip.setOnAction(e -> exportZipPackage());
        fileMenu.getItems().addAll(newProject, openProject, saveProject, new SeparatorMenuItem(), validateProject, exportCode, exportZip);

        // Templates menu
        Menu templatesMenu = new Menu("Quick Templates");
        MenuItem emptyTemplate = new MenuItem("Empty Interface");
        emptyTemplate.setOnAction(e -> loadTemplate(Templates.createEmptyTemplate()));
        MenuItem basicDialogTemplate = new MenuItem("Basic Dialog");
        basicDialogTemplate.setOnAction(e -> loadTemplate(Templates.createBasicDialogTemplate()));
        MenuItem shopTemplate = new MenuItem("Shop Interface");
        shopTemplate.setOnAction(e -> loadTemplate(Templates.createShopTemplate()));
        MenuItem teleportTemplate = new MenuItem("Teleport Menu");
        teleportTemplate.setOnAction(e -> loadTemplate(Templates.createTeleportMenuTemplate()));
        templatesMenu.getItems().addAll(emptyTemplate, basicDialogTemplate, shopTemplate, teleportTemplate);

        // Alignment menu
        Menu alignMenu = new Menu("Align");
        MenuItem alignLeft = new MenuItem("Align Left");
        alignLeft.setOnAction(e -> alignComponents("left"));
        MenuItem alignCenter = new MenuItem("Align Center");
        alignCenter.setOnAction(e -> alignComponents("center"));
        MenuItem alignRight = new MenuItem("Align Right");
        alignRight.setOnAction(e -> alignComponents("right"));
        MenuItem alignTop = new MenuItem("Align Top");
        alignTop.setOnAction(e -> alignComponents("top"));
        MenuItem alignMiddle = new MenuItem("Align Middle");
        alignMiddle.setOnAction(e -> alignComponents("middle"));
        MenuItem alignBottom = new MenuItem("Align Bottom");
        alignBottom.setOnAction(e -> alignComponents("bottom"));
        alignMenu.getItems().addAll(alignLeft, alignCenter, alignRight, new SeparatorMenuItem(), alignTop, alignMiddle, alignBottom);

        // Distribute menu
        Menu distributeMenu = new Menu("Distribute");
        MenuItem distributeHorizontal = new MenuItem("Distribute Horizontally");
        distributeHorizontal.setOnAction(e -> distributeComponents("horizontal"));
        MenuItem distributeVertical = new MenuItem("Distribute Vertically");
        distributeVertical.setOnAction(e -> distributeComponents("vertical"));
        distributeMenu.getItems().addAll(distributeHorizontal, distributeVertical);

        // Advanced menu
        Menu advancedMenu = new Menu("Advanced");
        MenuItem loadFromCache = new MenuItem("Load Interface from Cache");
        loadFromCache.setOnAction(e -> loadInterfaceFromCache());
        MenuItem suggestIds = new MenuItem("Suggest Free ID Range");
        suggestIds.setOnAction(e -> suggestFreeIdRange());
        MenuItem setCachePath = new MenuItem("Set Cache Path");
        setCachePath.setOnAction(e -> setCachePath());
        MenuItem setSpriteRoot = new MenuItem("Set Sprite Root Directory");
        setSpriteRoot.setOnAction(e -> setSpriteRootDirectory());
        advancedMenu.getItems().addAll(loadFromCache, suggestIds, setCachePath, new SeparatorMenuItem(), setSpriteRoot);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, templatesMenu, alignMenu, distributeMenu, advancedMenu);

        // Add component buttons
        Button addSpriteBtn = new Button("Add Sprite");
        addSpriteBtn.setOnAction(e -> addSprite());

        Button addButtonBtn = new Button("Add Button");
        addButtonBtn.setOnAction(e -> addButton());

        Button addTextBtn = new Button("Add Text");
        addTextBtn.setOnAction(e -> addText());

        Button addCloseBtn = new Button("Add Close Button");
        addCloseBtn.setOnAction(e -> addCloseButton());

        // Alignment buttons
        Button alignLeftBtn = new Button("← Left");
        alignLeftBtn.setOnAction(e -> alignComponents("left"));
        Button alignCenterBtn = new Button("↔ Center");
        alignCenterBtn.setOnAction(e -> alignComponents("center"));
        Button alignRightBtn = new Button("→ Right");
        alignRightBtn.setOnAction(e -> alignComponents("right"));

        toolBar.getItems().addAll(menuBar, new Separator(), addSpriteBtn, addButtonBtn, addTextBtn, addCloseBtn, new Separator(), alignLeftBtn, alignCenterBtn, alignRightBtn);

        return toolBar;
    }

    private void addSprite() {
        SpriteComponent sprite = new SpriteComponent();
        sprite.setName("Sprite " + (project.getComponents().size() + 1));
        project.addComponent(sprite);
        componentListView.refresh();
        canvas.render();
        codePreviewPanel.updatePreview();
    }

    private void addButton() {
        ButtonComponent button = new ButtonComponent();
        button.setName("Button " + (project.getComponents().size() + 1));
        project.addComponent(button);
        componentListView.refresh();
        canvas.render();
        codePreviewPanel.updatePreview();
    }

    private void addText() {
        TextComponent text = new TextComponent();
        text.setName("Text " + (project.getComponents().size() + 1));
        project.addComponent(text);
        componentListView.refresh();
        canvas.render();
        codePreviewPanel.updatePreview();
    }

    private void addCloseButton() {
        ButtonComponent closeBtn = new ButtonComponent();
        closeBtn.setName("Close Button");
        closeBtn.setX(475);
        closeBtn.setY(10);
        closeBtn.setWidth(20);
        closeBtn.setHeight(20);
        closeBtn.setNormalSpritePath("Interfaces/Common/CLOSE");
        closeBtn.setHoveredSpritePath("Interfaces/Common/CLOSE_HOVER");
        closeBtn.setActionName("Close");
        project.addComponent(closeBtn);
        componentListView.refresh();
        canvas.render();
        codePreviewPanel.updatePreview();
    }

    private void onComponentSelected(InterfaceComponent component) {
        propertyPanel.setComponent(component);
        spriteLibraryPanel.setSelectedComponent(component);
        // Sync selection with component list
        if (component != null) {
            componentListView.selectComponent(component);
        }
    }

    private void onComponentSelectedFromList(InterfaceComponent component) {
        propertyPanel.setComponent(component);
        spriteLibraryPanel.setSelectedComponent(component);
        // Sync selection with canvas
        if (component != null) {
            canvas.selectComponent(component);
        }
    }

    private void onComponentModified(InterfaceComponent component) {
        canvas.render();
        componentListView.refresh();
        codePreviewPanel.updatePreview();
    }

    private void onSpriteAssigned(InterfaceComponent component) {
        propertyPanel.setComponent(component);
        canvas.render();
        componentListView.refresh();
        codePreviewPanel.updatePreview();
    }

    private void onComponentsDuplicated(List<InterfaceComponent> components) {
        componentListView.refresh();
        codePreviewPanel.updatePreview();
    }

    private void newProject() {
        project = new InterfaceProject();
        componentListView.setProject(project);
        canvas.setProject(project);
        propertyPanel.setProject(project);
        codePreviewPanel.setProject(project);
        canvas.render();
    }

    private void loadTemplate(InterfaceProject template) {
        project = template;
        componentListView.setProject(project);
        canvas.setProject(project);
        propertyPanel.setProject(project);
        codePreviewPanel.setProject(project);
        canvas.render();
    }

    private void openProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Project");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                project = ProjectSerializer.loadProject(file);
                componentListView.setProject(project);
                canvas.setProject(project);
                propertyPanel.setProject(project);
                codePreviewPanel.setProject(project);
                canvas.render();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load project: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void saveProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Project");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                ProjectSerializer.saveProject(project, file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Project saved successfully!");
                alert.showAndWait();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save project: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void exportCode() {
        CodeGenerator generator = new CodeGenerator(project);
        String export = generator.generateExport();
        
        TextArea textArea = new TextArea(export);
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12;");
        
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefSize(800, 600);
        
        Stage exportStage = new Stage();
        exportStage.setTitle("Export - " + project.getName());
        exportStage.setScene(new javafx.scene.Scene(scrollPane));
        exportStage.show();
    }

    private void validateProject() {
        Validator.ValidationResult result = Validator.validateProject(project);
        
        StringBuilder message = new StringBuilder();
        
        if (!result.errors.isEmpty()) {
            message.append("ERRORS:\n");
            for (String error : result.errors) {
                message.append("  - ").append(error).append("\n");
            }
            message.append("\n");
        }
        
        if (!result.warnings.isEmpty()) {
            message.append("WARNINGS:\n");
            for (String warning : result.warnings) {
                message.append("  - ").append(warning).append("\n");
            }
        }
        
        if (!result.hasIssues()) {
            message.append("No issues found. Project is valid!");
        }
        
        Alert.AlertType alertType = result.isValid ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
        Alert alert = new Alert(alertType, message.toString());
        alert.setTitle("Validation Results");
        alert.setHeaderText(result.isValid ? "Validation Passed" : "Validation Issues Found");
        alert.showAndWait();
    }

    private void alignComponents(String alignment) {
        java.util.List<InterfaceComponent> selected = canvas.getSelectedComponents();
        if (selected.size() < 2) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select at least 2 components to align (Ctrl+click)");
            alert.showAndWait();
            return;
        }

        switch (alignment) {
            case "left":
                int leftX = selected.stream().mapToInt(InterfaceComponent::getX).min().getAsInt();
                selected.forEach(c -> c.setX(leftX));
                break;
            case "center":
                int centerX = selected.stream().mapToInt(c -> c.getX() + c.getWidth() / 2).min().getAsInt();
                selected.forEach(c -> c.setX(centerX - c.getWidth() / 2));
                break;
            case "right":
                int rightX = selected.stream().mapToInt(c -> c.getX() + c.getWidth()).max().getAsInt();
                selected.forEach(c -> c.setX(rightX - c.getWidth()));
                break;
            case "top":
                int topY = selected.stream().mapToInt(InterfaceComponent::getY).min().getAsInt();
                selected.forEach(c -> c.setY(topY));
                break;
            case "middle":
                int middleY = selected.stream().mapToInt(c -> c.getY() + c.getHeight() / 2).min().getAsInt();
                selected.forEach(c -> c.setY(middleY - c.getHeight() / 2));
                break;
            case "bottom":
                int bottomY = selected.stream().mapToInt(c -> c.getY() + c.getHeight()).max().getAsInt();
                selected.forEach(c -> c.setY(bottomY - c.getHeight()));
                break;
        }

        canvas.render();
        propertyPanel.refresh();
        codePreviewPanel.updatePreview();
    }

    private void distributeComponents(String direction) {
        java.util.List<InterfaceComponent> selected = canvas.getSelectedComponents();
        if (selected.size() < 3) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select at least 3 components to distribute (Ctrl+click)");
            alert.showAndWait();
            return;
        }

        // Sort by position
        if (direction.equals("horizontal")) {
            selected.sort((a, b) -> Integer.compare(a.getX(), b.getX()));
            int firstX = selected.get(0).getX();
            int lastX = selected.get(selected.size() - 1).getX();
            int totalWidth = lastX - firstX;
            int step = totalWidth / (selected.size() - 1);
            for (int i = 1; i < selected.size() - 1; i++) {
                selected.get(i).setX(firstX + (step * i));
            }
        } else {
            selected.sort((a, b) -> Integer.compare(a.getY(), b.getY()));
            int firstY = selected.get(0).getY();
            int lastY = selected.get(selected.size() - 1).getY();
            int totalHeight = lastY - firstY;
            int step = totalHeight / (selected.size() - 1);
            for (int i = 1; i < selected.size() - 1; i++) {
                selected.get(i).setY(firstY + (step * i));
            }
        }

        canvas.render();
        propertyPanel.refresh();
        codePreviewPanel.updatePreview();
    }

    private void exportZipPackage() {
        // Ask for sprite source directory
        DirectoryChooser spriteDirChooser = new DirectoryChooser();
        spriteDirChooser.setTitle("Select Sprite Source Directory");
        File spriteDir = spriteDirChooser.showDialog(primaryStage);
        
        // Ask for output location
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export ZIP Package");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Files", "*.zip"));
        fileChooser.setInitialFileName(project.getName() + "_package.zip");
        File outputFile = fileChooser.showSaveDialog(primaryStage);
        
        if (outputFile != null) {
            try {
                String spriteSourcePath = (spriteDir != null) ? spriteDir.getAbsolutePath() : "";
                ZipExporter.exportToZip(project, spriteSourcePath, outputFile.getAbsolutePath());
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                    "ZIP package exported successfully!\n\n" +
                    "Package contains:\n" +
                    "- " + project.getName() + ".java (Interface method)\n" +
                    "- Implementation_Guide.txt (Setup instructions)\n" +
                    "- sprites/ (Sprite files if source directory provided)\n" +
                    "- EXPORT_SUMMARY.txt (Project information)");
                alert.showAndWait();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to export ZIP: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void loadInterfaceFromCache() {
        if (cachePath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "Please set the cache path first using Advanced > Set Cache Path");
            alert.showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog("40000");
        dialog.setTitle("Load Interface from Cache");
        dialog.setHeaderText("Enter Interface ID to load");
        dialog.setContentText("Interface ID:");

        dialog.showAndWait().ifPresent(idStr -> {
            try {
                int interfaceId = Integer.parseInt(idStr);
                CacheReader.InterfaceData data = CacheReader.loadInterfaceFromCache(cachePath, interfaceId);
                
                if (data != null) {
                    // Show interface data in a dialog
                    StringBuilder info = new StringBuilder();
                    info.append("Interface Data:\n");
                    info.append("ID: ").append(data.id).append("\n");
                    info.append("Parent ID: ").append(data.parentId).append("\n");
                    info.append("Type: ").append(data.type).append("\n");
                    info.append("Size: ").append(data.width).append("x").append(data.height).append("\n");
                    info.append("Position: (").append(data.x).append(", ").append(data.y).append(")\n");
                    info.append("Children: ").append(data.children.size()).append("\n\n");
                    
                    for (CacheReader.ChildData child : data.children) {
                        info.append(child.toString()).append("\n");
                    }
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, info.toString());
                    alert.setTitle("Interface " + interfaceId);
                    alert.setHeaderText("Interface loaded successfully (read-only reference)");
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, 
                        "Interface " + interfaceId + " not found in cache");
                    alert.showAndWait();
                }
            } catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid interface ID");
                alert.showAndWait();
            }
        });
    }

    private void suggestFreeIdRange() {
        if (cachePath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "Please set the cache path first using Advanced > Set Cache Path");
            alert.showAndWait();
            return;
        }

        // Calculate required range size
        int requiredSize = project.getComponents().size() + 2; // +2 for interface ID and buffer
        
        int suggestedId = CacheReader.suggestFreeIdRange(cachePath, requiredSize);
        
        if (suggestedId > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "Suggested free ID range: " + suggestedId + " - " + (suggestedId + requiredSize - 1) + "\n\n" +
                "Required range size: " + requiredSize + " IDs\n" +
                "Suggested starting ID: " + suggestedId + "\n\n" +
                "Would you like to apply this ID range?");
            
            ButtonType applyButton = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(applyButton, cancelButton);
            
            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == applyButton) {
                    project.setInterfaceId(suggestedId);
                    // Reassign component IDs
                    int currentId = suggestedId + 1;
                    for (InterfaceComponent comp : project.getComponents()) {
                        comp.setId(currentId++);
                    }
                    
                    canvas.render();
                    propertyPanel.refresh();
                    codePreviewPanel.updatePreview();
                    
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION, 
                        "ID range applied successfully!");
                    successAlert.showAndWait();
                }
            });
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, 
                "Could not find a free ID range in the cache.\n" +
                "Please try a higher starting ID or check the cache path.");
            alert.showAndWait();
        }
    }

    private void setCachePath() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Cache Directory");
        File cacheDir = dirChooser.showDialog(primaryStage);
        
        if (cacheDir != null) {
            cachePath = cacheDir.getAbsolutePath();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "Cache path set to:\n" + cachePath);
            alert.showAndWait();
        }
    }

    private void setSpriteRootDirectory() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Sprite Root Directory");
        File spriteDir = dirChooser.showDialog(primaryStage);
        
        if (spriteDir != null) {
            SpriteLoader.setSpriteRootDirectory(spriteDir.getAbsolutePath());
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "Sprite root directory set to:\n" + spriteDir.getAbsolutePath() + "\n\n" +
                "This will be used for:\n" +
                "- Converting file paths to relative paths\n" +
                "- Loading sprites in the canvas\n" +
                "- Sprite thumbnails in Properties panel\n" +
                "- Sprite Library panel thumbnails");
            alert.showAndWait();
            
            // Refresh canvas to reload sprites with new root directory
            canvas.render();
            // Refresh sprite library to scan new directory
            spriteLibraryPanel.refresh();
        }
    }
            canvas.render();
        }
    }
}
