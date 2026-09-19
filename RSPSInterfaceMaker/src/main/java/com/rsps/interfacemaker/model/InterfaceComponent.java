package com.rsps.interfacemaker.model;

public class InterfaceComponent {
    private int id;
    private ComponentType type;
    private int x;
    private int y;
    private int width;
    private int height;
    private String name;
    private String tooltip;
    private int parentInterfaceId;
    private int childIndex;
    private int originalX;
    private int originalY;
    private boolean fromLoop;
    private String loopGroup = "";
    private String parentVarName = "";

    public InterfaceComponent() {
        this.x = 0;
        this.y = 0;
        this.width = 100;
        this.height = 20;
        this.name = "Component";
        this.tooltip = "";
    }

    public InterfaceComponent(ComponentType type) {
        this();
        this.type = type;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ComponentType getType() {
        return type;
    }

    public void setType(ComponentType type) {
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public int getParentInterfaceId() {
        return parentInterfaceId;
    }

    public void setParentInterfaceId(int parentInterfaceId) {
        this.parentInterfaceId = parentInterfaceId;
    }

    public int getChildIndex() {
        return childIndex;
    }

    public void setChildIndex(int childIndex) {
        this.childIndex = childIndex;
    }

    public int getOriginalX() {
        return originalX;
    }

    public void setOriginalX(int originalX) {
        this.originalX = originalX;
    }

    public int getOriginalY() {
        return originalY;
    }

    public void setOriginalY(int originalY) {
        this.originalY = originalY;
    }

    public boolean isFromLoop() {
        return fromLoop;
    }

    public void setFromLoop(boolean fromLoop) {
        this.fromLoop = fromLoop;
    }

    public String getLoopGroup() {
        return loopGroup;
    }

    public void setLoopGroup(String loopGroup) {
        this.loopGroup = loopGroup == null ? "" : loopGroup;
    }

    public String getParentVarName() {
        return parentVarName;
    }

    public void setParentVarName(String parentVarName) {
        this.parentVarName = parentVarName == null ? "" : parentVarName;
    }

    public boolean positionChanged() {
        return x != originalX || y != originalY;
    }
}
