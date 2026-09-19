package com.rsps.interfacemaker.util;

import com.rsps.interfacemaker.model.InterfaceComponent;
import com.rsps.interfacemaker.model.InterfaceProject;
import com.rsps.interfacemaker.model.LoopBoundsBlock;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes moved widgets back into the existing Interfaces.java method.
 * Only setBounds coordinates are patched. Loops are expanded only when a child moved.
 */
public class JavaInterfaceWriter {

    public static String preview(InterfaceProject project) {
        StringBuilder out = new StringBuilder();
        out.append("// Save to Client patches setBounds in ")
            .append(project.getSourceMethodName())
            .append("() — it does not regenerate the method.\n");
        if (project.getInterfacesFilePath() != null && !project.getInterfacesFilePath().isEmpty()) {
            out.append("// File: ").append(project.getInterfacesFilePath()).append('\n');
        }
        out.append('\n');
        List<InterfaceComponent> components = new ArrayList<>(project.getComponents());
        components.sort(Comparator.comparingInt(InterfaceComponent::getChildIndex));
        for (InterfaceComponent component : components) {
            String parent = component.getParentVarName().isEmpty() ? "parent" : component.getParentVarName();
            out.append("setBounds(").append(component.getId()).append(", ")
                .append(component.getX()).append(", ").append(component.getY()).append(", ")
                .append(component.getChildIndex()).append(", ").append(parent).append(");");
            if (component.positionChanged()) {
                out.append(" // moved from ").append(component.getOriginalX()).append(", ")
                    .append(component.getOriginalY());
            }
            out.append('\n');
        }
        return out.toString();
    }

    public static void saveToClient(InterfaceProject project) throws Exception {
        if (project.getInterfacesFilePath() == null || project.getInterfacesFilePath().isEmpty()) {
            throw new IllegalStateException("No Interfaces.java path. Load from Client first.");
        }
        String methodName = project.getSourceMethodName();
        if (methodName == null || methodName.isEmpty()) {
            methodName = project.getName();
        }
        File file = new File(project.getInterfacesFilePath());
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        JavaInterfaceParser.MethodSpan span = JavaInterfaceParser.findMethod(content, methodName);
        if (span == null) {
            throw new IllegalStateException("Method not found in Interfaces.java: " + methodName);
        }

        String method = content.substring(span.start, span.end);
        method = expandMovedLoops(method, project);
        method = patchLiteralBounds(method, project);

        String updated = content.substring(0, span.start) + method + content.substring(span.end);
        Files.writeString(file.toPath(), updated, StandardCharsets.UTF_8);
    }

    private static String expandMovedLoops(String method, InterfaceProject project) {
        for (LoopBoundsBlock loop : project.getLoopBlocks()) {
            if (loop.getOriginalText() == null || loop.getOriginalText().isEmpty()) {
                continue;
            }
            boolean moved = false;
            List<InterfaceComponent> members = new ArrayList<>();
            for (InterfaceComponent component : project.getComponents()) {
                if (loop.getGroupId().equals(component.getLoopGroup())) {
                    members.add(component);
                    if (component.positionChanged()) {
                        moved = true;
                    }
                }
            }
            if (!moved || members.isEmpty()) {
                continue;
            }
            int at = method.indexOf(loop.getOriginalText());
            if (at < 0) {
                System.err.println("Could not find original loop to expand: " + loop.getGroupId());
                continue;
            }
            String nl = method.contains("\r\n") ? "\r\n" : "\n";
            members.sort(Comparator.comparingInt(InterfaceComponent::getChildIndex));
            String indent = "\t\t";
            StringBuilder expanded = new StringBuilder();
            for (int i = 0; i < members.size(); i++) {
                InterfaceComponent component = members.get(i);
                if (i > 0) {
                    expanded.append(nl).append(indent);
                }
                String parent = component.getParentVarName().isEmpty()
                    ? loop.getParentVarName() : component.getParentVarName();
                expanded.append("setBounds(").append(component.getId()).append(", ")
                    .append(component.getX()).append(", ").append(component.getY()).append(", ")
                    .append(component.getChildIndex()).append(", ").append(parent).append(");");
            }
            method = method.substring(0, at) + expanded + method.substring(at + loop.getOriginalText().length());
        }
        return method;
    }

    private static String patchLiteralBounds(String method, InterfaceProject project) {
        for (InterfaceComponent component : project.getComponents()) {
            if (component.isFromLoop() && !component.positionChanged()) {
                continue;
            }
            String parent = component.getParentVarName();
            if (parent == null || parent.isEmpty()) {
                parent = "\\w+";
            } else {
                parent = Pattern.quote(parent);
            }
            Pattern pattern = Pattern.compile(
                "setBounds\\(\\s*" + component.getId() + "\\s*,\\s*-?\\d+\\s*,\\s*-?\\d+\\s*,\\s*"
                    + component.getChildIndex() + "\\s*,\\s*" + parent + "\\s*\\)");
            Matcher matcher = pattern.matcher(method);
            if (!matcher.find()) {
                continue;
            }
            String parentName = component.getParentVarName().isEmpty() ? "bank" : component.getParentVarName();
            String replacement = "setBounds(" + component.getId() + ", " + component.getX() + ", "
                + component.getY() + ", " + component.getChildIndex() + ", " + parentName + ")";
            method = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        }
        return method;
    }
}
