package com.rsps.interfacemaker.view;

import com.rsps.interfacemaker.model.InterfaceComponent;
import com.rsps.interfacemaker.model.InterfaceProject;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

public class ComponentListView extends VBox {
    private InterfaceProject project;
    private ListView<InterfaceComponent> componentList;
    private Runnable onComponentSelected;
    private Consumer<InterfaceComponent> onComponentSelectedWithComponent;
    private Runnable onComponentsChanged;

    public ComponentListView(InterfaceProject project) {
        this.project = project;
        initializeUI();
    }

    private void initializeUI() {
        setSpacing(10);
        setStyle("-fx-padding: 10;");

        Label label = new Label("Components");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        getChildren().add(label);

        componentList = new ListView<>();
        componentList.setCellFactory(list -> new ComponentListCell());
        componentList.setItems(javafx.collections.FXCollections.observableArrayList(project.getComponents()));
        
        componentList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (onComponentSelected != null && newVal != null) {
                onComponentSelected.run();
            }
            if (onComponentSelectedWithComponent != null && newVal != null) {
                onComponentSelectedWithComponent.accept(newVal);
            }
        });

        // Context menu for reordering
        ContextMenu contextMenu = new ContextMenu();
        MenuItem moveUp = new MenuItem("Move Up");
        moveUp.setOnAction(e -> moveComponentUp());
        MenuItem moveDown = new MenuItem("Move Down");
        moveDown.setOnAction(e -> moveComponentDown());
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(e -> deleteComponent());
        contextMenu.getItems().addAll(moveUp, moveDown, new SeparatorMenuItem(), delete);
        componentList.setContextMenu(contextMenu);

        getChildren().add(componentList);
    }

    private void moveComponentUp() {
        InterfaceComponent selected = componentList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int index = project.getComponents().indexOf(selected);
            if (index > 0) {
                project.getComponents().remove(index);
                project.getComponents().add(index - 1, selected);
                refresh();
                componentList.getSelectionModel().select(index - 1);
                if (onComponentsChanged != null) {
                    onComponentsChanged.run();
                }
            }
        }
    }

    private void moveComponentDown() {
        InterfaceComponent selected = componentList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int index = project.getComponents().indexOf(selected);
            if (index < project.getComponents().size() - 1) {
                project.getComponents().remove(index);
                project.getComponents().add(index + 1, selected);
                refresh();
                componentList.getSelectionModel().select(index + 1);
                if (onComponentsChanged != null) {
                    onComponentsChanged.run();
                }
            }
        }
    }

    private void deleteComponent() {
        InterfaceComponent selected = componentList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            project.removeComponent(selected);
            refresh();
            if (onComponentsChanged != null) {
                onComponentsChanged.run();
            }
        }
    }

    public void refresh() {
        componentList.setItems(javafx.collections.FXCollections.observableArrayList(project.getComponents()));
    }

    public void setProject(InterfaceProject project) {
        this.project = project;
        refresh();
    }

    public void setOnComponentSelected(Runnable onComponentSelected) {
        this.onComponentSelected = onComponentSelected;
    }

    public void setOnComponentSelectedWithComponent(Consumer<InterfaceComponent> onComponentSelectedWithComponent) {
        this.onComponentSelectedWithComponent = onComponentSelectedWithComponent;
    }

    public void setOnComponentsChanged(Runnable onComponentsChanged) {
        this.onComponentsChanged = onComponentsChanged;
    }

    public InterfaceComponent getSelectedComponent() {
        return componentList.getSelectionModel().getSelectedItem();
    }

    public void selectComponent(InterfaceComponent component) {
        if (component != null && project.getComponents().contains(component)) {
            componentList.getSelectionModel().select(component);
        }
    }

    private static class ComponentListCell extends ListCell<InterfaceComponent> {
        @Override
        protected void updateItem(InterfaceComponent component, boolean empty) {
            super.updateItem(component, empty);
            if (empty || component == null) {
                setText(null);
                setGraphic(null);
            } else {
                String typeIcon = getTypeIcon(component.getType());
                setText(typeIcon + " " + component.getName() + " (" + component.getId() + ")");
            }
        }

        private String getTypeIcon(com.rsps.interfacemaker.model.ComponentType type) {
            switch (type) {
                case SPRITE: return "🖼️";
                case HOVER_BUTTON:
                case HOVERED_BUTTON: return "🔘";
                case TEXT: return "📝";
                case TOOLTIP: return "💬";
                case CLOSE_BUTTON: return "❌";
                case CONTAINER: return "▭";
                case ITEM_SLOT: return "▣";
                default: return "❓";
            }
        }
    }
}
