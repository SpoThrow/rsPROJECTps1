package com.rsps.interfacemaker.model;

public class TextComponent extends InterfaceComponent {
    private String text;
    private int fontIndex;
    private int textColor;
    private boolean hasShadow;
    private boolean centered;

    public TextComponent() {
        super(ComponentType.TEXT);
        this.text = "Text";
        this.fontIndex = 0;
        this.textColor = 0xFFFFFF;
        this.hasShadow = true;
        this.centered = false;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getFontIndex() {
        return fontIndex;
    }

    public void setFontIndex(int fontIndex) {
        this.fontIndex = fontIndex;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public boolean isHasShadow() {
        return hasShadow;
    }

    public void setHasShadow(boolean hasShadow) {
        this.hasShadow = hasShadow;
    }

    public boolean isCentered() {
        return centered;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }
}
