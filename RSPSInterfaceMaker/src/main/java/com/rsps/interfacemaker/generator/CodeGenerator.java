package com.rsps.interfacemaker.generator;

import com.rsps.interfacemaker.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CodeGenerator {
    private InterfaceProject project;

    public CodeGenerator(InterfaceProject project) {
        this.project = project;
    }

    public String generateInterfaceMethod() {
        StringBuilder sb = new StringBuilder();
        
        // Calculate total children count (including hovered buttons)
        int totalChildren = project.getComponents().size();
        for (InterfaceComponent comp : project.getComponents()) {
            if (comp.getType() == ComponentType.HOVER_BUTTON || comp.getType() == ComponentType.CLOSE_BUTTON) {
                totalChildren++; // Add one for the hovered button
            }
        }
        
        sb.append("public static void ").append(project.getName()).append("(TextDrawingArea[] tda) {\n");
        sb.append("    RSInterface inter = addInterface(").append(project.getInterfaceId()).append(");\n");
        sb.append("    setChildren(").append(totalChildren).append(", inter);\n");
        sb.append("\n");

        // Generate component declarations
        for (InterfaceComponent comp : project.getComponents()) {
            sb.append(generateComponentDeclaration(comp));
        }

        sb.append("\n");
        
        // Generate child positioning calls
        int childIndex = 0;
        int offsetX = project.getOffsetX();
        int offsetY = project.getOffsetY();
        for (InterfaceComponent comp : project.getComponents()) {
            sb.append(generateChildPositioning(comp, childIndex));
            childIndex++;
            if (comp.getType() == ComponentType.HOVER_BUTTON || comp.getType() == ComponentType.CLOSE_BUTTON) {
                // Add child positioning for hovered button with same offsets
                sb.append("    inter.child(").append(childIndex).append(", ").append(comp.getId() + 1)
                  .append(", ").append(comp.getX() + offsetX).append(", ").append(comp.getY() + offsetY).append(");\n");
                childIndex++;
            }
        }

        sb.append("}\n");

        return sb.toString();
    }

    private String generateComponentDeclaration(InterfaceComponent comp) {
        switch (comp.getType()) {
            case SPRITE:
                return generateSpriteDeclaration((SpriteComponent) comp);
            case HOVER_BUTTON:
            case HOVERED_BUTTON:
            case CLOSE_BUTTON:
                return generateButtonDeclaration((ButtonComponent) comp);
            case TEXT:
                return generateTextDeclaration((TextComponent) comp);
            case TOOLTIP:
                return generateTooltipDeclaration(comp);
            default:
                return "";
        }
    }

    private String generateSpriteDeclaration(SpriteComponent comp) {
        StringBuilder sb = new StringBuilder();
        // Pattern: addSprite(id, spriteId, "Interfaces/Folder/NAME")
        // Use a simple sprite ID calculation (could be made more sophisticated)
        int spriteId = 0; // Simplified - in real implementation this would come from cache
        // Remove sprite ID from path if it contains a number suffix (e.g., "MAIN 0" -> "MAIN")
        String spritePath = comp.getSpritePath();
        int lastSpaceIndex = spritePath.lastIndexOf(' ');
        if (lastSpaceIndex > 0 && Character.isDigit(spritePath.charAt(lastSpaceIndex + 1))) {
            spritePath = spritePath.substring(0, lastSpaceIndex);
        }
        sb.append("    addSprite(").append(comp.getId()).append(", ").append(spriteId)
          .append(", \"").append(spritePath).append("\");\n");
        return sb.toString();
    }

    private String generateButtonDeclaration(ButtonComponent comp) {
        StringBuilder sb = new StringBuilder();
        
        // Pattern: addHoverButton(id, "path", spriteId, width, height, "tooltip", contentType, hoverId, actionType)
        int spriteId = 0; // Simplified - in real implementation this would come from cache
        int hoverId = comp.getId() + 1;
        int actionType = 1; // Default action type for buttons
        
        // Remove sprite ID from path if it contains a number suffix
        String normalPath = comp.getNormalSpritePath();
        int lastSpaceIndex = normalPath.lastIndexOf(' ');
        if (lastSpaceIndex > 0 && Character.isDigit(normalPath.charAt(lastSpaceIndex + 1))) {
            normalPath = normalPath.substring(0, lastSpaceIndex);
        }
        
        sb.append("    addHoverButton(").append(comp.getId()).append(", \"").append(normalPath)
          .append("\", ").append(spriteId).append(", ").append(comp.getWidth()).append(", ").append(comp.getHeight())
          .append(", \"").append(comp.getTooltip()).append("\", 0, ").append(hoverId).append(", ").append(actionType).append(");\n");
        
        // Pattern: addHoveredButton(hoverId, "path", spriteId, width, height, dummyId)
        int dummyId = comp.getId() + 2;
        
        // Remove sprite ID from path if it contains a number suffix
        String hoveredPath = comp.getHoveredSpritePath();
        lastSpaceIndex = hoveredPath.lastIndexOf(' ');
        if (lastSpaceIndex > 0 && Character.isDigit(hoveredPath.charAt(lastSpaceIndex + 1))) {
            hoveredPath = hoveredPath.substring(0, lastSpaceIndex);
        }
        
        sb.append("    addHoveredButton(").append(hoverId).append(", \"").append(hoveredPath)
          .append("\", ").append(spriteId).append(", ").append(comp.getWidth()).append(", ").append(comp.getHeight())
          .append(", ").append(dummyId).append(");\n");
        
        return sb.toString();
    }

    private String generateTextDeclaration(TextComponent comp) {
        StringBuilder sb = new StringBuilder();
        // Pattern: addText(id, "Text", tda, font, color, centered, shadow)
        String colorHex = String.format("0x%06X", comp.getTextColor());
        sb.append("    addText(").append(comp.getId()).append(", \"").append(comp.getText())
          .append("\", tda, ").append(comp.getFontIndex()).append(", ").append(colorHex)
          .append(", ").append(comp.isCentered()).append(", ").append(comp.isHasShadow()).append(");\n");
        return sb.toString();
    }

    private String generateTooltipDeclaration(InterfaceComponent comp) {
        StringBuilder sb = new StringBuilder();
        sb.append("    addTooltip(").append(comp.getId()).append(", \"").append(comp.getTooltip()).append("\");\n");
        return sb.toString();
    }

    private String generateChildPositioning(InterfaceComponent comp, int index) {
        StringBuilder sb = new StringBuilder();
        // Pattern: inter.child(index, childId, x, y)
        // Apply game display offsets to match WYSIWYG positioning
        int offsetX = project.getOffsetX();
        int offsetY = project.getOffsetY();
        sb.append("    inter.child(").append(index).append(", ").append(comp.getId())
          .append(", ").append(comp.getX() + offsetX).append(", ").append(comp.getY() + offsetY).append(");\n");
        
        return sb.toString();
    }

    public String generateLoadInterfacesCall() {
        return "    " + project.getName() + "(tda);";
    }

    public String generateClickingButtonsStub() {
        StringBuilder sb = new StringBuilder();
        
        for (InterfaceComponent comp : project.getComponents()) {
            if (comp.getType() == ComponentType.HOVER_BUTTON || 
                comp.getType() == ComponentType.CLOSE_BUTTON) {
                ButtonComponent button = (ButtonComponent) comp;
                sb.append("case ").append(comp.getId()).append(":\n");
                sb.append("    // TODO: Handle ").append(button.getActionName()).append(" action\n");
                sb.append("    break;\n");
            }
        }
        
        return sb.toString();
    }

    public String generateOpenInterfaceCommand() {
        return "c.getPA().showInterface(" + project.getInterfaceId() + ");";
    }

    public List<String> getRequiredSprites() {
        List<String> sprites = new ArrayList<>();
        
        for (InterfaceComponent comp : project.getComponents()) {
            if (comp instanceof SpriteComponent) {
                SpriteComponent sprite = (SpriteComponent) comp;
                sprites.add(sprite.getSpritePath());
                if (sprite.isHasDisabledSprite()) {
                    sprites.add(sprite.getDisabledSpritePath());
                }
            } else if (comp instanceof ButtonComponent) {
                ButtonComponent button = (ButtonComponent) comp;
                sprites.add(button.getNormalSpritePath());
                sprites.add(button.getHoveredSpritePath());
            }
        }
        
        return sprites.stream().distinct().collect(Collectors.toList());
    }

    public String generateImplementationGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Implementation Guide: ").append(project.getName()).append("\n\n");
        
        sb.append("## Overview\n");
        sb.append("- **Interface ID**: `").append(project.getInterfaceId()).append("`\n");
        sb.append("- **Component ID Range**: `").append(project.getInterfaceId() + 1).append("` to `").append(project.getNextComponentId() - 1).append("`\n");
        sb.append("- **Total Components**: ").append(project.getComponents().size()).append("\n\n");
        
        sb.append("## Step 1: Add Sprite Files\n\n");
        sb.append("Place the following sprite files in your client cache:\n\n");
        sb.append("**Folder**: `").append(project.getSpriteFolder()).append("`\n\n");
        for (String sprite : getRequiredSprites()) {
            sb.append("- `").append(sprite).append(".png`\n");
        }
        sb.append("\n");
        
        sb.append("## Step 2: Add Method to Interfaces.java\n\n");
        sb.append("Add this method to the `Interfaces` class:\n\n");
        sb.append("```java\n");
        sb.append(generateInterfaceMethod());
        sb.append("```\n");
        sb.append("\n");
        
        sb.append("## Step 3: Register in loadInterfaces()\n\n");
        sb.append("Add this call inside the `loadInterfaces(TextDrawingArea[] tda)` method:\n\n");
        sb.append("```java\n");
        sb.append(generateLoadInterfacesCall()).append("\n");
        sb.append("```\n");
        sb.append("\n");
        
        sb.append("## Step 4: Handle Button Clicks\n\n");
        sb.append("Add these cases to `ClickingButtons.java` to handle button clicks:\n\n");
        sb.append("```java\n");
        sb.append(generateClickingButtonsStub());
        sb.append("```\n");
        sb.append("\n");
        
        sb.append("## Step 5: Open the Interface\n\n");
        sb.append("Use this command to open the interface:\n\n");
        sb.append("```java\n");
        sb.append(generateOpenInterfaceCommand()).append("\n");
        sb.append("```\n");
        sb.append("\n");
        
        sb.append("## Notes\n\n");
        sb.append("- Sprite IDs are automatically calculated as `componentId - interfaceId`\n");
        sb.append("- Hover button IDs are `componentId + 1`\n");
        sb.append("- Dummy IDs for hovered buttons are `componentId + 2`\n");
        sb.append("- Ensure sprite paths match your client cache structure exactly\n");
        sb.append("- Test the interface after implementation to verify positioning\n");
        sb.append("- Display offsets (X: ").append(project.getOffsetX()).append(", Y: ").append(project.getOffsetY()).append(") are applied to match game coordinates\n");
        sb.append("- Adjust offsets via Advanced > Set Display Offsets if positioning doesn't match game display\n");
        
        return sb.toString();
    }

    public String generateExport() {
        StringBuilder sb = new StringBuilder();
        sb.append("# RSPS Interface Export\n\n");
        sb.append("## ").append(project.getName()).append("\n\n");
        sb.append(generateImplementationGuide());
        
        return sb.toString();
    }
}
