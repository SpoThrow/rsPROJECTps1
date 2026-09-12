package com.rsps.interfacemaker.view;

import com.rsps.interfacemaker.model.InterfaceComponent;
import com.rsps.interfacemaker.model.InterfaceProject;
import com.rsps.interfacemaker.model.SpriteComponent;
import com.rsps.interfacemaker.model.ButtonComponent;
import com.rsps.interfacemaker.model.TextComponent;
import com.rsps.interfacemaker.model.ComponentType;
import com.rsps.interfacemaker.util.SpriteLoader;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.control.ColorPicker;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.util.function.Consumer;
import java.io.File;

public class PropertyPanel extends VBox {
    private InterfaceProject project;
    private InterfaceComponent component;
    private Consumer<InterfaceComponent> onComponentModified;
    private javafx.stage.Stage stage;

    public PropertyPanel(InterfaceProject project) {
        this(project, null);
    }

    public PropertyPanel(InterfaceProject project, javafx.stage.Stage stage) {
        this.project = project;
        this.stage = stage;
        setSpacing(10);
        setStyle("-fx-padding: 10;");
        Label label = new Label("Properties");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        getChildren().add(label);
    }

    public void setComponent(InterfaceComponent component) {
        this.component = component;
        getChildren().clear();
        
        Label label = new Label("Properties");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        getChildren().add(label);

        if (component == null) {
            getChildren().add(new Label("No component selected"));
            return;
        }

        // Common properties
        addCommonProperties();

        // Type-specific properties
        switch (component.getType()) {
            case SPRITE:
                addSpriteProperties();
                break;
            case HOVER_BUTTON:
            case HOVERED_BUTTON:
            case CLOSE_BUTTON:
                addButtonProperties();
                break;
            case TEXT:
                addTextProperties();
                break;
            case TOOLTIP:
                // Tooltips use common properties only
                break;
        }
    }

    private void addCommonProperties() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        // Name
        grid.add(new Label("Name:"), 0, 0);
        TextField nameField = new TextField(component.getName());
        nameField.textProperty().addListener((obs, old, newVal) -> {
            component.setName(newVal);
            notifyModified();
        });
        grid.add(nameField, 1, 0);

        // ID
        grid.add(new Label("ID:"), 0, 1);
        TextField idField = new TextField(String.valueOf(component.getId()));
        idField.setEditable(false);
        grid.add(idField, 1, 1);

        // X
        grid.add(new Label("X:"), 0, 2);
        TextField xField = new TextField(String.valueOf(component.getX()));
        xField.textProperty().addListener((obs, old, newVal) -> {
            try {
                component.setX(Integer.parseInt(newVal));
                notifyModified();
            } catch (NumberFormatException e) {}
        });
        grid.add(xField, 1, 2);

        // Y
        grid.add(new Label("Y:"), 0, 3);
        TextField yField = new TextField(String.valueOf(component.getY()));
        yField.textProperty().addListener((obs, old, newVal) -> {
            try {
                component.setY(Integer.parseInt(newVal));
                notifyModified();
            } catch (NumberFormatException e) {}
        });
        grid.add(yField, 1, 3);

        // Width
        grid.add(new Label("Width:"), 0, 4);
        TextField widthField = new TextField(String.valueOf(component.getWidth()));
        widthField.textProperty().addListener((obs, old, newVal) -> {
            try {
                component.setWidth(Integer.parseInt(newVal));
                notifyModified();
            } catch (NumberFormatException e) {}
        });
        grid.add(widthField, 1, 4);

        // Height
        grid.add(new Label("Height:"), 0, 5);
        TextField heightField = new TextField(String.valueOf(component.getHeight()));
        heightField.textProperty().addListener((obs, old, newVal) -> {
            try {
                component.setHeight(Integer.parseInt(newVal));
                notifyModified();
            } catch (NumberFormatException e) {}
        });
        grid.add(heightField, 1, 5);

        // Tooltip
        grid.add(new Label("Tooltip:"), 0, 6);
        TextField tooltipField = new TextField(component.getTooltip());
        tooltipField.textProperty().addListener((obs, old, newVal) -> {
            component.setTooltip(newVal);
            notifyModified();
        });
        grid.add(tooltipField, 1, 6);

