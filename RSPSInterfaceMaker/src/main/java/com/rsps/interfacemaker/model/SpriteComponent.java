package com.rsps.interfacemaker.model;

public class SpriteComponent extends InterfaceComponent {
    private String spritePath;
    private int spriteId;
    private boolean hasDisabledSprite;
    private String disabledSpritePath;

    public SpriteComponent() {
        super(ComponentType.SPRITE);
        this.spritePath = "Interfaces/MyInterface/SPRITE";
        this.spriteId = 0;
        this.hasDisabledSprite = false;
    }

    public String getSpritePath() {
        return spritePath;
    }

    public void setSpritePath(String spritePath) {
        this.spritePath = spritePath;
    }
    
    public int getSpriteId() {
        return spriteId;
    }
    
    public void setSpriteId(int spriteId) {
        this.spriteId = spriteId;
    }

    public boolean isHasDisabledSprite() {
        return hasDisabledSprite;
    }

    public void setHasDisabledSprite(boolean hasDisabledSprite) {
        this.hasDisabledSprite = hasDisabledSprite;
    }

    public String getDisabledSpritePath() {
        return disabledSpritePath;
    }

    public void setDisabledSpritePath(String disabledSpritePath) {
        this.disabledSpritePath = disabledSpritePath;
    }
}
