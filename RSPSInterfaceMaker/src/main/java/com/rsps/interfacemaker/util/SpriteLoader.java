package com.rsps.interfacemaker.util;

import javafx.scene.image.Image;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SpriteLoader {
    private static Map<String, Image> spriteCache = new HashMap<>();
    private static String spriteRootDirectory = "";
    
    public static void setSpriteRootDirectory(String path) {
        spriteRootDirectory = path;
        clearCache(); // Clear cache when root directory changes
    }
    
    public static String getSpriteRootDirectory() {
        return spriteRootDirectory;
    }
    
    public static Image loadSprite(String spritePath) {
        return loadSprite(spritePath, 0); // Default sprite ID
    }
    
    public static Image loadSprite(String spritePath, int spriteId) {
        if (spritePath == null || spritePath.isEmpty()) {
            return null;
        }
        
        // Create cache key with sprite ID
        String cacheKey = spritePath + "_" + spriteId;
        
        // Check cache first
        if (spriteCache.containsKey(cacheKey)) {
            return spriteCache.get(cacheKey);
        }
        
        // Try to load the sprite
        Image image = null;
        
        // RSPS naming convention: path + space + spriteId + .png (e.g., "MAIN 0.png")
        // Also try standard naming: path + .png (e.g., "MAIN.png")
        String[] variations = {
            spritePath + " " + spriteId + ".png",        // RSPS format: "MAIN 0.png"
            spritePath + " " + spriteId + ".PNG",        // RSPS format uppercase
            spritePath + spriteId + ".png",              // No space: "MAIN0.png"
            spritePath + ".png",                         // Standard: "MAIN.png"
            spritePath + ".PNG"                          // Standard uppercase
        };
        
        for (String variation : variations) {
            try {
                String fullPath = variation.replace("/", File.separator);
                File spriteFile;
                
                if (!spriteRootDirectory.isEmpty()) {
                    spriteFile = new File(spriteRootDirectory, fullPath);
                } else {
                    spriteFile = new File(fullPath);
                }
                
                if (spriteFile.exists()) {
                    image = new Image(spriteFile.toURI().toString());
                    System.out.println("Loaded sprite: " + spriteFile.getAbsolutePath());
                    break;
                }
            } catch (Exception e) {
                System.out.println("Failed to load sprite: " + variation + " - " + e.getMessage());
            }
        }
        
        if (image == null) {
            System.out.println("Sprite not found: " + spritePath + " (spriteId: " + spriteId + ", Root: " + spriteRootDirectory + ")");
        }
        
        // Cache the result (even if null to avoid repeated failed loads)
        spriteCache.put(cacheKey, image);
        
        return image;
    }
    
    public static void clearCache() {
        spriteCache.clear();
    }
    
    public static boolean spriteExists(String spritePath) {
        return loadSprite(spritePath) != null;
    }
}