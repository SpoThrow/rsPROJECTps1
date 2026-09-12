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
        if (spritePath == null || spritePath.isEmpty()) {
            return null;
        }
        
        // Check cache first
        if (spriteCache.containsKey(spritePath)) {
            return spriteCache.get(spritePath);
        }
        
        // Try to load the sprite
        Image image = null;
        
        // Try with different file extensions
        String[] extensions = {".png", ".PNG", ".gif", ".GIF", ".jpg", ".JPG"};
        
        for (String ext : extensions) {
            try {
                String fullPath = spritePath.replace("/", File.separator);
                File spriteFile;
                
                if (!spriteRootDirectory.isEmpty()) {
                    spriteFile = new File(spriteRootDirectory, fullPath + ext);
                } else {
                    spriteFile = new File(fullPath + ext);
                }
                
                if (spriteFile.exists()) {
                    image = new Image(spriteFile.toURI().toString());
                    break;
                }
            } catch (Exception e) {
                // Try next extension
            }
        }
        
        // Cache the result (even if null to avoid repeated failed loads)
        spriteCache.put(spritePath, image);
        
        return image;
    }
    
    public static void clearCache() {
        spriteCache.clear();
    }
    
    public static boolean spriteExists(String spritePath) {
        return loadSprite(spritePath) != null;
    }
}