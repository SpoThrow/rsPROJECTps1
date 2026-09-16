package com.rsps.interfacemaker.util;

import com.rsps.interfacemaker.model.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class JavaInterfaceParser {
    
    /**
     * Parse an interface method from Interfaces.java file
     * @param interfacesFilePath Path to Interfaces.java
     * @param methodName Name of the interface method to parse
     * @return InterfaceProject or null if not found
     */
    public static InterfaceProject parseInterfaceMethod(String interfacesFilePath, String methodName) {
        try {
            File file = new File(interfacesFilePath);
            if (!file.exists()) {
                System.err.println("File not found: " + interfacesFilePath);
                return null;
            }
            
            String content = readFile(file);
            System.out.println("File read successfully, length: " + content.length());
            
            // Find the interface method - more flexible pattern
            String methodPattern = "public static void " + methodName + "\\(TextDrawingArea\\[\\] tda\\)\\s*\\{([^}]+)\\}";
            Pattern pattern = Pattern.compile(methodPattern, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);
            
            if (!matcher.find()) {
                System.err.println("Method not found: " + methodName);
                System.err.println("Available methods: " + getAvailableMethods(interfacesFilePath));
                return null;
            }
            
            String methodBody = matcher.group(1);
            System.out.println("Method body found, length: " + methodBody.length());
            System.out.println("Method body preview: " + methodBody.substring(0, Math.min(200, methodBody.length())));
            
            // Extract interface ID from addInterface call
            int interfaceId = extractInterfaceId(methodBody);
            if (interfaceId == -1) {
                System.err.println("Interface ID not found in method body");
                return null;
            }
            
            System.out.println("Interface ID extracted: " + interfaceId);
            
            // Create project
            InterfaceProject project = new InterfaceProject();
            project.setInterfaceId(interfaceId);
            project.setName(methodName);
            
            // Parse components from method body
            parseComponents(methodBody, project);
            System.out.println("Total components parsed: " + project.getComponents().size());
            
            return project;
            
        } catch (Exception e) {
            System.err.println("Error parsing interface method: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Extract interface ID from addInterface call
     */
    private static int extractInterfaceId(String methodBody) {
        Pattern pattern = Pattern.compile("addInterface\\((\\d+)\\)");
        Matcher matcher = pattern.matcher(methodBody);
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        return -1;
    }
    
    /**
     * Parse components from method body
     */
    private static void parseComponents(String methodBody, InterfaceProject project) {
        System.out.println("Starting component parsing...");
        
        // Parse sprite declarations
        parseSprites(methodBody, project);
        System.out.println("Sprites parsed: " + project.getComponents().size());
        
        // Parse button declarations
        parseButtons(methodBody, project);
        System.out.println("Buttons parsed, total components: " + project.getComponents().size());
        
        // Parse text declarations
        parseText(methodBody, project);
        System.out.println("Text parsed, total components: " + project.getComponents().size());
        
        // Parse child positioning using setBounds
        parseChildPositioning(methodBody, project);
        System.out.println("Child positioning applied");
    }
    
    /**
     * Parse sprite components
     */
    private static void parseSprites(String methodBody, InterfaceProject project) {
        Pattern pattern = Pattern.compile("addSprite\\((\\d+),\\s*(\\d+),\\s*\"([^\"]+)\"\\)");
        Matcher matcher = pattern.matcher(methodBody);
        
        int spriteCount = 0;
        while (matcher.find()) {
            spriteCount++;
            int id = Integer.parseInt(matcher.group(1));
            int spriteId = Integer.parseInt(matcher.group(2));
            String spritePath = matcher.group(3);
            
            System.out.println("Found sprite: ID=" + id + ", spriteId=" + spriteId + ", path=" + spritePath);
            
            SpriteComponent sprite = new SpriteComponent();
            sprite.setId(id);
            sprite.setSpritePath(spritePath);
            sprite.setSpriteId(spriteId);
            sprite.setName("Sprite_" + id);
            sprite.setWidth(100); // Default size
            sprite.setHeight(20);
            
            project.addComponent(sprite);
        }
        System.out.println("Total sprites parsed: " + spriteCount);
    }
    
    /**
     * Parse button components
     */
    private static void parseButtons(String methodBody, InterfaceProject project) {
        // Parse hover buttons - pattern: addHoverButton(id, path, spriteId, width, height, tooltip, type, hoverId, config)
        // The actual format in your code: addHoverButton(50006, "Interfaces/POS/BUY", 8, 16, 16, "Close", 0, 50007, 1);
        Pattern hoverPattern = Pattern.compile("addHoverButton\\((\\d+),\\s*\"([^\"]+)\",\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*\"([^\"]+)\",\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\)");
        Matcher hoverMatcher = hoverPattern.matcher(methodBody);
        
        int buttonCount = 0;
        while (hoverMatcher.find()) {
            buttonCount++;
            int id = Integer.parseInt(hoverMatcher.group(1));
            String spritePath = hoverMatcher.group(2);
            int spriteId = Integer.parseInt(hoverMatcher.group(3));
            int width = Integer.parseInt(hoverMatcher.group(4));
            int height = Integer.parseInt(hoverMatcher.group(5));
            String tooltip = hoverMatcher.group(6);
            
            System.out.println("Found button: ID=" + id + ", path=" + spritePath + ", spriteId=" + spriteId + ", size=" + width + "x" + height);
            
            ButtonComponent button = new ButtonComponent();
            button.setId(id);
            button.setNormalSpritePath(spritePath);
            button.setNormalSpriteId(spriteId);
            button.setWidth(width);
            button.setHeight(height);
            button.setTooltip(tooltip);
            button.setName("Button_" + id);
            
            project.addComponent(button);
        }
        
        if (buttonCount == 0) {
            System.out.println("No buttons found with standard pattern, checking method body:");
            System.out.println("Method body preview: " + methodBody.substring(0, Math.min(200, methodBody.length())));
            // Try to find any addHoverButton calls to debug
            Pattern debugPattern = Pattern.compile("addHoverButton\\([^)]+\\)");
            Matcher debugMatcher = debugPattern.matcher(methodBody);
            if (debugMatcher.find()) {
                System.out.println("Found addHoverButton call: " + debugMatcher.group());
            }
        }
        
        System.out.println("Total buttons parsed: " + buttonCount);
    }
    
    /**
     * Parse text components
     */
    private static void parseText(String methodBody, InterfaceProject project) {
        // Pattern: addText(id, "text", tda, font, color, centered, shadow)
        Pattern pattern = Pattern.compile("addText\\((\\d+),\\s*\"([^\"]+)\",\\s*tda,\\s*(\\d+),\\s*(0x[0-9A-Fa-f]+|\\d+),\\s*(true|false),\\s*(true|false)\\)");
        Matcher matcher = pattern.matcher(methodBody);
        
        int textCount = 0;
        while (matcher.find()) {
            textCount++;
            int id = Integer.parseInt(matcher.group(1));
            String text = matcher.group(2);
            int fontIndex = Integer.parseInt(matcher.group(3));
            String colorStr = matcher.group(4);
            boolean centered = Boolean.parseBoolean(matcher.group(5));
            boolean shadow = Boolean.parseBoolean(matcher.group(6));
            
            int color = colorStr.startsWith("0x") ? Integer.parseInt(colorStr.substring(2), 16) : Integer.parseInt(colorStr);
            
            System.out.println("Found text: ID=" + id + ", text=" + text + ", font=" + fontIndex + ", color=" + color);
            
            TextComponent textComponent = new TextComponent();
            textComponent.setId(id);
            textComponent.setText(text);
            textComponent.setFontIndex(fontIndex);
            textComponent.setTextColor(color);
            textComponent.setCentered(centered);
            textComponent.setHasShadow(shadow);
            textComponent.setName("Text_" + id);
            textComponent.setWidth(100);
            textComponent.setHeight(20);
            
            project.addComponent(textComponent);
        }
        System.out.println("Total text components parsed: " + textCount);
    }
    
    /**
     * Parse child positioning using setBounds or inter.child and apply to components
     */
    private static void parseChildPositioning(String methodBody, InterfaceProject project) {
        // Pattern for setBounds(childId, componentId, x, y, parent)
        Pattern setBoundsPattern = Pattern.compile("setBounds\\((\\d+),\\s*(\\d+),\\s*(-?\\d+),\\s*(-?\\d+),\\s*(\\w+)\\)");
        Matcher setBoundsMatcher = setBoundsPattern.matcher(methodBody);
        
        // Pattern for inter.child(childId, componentId, x, y)
        Pattern childPattern = Pattern.compile("(?:inter|rsi)\\.child\\((\\d+),\\s*(\\d+),\\s*(-?\\d+),\\s*(-?\\d+)\\)");
        Matcher childMatcher = childPattern.matcher(methodBody);
        
        int boundsFound = 0;
        int childFound = 0;
        int componentsUpdated = 0;
        
        // Process setBounds calls
        while (setBoundsMatcher.find()) {
            boundsFound++;
            int childIndex = Integer.parseInt(setBoundsMatcher.group(1));
            int componentId = Integer.parseInt(setBoundsMatcher.group(2));
            int x = Integer.parseInt(setBoundsMatcher.group(3));
            int y = Integer.parseInt(setBoundsMatcher.group(4));
            String parent = setBoundsMatcher.group(5);
            
            System.out.println("setBounds found: child=" + childIndex + ", componentId=" + componentId + ", x=" + x + ", y=" + y + ", parent=" + parent);
            
            // Find component by ID and set position
            for (InterfaceComponent comp : project.getComponents()) {
                if (comp.getId() == componentId) {
                    comp.setX(x);
                    comp.setY(y);
                    componentsUpdated++;
                    System.out.println("  -> Updated component " + comp.getName() + " to position (" + x + ", " + y + ")");
                    break;
                }
            }
        }
        
        // Process inter.child calls
        while (childMatcher.find()) {
            childFound++;
            int childIndex = Integer.parseInt(childMatcher.group(1));
            int componentId = Integer.parseInt(childMatcher.group(2));
            int x = Integer.parseInt(childMatcher.group(3));
            int y = Integer.parseInt(childMatcher.group(4));
            
            System.out.println("inter.child found: child=" + childIndex + ", componentId=" + componentId + ", x=" + x + ", y=" + y);
            
            // Find component by ID and set position
            for (InterfaceComponent comp : project.getComponents()) {
                if (comp.getId() == componentId) {
                    comp.setX(x);
                    comp.setY(y);
                    componentsUpdated++;
                    System.out.println("  -> Updated component " + comp.getName() + " to position (" + x + ", " + y + ")");
                    break;
                }
            }
        }
        
        System.out.println("Total setBounds calls found: " + boundsFound);
        System.out.println("Total inter.child calls found: " + childFound);
        System.out.println("Total components position updated: " + componentsUpdated);
    }
    
    /**
     * Read file content
     */
    private static String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    /**
     * Get list of available interface method names from Interfaces.java
     */
    public static List<String> getAvailableMethods(String interfacesFilePath) {
        List<String> methods = new ArrayList<>();
        
        try {
            File file = new File(interfacesFilePath);
            if (!file.exists()) {
                return methods;
            }
            
            String content = readFile(file);
            
            // Find all public static void methods that take TextDrawingArea[] parameter
            Pattern pattern = Pattern.compile("public static void (\\w+)\\(TextDrawingArea\\[\\] tda\\)");
            Matcher matcher = pattern.matcher(content);
            
            while (matcher.find()) {
                methods.add(matcher.group(1));
            }
            
        } catch (Exception e) {
            System.err.println("Error reading interface methods: " + e.getMessage());
        }
        
        return methods;
    }
}