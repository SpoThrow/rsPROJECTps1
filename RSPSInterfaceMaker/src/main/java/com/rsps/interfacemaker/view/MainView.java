package com.rsps.interfacemaker.view;

import com.rsps.interfacemaker.model.InterfaceComponent;
import com.rsps.interfacemaker.model.InterfaceProject;
import com.rsps.interfacemaker.model.SpriteComponent;
import com.rsps.interfacemaker.model.ButtonComponent;
import com.rsps.interfacemaker.model.TextComponent;
import com.rsps.interfacemaker.model.EditHistory;
import com.rsps.interfacemaker.generator.CodeGenerator;
import com.rsps.interfacemaker.util.ProjectSerializer;
import com.rsps.interfacemaker.util.Templates;
import com.rsps.interfacemaker.util.Validator;
import com.rsps.interfacemaker.util.CacheReader;
import com.rsps.interfacemaker.util.JavaInterfaceParser;
import com.rsps.interfacemaker.util.JavaInterfaceWriter;
import com.rsps.interfacemaker.util.ClientWorkspace;
import com.rsps.interfacemaker.util.ZipExporter;
import com.rsps.interfacemaker.util.SpriteLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.time.format.DateTimeFormatter;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javafx.geometry.Orientation;

public class MainView extends BorderPane {
    private InterfaceProject project;
    private InterfaceCanvas canvas;
    private ComponentListView componentListView;
    private SpriteLibraryPanel spriteLibraryPanel;
    private PropertyPanel propertyPanel;
    private CodePreviewPanel codePreviewPanel;
    private TextArea debugConsole;
    private Stage primaryStage;
    private String cachePath = "";
    private String interfacesFilePath = "";
    private final EditHistory history = new EditHistory();
    private Label zoomLabel;

    public MainView(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.project = new InterfaceProject();
        initializeUI();
    }

