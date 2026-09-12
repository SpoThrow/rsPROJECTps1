package com.rsps.interfacemaker.util;

import com.rsps.interfacemaker.model.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Validator {
    
    public static class ValidationResult {
        public boolean isValid;
        public List<String> warnings;
        public List<String> errors;
        
        public ValidationResult() {
            this.warnings = new ArrayList<>();
            this.errors = new ArrayList<>();
            this.isValid = true;
        }
        
        public void addWarning(String message) {
            warnings.add(message);
        }
        
        public void addError(String message) {
            errors.add(message);
            isValid = false;
        }
        
        public boolean hasIssues() {
            return !warnings.isEmpty() || !errors.isEmpty();
        }
    }
    
    public static ValidationResult validateProject(InterfaceProject project) {
        ValidationResult result = new ValidationResult();
        
        if (project == null) {
            result.addError("Project is null");
            return result;
        }
        
        // Check for duplicate IDs
        Set<Integer> ids = new HashSet<>();
        for (InterfaceComponent comp : project.getComponents()) {
            if (ids.contains(comp.getId())) {
                result.addError("Duplicate component ID: " + comp.getId() + " (" + comp.getName() + ")");
            }
            ids.add(comp.getId());
        }
        
        // Check for ID conflicts with interface ID
        if (project.getComponents().stream().anyMatch(c -> c.getId() == project.getInterfaceId())) {
            result.addError("Component ID conflicts with interface ID: " + project.getInterfaceId());
        }
        
        // Check for ID range issues
        if (!project.getComponents().isEmpty()) {
            int minId = project.getComponents().stream().mapToInt(InterfaceComponent::getId).min().getAsInt();
            int maxId = project.getComponents().stream().mapToInt(InterfaceComponent::getId).max().getAsInt();
            
            if (minId <= project.getInterfaceId()) {
                result.addWarning("Component IDs should be greater than interface ID");
            }
            
            if (maxId - minId > 100) {
                result.addWarning("Large ID range detected (" + minId + " to " + maxId + "). Consider using a more compact range.");
            }
        }
        
        // Check for empty sprite paths
        for (InterfaceComponent comp : project.getComponents()) {
            if (comp instanceof SpriteComponent) {
                SpriteComponent sprite = (SpriteComponent) comp;
                if (sprite.getSpritePath() == null || sprite.getSpritePath().isEmpty()) {
                    result.addError("Sprite component '" + sprite.getName() + "' has empty sprite path");
                }
            }
            
            if (comp instanceof ButtonComponent) {
                ButtonComponent button = (ButtonComponent) comp;
                if (button.getNormalSpritePath() == null || button.getNormalSpritePath().isEmpty()) {
                    result.addError("Button component '" + button.getName() + "' has empty normal sprite path");
                }
                if (button.getHoveredSpritePath() == null || button.getHoveredSpritePath().isEmpty()) {
                    result.addError("Button component '" + button.getName() + "' has empty hovered sprite path");
                }
            }
        }
        
        // Check for components outside canvas bounds
        for (InterfaceComponent comp : project.getComponents()) {
            if (comp.getX() < 0 || comp.getY() < 0) {
                result.addWarning("Component '" + comp.getName() + "' has negative position");
            }
            if (comp.getX() + comp.getWidth() > 512) {
                result.addWarning("Component '" + comp.getName() + "' extends beyond canvas width (512)");
            }
            if (comp.getY() + comp.getHeight() > 334) {
                result.addWarning("Component '" + comp.getName() + "' extends beyond canvas height (334)");
            }
        }
        
        // Check for overlapping components
        List<InterfaceComponent> components = project.getComponents();
        for (int i = 0; i < components.size(); i++) {
            for (int j = i + 1; j < components.size(); j++) {
                InterfaceComponent a = components.get(i);
                InterfaceComponent b = components.get(j);
                
                if (rectanglesOverlap(a.getX(), a.getY(), a.getWidth(), a.getHeight(),
                                     b.getX(), b.getY(), b.getWidth(), b.getHeight())) {
                    result.addWarning("Components overlap: '" + a.getName() + "' and '" + b.getName() + "'");
                }
            }
        }
        
        // Check for empty text
        for (InterfaceComponent comp : project.getComponents()) {
            if (comp instanceof TextComponent) {
                TextComponent text = (TextComponent) comp;
                if (text.getText() == null || text.getText().isEmpty()) {
                    result.addWarning("Text component '" + text.getName() + "' has empty text");
                }
            }
        }
        
        // Check for missing action names on buttons
        for (InterfaceComponent comp : project.getComponents()) {
            if (comp instanceof ButtonComponent) {
                ButtonComponent button = (ButtonComponent) comp;
                if (button.getActionName() == null || button.getActionName().isEmpty()) {
                    result.addWarning("Button component '" + button.getName() + "' has no action name");
                }
            }
        }
        
        return result;
    }
    
    private static boolean rectanglesOverlap(int x1, int y1, int w1, int h1,
                                               int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }
}
