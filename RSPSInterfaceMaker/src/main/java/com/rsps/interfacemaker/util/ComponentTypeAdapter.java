package com.rsps.interfacemaker.util;

import com.google.gson.*;
import com.rsps.interfacemaker.model.ComponentType;
import java.lang.reflect.Type;

public class ComponentTypeAdapter implements JsonSerializer<ComponentType>, JsonDeserializer<ComponentType> {
    @Override
    public JsonElement serialize(ComponentType type, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(type.name());
    }

    @Override
    public ComponentType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return ComponentType.valueOf(json.getAsString());
        } catch (IllegalArgumentException e) {
            return ComponentType.SPRITE; // Default fallback
        }
    }
}