    private void initializeUI() {
        // Top toolbar
        ToolBar toolBar = createToolBar();
        setTop(toolBar);

        // Main vertical split: Top (main content) vs Bottom (debug console)
        SplitPane mainVerticalSplit = new SplitPane();
        mainVerticalSplit.setOrientation(Orientation.VERTICAL);
        mainVerticalSplit.setDividerPositions(0.85); // Main content 85%, Debug console 15%

        // Main horizontal split: Left panel vs Right area
        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.setDividerPositions(0.15); // Left panel 15%, Right area 85%

        // Left: Component list + Sprite Library (tabbed)
        TabPane leftTabPane = new TabPane();
        
        // Components tab
        componentListView = new ComponentListView(project);
        componentListView.setOnComponentSelectedWithComponent(this::onComponentSelectedFromList);
        componentListView.setOnComponentsChanged(this::onComponentsChanged);
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
        canvas.setOnComponentsChanged(this::onComponentsChanged);
        canvas.setOnEditStarted(() -> history.push(project));
        canvas.setOnZoomChanged(this::updateZoomLabel);
        ScrollPane centerScroll = new ScrollPane(canvas);
        centerScroll.setFitToWidth(false);
        centerScroll.setFitToHeight(false);
        centerScroll.setPannable(true);
        centerScroll.setStyle("-fx-background: #1a1a1a;");
        
        // Right sidebar: Properties vs Code Preview
        SplitPane sidebarSplitPane = new SplitPane();
        sidebarSplitPane.setOrientation(Orientation.VERTICAL);
        sidebarSplitPane.setDividerPositions(0.4); // Properties 40%, Code Preview 60%

        // Right: Property panel
        propertyPanel = new PropertyPanel(project);
        propertyPanel.setStage(primaryStage);
        propertyPanel.setOnComponentModified(this::onComponentModified);
        ScrollPane rightScroll = new ScrollPane(propertyPanel);
        rightScroll.setFitToWidth(true);
        rightScroll.setFitToHeight(true);
        rightScroll.setMinWidth(200);
        rightScroll.setMaxWidth(350);

        // Far right: Code preview
        codePreviewPanel = new CodePreviewPanel(project);
        codePreviewPanel.setMinWidth(250);
        codePreviewPanel.setMaxWidth(450);

        sidebarSplitPane.getItems().addAll(rightScroll, codePreviewPanel);
        rightSplitPane.getItems().addAll(centerScroll, sidebarSplitPane);
        mainSplitPane.getItems().addAll(leftTabPane, rightSplitPane);
        
        // Debug console at bottom
        debugConsole = new TextArea();
        debugConsole.setStyle("-fx-background-color: #0a0a0a; -fx-text-fill: #00ff00; -fx-font-family: monospace; -fx-font-size: 11px;");
        debugConsole.setEditable(false);
        debugConsole.setWrapText(true);
        debugConsole.setPromptText("Debug Console:");
        debugConsole.setMinHeight(80);
        debugConsole.setMaxHeight(200);
        
        mainVerticalSplit.getItems().addAll(mainSplitPane, debugConsole);
        setCenter(mainVerticalSplit);
        
        // Log startup message
        logDebug("RSPS Interface Maker started");
        detectClientWorkspace();
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem loadFromClient = new MenuItem("Load from Client...");
        loadFromClient.setOnAction(e -> loadInterfaceFromJavaSource());
        MenuItem saveClientItem = new MenuItem("Save to Client");
        saveClientItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveClientItem.setOnAction(e -> saveToClient());
        MenuItem newProject = new MenuItem("New Project");
        newProject.setOnAction(e -> newProject());
        MenuItem openProject = new MenuItem("Open Project");
        openProject.setOnAction(e -> openProject());
        MenuItem saveProject = new MenuItem("Save Project");
        saveProject.setOnAction(e -> saveProject());
        MenuItem renameProject = new MenuItem("Rename Interface");
        renameProject.setOnAction(e -> renameInterface());
        MenuItem validateProject = new MenuItem("Validate Project");
        validateProject.setOnAction(e -> validateProject());
        MenuItem exportCode = new MenuItem("Export Code");
        exportCode.setOnAction(e -> exportCode());
        MenuItem exportZip = new MenuItem("Export as ZIP Package");
        exportZip.setOnAction(e -> exportZipPackage());
        fileMenu.getItems().addAll(loadFromClient, saveClientItem, new SeparatorMenuItem(),
            newProject, openProject, saveProject, renameProject, new SeparatorMenuItem(),
            validateProject, exportCode, exportZip);

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

        Menu editMenu = new Menu("Edit");
        MenuItem undoItem = new MenuItem("Undo");
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        undoItem.setOnAction(e -> undo());
        MenuItem redoItem = new MenuItem("Redo");
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        redoItem.setOnAction(e -> redo());
        editMenu.getItems().addAll(undoItem, redoItem);

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
        MenuItem loadFromCache = new MenuItem("Load Interface from Java Source");
        loadFromCache.setOnAction(e -> loadInterfaceFromJavaSource());
        MenuItem suggestIds = new MenuItem("Suggest Free ID Range");
        suggestIds.setOnAction(e -> suggestFreeIdRange());
        MenuItem setCachePath = new MenuItem("Set Cache Path");
        setCachePath.setOnAction(e -> setCachePath());
        MenuItem setInterfacesPath = new MenuItem("Set Interfaces.java Path");
        setInterfacesPath.setOnAction(e -> setInterfacesPath());
        MenuItem setSpriteRoot = new MenuItem("Set Sprite Root Directory");
        setSpriteRoot.setOnAction(e -> setSpriteRootDirectory());
        MenuItem setDisplayOffsets = new MenuItem("Set Display Offsets");
        setDisplayOffsets.setOnAction(e -> setDisplayOffsets());
        advancedMenu.getItems().addAll(loadFromCache, suggestIds, setCachePath, setInterfacesPath, new SeparatorMenuItem(), setSpriteRoot, setDisplayOffsets);

        // View menu
        Menu viewMenu = new Menu("View");
        CheckMenuItem toggleGrid = new CheckMenuItem("Show Grid");
        toggleGrid.setSelected(true);
        toggleGrid.setOnAction(e -> {
            canvas.toggleGrid();
            toggleGrid.setSelected(canvas.isGridVisible());
        });
        viewMenu.getItems().add(toggleGrid);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, editMenu, templatesMenu, alignMenu, distributeMenu, advancedMenu, viewMenu);

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

        Button saveClientBtn = new Button("Save to Client");
        saveClientBtn.setOnAction(e -> saveToClient());