        getChildren().add(grid);
    }

    private void addSpriteProperties() {
        if (!(component instanceof SpriteComponent)) return;
        SpriteComponent sprite = (SpriteComponent) component;

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        grid.add(new Label("Sprite Path:"), 0, 0);
        HBox spritePathBox = new HBox(5);
        TextField spritePathField = new TextField(sprite.getSpritePath());
        spritePathField.textProperty().addListener((obs, old, newVal) -> {
            sprite.setSpritePath(newVal);
            notifyModified();
        });
        
        Button browseButton = new Button("Browse...");
        browseButton.setOnAction(e -> browseSpriteFile(spritePathField));
        
        spritePathBox.getChildren().addAll(spritePathField, browseButton);
        grid.add(spritePathBox, 1, 0);

        // Add sprite thumbnail preview
        if (!sprite.getSpritePath().isEmpty()) {
            javafx.scene.image.Image image = SpriteLoader.loadSprite(sprite.getSpritePath());
            if (image != null) {
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(100);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(true);
                grid.add(new Label("Preview:"), 0, 1);
                grid.add(imageView, 1, 1);
            }
        }

        grid.add(new Label("Has Disabled:"), 0, 2);
        CheckBox hasDisabledCheck = new CheckBox();
        hasDisabledCheck.setSelected(sprite.isHasDisabledSprite());
        hasDisabledCheck.selectedProperty().addListener((obs, old, newVal) -> {
            sprite.setHasDisabledSprite(newVal);
            notifyModified();
        });
        grid.add(hasDisabledCheck, 1, 2);

        grid.add(new Label("Disabled Path:"), 0, 3);
        TextField disabledPathField = new TextField(sprite.getDisabledSpritePath());
        disabledPathField.textProperty().addListener((obs, old, newVal) -> {
            sprite.setDisabledSpritePath(newVal);
            notifyModified();
        });
        grid.add(disabledPathField, 1, 3);

        getChildren().add(new Separator());
        getChildren().add(new Label("Sprite Properties"));
        getChildren().add(grid);
    }

    private void addButtonProperties() {
        if (!(component instanceof ButtonComponent)) return;
        ButtonComponent button = (ButtonComponent) component;

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        grid.add(new Label("Normal Sprite:"), 0, 0);
        HBox normalBox = new HBox(5);
        TextField normalField = new TextField(button.getNormalSpritePath());
        normalField.textProperty().addListener((obs, old, newVal) -> {
            button.setNormalSpritePath(newVal);
            notifyModified();
        });
        
        Button browseNormalButton = new Button("Browse...");
        browseNormalButton.setOnAction(e -> browseSpriteFile(normalField));
        
        normalBox.getChildren().addAll(normalField, browseNormalButton);
        grid.add(normalBox, 1, 0);

        grid.add(new Label("Hovered Sprite:"), 0, 1);
        HBox hoveredBox = new HBox(5);
        TextField hoveredField = new TextField(button.getHoveredSpritePath());
        hoveredField.textProperty().addListener((obs, old, newVal) -> {
            button.setHoveredSpritePath(newVal);
            notifyModified();
        });
        
        Button browseHoveredButton = new Button("Browse...");
        browseHoveredButton.setOnAction(e -> browseSpriteFile(hoveredField));
        
        hoveredBox.getChildren().addAll(hoveredField, browseHoveredButton);
        grid.add(hoveredBox, 1, 1);

        // Add sprite thumbnail preview
        if (!button.getNormalSpritePath().isEmpty()) {
            javafx.scene.image.Image image = SpriteLoader.loadSprite(button.getNormalSpritePath());
            if (image != null) {
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(100);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(true);
                grid.add(new Label("Preview:"), 0, 2);
                grid.add(imageView, 1, 2);
            }
        }

        grid.add(new Label("Action Name:"), 0, 3);
        TextField actionField = new TextField(button.getActionName());
        actionField.textProperty().addListener((obs, old, newVal) -> {
            button.setActionName(newVal);
            notifyModified();
        });
        grid.add(actionField, 1, 3);

        grid.add(new Label("Action ID:"), 0, 4);
        TextField actionIdField = new TextField(String.valueOf(button.getActionId()));
        actionIdField.textProperty().addListener((obs, old, newVal) -> {
            try {
                button.setActionId(Integer.parseInt(newVal));
                notifyModified();
            } catch (NumberFormatException e) {}
        });
        grid.add(actionIdField, 1, 4);

        getChildren().add(new Separator());
        getChildren().add(new Label("Button Properties"));
        getChildren().add(grid);
    }

    private void addTextProperties() {
        if (!(component instanceof TextComponent)) return;
        TextComponent text = (TextComponent) component;

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        grid.add(new Label("Text:"), 0, 0);
        TextField textField = new TextField(text.getText());
        textField.textProperty().addListener((obs, old, newVal) -> {
            text.setText(newVal);
            notifyModified();
        });
        grid.add(textField, 1, 0);

        grid.add(new Label("Font Index:"), 0, 1);
        TextField fontField = new TextField(String.valueOf(text.getFontIndex()));
        fontField.textProperty().addListener((obs, old, newVal) -> {
            try {
                text.setFontIndex(Integer.parseInt(newVal));
                notifyModified();
            } catch (NumberFormatException e) {}
        });
        grid.add(fontField, 1, 1);

        grid.add(new Label("Text Color:"), 0, 2);
        ColorPicker colorPicker = new ColorPicker(Color.rgb(
            (text.getTextColor() >> 16) & 0xFF,
            (text.getTextColor() >> 8) & 0xFF,
            text.getTextColor() & 0xFF
        ));
        colorPicker.setOnAction(e -> {
            Color color = colorPicker.getValue();
            int rgb = ((int)(color.getRed() * 255) << 16) |
                      ((int)(color.getGreen() * 255) << 8) |
                      (int)(color.getBlue() * 255);
            text.setTextColor(rgb);
            notifyModified();
        });
        grid.add(colorPicker, 1, 2);

        grid.add(new Label("Has Shadow:"), 0, 3);
        CheckBox shadowCheck = new CheckBox();
        shadowCheck.setSelected(text.isHasShadow());
        shadowCheck.selectedProperty().addListener((obs, old, newVal) -> {
            text.setHasShadow(newVal);
            notifyModified();
        });
        grid.add(shadowCheck, 1, 3);

        grid.add(new Label("Centered:"), 0, 4);
        CheckBox centeredCheck = new CheckBox();
        centeredCheck.setSelected(text.isCentered());
        centeredCheck.selectedProperty().addListener((obs, old, newVal) -> {
            text.setCentered(newVal);
            notifyModified();
        });
        grid.add(centeredCheck, 1, 4);

        getChildren().add(new Separator());
        getChildren().add(new Label("Text Properties"));
        getChildren().add(grid);
    }

    private void notifyModified() {
        if (onComponentModified != null) {
            onComponentModified.accept(component);
        }
    }

    public void setProject(InterfaceProject project) {
        this.project = project;
        // Clear component selection when project changes
        this.component = null;
        getChildren().clear();
        Label label = new Label("Properties");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        getChildren().add(label);
        getChildren().add(new Label("No component selected"));
    }

    public void setStage(javafx.stage.Stage stage) {
        this.stage = stage;
    }

    public void setOnComponentModified(Consumer<InterfaceComponent> onComponentModified) {
        this.onComponentModified = onComponentModified;
    }

    public void refresh() {
        if (component != null) {
            setComponent(component);
        }
    }

    private void browseSpriteFile(TextField textField) {
        if (stage == null) {
            System.err.println("Stage is null, cannot show file chooser");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Sprite File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.PNG", "*.gif", "*.GIF", "*.jpg", "*.JPG")
        );
        
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            String rootDir = SpriteLoader.getSpriteRootDirectory();
            String relativePath;
            
            if (!rootDir.isEmpty() && selectedFile.getAbsolutePath().startsWith(rootDir)) {
                // Calculate relative path from root directory
                relativePath = selectedFile.getAbsolutePath().substring(rootDir.length());
                // Convert file separators to forward slashes and remove leading separator
                relativePath = relativePath.replace(File.separator, "/").replaceFirst("^/", "");
                // Remove file extension
                int dotIndex = relativePath.lastIndexOf('.');
                if (dotIndex > 0) {
                    relativePath = relativePath.substring(0, dotIndex);
                }
            } else {
                // Use just the filename without extension
                String fileName = selectedFile.getName();
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    fileName = fileName.substring(0, dotIndex);
                }
                relativePath = "Interfaces/" + fileName;
            }
            
            textField.setText(relativePath);
            notifyModified();
        }
    }
}
