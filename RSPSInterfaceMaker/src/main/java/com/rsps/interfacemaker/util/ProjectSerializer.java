package com.rsps.interfacemaker.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.rsps.interfacemaker.model.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.List;

public class ProjectSerializer {
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(ComponentType.class, new ComponentTypeAdapter())
        .create();

    public static void saveProject(InterfaceProject project, File file) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(project, writer);
        }
    }

    public static InterfaceProject loadProject(File file) throws IOException {
        try (Reader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            
            InterfaceProject project = new InterfaceProject();
            project.setName(json.get("name").getAsString());
            project.setInterfaceId(json.get("interfaceId").getAsInt());
            project.setSpriteFolder(json.get("spriteFolder").getAsString());
            project.setNextComponentId(json.get("nextComponentId").getAsInt());
            
            if (json.has("spriteRootDirectory")) {
                project.setSpriteRootDirectory(json.get("spriteRootDirectory").getAsString());
            }
            if (json.has("cachePath")) {
                project.setCachePath(json.get("cachePath").getAsString());
            }
            if (json.has("offsetX")) {
                project.setOffsetX(json.get("offsetX").getAsInt());
            }
            if (json.has("offsetY")) {
                project.setOffsetY(json.get("offsetY").getAsInt());
            }
            if (json.has("interfacesFilePath")) {
                project.setInterfacesFilePath(json.get("interfacesFilePath").getAsString());
            }
            
            JsonArray componentsArray = json.getAsJsonArray("components");
            for (int i = 0; i < componentsArray.size(); i++) {
                JsonObject compJson = componentsArray.get(i).getAsJsonObject();
                ComponentType type = ComponentType.valueOf(compJson.get("type").getAsString());
                InterfaceComponent comp = deserializeComponent(compJson, type);
                project.addComponent(comp);
            }
            
            return project;
        }
    }
    
    private static InterfaceComponent deserializeComponent(JsonObject json, ComponentType type) {
        switch (type) {
            case SPRITE:
                SpriteComponent sprite = new SpriteComponent();
                sprite.setId(json.get("id").getAsInt());
                sprite.setX(json.get("x").getAsInt());
                sprite.setY(json.get("y").getAsInt());
                sprite.setWidth(json.get("width").getAsInt());
                sprite.setHeight(json.get("height").getAsInt());
                sprite.setName(json.get("name").getAsString());
                sprite.setTooltip(json.get("tooltip").getAsString());
                sprite.setParentInterfaceId(json.get("parentInterfaceId").getAsInt());
                sprite.setChildIndex(json.get("childIndex").getAsInt());
                if (json.has("spritePath")) {
                    sprite.setSpritePath(json.get("spritePath").getAsString());
                }
                if (json.has("spriteId")) {
                    sprite.setSpriteId(json.get("spriteId").getAsInt());
                }
                if (json.has("hasDisabledSprite")) {
                    sprite.setHasDisabledSprite(json.get("hasDisabledSprite").getAsBoolean());
                }
                if (json.has("disabledSpritePath")) {
                    sprite.setDisabledSpritePath(json.get("disabledSpritePath").getAsString());
                }
                return sprite;
            case HOVER_BUTTON:
            case HOVERED_BUTTON:
            case CLOSE_BUTTON:
                ButtonComponent button = new ButtonComponent();
                button.setId(json.get("id").getAsInt());
                button.setX(json.get("x").getAsInt());
                button.setY(json.get("y").getAsInt());
                button.setWidth(json.get("width").getAsInt());
                button.setHeight(json.get("height").getAsInt());
                button.setName(json.get("name").getAsString());
                button.setTooltip(json.get("tooltip").getAsString());
                button.setParentInterfaceId(json.get("parentInterfaceId").getAsInt());
                button.setChildIndex(json.get("childIndex").getAsInt());
                if (json.has("normalSpritePath")) {
                    button.setNormalSpritePath(json.get("normalSpritePath").getAsString());
                }
                if (json.has("normalSpriteId")) {
                    button.setNormalSpriteId(json.get("normalSpriteId").getAsInt());
                }
                if (json.has("hoveredSpritePath")) {
                    button.setHoveredSpritePath(json.get("hoveredSpritePath").getAsString());
                }
                if (json.has("hoveredSpriteId")) {
                    button.setHoveredSpriteId(json.get("hoveredSpriteId").getAsInt());
                }
                if (json.has("actionName")) {
                    button.setActionName(json.get("actionName").getAsString());
                }
                if (json.has("actionId")) {
                    button.setActionId(json.get("actionId").getAsInt());
                }
                return button;
            case TEXT:
                TextComponent text = new TextComponent();
                text.setId(json.get("id").getAsInt());
                text.setX(json.get("x").getAsInt());
                text.setY(json.get("y").getAsInt());
                text.setWidth(json.get("width").getAsInt());
                text.setHeight(json.get("height").getAsInt());
                text.setName(json.get("name").getAsString());
                text.setTooltip(json.get("tooltip").getAsString());
                text.setParentInterfaceId(json.get("parentInterfaceId").getAsInt());
                text.setChildIndex(json.get("childIndex").getAsInt());
                if (json.has("text")) {
                    text.setText(json.get("text").getAsString());
                }
                if (json.has("fontIndex")) {
                    text.setFontIndex(json.get("fontIndex").getAsInt());
                }
                if (json.has("textColor")) {
                    text.setTextColor(json.get("textColor").getAsInt());
                }
                if (json.has("hasShadow")) {
                    text.setHasShadow(json.get("hasShadow").getAsBoolean());
                }
                if (json.has("centered")) {
                    text.setCentered(json.get("centered").getAsBoolean());
                }
                return text;
            default:
                InterfaceComponent comp = new InterfaceComponent(type);
                comp.setId(json.get("id").getAsInt());
                comp.setX(json.get("x").getAsInt());
                comp.setY(json.get("y").getAsInt());
                comp.setWidth(json.get("width").getAsInt());
                comp.setHeight(json.get("height").getAsInt());
                comp.setName(json.get("name").getAsString());
                comp.setTooltip(json.get("tooltip").getAsString());
                comp.setParentInterfaceId(json.get("parentInterfaceId").getAsInt());
                comp.setChildIndex(json.get("childIndex").getAsInt());
                return comp;
        }
    }
}
