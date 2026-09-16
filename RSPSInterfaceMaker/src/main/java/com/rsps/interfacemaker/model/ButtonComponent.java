package com.rsps.interfacemaker.model;

public class ButtonComponent extends InterfaceComponent {
    private String normalSpritePath;
    private int normalSpriteId;
    private String hoveredSpritePath;
    private int hoveredSpriteId;
    private String actionName;
    private int actionId;

    public ButtonComponent() {
        super(ComponentType.HOVER_BUTTON);
        this.normalSpritePath = "Interfaces/MyInterface/BUTTON";
        this.normalSpriteId = 0;
        this.hoveredSpritePath = "Interfaces/MyInterface/BUTTON_HOVER";
        this.hoveredSpriteId = 0;
        this.actionName = "Button";
        this.actionId = 0;
    }

    public String getNormalSpritePath() {
        return normalSpritePath;
    }

    public void setNormalSpritePath(String normalSpritePath) {
        this.normalSpritePath = normalSpritePath;
    }
    
    public int getNormalSpriteId() {
        return normalSpriteId;
    }
    
    public void setNormalSpriteId(int normalSpriteId) {
        this.normalSpriteId = normalSpriteId;
    }

    public String getHoveredSpritePath() {
        return hoveredSpritePath;
    }

    public void setHoveredSpritePath(String hoveredSpritePath) {
        this.hoveredSpritePath = hoveredSpritePath;
    }
    
    public int getHoveredSpriteId() {
        return hoveredSpriteId;
    }
    
    public void setHoveredSpriteId(int hoveredSpriteId) {
        this.hoveredSpriteId = hoveredSpriteId;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public int getActionId() {
        return actionId;
    }

    public void setActionId(int actionId) {
        this.actionId = actionId;
    }
}
