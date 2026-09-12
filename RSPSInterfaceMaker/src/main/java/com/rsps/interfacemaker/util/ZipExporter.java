package com.rsps.interfacemaker.util;

import com.rsps.interfacemaker.generator.CodeGenerator;
import com.rsps.interfacemaker.model.InterfaceProject;
import com.rsps.interfacemaker.model.InterfaceComponent;
import com.rsps.interfacemaker.model.SpriteComponent;
import com.rsps.interfacemaker.model.ButtonComponent;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import java.util.*;

public class ZipExporter {
    
    /**
     * Export interface as a complete zip package
     * @param project The interface project to export
     * @param spriteSourceDir Directory containing source sprite files (can be null)
     * @param outputPath Path for the output zip file
     * @throws IOException if export fails
     */
    public static void exportToZip(InterfaceProject project, String spriteSourceDir, String outputPath) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputPath))) {
            // Add Java method file
            addJavaMethod(project, zipOut);
            
            // Add implementation guide
            addImplementationGuide(project, zipOut);
            
            // Add sprite files if source directory provided
            if (spriteSourceDir != null && !spriteSourceDir.isEmpty()) {
                addSpriteFiles(project, spriteSourceDir, zipOut);
            }
            
            // Add summary file
            addSummary(project, zipOut);
        }
    }
    
    private static void addJavaMethod(InterfaceProject project, ZipOutputStream zipOut) throws IOException {
        CodeGenerator generator = new CodeGenerator(project);
        String javaCode = generator.generateInterfaceMethod();
        
        ZipEntry entry = new ZipEntry(project.getName() + ".java");
        zipOut.putNextEntry(entry);
        zipOut.write(javaCode.getBytes());
        zipOut.closeEntry();
    }
    
    private static void addImplementationGuide(InterfaceProject project, ZipOutputStream zipOut) throws IOException {
        CodeGenerator generator = new CodeGenerator(project);
        String guide = generator.generateImplementationGuide();
        
        ZipEntry entry = new ZipEntry("Implementation_Guide.txt");
        zipOut.putNextEntry(entry);
        zipOut.write(guide.getBytes());
        zipOut.closeEntry();
    }
    
    private static void addSpriteFiles(InterfaceProject project, String sourceDir, ZipOutputStream zipOut) throws IOException {
        Set<String> spritePaths = new HashSet<>();
        
        // Collect all sprite paths from components
        for (InterfaceComponent comp : project.getComponents()) {
            if (comp instanceof SpriteComponent) {
                SpriteComponent sprite = (SpriteComponent) comp;
                if (sprite.getSpritePath() != null && !sprite.getSpritePath().isEmpty()) {
                    spritePaths.add(sprite.getSpritePath());
                }
                if (sprite.isHasDisabledSprite() && sprite.getDisabledSpritePath() != null) {
                    spritePaths.add(sprite.getDisabledSpritePath());
                }
            } else if (comp instanceof ButtonComponent) {
                ButtonComponent button = (ButtonComponent) comp;
                if (button.getNormalSpritePath() != null && !button.getNormalSpritePath().isEmpty()) {
                    spritePaths.add(button.getNormalSpritePath());
                }
                if (button.getHoveredSpritePath() != null && !button.getHoveredSpritePath().isEmpty()) {
                    spritePaths.add(button.getHoveredSpritePath());
                }
            }
        }
        
        // Copy sprite files to zip
        File sourceFile = new File(sourceDir);
        for (String spritePath : spritePaths) {
            // Convert path to file system path
            String fsPath = spritePath.replace("/", File.separator);
            File spriteFile = new File(sourceFile, fsPath);
            
            if (spriteFile.exists()) {
                // Add sprite to zip with proper structure
                String zipPath = "sprites/" + spritePath;
                addFileToZip(spriteFile, zipPath, zipOut);
            } else {
                // Try with different extensions
                for (String ext : new String[]{".png", ".PNG", ".gif", ".GIF"}) {
                    File extFile = new File(sourceFile, fsPath + ext);
                    if (extFile.exists()) {
                        String zipPath = "sprites/" + spritePath + ext;
                        addFileToZip(extFile, zipPath, zipOut);
                        break;
                    }
                }
            }
        }
    }
    
    private static void addFileToZip(File file, String zipPath, ZipOutputStream zipOut) throws IOException {
        ZipEntry entry = new ZipEntry(zipPath);
        zipOut.putNextEntry(entry);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zipOut.write(buffer, 0, length);
            }
        }
        
        zipOut.closeEntry();
    }
    
    private static void addSummary(InterfaceProject project, ZipOutputStream zipOut) throws IOException {
        StringBuilder summary = new StringBuilder();
        summary.append("Interface Export Summary\n");
        summary.append("========================\n\n");
        summary.append("Interface Name: ").append(project.getName()).append("\n");
        summary.append("Interface ID: ").append(project.getInterfaceId()).append("\n");
        summary.append("Sprite Folder: ").append(project.getSpriteFolder()).append("\n");
        summary.append("Total Components: ").append(project.getComponents().size()).append("\n\n");
        
        summary.append("Component Breakdown:\n");
        summary.append("-------------------\n");
        for (InterfaceComponent comp : project.getComponents()) {
            summary.append("- ").append(comp.getName())
                  .append(" (ID: ").append(comp.getId())
                  .append(", Type: ").append(comp.getType())
                  .append(", Pos: ").append(comp.getX()).append(",").append(comp.getY())
                  .append(", Size: ").append(comp.getWidth()).append("x").append(comp.getHeight())
                  .append(")\n");
        }
        
        summary.append("\nGenerated by RSPS Interface Maker\n");
        
        ZipEntry entry = new ZipEntry("EXPORT_SUMMARY.txt");
        zipOut.putNextEntry(entry);
        zipOut.write(summary.toString().getBytes());
        zipOut.closeEntry();
    }
    
    /**
     * Get the required sprite paths for the project
     * @param project The interface project
     * @return Set of sprite paths
     */
    public static Set<String> getRequiredSpritePaths(InterfaceProject project) {
        Set<String> spritePaths = new HashSet<>();
        
        for (InterfaceComponent comp : project.getComponents()) {
            if (comp instanceof SpriteComponent) {
                SpriteComponent sprite = (SpriteComponent) comp;
                if (sprite.getSpritePath() != null && !sprite.getSpritePath().isEmpty()) {
                    spritePaths.add(sprite.getSpritePath());
                }
                if (sprite.isHasDisabledSprite() && sprite.getDisabledSpritePath() != null) {
                    spritePaths.add(sprite.getDisabledSpritePath());
                }
            } else if (comp instanceof ButtonComponent) {
                ButtonComponent button = (ButtonComponent) comp;
                if (button.getNormalSpritePath() != null && !button.getNormalSpritePath().isEmpty()) {
                    spritePaths.add(button.getNormalSpritePath());
                }
                if (button.getHoveredSpritePath() != null && !button.getHoveredSpritePath().isEmpty()) {
                    spritePaths.add(button.getHoveredSpritePath());
                }
            }
        }
        
        return spritePaths;
    }
}