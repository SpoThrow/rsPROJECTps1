package com.rsps.interfacemaker.view;

import com.rsps.interfacemaker.model.InterfaceComponent;
import com.rsps.interfacemaker.model.InterfaceProject;
import com.rsps.interfacemaker.model.ComponentType;
import com.rsps.interfacemaker.model.SpriteComponent;
import com.rsps.interfacemaker.model.ButtonComponent;
import com.rsps.interfacemaker.model.TextComponent;
import com.rsps.interfacemaker.util.SpriteLoader;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class InterfaceCanvas extends Canvas {
    private InterfaceProject project;
    private InterfaceComponent selectedComponent;
    private List<InterfaceComponent> selectedComponents = new ArrayList<>();
    private InterfaceComponent draggingComponent;
    private double dragOffsetX;
    private double dragOffsetY;
    private Consumer<InterfaceComponent> onComponentSelected;
    private Consumer<List<InterfaceComponent>> onComponentsDuplicated;
    private Runnable onComponentsChanged;
    
    private List<InterfaceComponent> clipboard = new ArrayList<>();
    private ContextMenu contextMenu;
    private Map<InterfaceComponent, double[]> dragOffsets = new HashMap<>();
    private boolean showGrid = true;

    private static final int CANVAS_WIDTH = 512;
    private static final int CANVAS_HEIGHT = 334;

    public InterfaceCanvas(InterfaceProject project) {
        super(CANVAS_WIDTH, CANVAS_HEIGHT);
        this.project = project;
        initializeEvents();
        render();
    }

    private void initializeEvents() {
        setOnMousePressed(this::onMousePressed);
        setOnMouseDragged(this::onMouseDragged);
        setOnMouseReleased(this::onMouseReleased);
        setOnKeyPressed(this::onKeyPressed);
        setFocusTraversable(true);
        
        // Create context menu
        contextMenu = new ContextMenu();
        MenuItem duplicateItem = new MenuItem("Duplicate");
        duplicateItem.setOnAction(e -> duplicateSelectedComponents());
        contextMenu.getItems().add(duplicateItem);
        
        // Show context menu on right-click
        setOnContextMenuRequested(e -> {
            if (!selectedComponents.isEmpty()) {
                contextMenu.show(this, e.getScreenX(), e.getScreenY());
            }
            e.consume();
        });
    }

    private void onMousePressed(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        boolean isCtrlDown = e.isControlDown();

        List<InterfaceComponent> ordered = drawOrdered();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            InterfaceComponent comp = ordered.get(i);
            if (x >= comp.getX() && x <= comp.getX() + comp.getWidth() &&
                y >= comp.getY() && y <= comp.getY() + comp.getHeight()) {
                
                if (isCtrlDown) {
                    // Multi-select: toggle selection
                    if (selectedComponents.contains(comp)) {
                        selectedComponents.remove(comp);
                    } else {
                        selectedComponents.add(comp);
                    }
                    selectedComponent = comp; // Last clicked becomes primary selection
                } else {
                    selectedComponents.clear();
                    selectedComponents.add(comp);
                    selectedComponent = comp;
                    draggingComponent = comp;
                    dragOffsetX = x - comp.getX();
                    dragOffsetY = y - comp.getY();
                    for (InterfaceComponent other : project.getComponents()) {
                        if (other != comp && other.getX() == comp.getX() && other.getY() == comp.getY()) {
                            selectedComponents.add(other);
                        }
                    }
                    dragOffsets.clear();
                    for (InterfaceComponent selectedComp : selectedComponents) {
                        dragOffsets.put(selectedComp, new double[]{
                            x - selectedComp.getX(),
                            y - selectedComp.getY()
                        });
                    }
                }
                
                if (onComponentSelected != null) {
                    onComponentSelected.accept(comp);
                }
                render();
                requestFocus();
                return;
            }
        }

        // Clicked on empty space
        if (!isCtrlDown) {
            selectedComponents.clear();
            selectedComponent = null;
            if (onComponentSelected != null) {
                onComponentSelected.accept(null);
            }
            render();
        }
    }

    private void onMouseDragged(MouseEvent e) {
        if (draggingComponent != null) {
            // Move all selected components maintaining their relative positions
            for (InterfaceComponent comp : selectedComponents) {
                double[] offsets = dragOffsets.get(comp);
                if (offsets != null) {
                    comp.setX((int)(e.getX() - offsets[0]));
                    comp.setY((int)(e.getY() - offsets[1]));
                }
            }
            
            render();
            if (onComponentsChanged != null) {
                onComponentsChanged.run();
            }
        }
    }

    private void onMouseReleased(MouseEvent e) {
        draggingComponent = null;
    }

    private void onKeyPressed(KeyEvent e) {
        if (!selectedComponents.isEmpty()) {
            // Check for copy/paste/duplicate shortcuts
            if (e.isControlDown()) {
                if (e.getCode() == KeyCode.C) {
                    copySelectedComponents();
                    e.consume();
                    return;
                } else if (e.getCode() == KeyCode.V) {
                    pasteComponents();
                    e.consume();
                    return;
                } else if (e.getCode() == KeyCode.D) {
                    duplicateSelectedComponents();
                    e.consume();
                    return;
                }
            }
            
            int delta = e.isShiftDown() ? 10 : 1;
            switch (e.getCode()) {
                case UP:
                    for (InterfaceComponent comp : selectedComponents) {
                        comp.setY(comp.getY() - delta);
                    }
                    render();
                    if (onComponentsChanged != null) {
                        onComponentsChanged.run();
                    }
                    break;
                case DOWN:
                    for (InterfaceComponent comp : selectedComponents) {
                        comp.setY(comp.getY() + delta);
                    }
                    render();
                    if (onComponentsChanged != null) {
                        onComponentsChanged.run();
                    }
                    break;
                case LEFT:
                    for (InterfaceComponent comp : selectedComponents) {
                        comp.setX(comp.getX() - delta);
                    }
                    render();
                    if (onComponentsChanged != null) {
                        onComponentsChanged.run();
                    }
                    break;
                case RIGHT:
                    for (InterfaceComponent comp : selectedComponents) {
                        comp.setX(comp.getX() + delta);
                    }
                    render();
                    if (onComponentsChanged != null) {
                        onComponentsChanged.run();
                    }
                    break;
                case DELETE:
                    for (InterfaceComponent comp : selectedComponents) {
                        project.removeComponent(comp);
                    }
                    selectedComponents.clear();
                    selectedComponent = null;
                    if (onComponentSelected != null) {
                        onComponentSelected.accept(null);
                    }
                    render();
                    if (onComponentsChanged != null) {
                        onComponentsChanged.run();
                    }
                    break;
            }
        }
    }

    public void render() {
        GraphicsContext gc = getGraphicsContext2D();
        
        // Clear canvas with dark background
        gc.setFill(Color.rgb(30, 30, 30));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Draw grid (if enabled)
        if (showGrid) {
            gc.setStroke(Color.rgb(50, 50, 50));
            gc.setLineWidth(0.5);
            for (int x = 0; x <= CANVAS_WIDTH; x += 32) {
                gc.strokeLine(x, 0, x, CANVAS_HEIGHT);
            }
            for (int y = 0; y <= CANVAS_HEIGHT; y += 32) {
                gc.strokeLine(0, y, CANVAS_WIDTH, y);
            }
        }

        // Draw components
        for (InterfaceComponent comp : drawOrdered()) {
            drawComponent(gc, comp);
        }

        // Draw selection outlines
        for (InterfaceComponent comp : selectedComponents) {
            // Draw outer glow effect
            gc.setStroke(Color.rgb(255, 255, 0, 0.3));
            gc.setLineWidth(4);
            gc.strokeRect(
                comp.getX() - 2,
                comp.getY() - 2,
                comp.getWidth() + 4,
                comp.getHeight() + 4
            );
            
            // Draw main selection outline
            gc.setStroke(Color.rgb(255, 255, 0));
            gc.setLineWidth(2);
            gc.strokeRect(
                comp.getX() - 1,
                comp.getY() - 1,
                comp.getWidth() + 2,
                comp.getHeight() + 2
            );
            
            // Draw corner handles for visual clarity
            gc.setFill(Color.rgb(255, 255, 0));
            int handleSize = 4;
            gc.fillRect(comp.getX() - handleSize, comp.getY() - handleSize, handleSize, handleSize);
            gc.fillRect(comp.getX() + comp.getWidth() - handleSize, comp.getY() - handleSize, handleSize, handleSize);
            gc.fillRect(comp.getX() - handleSize, comp.getY() + comp.getHeight() - handleSize, handleSize, handleSize);
            gc.fillRect(comp.getX() + comp.getWidth() - handleSize, comp.getY() + comp.getHeight() - handleSize, handleSize, handleSize);
        }
    }

    private void drawComponent(GraphicsContext gc, InterfaceComponent comp) {
        switch (comp.getType()) {
            case SPRITE:
                drawSprite(gc, comp);
                break;
            case HOVER_BUTTON:
            case HOVERED_BUTTON:
                drawButton(gc, comp);
                break;
            case TEXT:
                drawText(gc, comp);
                break;
            case CLOSE_BUTTON:
                drawCloseButton(gc, comp);
                break;
            case TOOLTIP:
                drawTooltip(gc, comp);
                break;
            case CONTAINER:
                drawContainer(gc, comp);
                break;
            case ITEM_SLOT:
                drawItemSlot(gc, comp);
                break;
        }
    }

    private List<InterfaceComponent> drawOrdered() {
        List<InterfaceComponent> ordered = new ArrayList<>(project.getComponents());
        ordered.sort(Comparator.comparingInt(InterfaceComponent::getChildIndex));
        return ordered;
    }

    private void drawSprite(GraphicsContext gc, InterfaceComponent comp) {
        // Try to load and render the actual sprite
        if (comp instanceof SpriteComponent) {
            SpriteComponent sprite = (SpriteComponent) comp;
            String spritePath = sprite.getSpritePath();
            int spriteId = sprite.getSpriteId();
            
            if (spritePath != null && !spritePath.isEmpty()) {
                Image image = SpriteLoader.loadSprite(spritePath, spriteId);
                
                if (image != null) {
                    gc.drawImage(image, comp.getX(), comp.getY());
                    return;
                }
            }
            
            // Fallback to colored rectangle if sprite not found
            gc.setFill(Color.rgb(100, 100, 150));
            gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            gc.setStroke(Color.rgb(150, 150, 200));
            gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
        } else {
            // Fallback for non-SpriteComponent
            gc.setFill(Color.rgb(100, 100, 150));
            gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            gc.setStroke(Color.rgb(150, 150, 200));
            gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
        }
    }

    private void drawButton(GraphicsContext gc, InterfaceComponent comp) {
        // Try to load and render the actual button sprite
        if (comp instanceof ButtonComponent) {
            ButtonComponent button = (ButtonComponent) comp;
            String spritePath = button.getNormalSpritePath();
            
            if (spritePath != null && !spritePath.isEmpty()) {
                Image image = SpriteLoader.loadSprite(spritePath, button.getNormalSpriteId());
                
                if (image != null) {
                    gc.drawImage(image, comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
                    return;
                }
            }
            
            // Fallback to colored rectangle if sprite not found
            gc.setFill(Color.rgb(100, 150, 100));
            gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            gc.setStroke(Color.rgb(150, 200, 150));
            gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
        } else {
            // Fallback for non-ButtonComponent
            gc.setFill(Color.rgb(100, 150, 100));
            gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            gc.setStroke(Color.rgb(150, 200, 150));
            gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
        }
    }

    private void drawText(GraphicsContext gc, InterfaceComponent comp) {
        String message = comp.getName();
        int color = 0xFFFFFF;
        if (comp instanceof TextComponent) {
            TextComponent text = (TextComponent) comp;
            message = text.getText();
            color = text.getTextColor();
        }
        gc.setFill(Color.rgb((color >> 16) & 255, (color >> 8) & 255, color & 255));
        gc.fillText(message == null ? "" : message, comp.getX(), comp.getY() + Math.max(11, comp.getHeight() - 2));
    }

    private void drawCloseButton(GraphicsContext gc, InterfaceComponent comp) {
        // Try to load and render the actual close button sprite
        if (comp instanceof ButtonComponent) {
            ButtonComponent button = (ButtonComponent) comp;
            String spritePath = button.getNormalSpritePath();
            
            if (spritePath != null && !spritePath.isEmpty()) {
                Image image = SpriteLoader.loadSprite(spritePath, button.getNormalSpriteId());
                
                if (image != null) {
                    gc.drawImage(image, comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
                    return;
                }
            }
            
            // Fallback to colored rectangle with X if sprite not found
            gc.setFill(Color.rgb(150, 50, 50));
            gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            gc.setStroke(Color.rgb(200, 100, 100));
            gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            // Draw X
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeLine(comp.getX() + 5, comp.getY() + 5, comp.getX() + comp.getWidth() - 5, comp.getY() + comp.getHeight() - 5);
            gc.strokeLine(comp.getX() + comp.getWidth() - 5, comp.getY() + 5, comp.getX() + 5, comp.getY() + comp.getHeight() - 5);
        } else {
            // Fallback for non-ButtonComponent
            gc.setFill(Color.rgb(150, 50, 50));
            gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            gc.setStroke(Color.rgb(200, 100, 100));
            gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            // Draw X
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeLine(comp.getX() + 5, comp.getY() + 5, comp.getX() + comp.getWidth() - 5, comp.getY() + comp.getHeight() - 5);
            gc.strokeLine(comp.getX() + comp.getWidth() - 5, comp.getY() + 5, comp.getX() + 5, comp.getY() + comp.getHeight() - 5);
        }
    }

    private void drawTooltip(GraphicsContext gc, InterfaceComponent comp) {
        gc.setFill(Color.rgb(255, 255, 200));
        gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
        gc.setStroke(Color.rgb(200, 200, 150));
        gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
    }

    private void drawContainer(GraphicsContext gc, InterfaceComponent comp) {
        gc.setFill(Color.rgb(80, 140, 180, 0.18));
        gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
        gc.setStroke(Color.rgb(120, 180, 220, 0.9));
        gc.setLineWidth(1);
        gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
        gc.setFill(Color.rgb(180, 220, 255));
        gc.fillText(comp.getName() + " [" + comp.getId() + "]", comp.getX() + 4, comp.getY() + 12);
    }

    private void drawItemSlot(GraphicsContext gc, InterfaceComponent comp) {
        gc.setFill(Color.rgb(90, 70, 40, 0.45));
        gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
        gc.setStroke(Color.rgb(180, 150, 80));
        gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
    }

    public void setProject(InterfaceProject project) {
        this.project = project;
        this.selectedComponent = null;
        render();
    }

    public void setOnComponentSelected(Consumer<InterfaceComponent> onComponentSelected) {
        this.onComponentSelected = onComponentSelected;
    }

    public InterfaceComponent getSelectedComponent() {
        return selectedComponent;
    }

    public java.util.List<InterfaceComponent> getSelectedComponents() {
        return selectedComponents;
    }

    public void selectComponent(InterfaceComponent component) {
        if (component != null && project.getComponents().contains(component)) {
            selectedComponents.clear();
            selectedComponents.add(component);
            selectedComponent = component;
            if (onComponentSelected != null) {
                onComponentSelected.accept(component);
            }
            render();
        }
    }
    
    private void copySelectedComponents() {
        clipboard.clear();
        for (InterfaceComponent comp : selectedComponents) {
            clipboard.add(deepCopyComponent(comp));
        }
    }
    
    private void pasteComponents() {
        if (clipboard.isEmpty()) {
            return;
        }
        
        selectedComponents.clear();
        int nextId = findNextAvailableId();
        
        for (InterfaceComponent comp : clipboard) {
            InterfaceComponent newComp = deepCopyComponent(comp);
            newComp.setId(nextId++);
            newComp.setX(newComp.getX() + 20); // Offset by 20 pixels
            newComp.setY(newComp.getY() + 20);
            newComp.setName(newComp.getName() + " (Copy)");
            project.addComponent(newComp);
            selectedComponents.add(newComp);
        }
        
        if (!selectedComponents.isEmpty()) {
            selectedComponent = selectedComponents.get(0);
            if (onComponentSelected != null) {
                onComponentSelected.accept(selectedComponent);
            }
        }
        
        if (onComponentsDuplicated != null) {
            onComponentsDuplicated.accept(selectedComponents);
        }
        
        render();
    }
    
    private void duplicateSelectedComponents() {
        if (selectedComponents.isEmpty()) {
            return;
        }
        
        List<InterfaceComponent> newComponents = new ArrayList<>();
        int nextId = findNextAvailableId();
        
        for (InterfaceComponent comp : selectedComponents) {
            InterfaceComponent newComp = deepCopyComponent(comp);
            newComp.setId(nextId++);
            newComp.setX(newComp.getX() + 20); // Offset by 20 pixels
            newComp.setY(newComp.getY() + 20);
            newComp.setName(newComp.getName() + " (Copy)");
            project.addComponent(newComp);
            newComponents.add(newComp);
        }
        
        // Select the newly duplicated components
        selectedComponents.clear();
        selectedComponents.addAll(newComponents);
        if (!newComponents.isEmpty()) {
            selectedComponent = newComponents.get(0);
            if (onComponentSelected != null) {
                onComponentSelected.accept(selectedComponent);
            }
        }
        
        if (onComponentsDuplicated != null) {
            onComponentsDuplicated.accept(newComponents);
        }
        
        render();
    }
    
    private InterfaceComponent deepCopyComponent(InterfaceComponent original) {
        InterfaceComponent copy;
        
        switch (original.getType()) {
            case SPRITE:
                SpriteComponent spriteCopy = new SpriteComponent();
                SpriteComponent originalSprite = (SpriteComponent) original;
                spriteCopy.setSpritePath(originalSprite.getSpritePath());
                copy = spriteCopy;
                break;
            case HOVER_BUTTON:
            case HOVERED_BUTTON:
            case CLOSE_BUTTON:
                ButtonComponent buttonCopy = new ButtonComponent();
                ButtonComponent originalButton = (ButtonComponent) original;
                buttonCopy.setNormalSpritePath(originalButton.getNormalSpritePath());
                buttonCopy.setHoveredSpritePath(originalButton.getHoveredSpritePath());
                buttonCopy.setTooltip(originalButton.getTooltip());
                buttonCopy.setActionName(originalButton.getActionName());
                buttonCopy.setActionId(originalButton.getActionId());
                copy = buttonCopy;
                break;
            case TEXT:
                TextComponent textCopy = new TextComponent();
                TextComponent originalText = (TextComponent) original;
                textCopy.setText(originalText.getText());
                textCopy.setFontIndex(originalText.getFontIndex());
                textCopy.setTextColor(originalText.getTextColor());
                textCopy.setHasShadow(originalText.isHasShadow());
                textCopy.setCentered(originalText.isCentered());
                copy = textCopy;
                break;
            case TOOLTIP:
                copy = new InterfaceComponent(ComponentType.TOOLTIP);
                break;
            default:
                copy = new InterfaceComponent(original.getType());
                break;
        }
        
        // Copy common properties
        copy.setName(original.getName());
        copy.setId(original.getId());
        copy.setX(original.getX());
        copy.setY(original.getY());
        copy.setWidth(original.getWidth());
        copy.setHeight(original.getHeight());
        
        return copy;
    }
    
    private int findNextAvailableId() {
        int maxId = 0;
        for (InterfaceComponent comp : project.getComponents()) {
            if (comp.getId() > maxId) {
                maxId = comp.getId();
            }
        }
        return maxId + 1;
    }
    
    public void setOnComponentsDuplicated(Consumer<List<InterfaceComponent>> onComponentsDuplicated) {
        this.onComponentsDuplicated = onComponentsDuplicated;
    }
    
    public void setOnComponentsChanged(Runnable onComponentsChanged) {
        this.onComponentsChanged = onComponentsChanged;
    }
    
    public void toggleGrid() {
        showGrid = !showGrid;
        render();
    }
    
    public boolean isGridVisible() {
        return showGrid;
    }
}
