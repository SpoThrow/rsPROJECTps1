package com.rsps.interfacemaker.view;

import com.rsps.interfacemaker.model.InterfaceComponent;
import com.rsps.interfacemaker.model.InterfaceProject;
import com.rsps.interfacemaker.model.ComponentType;
import com.rsps.interfacemaker.model.SpriteComponent;
import com.rsps.interfacemaker.model.ButtonComponent;
import com.rsps.interfacemaker.util.SpriteLoader;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import java.util.function.Consumer;

public class InterfaceCanvas extends Canvas {
    private InterfaceProject project;
    private InterfaceComponent selectedComponent;
    private java.util.List<InterfaceComponent> selectedComponents = new java.util.ArrayList<>();
    private InterfaceComponent draggingComponent;
    private double dragOffsetX;
    private double dragOffsetY;
    private Consumer<InterfaceComponent> onComponentSelected;

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
    }

    private void onMousePressed(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        boolean isCtrlDown = e.isControlDown();

        // Find component under mouse (reverse order for top-most first)
        for (int i = project.getComponents().size() - 1; i >= 0; i--) {
            InterfaceComponent comp = project.getComponents().get(i);
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
                    // Single select
                    selectedComponents.clear();
                    selectedComponents.add(comp);
                    selectedComponent = comp;
                    draggingComponent = comp;
                    dragOffsetX = x - comp.getX();
                    dragOffsetY = y - comp.getY();
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
            int newX = (int)(e.getX() - dragOffsetX);
            int newY = (int)(e.getY() - dragOffsetY);
            int deltaX = newX - draggingComponent.getX();
            int deltaY = newY - draggingComponent.getY();
            
            // Move all selected components
            for (InterfaceComponent comp : selectedComponents) {
                comp.setX(comp.getX() + deltaX);
                comp.setY(comp.getY() + deltaY);
            }
            
            render();
        }
    }

    private void onMouseReleased(MouseEvent e) {
        draggingComponent = null;
    }

    private void onKeyPressed(KeyEvent e) {
        if (!selectedComponents.isEmpty()) {
            int delta = e.isShiftDown() ? 10 : 1;
            switch (e.getCode()) {
                case UP:
                    for (InterfaceComponent comp : selectedComponents) {
                        comp.setY(comp.getY() - delta);
                    }
                    render();
                    break;
                case DOWN:
                    for (InterfaceComponent comp : selectedComponents) {
                        comp.setY(comp.getY() + delta);
                    }
                    render();
                    break;
                case LEFT:
                    for (InterfaceComponent comp : selectedComponents) {
                        comp.setX(comp.getX() - delta);
                    }
                    render();
                    break;
                case RIGHT:
                    for (InterfaceComponent comp : selectedComponents) {
                        comp.setX(comp.getX() + delta);
                    }
                    render();
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
                    break;
            }
        }
    }

    public void render() {
        GraphicsContext gc = getGraphicsContext2D();
        
        // Clear canvas with dark background
        gc.setFill(Color.rgb(30, 30, 30));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Draw grid
        gc.setStroke(Color.rgb(50, 50, 50));
        gc.setLineWidth(0.5);
        for (int x = 0; x <= CANVAS_WIDTH; x += 32) {
            gc.strokeLine(x, 0, x, CANVAS_HEIGHT);
        }
        for (int y = 0; y <= CANVAS_HEIGHT; y += 32) {
            gc.strokeLine(0, y, CANVAS_WIDTH, y);
        }

        // Draw components
        for (InterfaceComponent comp : project.getComponents()) {
            drawComponent(gc, comp);
        }

        // Draw selection outlines
        for (InterfaceComponent comp : selectedComponents) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(2);
            gc.strokeRect(
                comp.getX() - 1,
                comp.getY() - 1,
                comp.getWidth() + 2,
                comp.getHeight() + 2
            );
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
        }
    }

    private void drawSprite(GraphicsContext gc, InterfaceComponent comp) {
        // Try to load and render the actual sprite
        if (comp instanceof SpriteComponent) {
            SpriteComponent sprite = (SpriteComponent) comp;
            Image image = SpriteLoader.loadSprite(sprite.getSpritePath());
            
            if (image != null) {
                gc.drawImage(image, comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            } else {
                // Fallback to colored rectangle if sprite not found
                gc.setFill(Color.rgb(100, 100, 150));
                gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
                gc.setStroke(Color.rgb(150, 150, 200));
                gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            }
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
            Image image = SpriteLoader.loadSprite(button.getNormalSpritePath());
            
            if (image != null) {
                gc.drawImage(image, comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            } else {
                // Fallback to colored rectangle if sprite not found
                gc.setFill(Color.rgb(100, 150, 100));
                gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
                gc.setStroke(Color.rgb(150, 200, 150));
                gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            }
        } else {
            // Fallback for non-ButtonComponent
            gc.setFill(Color.rgb(100, 150, 100));
            gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            gc.setStroke(Color.rgb(150, 200, 150));
            gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
        }
    }

    private void drawText(GraphicsContext gc, InterfaceComponent comp) {
        gc.setFill(Color.rgb(200, 200, 200));
        gc.fillRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
        gc.setStroke(Color.rgb(255, 255, 255));
        gc.strokeRect(comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
    }

    private void drawCloseButton(GraphicsContext gc, InterfaceComponent comp) {
        // Try to load and render the actual close button sprite
        if (comp instanceof ButtonComponent) {
            ButtonComponent button = (ButtonComponent) comp;
            Image image = SpriteLoader.loadSprite(button.getNormalSpritePath());
            
            if (image != null) {
                gc.drawImage(image, comp.getX(), comp.getY(), comp.getWidth(), comp.getHeight());
            } else {
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
            }
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
}
