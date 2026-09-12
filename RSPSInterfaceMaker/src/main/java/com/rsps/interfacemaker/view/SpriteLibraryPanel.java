package com.rsps.interfacemaker.view;

import com.rsps.interfacemaker.model.InterfaceComponent;
import com.rsps.interfacemaker.model.SpriteComponent;
import com.rsps.interfacemaker.model.ButtonComponent;
import com.rsps.interfacemaker.util.SpriteLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SpriteLibraryPanel extends VBox {
    private InterfaceComponent selectedComponent;
    private Consumer<InterfaceComponent> onSpriteAssigned;
    private ScrollPane scrollPane;
    private GridPane spriteGrid;
    private List<SpriteInfo> sprites = new ArrayList<>();
    
    public SpriteLibraryPanel() {
        setSpacing(10);
        setStyle("-fx-padding: 10;");
        
        // Header
        Label label = new Label("Sprite Library");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        getChildren().add(label);
        
        // Scan button
        Button scanButton = new Button("🔄 Scan Sprites");
        scanButton.setOnAction(e -> scanSpriteDirectory());
        getChildren().add(scanButton);
        
        // Sprite grid
        spriteGrid = new GridPane();
        spriteGrid.setHgap(10);
        spriteGrid.setVgap(10);
        spriteGrid.setStyle("-fx-background-color: #1a1a1a;");
        
        scrollPane = new ScrollPane(spriteGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: #1a1a1a;");
        
        getChildren().add(scrollPane);
        
        // Scan automatically if sprite root is set
        if (!SpriteLoader.getSpriteRootDirectory().isEmpty()) {
            scanSpriteDirectory();
        } else {
            // Show message if no root directory set
            Label noRootLabel = new Label("Set Sprite Root Directory via:\nAdvanced > Set Sprite Root Directory");
            noRootLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
            noRootLabel.setWrapText(true);
            spriteGrid.add(noRootLabel, 0, 0);
        }
    }
    
    private void scanSpriteDirectory() {
        String rootDir = SpriteLoader.getSpriteRootDirectory();
        if (rootDir.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "Please set the Sprite Root Directory first using Advanced > Set Sprite Root Directory");
            alert.showAndWait();
            return;
        }
        
        sprites.clear();
        spriteGrid.getChildren().clear();
        
        File rootFile = new File(rootDir);
        if (!rootFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Sprite Root Directory does not exist");
            alert.showAndWait();
            return;
        }
        
        // Recursively scan for image files
        scanDirectoryRecursive(rootFile, rootDir);
        
        // Display thumbnails
        displayThumbnails();
        
        if (sprites.isEmpty()) {
            Label noSpritesLabel = new Label("No sprite files found in:\n" + rootDir);
            noSpritesLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
            noSpritesLabel.setWrapText(true);
            spriteGrid.add(noSpritesLabel, 0, 0);
        }
    }
    
    private void scanDirectoryRecursive(File directory, String rootDir) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectoryRecursive(file, rootDir);
            } else {
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".png") || fileName.endsWith(".gif") || fileName.endsWith(".jpg")) {
                    // Calculate relative path
                    String relativePath = file.getAbsolutePath().substring(rootDir.length());
                    relativePath = relativePath.replace("\\", "/").replaceFirst("^/", "");
                    // Remove file extension
                    int dotIndex = relativePath.lastIndexOf('.');
                    if (dotIndex > 0) {
                        relativePath = relativePath.substring(0, dotIndex);
                    }
                    
                    SpriteInfo spriteInfo = new SpriteInfo();
                    spriteInfo.file = file;
                    spriteInfo.relativePath = relativePath;
                    spriteInfo.image = new Image(file.toURI().toString());
                    sprites.add(spriteInfo);
                }
            }
        }
    }
    
    private void displayThumbnails() {
        spriteGrid.getChildren().clear();
        
        int columns = 4;
        int row = 0;
        int col = 0;
        
        for (SpriteInfo sprite : sprites) {
            VBox thumbnailBox = createThumbnail(sprite);
            spriteGrid.add(thumbnailBox, col, row);
            
            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }
    }
    
    private VBox createThumbnail(SpriteInfo sprite) {
        VBox box = new VBox(5);
        box.setStyle("-fx-background-color: #2a2a2a; -fx-padding: 5; -fx-cursor: hand;");
        box.setOnMouseClicked(e -> assignSprite(sprite));
        
        ImageView imageView = new ImageView(sprite.image);
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);
        
        Label pathLabel = new Label(sprite.relativePath);
        pathLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 10px; -fx-font-family: monospace;");
        pathLabel.setWrapText(true);
        pathLabel.setMaxWidth(90);
        
        box.getChildren().addAll(imageView, pathLabel);
        
        return box;
    }
    
    private void assignSprite(SpriteInfo sprite) {
        if (selectedComponent == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "Please select a component first to assign a sprite");
            alert.showAndWait();
            return;
        }
        
        // Assign sprite based on component type
        if (selectedComponent instanceof SpriteComponent) {
            SpriteComponent spriteComp = (SpriteComponent) selectedComponent;
            spriteComp.setSpritePath(sprite.relativePath);
        } else if (selectedComponent instanceof ButtonComponent) {
            ButtonComponent buttonComp = (ButtonComponent) selectedComponent;
            buttonComp.setNormalSpritePath(sprite.relativePath);
            buttonComp.setHoveredSpritePath(sprite.relativePath); // Use same for both
        }
        
        // Notify that component was modified
        if (onSpriteAssigned != null) {
            onSpriteAssigned.accept(selectedComponent);
        }
    }
    
    public void setSelectedComponent(InterfaceComponent component) {
        this.selectedComponent = component;
    }
    
    public void setOnSpriteAssigned(Consumer<InterfaceComponent> onSpriteAssigned) {
        this.onSpriteAssigned = onSpriteAssigned;
    }
    
    public void refresh() {
        if (!SpriteLoader.getSpriteRootDirectory().isEmpty()) {
            scanSpriteDirectory();
        }
    }
    
    private static class SpriteInfo {
        File file;
        String relativePath;
        Image image;
    }
}