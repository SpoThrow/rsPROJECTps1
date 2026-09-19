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
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.Cursor;
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
    private double zoom = 1.0;
    private boolean resizing;
    private Runnable onEditStarted;
    private Runnable onZoomChanged;
    private boolean editStarted;

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
        setOnMouseMoved(this::onMouseMoved);
        setOnScroll(this::onScroll);
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

    private int ax(InterfaceComponent component) {
        return project.absX(component);
    }

    private int ay(InterfaceComponent component) {
        return project.absY(component);
    }

    private double lx(MouseEvent e) {
        return e.getX() / zoom;
    }

    private double ly(MouseEvent e) {
        return e.getY() / zoom;
    }

    private void setAbs(InterfaceComponent component, int absX, int absY) {
        InterfaceComponent parent = project.parentOf(component);
        if (parent == null) {
            component.setX(absX);
            component.setY(absY);
        } else {
            component.setX(absX - project.absX(parent));
            component.setY(absY - project.absY(parent) + parent.getPreviewScroll());
        }
    }

    private boolean canResize(InterfaceComponent component) {
        return component.getType() == ComponentType.CONTAINER
            || component.getType() == ComponentType.RECTANGLE
            || component.getScrollMax() > 0;
    }

    private boolean overResizeHandle(InterfaceComponent component, double x, double y) {
        double handle = Math.max(6, 8 / zoom);
        int right = ax(component) + component.getWidth();
        int bottom = ay(component) + component.getHeight();
        return x >= right - handle && x <= right + handle && y >= bottom - handle && y <= bottom + handle;
    }

    private void beginEdit() {
        if (!editStarted && onEditStarted != null) {
            onEditStarted.run();
            editStarted = true;
        }
    }

    private void onMouseMoved(MouseEvent e) {
        if (selectedComponent != null && canResize(selectedComponent) && overResizeHandle(selectedComponent, lx(e), ly(e))) {
            setCursor(Cursor.SE_RESIZE);
        } else {
            setCursor(Cursor.DEFAULT);
        }
    }

    private void onScroll(ScrollEvent e) {
        if (e.isControlDown()) {
            double factor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            setZoom(zoom * factor);
            e.consume();
            return;
        }
        double x = e.getX() / zoom;
        double y = e.getY() / zoom;
        InterfaceComponent scroll = scrollableAt(x, y);
        if (scroll != null && scroll.isScrollable()) {
            int step = e.getDeltaY() > 0 ? -16 : 16;
            scroll.setPreviewScroll(scroll.getPreviewScroll() + step);
            render();
            e.consume();
        }
    }

    private InterfaceComponent scrollableAt(double x, double y) {
        List<InterfaceComponent> ordered = hitOrdered();
        for (InterfaceComponent component : ordered) {
            if (component.isScrollable()
                && x >= ax(component) && x <= ax(component) + component.getWidth() + 16
                && y >= ay(component) && y <= ay(component) + component.getHeight()) {
                return component;
            }
        }
        return null;
    }

    private List<InterfaceComponent> hitOrdered() {
        List<InterfaceComponent> ordered = new ArrayList<>(project.getComponents());
        ordered.sort((a, b) -> {
            int depth = Integer.compare(depthOf(b), depthOf(a));
            if (depth != 0) {
                return depth;
            }
            return Integer.compare(b.getChildIndex(), a.getChildIndex());
        });
        return ordered;
    }

    private int depthOf(InterfaceComponent component) {
        int depth = 0;
        InterfaceComponent parent = project.parentOf(component);
        while (parent != null && depth < 12) {
            depth++;
            parent = project.parentOf(parent);
        }
        return depth;
    }

    private boolean insideParentClip(InterfaceComponent component, double x, double y) {
        InterfaceComponent parent = project.parentOf(component);
        if (parent == null) {
            return true;
        }
        return x >= ax(parent) && x <= ax(parent) + parent.getWidth()
            && y >= ay(parent) && y <= ay(parent) + parent.getHeight();
    }

    private void onMousePressed(MouseEvent e) {
        double x = lx(e);
        double y = ly(e);
        boolean isCtrlDown = e.isControlDown();
        editStarted = false;
        resizing = false;

        if (selectedComponent != null && canResize(selectedComponent) && overResizeHandle(selectedComponent, x, y)) {
            beginEdit();
            resizing = true;
            draggingComponent = selectedComponent;
            requestFocus();
            return;
        }

        for (InterfaceComponent comp : hitOrdered()) {
            if (!insideParentClip(comp, x, y)) {
                continue;
            }
            if (x >= ax(comp) && x <= ax(comp) + comp.getWidth() &&
                y >= ay(comp) && y <= ay(comp) + comp.getHeight()) {

                if (isCtrlDown) {
                    if (selectedComponents.contains(comp)) {
                        selectedComponents.remove(comp);
                    } else {
                        selectedComponents.add(comp);
                    }
                    selectedComponent = comp;
                } else {
                    selectedComponents.clear();
                    selectedComponents.add(comp);
                    selectedComponent = comp;
                    draggingComponent = comp;
                    for (InterfaceComponent other : project.getComponents()) {
                        if (other != comp && other.getParentInterfaceId() == comp.getParentInterfaceId()
                            && other.getX() == comp.getX() && other.getY() == comp.getY()) {
                            selectedComponents.add(other);
                        }
                    }
                    dragOffsets.clear();
                    for (InterfaceComponent selectedComp : selectedComponents) {
                        dragOffsets.put(selectedComp, new double[]{
                            x - ax(selectedComp),
                            y - ay(selectedComp)
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
        if (draggingComponent == null) {
            return;
        }
        beginEdit();
        double x = lx(e);
        double y = ly(e);
        if (resizing && selectedComponent != null) {
            int newW = Math.max(16, (int) x - ax(selectedComponent));
            int newH = Math.max(16, (int) y - ay(selectedComponent));
            selectedComponent.setWidth(newW);
            selectedComponent.setHeight(newH);
            if (selectedComponent.getScrollMax() > 0 && selectedComponent.getScrollMax() < newH) {
                selectedComponent.setScrollMax(newH);
            }
            render();
            if (onComponentsChanged != null) {
                onComponentsChanged.run();
            }
            return;
        }
        for (InterfaceComponent comp : selectedComponents) {
            if (parentSelected(comp)) {
                continue;
            }
            double[] offsets = dragOffsets.get(comp);
            if (offsets != null) {
                setAbs(comp, (int) (x - offsets[0]), (int) (y - offsets[1]));
            }
        }
        render();
        if (onComponentsChanged != null) {
            onComponentsChanged.run();
        }
    }

    private boolean parentSelected(InterfaceComponent component) {
        InterfaceComponent parent = project.parentOf(component);
        while (parent != null) {
            if (selectedComponents.contains(parent)) {
                return true;
            }
            parent = project.parentOf(parent);
        }
        return false;
    }

    private void onMouseReleased(MouseEvent e) {
        draggingComponent = null;
        resizing = false;
        editStarted = false;
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
            if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN
                || e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.RIGHT) {
                beginEdit();
            }
            switch (e.getCode()) {
                case UP:
                    for (InterfaceComponent comp : selectedComponents) {
                        if (!parentSelected(comp)) {
                            setAbs(comp, ax(comp), ay(comp) - delta);
                        }
                    }
                    render();
                    if (onComponentsChanged != null) {
                        onComponentsChanged.run();
                    }
                    break;
                case DOWN:
                    for (InterfaceComponent comp : selectedComponents) {
                        if (!parentSelected(comp)) {
                            setAbs(comp, ax(comp), ay(comp) + delta);
                        }
                    }
                    render();
                    if (onComponentsChanged != null) {
                        onComponentsChanged.run();
                    }
                    break;
                case LEFT:
                    for (InterfaceComponent comp : selectedComponents) {
                        if (!parentSelected(comp)) {
                            setAbs(comp, ax(comp) - delta, ay(comp));
                        }
                    }
                    render();
                    if (onComponentsChanged != null) {
                        onComponentsChanged.run();
                    }
                    break;
                case RIGHT:
                    for (InterfaceComponent comp : selectedComponents) {
                        if (!parentSelected(comp)) {
                            setAbs(comp, ax(comp) + delta, ay(comp));
                        }
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
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.scale(zoom, zoom);

        gc.setFill(Color.rgb(30, 30, 30));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

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

        drawLayer(gc, project.getInterfaceId());

        for (InterfaceComponent comp : selectedComponents) {
            int x = ax(comp);
            int y = ay(comp);
            gc.setStroke(Color.rgb(255, 255, 0, 0.3));
            gc.setLineWidth(4);
            gc.strokeRect(x - 2, y - 2, comp.getWidth() + 4, comp.getHeight() + 4);
            gc.setStroke(Color.rgb(255, 255, 0));
            gc.setLineWidth(2);
            gc.strokeRect(x - 1, y - 1, comp.getWidth() + 2, comp.getHeight() + 2);
            gc.setFill(Color.rgb(255, 255, 0));
            int handleSize = 4;
            gc.fillRect(x - handleSize, y - handleSize, handleSize, handleSize);
            gc.fillRect(x + comp.getWidth() - handleSize, y - handleSize, handleSize, handleSize);
            gc.fillRect(x - handleSize, y + comp.getHeight() - handleSize, handleSize, handleSize);
            gc.fillRect(x + comp.getWidth() - handleSize, y + comp.getHeight() - handleSize, handleSize, handleSize);
        }
    }

    private void drawLayer(GraphicsContext gc, int parentId) {
        for (InterfaceComponent comp : project.childrenOf(parentId)) {
            drawComponent(gc, comp);
            if (!project.childrenOf(comp.getId()).isEmpty() || comp.isScrollable()) {
                gc.save();
                gc.beginPath();
                gc.rect(ax(comp), ay(comp), comp.getWidth(), comp.getHeight());
                gc.closePath();
                gc.clip();
                drawLayer(gc, comp.getId());
                gc.restore();
                if (comp.isScrollable()) {
                    drawScrollbar(gc, comp);
                }
            }
        }
    }

    private void drawScrollbar(GraphicsContext gc, InterfaceComponent comp) {
        int x = ax(comp) + comp.getWidth();
        int y = ay(comp);
        int h = comp.getHeight();
        int max = Math.max(comp.getScrollMax(), h + 1);
        gc.setFill(Color.rgb(0, 0, 1));
        gc.fillRect(x, y, 16, h);
        gc.setFill(Color.rgb(61, 52, 38));
        gc.fillRect(x, y + 16, 15, Math.max(0, h - 32));
        gc.setFill(Color.rgb(129, 112, 81));
        int track = Math.max(8, h - 32);
        int thumb = Math.max(8, track * h / max);
        int travel = Math.max(1, max - h);
        int thumbY = y + 16 + (track - thumb) * comp.getPreviewScroll() / travel;
        gc.fillRect(x, thumbY, 16, thumb);
        gc.setStroke(Color.rgb(115, 101, 74));
        gc.strokeRect(x, y, 16, h);
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
            case RECTANGLE:
                drawRectangle(gc, comp);
                break;
        }
    }

    private List<InterfaceComponent> drawOrdered() {
        List<InterfaceComponent> ordered = new ArrayList<>(project.getComponents());
        ordered.sort(Comparator.comparingInt(InterfaceComponent::getChildIndex));
        return ordered;
    }

    private void drawSprite(GraphicsContext gc, InterfaceComponent comp) {
        int x = ax(comp);
        int y = ay(comp);
        if (comp instanceof SpriteComponent) {
            SpriteComponent sprite = (SpriteComponent) comp;
            String spritePath = sprite.getSpritePath();
            int spriteId = sprite.getSpriteId();
            if (spritePath != null && !spritePath.isEmpty()) {
                Image image = SpriteLoader.loadSprite(spritePath, spriteId);
                if (image != null) {
                    gc.drawImage(image, x, y);
                    return;
                }
            }
        }
        gc.setFill(Color.rgb(100, 100, 150));
        gc.fillRect(x, y, comp.getWidth(), comp.getHeight());
        gc.setStroke(Color.rgb(150, 150, 200));
        gc.strokeRect(x, y, comp.getWidth(), comp.getHeight());
    }

    private void drawButton(GraphicsContext gc, InterfaceComponent comp) {
        int x = ax(comp);
        int y = ay(comp);
        if (comp instanceof ButtonComponent) {
            ButtonComponent button = (ButtonComponent) comp;
            String spritePath = button.getNormalSpritePath();
            if (spritePath != null && !spritePath.isEmpty()) {
                Image image = SpriteLoader.loadSprite(spritePath, button.getNormalSpriteId());
                if (image != null) {
                    gc.drawImage(image, x, y, comp.getWidth(), comp.getHeight());
                    return;
                }
            }
        }
        gc.setFill(Color.rgb(100, 150, 100));
        gc.fillRect(x, y, comp.getWidth(), comp.getHeight());
        gc.setStroke(Color.rgb(150, 200, 150));
        gc.strokeRect(x, y, comp.getWidth(), comp.getHeight());
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
        gc.fillText(message == null ? "" : message, ax(comp), ay(comp) + Math.max(11, comp.getHeight() - 2));
    }

    private void drawCloseButton(GraphicsContext gc, InterfaceComponent comp) {
        int x = ax(comp);
        int y = ay(comp);
        if (comp instanceof ButtonComponent) {
            ButtonComponent button = (ButtonComponent) comp;
            String spritePath = button.getNormalSpritePath();
            if (spritePath != null && !spritePath.isEmpty()) {
                Image image = SpriteLoader.loadSprite(spritePath, button.getNormalSpriteId());
                if (image != null) {
                    gc.drawImage(image, x, y, comp.getWidth(), comp.getHeight());
                    return;
                }
            }
        }
        gc.setFill(Color.rgb(150, 50, 50));
        gc.fillRect(x, y, comp.getWidth(), comp.getHeight());
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeLine(x + 5, y + 5, x + comp.getWidth() - 5, y + comp.getHeight() - 5);
        gc.strokeLine(x + comp.getWidth() - 5, y + 5, x + 5, y + comp.getHeight() - 5);
    }

    private void drawTooltip(GraphicsContext gc, InterfaceComponent comp) {
        gc.setFill(Color.rgb(255, 255, 200));
        gc.fillRect(ax(comp), ay(comp), comp.getWidth(), comp.getHeight());
        gc.setStroke(Color.rgb(200, 200, 150));
        gc.strokeRect(ax(comp), ay(comp), comp.getWidth(), comp.getHeight());
    }

    private void drawRectangle(GraphicsContext gc, InterfaceComponent comp) {
        int color = comp.getFillColor();
        Color fill = Color.rgb((color >> 16) & 255, (color >> 8) & 255, color & 255, comp.isFilled() ? 0.92 : 0.0);
        gc.setFill(fill);
        gc.fillRect(ax(comp), ay(comp), comp.getWidth(), comp.getHeight());
        gc.setStroke(Color.rgb((color >> 16) & 255, (color >> 8) & 255, color & 255));
        gc.strokeRect(ax(comp), ay(comp), comp.getWidth(), comp.getHeight());
    }

    private void drawContainer(GraphicsContext gc, InterfaceComponent comp) {
        gc.setFill(Color.rgb(80, 140, 180, 0.18));
        gc.fillRect(ax(comp), ay(comp), comp.getWidth(), comp.getHeight());
        gc.setStroke(Color.rgb(120, 180, 220, 0.9));
        gc.setLineWidth(1);
        gc.strokeRect(ax(comp), ay(comp), comp.getWidth(), comp.getHeight());
        gc.setFill(Color.rgb(180, 220, 255));
        gc.fillText(comp.getName() + " [" + comp.getId() + "]", ax(comp) + 4, ay(comp) + 12);
    }

    private void drawItemSlot(GraphicsContext gc, InterfaceComponent comp) {
        gc.setFill(Color.rgb(90, 70, 40, 0.45));
        gc.fillRect(ax(comp), ay(comp), comp.getWidth(), comp.getHeight());
        gc.setStroke(Color.rgb(180, 150, 80));
        gc.strokeRect(ax(comp), ay(comp), comp.getWidth(), comp.getHeight());
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

    public void setOnEditStarted(Runnable onEditStarted) {
        this.onEditStarted = onEditStarted;
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = Math.max(0.5, Math.min(4.0, zoom));
        setWidth(CANVAS_WIDTH * this.zoom);
        setHeight(CANVAS_HEIGHT * this.zoom);
        render();
        if (onZoomChanged != null) {
            onZoomChanged.run();
        }
    }

    public void setOnZoomChanged(Runnable onZoomChanged) {
        this.onZoomChanged = onZoomChanged;
    }
    
    public void toggleGrid() {
        showGrid = !showGrid;
        render();
    }
    
    public boolean isGridVisible() {
        return showGrid;
    }
}
