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
    private String sourceVarName = "";
    private int originalWidth;
    private int originalHeight;
    private int scrollMax;
    private int originalScrollMax;
    private int previewScroll;
    private int fillColor = 0x2B2319;
    private boolean filled = true;

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

    public boolean sizeChanged() {
        return (originalWidth > 0 && width != originalWidth)
            || (originalHeight > 0 && height != originalHeight)
            || scrollMax != originalScrollMax;
    }

    public String getSourceVarName() {
        return sourceVarName;
    }

    public void setSourceVarName(String sourceVarName) {
        this.sourceVarName = sourceVarName == null ? "" : sourceVarName;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public void setOriginalWidth(int originalWidth) {
        this.originalWidth = originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    public void setOriginalHeight(int originalHeight) {
        this.originalHeight = originalHeight;
    }

    public int getScrollMax() {
        return scrollMax;
    }

    public void setScrollMax(int scrollMax) {
        this.scrollMax = Math.max(0, scrollMax);
        if (previewScroll > Math.max(0, this.scrollMax - height)) {
            previewScroll = Math.max(0, this.scrollMax - height);
        }
    }

    public int getOriginalScrollMax() {
        return originalScrollMax;
    }

    public void setOriginalScrollMax(int originalScrollMax) {
        this.originalScrollMax = originalScrollMax;
    }

    public int getPreviewScroll() {
        return previewScroll;
    }

    public void setPreviewScroll(int previewScroll) {
        int max = Math.max(0, scrollMax - height);
        if (previewScroll < 0) {
            previewScroll = 0;
        }
        if (previewScroll > max) {
            previewScroll = max;
        }
        this.previewScroll = previewScroll;
    }

    public int getFillColor() {
        return fillColor;
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
    }

    public boolean isFilled() {
        return filled;
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    public boolean isScrollable() {
        return scrollMax > height && height > 0;
    }
}