        Button zoomOutBtn = new Button("Zoom -");
        zoomOutBtn.setOnAction(e -> {
            canvas.setZoom(canvas.getZoom() / 1.25);
            updateZoomLabel();
        });
        zoomLabel = new Label("100%");
        Button zoomInBtn = new Button("Zoom +");
        zoomInBtn.setOnAction(e -> {
            canvas.setZoom(canvas.getZoom() * 1.25);
            updateZoomLabel();
        });
        Button undoBtn = new Button("Undo");
        undoBtn.setOnAction(e -> undo());
        Button redoBtn = new Button("Redo");
        redoBtn.setOnAction(e -> redo());

        toolBar.getItems().addAll(menuBar, new Separator(), saveClientBtn, undoBtn, redoBtn, new Separator(),
            zoomOutBtn, zoomLabel, zoomInBtn, new Separator(),
            addSpriteBtn, addButtonBtn, addTextBtn, addCloseBtn, new Separator(),
            alignLeftBtn, alignCenterBtn, alignRightBtn);

        return toolBar;
    }

    private void addSprite() {
        try {
            logDebug("ACTION: Adding new sprite component");
            SpriteComponent sprite = new SpriteComponent();
            sprite.setName("Sprite " + (project.getComponents().size() + 1));
            project.addComponent(sprite);
            componentListView.refresh();
            canvas.render();
            codePreviewPanel.updatePreview();
            logDebug("SUCCESS: Sprite component added, ID: " + sprite.getId());
        } catch (Exception e) {
            logDebug("ERROR: Failed to add sprite component - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addButton() {
        try {
            logDebug("ACTION: Adding new button component");
            ButtonComponent button = new ButtonComponent();
            button.setName("Button " + (project.getComponents().size() + 1));
            project.addComponent(button);
            componentListView.refresh();
            canvas.render();
            codePreviewPanel.updatePreview();
            logDebug("SUCCESS: Button component added, ID: " + button.getId());
        } catch (Exception e) {
            logDebug("ERROR: Failed to add button component - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addText() {
        try {
            logDebug("ACTION: Adding new text component");
            TextComponent text = new TextComponent();
            text.setName("Text " + (project.getComponents().size() + 1));
            project.addComponent(text);
            componentListView.refresh();
            canvas.render();
            codePreviewPanel.updatePreview();
            logDebug("SUCCESS: Text component added, ID: " + text.getId());
        } catch (Exception e) {
            logDebug("ERROR: Failed to add text component - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addCloseButton() {
        try {
            logDebug("ACTION: Adding close button component");
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
            logDebug("SUCCESS: Close button component added, ID: " + closeBtn.getId());
        } catch (Exception e) {
            logDebug("ERROR: Failed to add close button component - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onComponentSelected(InterfaceComponent component) {
        try {
            logDebug("ACTION: Component selected on canvas: " + (component != null ? component.getName() : "null"));
            propertyPanel.setComponent(component);
            spriteLibraryPanel.setSelectedComponent(component);
            // Sync selection with component list
            if (component != null) {
                componentListView.selectComponent(component);
            }
        } catch (Exception e) {
            logDebug("ERROR: Failed to select component - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onComponentSelectedFromList(InterfaceComponent component) {
        try {
            logDebug("ACTION: Component selected from list: " + (component != null ? component.getName() : "null"));
            propertyPanel.setComponent(component);
            spriteLibraryPanel.setSelectedComponent(component);
            // Sync selection with canvas
            if (component != null) {
                canvas.selectComponent(component);
            }
        } catch (Exception e) {
            logDebug("ERROR: Failed to select component from list - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onComponentModified(InterfaceComponent component) {
        try {
            logDebug("ACTION: Component modified: " + (component != null ? component.getName() : "null"));
            canvas.render();
            componentListView.refresh();
            codePreviewPanel.updatePreview();
        } catch (Exception e) {
            logDebug("ERROR: Failed to handle component modification - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onComponentsChanged() {
        try {
            logDebug("ACTION: Components changed, updating code preview");
            codePreviewPanel.updatePreview();
        } catch (Exception e) {
            logDebug("ERROR: Failed to handle components changed - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onSpriteAssigned(InterfaceComponent component) {
        try {
            logDebug("ACTION: Sprite assigned to component: " + (component != null ? component.getName() : "null"));
            propertyPanel.setComponent(component);
            canvas.render();
            componentListView.refresh();
            codePreviewPanel.updatePreview();
        } catch (Exception e) {
            logDebug("ERROR: Failed to handle sprite assignment - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onComponentsDuplicated(List<InterfaceComponent> components) {
        try {
            logDebug("ACTION: Components duplicated, count: " + components.size());
            componentListView.refresh();
            codePreviewPanel.updatePreview();
        } catch (Exception e) {
            logDebug("ERROR: Failed to handle component duplication - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void newProject() {
        try {
            logDebug("ACTION: Creating new project");
            TextInputDialog dialog = new TextInputDialog("NewInterface");
            dialog.setTitle("New Interface");
            dialog.setHeaderText("Enter a name for your new interface:");
            dialog.setContentText("Interface Name:");
            
            dialog.showAndWait().ifPresent(name -> {
                logDebug("New project name: " + name);
                project = new InterfaceProject();
                project.setName(name);
                componentListView.setProject(project);
                canvas.setProject(project);
                propertyPanel.setProject(project);
                codePreviewPanel.setProject(project);
                canvas.render();
                logDebug("SUCCESS: New project created");
            });
        } catch (Exception e) {
            logDebug("ERROR: Failed to create new project - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadTemplate(InterfaceProject template) {
        try {
            logDebug("ACTION: Loading template: " + template.getName());
            TextInputDialog dialog = new TextInputDialog(template.getName());
            dialog.setTitle("Load Template");
            dialog.setHeaderText("Enter a name for this interface:");
            dialog.setContentText("Interface Name:");
            
            dialog.showAndWait().ifPresent(name -> {
                logDebug("Template loaded with name: " + name);
                project = template;
                project.setName(name);
                componentListView.setProject(project);
                canvas.setProject(project);
                propertyPanel.setProject(project);
                codePreviewPanel.setProject(project);
                canvas.render();
                logDebug("SUCCESS: Template loaded");
            });
        } catch (Exception e) {
            logDebug("ERROR: Failed to load template - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void renameInterface() {
        try {
            logDebug("ACTION: Renaming interface");
            TextInputDialog dialog = new TextInputDialog(project.getName());
            dialog.setTitle("Rename Interface");
            dialog.setHeaderText("Enter a new name for this interface:");
            dialog.setContentText("Interface Name:");
            
            dialog.showAndWait().ifPresent(name -> {
                logDebug("Renaming from '" + project.getName() + "' to '" + name + "'");
                project.setName(name);
                codePreviewPanel.updatePreview();
                logDebug("SUCCESS: Interface renamed");
            });
        } catch (Exception e) {
            logDebug("ERROR: Failed to rename interface - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openProject() {
        try {
            logDebug("ACTION: Opening project");
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Project");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                logDebug("Opening file: " + file.getAbsolutePath());
                project = ProjectSerializer.loadProject(file);
                logDebug("Project loaded: " + project.getName() + ", components: " + project.getComponents().size());
                
                // Restore sprite root directory if saved
                if (project.getSpriteRootDirectory() != null && !project.getSpriteRootDirectory().isEmpty()) {
                    logDebug("Restoring sprite root: " + project.getSpriteRootDirectory());
                    SpriteLoader.setSpriteRootDirectory(project.getSpriteRootDirectory());
                    spriteLibraryPanel.refresh();
                }
                
                // Restore cache path if saved
                if (project.getCachePath() != null && !project.getCachePath().isEmpty()) {
                    logDebug("Restoring cache path: " + project.getCachePath());
                    cachePath = project.getCachePath();
                }
                
                // Restore interfaces file path if saved
                if (project.getInterfacesFilePath() != null && !project.getInterfacesFilePath().isEmpty()) {
                    logDebug("Restoring interfaces file path: " + project.getInterfacesFilePath());
                    interfacesFilePath = project.getInterfacesFilePath();
                }
                
                componentListView.setProject(project);
                canvas.setProject(project);
                propertyPanel.setProject(project);
                codePreviewPanel.setProject(project);
                canvas.render();
                logDebug("SUCCESS: Project opened successfully");
            }
        } catch (IOException e) {
            logDebug("ERROR: Failed to open project - " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load project: " + e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            logDebug("ERROR: Unexpected error opening project - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveProject() {
        try {
            logDebug("ACTION: Saving project");
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Project");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                logDebug("Saving to file: " + file.getAbsolutePath());
                // Save current sprite root directory and cache path to project
                project.setSpriteRootDirectory(SpriteLoader.getSpriteRootDirectory());
                project.setCachePath(cachePath);
                project.setInterfacesFilePath(interfacesFilePath);
                
                ProjectSerializer.saveProject(project, file);
                logDebug("SUCCESS: Project saved successfully");
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Project saved successfully!");
                alert.showAndWait();
            }
        } catch (IOException e) {
            logDebug("ERROR: Failed to save project - " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save project: " + e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            logDebug("ERROR: Unexpected error saving project - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void exportCode() {
        try {
            logDebug("ACTION: Exporting code");
            CodeGenerator generator = new CodeGenerator(project);
            String export = generator.generateExport();
            logDebug("Code generated, length: " + export.length() + " characters");
            
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
            logDebug("SUCCESS: Export window opened");
        } catch (Exception e) {
            logDebug("ERROR: Failed to export code - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateProject() {
        try {
            logDebug("ACTION: Validating project");
            Validator.ValidationResult result = Validator.validateProject(project);
            logDebug("Validation complete - errors: " + result.errors.size() + ", warnings: " + result.warnings.size());
            
            StringBuilder message = new StringBuilder();
            
            if (!result.errors.isEmpty()) {
                message.append("ERRORS:\n");
                for (String error : result.errors) {
                    message.append("  - ").append(error).append("\n");
                    logDebug("  ERROR: " + error);
                }
                message.append("\n");
            }
            
            if (!result.warnings.isEmpty()) {
                message.append("WARNINGS:\n");
                for (String warning : result.warnings) {
                    message.append("  - ").append(warning).append("\n");
                    logDebug("  WARNING: " + warning);
                }
            }
            
            if (!result.hasIssues()) {
                message.append("No issues found. Project is valid!");
                logDebug("  SUCCESS: No issues found");
            }
            
            Alert.AlertType alertType = result.isValid ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
            Alert alert = new Alert(alertType, message.toString());
            alert.setTitle("Validation Results");
            alert.setHeaderText(result.isValid ? "Validation Passed" : "Validation Issues Found");
            alert.showAndWait();
        } catch (Exception e) {
            logDebug("ERROR: Failed to validate project - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void alignComponents(String alignment) {
        try {
            logDebug("ACTION: Aligning components - " + alignment);
            java.util.List<InterfaceComponent> selected = canvas.getSelectedComponents();
            if (selected.size() < 2) {
                logDebug("  WARNING: Less than 2 components selected");
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select at least 2 components to align (Ctrl+click)");
                alert.showAndWait();
                return;
            }
            logDebug("  Aligning " + selected.size() + " components");

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
            logDebug("SUCCESS: Components aligned");
        } catch (Exception e) {
            logDebug("ERROR: Failed to align components - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void distributeComponents(String direction) {
        try {
            logDebug("ACTION: Distributing components - " + direction);
            java.util.List<InterfaceComponent> selected = canvas.getSelectedComponents();
            if (selected.size() < 3) {
                logDebug("  WARNING: Less than 3 components selected");
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select at least 3 components to distribute (Ctrl+click)");
                alert.showAndWait();
                return;
            }
            logDebug("  Distributing " + selected.size() + " components");

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
            logDebug("SUCCESS: Components distributed");
        } catch (Exception e) {
            logDebug("ERROR: Failed to distribute components - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void exportZipPackage() {
        try {
            logDebug("ACTION: Exporting ZIP package");
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
                logDebug("Exporting to: " + outputFile.getAbsolutePath());
                String spriteSourcePath = (spriteDir != null) ? spriteDir.getAbsolutePath() : "";
                logDebug("Sprite source: " + (spriteSourcePath.isEmpty() ? "none" : spriteSourcePath));
                ZipExporter.exportToZip(project, spriteSourcePath, outputFile.getAbsolutePath());
                logDebug("SUCCESS: ZIP package exported");
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                    "ZIP package exported successfully!\n\n" +
                    "Package contains:\n" +
                    "- " + project.getName() + ".java (Interface method)\n" +
                    "- Implementation_Guide.txt (Setup instructions)\n" +
                    "- sprites/ (Sprite files if source directory provided)\n" +
                    "- EXPORT_SUMMARY.txt (Project information)");
                alert.showAndWait();
            }
        } catch (IOException e) {
            logDebug("ERROR: Failed to export ZIP - " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to export ZIP: " + e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            logDebug("ERROR: Unexpected error exporting ZIP - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadInterfaceFromJavaSource() {
        try {
            logDebug("ACTION: Loading interface from Java source");
            if (interfacesFilePath.isEmpty()) {
                ClientWorkspace workspace = ClientWorkspace.detect();
                if (workspace.getInterfacesJava() != null) {
                    interfacesFilePath = workspace.getInterfacesJava().getAbsolutePath();
                    for (File root : workspace.getSpriteRoots()) {
                        SpriteLoader.addSearchRoot(root);
                    }
                }
            }
            if (interfacesFilePath.isEmpty()) {
                setInterfacesPath();
            }
            if (interfacesFilePath.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Select Interfaces.java first (File > Load from Client or Advanced > Set Interfaces.java Path).");
                alert.showAndWait();
                return;
            }

            logDebug("Reading available methods from: " + interfacesFilePath);
            List<String> availableMethods = JavaInterfaceParser.getAvailableMethods(interfacesFilePath);
            logDebug("Found " + availableMethods.size() + " available methods");

            if (availableMethods.isEmpty()) {
                logDebug("  WARNING: No interface methods found");
                Alert alert = new Alert(Alert.AlertType.WARNING,
                    "No interface methods found in Interfaces.java");
                alert.showAndWait();
                return;
            }

            String initial = availableMethods.contains("bank") ? "bank" : availableMethods.get(0);
            ChoiceDialog<String> dialog = new ChoiceDialog<>(initial, availableMethods);
            dialog.setTitle("Load from Client");
            dialog.setHeaderText("Drag children, then File > Save to Client.\nOnly setBounds X/Y are written back.");
            dialog.setContentText("Interface method:");

            dialog.showAndWait().ifPresent(methodName -> {
                logDebug("Loading interface method: " + methodName);
                InterfaceProject loadedProject = JavaInterfaceParser.parseInterfaceMethod(interfacesFilePath, methodName);
                if (loadedProject != null) {
                    applyLoadedProject(loadedProject);
                    logDebug("Interface loaded: " + methodName + " id=" + project.getInterfaceId()
                        + " children=" + project.getComponents().size());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        methodName + "() loaded (" + project.getComponents().size() + " children).\n\n"
                            + "Drag or arrow-nudge widgets, then Save to Client (Ctrl+S).");
                    alert.showAndWait();
                } else {
                    logDebug("ERROR: Failed to parse interface method: " + methodName);
                    Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Failed to parse interface method: " + methodName);
                    alert.showAndWait();
                }
            });
        } catch (Exception e) {
            logDebug("ERROR: Failed to load interface from Java source - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveToClient() {
        try {
            if (!project.isClientLinked() || project.getInterfacesFilePath() == null
                || project.getInterfacesFilePath().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Load an interface from the client first (File > Load from Client).");
                alert.showAndWait();
                return;
            }
            JavaInterfaceWriter.saveToClient(project);
            for (InterfaceComponent component : project.getComponents()) {
                component.setOriginalX(component.getX());
                component.setOriginalY(component.getY());
                component.setOriginalWidth(component.getWidth());
                component.setOriginalHeight(component.getHeight());
                component.setOriginalScrollMax(component.getScrollMax());
            }
            codePreviewPanel.updatePreview();
            logDebug("Saved setBounds to " + project.getInterfacesFilePath());
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Wrote positions into " + project.getSourceMethodName() + "() in Interfaces.java.\n"
                    + "Recompile the client to see them in-game.");
            alert.showAndWait();
        } catch (Exception e) {
            logDebug("ERROR: Save to Client failed - " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Save to Client failed:\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    private void detectClientWorkspace() {
        ClientWorkspace workspace = ClientWorkspace.detect();
        if (workspace.getInterfacesJava() != null) {
            interfacesFilePath = workspace.getInterfacesJava().getAbsolutePath();
            logDebug("Client Interfaces.java: " + interfacesFilePath);
        } else {
            logDebug("Interfaces.java not auto-detected");
        }
        for (File root : workspace.getSpriteRoots()) {
            SpriteLoader.addSearchRoot(root);
            logDebug("Sprite root: " + root.getAbsolutePath());
            if (SpriteLoader.getSpriteRootDirectory().isEmpty()) {
                SpriteLoader.setSpriteRootDirectory(root.getAbsolutePath());
            }
        }
        if (!interfacesFilePath.isEmpty()) {
            InterfaceProject loaded = JavaInterfaceParser.parseInterfaceMethod(interfacesFilePath, "bank");
            if (loaded != null) {
                applyLoadedProject(loaded);
                logDebug("Auto-loaded bank() with " + loaded.getComponents().size() + " children");
            } else {
                logDebug("Could not parse bank()");
            }
        }
    }

    private void applyLoadedProject(InterfaceProject loaded) {
        loaded.setInterfacesFilePath(interfacesFilePath);
        loaded.setClientLinked(true);
        applySpriteSizes(loaded);
        project = loaded;
        componentListView.setProject(project);
        canvas.setProject(project);
        propertyPanel.setProject(project);
        codePreviewPanel.setProject(project);
        canvas.render();
        history.clear();
        if (primaryStage != null) {
            primaryStage.setTitle("RSPS Interface Maker — " + project.getName());
        }
    }

    private void undo() {
        if (!history.canUndo()) {
            return;
        }
        history.undo(project);
        canvas.render();
        propertyPanel.refresh();
        codePreviewPanel.updatePreview();
        logDebug("Undo");
    }

    private void redo() {
        if (!history.canRedo()) {
            return;
        }
        history.redo(project);
        canvas.render();
        propertyPanel.refresh();
        codePreviewPanel.updatePreview();
        logDebug("Redo");
    }

    private void updateZoomLabel() {
        if (zoomLabel != null) {
            zoomLabel.setText(Math.round(canvas.getZoom() * 100) + "%");
        }
    }

    private void applySpriteSizes(InterfaceProject loaded) {
        for (InterfaceComponent component : loaded.getComponents()) {
            if (!(component instanceof SpriteComponent)) {
                continue;
            }
            SpriteComponent sprite = (SpriteComponent) component;
            if (sprite.getSpritePath() == null || sprite.getSpritePath().isEmpty()) {
                continue;
            }
            Image image = SpriteLoader.loadSprite(sprite.getSpritePath(), sprite.getSpriteId());
            if (image != null && !image.isError()) {
                int width = (int) Math.round(image.getWidth());
                int height = (int) Math.round(image.getHeight());
                if (width > 0 && height > 0) {
                    component.setWidth(width);
                    component.setHeight(height);
                }
            }
        }
    }

    private void setInterfacesPath() {
        try {
            logDebug("ACTION: Setting Interfaces.java path");
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Interfaces.java File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Files", "*.java"));
            File file = fileChooser.showOpenDialog(primaryStage);
            
            if (file != null) {
                interfacesFilePath = file.getAbsolutePath();
                logDebug("Interfaces.java path set to: " + interfacesFilePath);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                    "Interfaces.java path set to:\n" + interfacesFilePath + "\n\n" +
                    "You can now load existing interfaces from this file.");
                alert.showAndWait();
            }
        } catch (Exception e) {
            logDebug("ERROR: Failed to set Interfaces.java path - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void suggestFreeIdRange() {
        try {
            logDebug("ACTION: Suggesting free ID range");
            if (cachePath.isEmpty()) {
                logDebug("  WARNING: Cache path not set");
                Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                    "Please set the cache path first using Advanced > Set Cache Path");
                alert.showAndWait();
                return;
            }

            // Calculate required range size
            int requiredSize = project.getComponents().size() + 2; // +2 for interface ID and buffer
            logDebug("Required range size: " + requiredSize + " IDs");
            
            int suggestedId = CacheReader.suggestFreeIdRange(cachePath, requiredSize);
            logDebug("Suggested starting ID: " + suggestedId);
            
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
                        logDebug("Applying ID range starting at: " + suggestedId);
                        project.setInterfaceId(suggestedId);
                        // Reassign component IDs
                        int currentId = suggestedId + 1;
                        for (InterfaceComponent comp : project.getComponents()) {
                            comp.setId(currentId++);
                        }
                        
                        canvas.render();
                        propertyPanel.refresh();
                        codePreviewPanel.updatePreview();
                        logDebug("SUCCESS: ID range applied");
                        
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION, 
                            "ID range applied successfully!");
                        successAlert.showAndWait();
                    }
                });
            } else {
                logDebug("  WARNING: Could not find free ID range");
                Alert alert = new Alert(Alert.AlertType.WARNING, 
                    "Could not find a free ID range in the cache.\n" +
                    "Please try a higher starting ID or check the cache path.");
                alert.showAndWait();
            }
        } catch (Exception e) {
            logDebug("ERROR: Failed to suggest free ID range - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setCachePath() {
        try {
            logDebug("ACTION: Setting cache path");
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Select Cache Directory");
            File cacheDir = dirChooser.showDialog(primaryStage);
            
            if (cacheDir != null) {
                cachePath = cacheDir.getAbsolutePath();
                logDebug("Cache path set to: " + cachePath);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                    "Cache path set to:\n" + cachePath);
                alert.showAndWait();
            }
        } catch (Exception e) {
            logDebug("ERROR: Failed to set cache path - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setSpriteRootDirectory() {
        try {
            logDebug("ACTION: Setting sprite root directory");
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Select Sprite Root Directory");
            File spriteDir = dirChooser.showDialog(primaryStage);
            
            if (spriteDir != null) {
                String spritePath = spriteDir.getAbsolutePath();
                logDebug("Sprite root directory set to: " + spritePath);
                SpriteLoader.setSpriteRootDirectory(spritePath);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                    "Sprite root directory set to:\n" + spritePath + "\n\n" +
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
                logDebug("SUCCESS: Sprite root directory set and libraries refreshed");
            }
        } catch (Exception e) {
            logDebug("ERROR: Failed to set sprite root directory - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setDisplayOffsets() {
        try {
            logDebug("ACTION: Setting display offsets");
            logDebug("Current offsets - X: " + project.getOffsetX() + ", Y: " + project.getOffsetY());
            TextInputDialog xDialog = new TextInputDialog(String.valueOf(project.getOffsetX()));
            xDialog.setTitle("Set Display Offsets");
            xDialog.setHeaderText("Set X and Y offsets for game display");
            xDialog.setContentText("X Offset (default: 12):");
            
            xDialog.showAndWait().ifPresent(xOffsetStr -> {
                try {
                    int xOffset = Integer.parseInt(xOffsetStr);
                    logDebug("X offset set to: " + xOffset);
                    
                    TextInputDialog yDialog = new TextInputDialog(String.valueOf(project.getOffsetY()));
                    yDialog.setTitle("Set Display Offsets");
                    yDialog.setHeaderText("Set Y offset for game display");
                    yDialog.setContentText("Y Offset (default: 14):");
                    
                    yDialog.showAndWait().ifPresent(yOffsetStr -> {
                        try {
                            int yOffset = Integer.parseInt(yOffsetStr);
                            logDebug("Y offset set to: " + yOffset);
                            project.setOffsetX(xOffset);
                            project.setOffsetY(yOffset);
                            logDebug("SUCCESS: Display offsets updated - X: " + xOffset + ", Y: " + yOffset);
                            
                            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                                "Display offsets set to:\n" +
                                "X: " + xOffset + "\n" +
                                "Y: " + yOffset + "\n\n" +
                                "These offsets will be applied to all child positioning\n" +
                                "in the generated code to match game display coordinates.");
                            alert.showAndWait();
                            
                            codePreviewPanel.updatePreview();
                        } catch (NumberFormatException e) {
                            logDebug("ERROR: Invalid Y offset value");
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Invalid Y offset value");
                            errorAlert.showAndWait();
                        }
                    });
                } catch (NumberFormatException e) {
                    logDebug("ERROR: Invalid X offset value");
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Invalid X offset value");
                    errorAlert.showAndWait();
                }
            });
        } catch (Exception e) {
            logDebug("ERROR: Failed to set display offsets - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logDebug(String message) {
        if (debugConsole != null) {
            String timestamp = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss").format(java.time.LocalDateTime.now());
            debugConsole.appendText("[" + timestamp + "] " + message + "\n");
        }
    }
}
