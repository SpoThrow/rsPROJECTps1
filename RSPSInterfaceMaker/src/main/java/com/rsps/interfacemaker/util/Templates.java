package com.rsps.interfacemaker.util;

import com.rsps.interfacemaker.model.*;
import java.util.ArrayList;
import java.util.List;

public class Templates {
    
    public static InterfaceProject createEmptyTemplate() {
        InterfaceProject project = new InterfaceProject();
        project.setName("NewInterface");
        project.setInterfaceId(45000);
        project.setSpriteFolder("Interfaces/NewInterface");
        return project;
    }
    
    public static InterfaceProject createBasicDialogTemplate() {
        InterfaceProject project = new InterfaceProject();
        project.setName("BasicDialog");
        project.setInterfaceId(45000);
        project.setSpriteFolder("Interfaces/Dialog");
        
        // Background sprite
        SpriteComponent background = new SpriteComponent();
        background.setName("Background");
        background.setId(project.getNextComponentId());
        background.setX(0);
        background.setY(0);
        background.setWidth(512);
        background.setHeight(334);
        background.setSpritePath("Interfaces/Dialog/BACKGROUND");
        project.addComponent(background);
        
        // Close button
        ButtonComponent closeBtn = new ButtonComponent();
        closeBtn.setName("Close Button");
        closeBtn.setId(project.getNextComponentId());
        closeBtn.setX(475);
        closeBtn.setY(10);
        closeBtn.setWidth(20);
        closeBtn.setHeight(20);
        closeBtn.setNormalSpritePath("Interfaces/Common/CLOSE");
        closeBtn.setHoveredSpritePath("Interfaces/Common/CLOSE_HOVER");
        closeBtn.setTooltip("Close");
        closeBtn.setActionName("Close");
        project.addComponent(closeBtn);
        
        // Title
        TextComponent title = new TextComponent();
        title.setName("Title");
        title.setId(project.getNextComponentId());
        title.setX(20);
        title.setY(20);
        title.setWidth(200);
        title.setHeight(20);
        title.setText("Dialog Title");
        title.setFontIndex(0);
        title.setTextColor(0xFFFFFF);
        title.setHasShadow(true);
        title.setCentered(false);
        project.addComponent(title);
        
        return project;
    }
    
    public static InterfaceProject createShopTemplate() {
        InterfaceProject project = new InterfaceProject();
        project.setName("ShopInterface");
        project.setInterfaceId(45000);
        project.setSpriteFolder("Interfaces/Shop");
        
        // Background
        SpriteComponent background = new SpriteComponent();
        background.setName("Background");
        background.setId(project.getNextComponentId());
        background.setX(0);
        background.setY(0);
        background.setWidth(512);
        background.setHeight(334);
        background.setSpritePath("Interfaces/Shop/BACKGROUND");
        project.addComponent(background);
        
        // Close button
        ButtonComponent closeBtn = new ButtonComponent();
        closeBtn.setName("Close Button");
        closeBtn.setId(project.getNextComponentId());
        closeBtn.setX(475);
        closeBtn.setY(10);
        closeBtn.setWidth(20);
        closeBtn.setHeight(20);
        closeBtn.setNormalSpritePath("Interfaces/Common/CLOSE");
        closeBtn.setHoveredSpritePath("Interfaces/Common/CLOSE_HOVER");
        closeBtn.setTooltip("Close");
        closeBtn.setActionName("Close");
        project.addComponent(closeBtn);
        
        // Title
        TextComponent title = new TextComponent();
        title.setName("Title");
        title.setId(project.getNextComponentId());
        title.setX(20);
        title.setY(20);
        title.setWidth(200);
        title.setHeight(20);
        title.setText("Shop Name");
        title.setFontIndex(0);
        title.setTextColor(0xFFFFFF);
        title.setHasShadow(true);
        title.setCentered(false);
        project.addComponent(title);
        
        // Buy button
        ButtonComponent buyBtn = new ButtonComponent();
        buyBtn.setName("Buy Button");
        buyBtn.setId(project.getNextComponentId());
        buyBtn.setX(50);
        buyBtn.setY(100);
        buyBtn.setWidth(100);
        buyBtn.setHeight(25);
        buyBtn.setNormalSpritePath("Interfaces/Shop/BUTTON");
        buyBtn.setHoveredSpritePath("Interfaces/Shop/BUTTON_HOVER");
        buyBtn.setTooltip("Buy Item");
        buyBtn.setActionName("BuyItem");
        project.addComponent(buyBtn);
        
        return project;
    }
    
    public static InterfaceProject createTeleportMenuTemplate() {
        InterfaceProject project = new InterfaceProject();
        project.setName("TeleportMenu");
        project.setInterfaceId(45000);
        project.setSpriteFolder("Interfaces/Teleport");
        
        // Background
        SpriteComponent background = new SpriteComponent();
        background.setName("Background");
        background.setId(project.getNextComponentId());
        background.setX(0);
        background.setY(0);
        background.setWidth(512);
        background.setHeight(334);
        background.setSpritePath("Interfaces/Teleport/BACKGROUND");
        project.addComponent(background);
        
        // Close button
        ButtonComponent closeBtn = new ButtonComponent();
        closeBtn.setName("Close Button");
        closeBtn.setId(project.getNextComponentId());
        closeBtn.setX(475);
        closeBtn.setY(10);
        closeBtn.setWidth(20);
        closeBtn.setHeight(20);
        closeBtn.setNormalSpritePath("Interfaces/Common/CLOSE");
        closeBtn.setHoveredSpritePath("Interfaces/Common/CLOSE_HOVER");
        closeBtn.setTooltip("Close");
        closeBtn.setActionName("Close");
        project.addComponent(closeBtn);
        
        // Title
        TextComponent title = new TextComponent();
        title.setName("Title");
        title.setId(project.getNextComponentId());
        title.setX(20);
        title.setY(20);
        title.setWidth(200);
        title.setHeight(20);
        title.setText("Teleport Menu");
        title.setFontIndex(0);
        title.setTextColor(0xFFFFFF);
        title.setHasShadow(true);
        title.setCentered(false);
        project.addComponent(title);
        
        // Teleport buttons
        String[] teleportNames = {"Home", "Varrock", "Lumbridge", "Falador"};
        for (int i = 0; i < teleportNames.length; i++) {
            ButtonComponent teleportBtn = new ButtonComponent();
            teleportBtn.setName("Teleport " + teleportNames[i]);
            teleportBtn.setId(project.getNextComponentId());
            teleportBtn.setX(50);
            teleportBtn.setY(60 + (i * 35));
            teleportBtn.setWidth(120);
            teleportBtn.setHeight(30);
            teleportBtn.setNormalSpritePath("Interfaces/Teleport/BUTTON");
            teleportBtn.setHoveredSpritePath("Interfaces/Teleport/BUTTON_HOVER");
            teleportBtn.setTooltip("Teleport to " + teleportNames[i]);
            teleportBtn.setActionName("Teleport" + teleportNames[i]);
            project.addComponent(teleportBtn);
        }
        
        return project;
    }
}
