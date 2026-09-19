package com.rsps.interfacemaker.model;

import java.util.ArrayList;
import java.util.List;

public class InterfaceProject {
    private String name;
    private int interfaceId;
    private List<InterfaceComponent> components;
    private String spriteFolder;
    private int nextComponentId;
    private String spriteRootDirectory;
    private String cachePath;
    private String interfacesFilePath;
    private String sourceMethodName = "";
    private boolean clientLinked;
    private List<LoopBoundsBlock> loopBlocks = new ArrayList<>();
    private int offsetX = 12;  // Default X offset for game display
    private int offsetY = 14;  // Default Y offset for game display

    public InterfaceProject() {
        this.name = "NewInterface";
        this.interfaceId = 45000;
        this.components = new ArrayList<>();
        this.spriteFolder = "Interfaces/" + name + "/";
        this.nextComponentId = interfaceId + 1;
        this.spriteRootDirectory = "";
        this.cachePath = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.spriteFolder = "Interfaces/" + name + "/";
    }

    public int getInterfaceId() {
        return interfaceId;
    }

    public void setInterfaceId(int interfaceId) {
        this.interfaceId = interfaceId;
        this.nextComponentId = interfaceId + 1;
    }

    public List<InterfaceComponent> getComponents() {
        return components;
    }

    public void addComponent(InterfaceComponent component) {
        component.setId(nextComponentId++);
        component.setParentInterfaceId(interfaceId);
        component.setChildIndex(components.size());
        components.add(component);
    }

    /** Keep parsed widget IDs and child slots. Used when loading Interfaces.java. */
    public void addExistingComponent(InterfaceComponent component) {
        if (component.getParentInterfaceId() == 0) {
            component.setParentInterfaceId(interfaceId);
        }
        components.add(component);
        if (component.getId() >= nextComponentId) {
            nextComponentId = component.getId() + 1;
        }
    }

    public void removeComponent(InterfaceComponent component) {
        components.remove(component);
        if (!clientLinked) {
            for (int i = 0; i < components.size(); i++) {
                components.get(i).setChildIndex(i);
            }
        }
    }

    public String getSpriteFolder() {
        return spriteFolder;
    }

    public void setSpriteFolder(String spriteFolder) {
        this.spriteFolder = spriteFolder;
    }

    public int getNextComponentId() {
        return nextComponentId;
    }

    public void setNextComponentId(int nextComponentId) {
        this.nextComponentId = nextComponentId;
    }

    public String getSpriteRootDirectory() {
        return spriteRootDirectory;
    }

    public void setSpriteRootDirectory(String spriteRootDirectory) {
        this.spriteRootDirectory = spriteRootDirectory;
    }

    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
    }

    public String getInterfacesFilePath() {
        return interfacesFilePath;
    }

    public void setInterfacesFilePath(String interfacesFilePath) {
        this.interfacesFilePath = interfacesFilePath;
    }

    public String getSourceMethodName() {
        return sourceMethodName;
    }

    public void setSourceMethodName(String sourceMethodName) {
        this.sourceMethodName = sourceMethodName == null ? "" : sourceMethodName;
    }

    public boolean isClientLinked() {
        return clientLinked;
    }

    public void setClientLinked(boolean clientLinked) {
        this.clientLinked = clientLinked;
    }

    public List<LoopBoundsBlock> getLoopBlocks() {
        return loopBlocks;
    }

    public void setLoopBlocks(List<LoopBoundsBlock> loopBlocks) {
        this.loopBlocks = loopBlocks == null ? new ArrayList<>() : loopBlocks;
    }

    public InterfaceComponent findById(int id) {
        for (InterfaceComponent component : components) {
            if (component.getId() == id) {
                return component;
            }
        }
        return null;
    }

    public InterfaceComponent parentOf(InterfaceComponent component) {
        int parentId = component.getParentInterfaceId();
        if (parentId == 0 || parentId == interfaceId) {
            return null;
        }
        return findById(parentId);
    }

    public int absX(InterfaceComponent component) {
        InterfaceComponent parent = parentOf(component);
        if (parent == null) {
            return component.getX();
        }
        return absX(parent) + component.getX();
    }

    public int absY(InterfaceComponent component) {
        InterfaceComponent parent = parentOf(component);
        if (parent == null) {
            return component.getY();
        }
        return absY(parent) + component.getY() - parent.getPreviewScroll();
    }

    public List<InterfaceComponent> childrenOf(int parentId) {
        List<InterfaceComponent> children = new ArrayList<>();
        for (InterfaceComponent component : components) {
            int pid = component.getParentInterfaceId();
            if (pid == parentId || (parentId == interfaceId && (pid == 0 || pid == interfaceId))) {
                children.add(component);
            }
        }
        children.sort((a, b) -> Integer.compare(a.getChildIndex(), b.getChildIndex()));
        return children;
    }
}
