package com.rsps.interfacemaker.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.rsps.interfacemaker.model.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.List;

public class ProjectSerializer {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void saveProject(InterfaceProject project, File file) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(project, writer);
        }
    }

    public static InterfaceProject loadProject(File file) throws IOException {
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, InterfaceProject.class);
        }
    }
}
