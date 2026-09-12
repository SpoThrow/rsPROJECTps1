package com.rsps.interfacemaker.model;

public class ButtonComponent extends InterfaceComponent {
    private String normalSpritePath;
    private String hoveredSpritePath;
    private String actionName;
    private int actionId;

    public ButtonComponent() {
        super(ComponentType.HOVER_BUTTON);
        this.normalSpritePath = "Interfaces/MyInterface/BUTTON";
        this.hoveredSpritePath = "Interfaces/MyInterface/BUTTON_HOVER";
        this.actionName = "Button";
        this.actionId = 0;
    }

    public String getNormalSpritePath() {
        return normalSpritePath;
    }

    public void setNormalSpritePath(String normalSpritePath) {
        this.normalSpritePath = normalSpritePath;
    }

    public String getHoveredSpritePath() {
        return hoveredSpritePath;
    }

    public void setHoveredSpritePath(String hoveredSpritePath) {
        this.hoveredSpritePath = hoveredSpritePath;
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
