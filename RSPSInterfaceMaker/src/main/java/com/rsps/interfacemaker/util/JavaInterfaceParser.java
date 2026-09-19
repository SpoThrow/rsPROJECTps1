package com.rsps.interfacemaker.util;

import com.rsps.interfacemaker.model.ButtonComponent;
import com.rsps.interfacemaker.model.ComponentType;
import com.rsps.interfacemaker.model.InterfaceComponent;
import com.rsps.interfacemaker.model.InterfaceProject;
import com.rsps.interfacemaker.model.LoopBoundsBlock;
import com.rsps.interfacemaker.model.SpriteComponent;
import com.rsps.interfacemaker.model.TextComponent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads a method from Interfaces.java with brace matching so nested blocks (bank tabs) parse.
 * Widget IDs from source are kept. Positions come from setBounds(ID, x, y, frame, parent).
 */
public class JavaInterfaceParser {
    private static final Pattern METHOD_SIG = Pattern.compile(
        "(?:public|private|protected)?\\s*static\\s+void\\s+(\\w+)\\s*\\(");

    private final Map<String, Integer> intVars = new HashMap<>();
    private final Map<String, Integer> ifaceVars = new HashMap<>();
    private final Map<String, int[]> arrays = new HashMap<>();
    private final Map<Integer, String> varNames = new HashMap<>();
    private final Map<String, Map<Integer, ChildSlot>> childSlots = new HashMap<>();
    private final Map<Integer, InterfaceComponent> widgets = new LinkedHashMap<>();
    private final List<Placement> placements = new ArrayList<>();
    private final List<LoopBoundsBlock> loopBlocks = new ArrayList<>();
    private int rootInterfaceId = -1;

    public static InterfaceProject parseInterfaceMethod(String interfacesFilePath, String methodName) {
        try {
            File file = new File(interfacesFilePath);
            if (!file.exists()) {
                System.err.println("File not found: " + interfacesFilePath);
                return null;
            }

            String content = readFile(file);
            MethodSpan span = findMethod(content, methodName);
            if (span == null) {
                System.err.println("Method not found: " + methodName);
                System.err.println("Available methods: " + getAvailableMethods(interfacesFilePath));
                return null;
            }

            JavaInterfaceParser parser = new JavaInterfaceParser();
            parser.scan(span.body);
            parser.flushChildSlots();
            if (parser.rootInterfaceId < 0) {
                System.err.println("Interface ID not found in method body");
                return null;
            }

            InterfaceProject project = new InterfaceProject();
            project.setInterfaceId(parser.rootInterfaceId);
            project.setName(methodName);
            project.setSourceMethodName(methodName);
            project.setInterfacesFilePath(interfacesFilePath);
            project.setClientLinked(true);
            project.setLoopBlocks(parser.loopBlocks);

            List<Placement> allPlacements = new ArrayList<>(parser.placements);
            allPlacements.sort(Comparator.comparingInt((Placement p) -> p.parentId == parser.rootInterfaceId ? 0 : 1)
                .thenComparingInt(p -> p.frame));

            for (Placement placement : allPlacements) {
                InterfaceComponent component = parser.widgets.get(placement.id);
                if (component == null) {
                    component = parser.placeholder(placement.id);
                    parser.widgets.put(placement.id, component);
                }
                component.setId(placement.id);
                component.setX(placement.x);
                component.setY(placement.y);
                component.setOriginalX(placement.x);
                component.setOriginalY(placement.y);
                component.setChildIndex(placement.frame);
                component.setFromLoop(placement.fromLoop);
                component.setLoopGroup(placement.loopGroup);
                component.setParentVarName(placement.parentVar);
                component.setParentInterfaceId(placement.parentId);
                String sourceVar = parser.varNames.get(placement.id);
                if (sourceVar != null) {
                    component.setSourceVarName(sourceVar);
                }
                component.setOriginalWidth(component.getWidth());
                component.setOriginalHeight(component.getHeight());
                component.setOriginalScrollMax(component.getScrollMax());
                if (project.findById(placement.id) == null) {
                    project.addExistingComponent(component);
                }
            }

            System.out.println("Parsed " + methodName + " id=" + parser.rootInterfaceId
                + " children=" + project.getComponents().size());
            return project;
        } catch (Exception e) {
            System.err.println("Error parsing interface method: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> getAvailableMethods(String interfacesFilePath) {
        List<String> methods = new ArrayList<>();
        try {
            String content = readFile(new File(interfacesFilePath));
            Matcher matcher = METHOD_SIG.matcher(content);
            while (matcher.find()) {
                String name = matcher.group(1);
                MethodSpan span = findMethod(content, name);
                if (span == null) {
                    continue;
                }
                if (span.body.contains("addInterface(") || span.body.contains("addTabInterface(")
                    || span.body.contains("addTab(")) {
                    methods.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading interface methods: " + e.getMessage());
        }
        methods.sort((a, b) -> {
            if ("bank".equals(a)) {
                return -1;
            }
            if ("bank".equals(b)) {
                return 1;
            }
            return a.compareToIgnoreCase(b);
        });
        return methods;
    }

    private void scan(String body) {
        int i = 0;
        while (i < body.length()) {
            i = skipWsComments(body, i);
            if (i >= body.length()) {
                break;
            }
            if (keywordAt(body, i, "if") || keywordAt(body, i, "while") || keywordAt(body, i, "switch")) {
                i = skipControl(body, i);
                continue;
            }
            if (keywordAt(body, i, "for")) {
                i = handleFor(body, i);
                continue;
            }
            int end = findStatementEnd(body, i);
            if (end < i) {
                break;
            }
            String stmt = body.substring(i, end).trim();
            if (stmt.endsWith(";")) {
                stmt = stmt.substring(0, stmt.length() - 1).trim();
            }
            interpret(stmt);
            i = end < body.length() && body.charAt(end) == ';' ? end + 1 : Math.max(end, i + 1);
        }
    }

    private int handleFor(String body, int start) {
        int headerOpen = skipWsComments(body, start + 3);
        if (headerOpen >= body.length() || body.charAt(headerOpen) != '(') {
            return start + 3;
        }
        int headerClose = matchParen(body, headerOpen);
        if (headerClose < 0) {
            return start + 3;
        }
        int bodyOpen = skipWsComments(body, headerClose + 1);
        if (bodyOpen >= body.length() || body.charAt(bodyOpen) != '{') {
            return start + 3;
        }
        int bodyClose = matchBrace(body, bodyOpen);
        if (bodyClose < 0) {
            return start + 3;
        }

        String header = body.substring(headerOpen + 1, headerClose);
        String loopBody = body.substring(bodyOpen + 1, bodyClose);
        String original = body.substring(start, bodyClose + 1);
        String groupId = "loop_" + start;
        String[] parts = splitTop(header, ';');
        if (parts.length != 3) {
            return bodyClose + 1;
        }

        String init = parts[0].trim();
        String loopVar = loopIndexName(init);
        String parentVar = "";
        try {
            interpret(init);
            evalCondition(parts[1].trim());
        } catch (RuntimeException e) {
            System.err.println("Skipping for-loop (" + e.getMessage() + "): " + header.trim());
            if (loopVar != null) {
                intVars.remove(loopVar);
            }
            return bodyClose + 1;
        }
        String cond = parts[1].trim();
        String incr = parts[2].trim();

        int guard = 0;
        try {
            while (evalCondition(cond) && guard++ < 512) {
                int before = placements.size();
                scan(loopBody);
                for (int p = before; p < placements.size(); p++) {
                    Placement placement = placements.get(p);
                    placement.fromLoop = true;
                    placement.loopGroup = groupId;
                    if (parentVar.isEmpty()) {
                        parentVar = placement.parentVar;
                    }
                }
                interpret(incr);
            }
        } catch (RuntimeException e) {
            System.err.println("Stopping for-loop (" + e.getMessage() + ")");
        }
        if (loopVar != null) {
            intVars.remove(loopVar);
        }

        loopBlocks.add(new LoopBoundsBlock(groupId, original, parentVar));
        return bodyClose + 1;
    }

    private void interpret(String stmt) {
        if (stmt == null || stmt.isEmpty()) {
            return;
        }
        stmt = stmt.replaceAll("\\s+", " ").trim();
        if (stmt.endsWith("++") || stmt.startsWith("++") || stmt.endsWith("--") || stmt.startsWith("--")) {
            try {
                evalExpr(stmt);
            } catch (RuntimeException ignored) {
            }
            return;
        }
        if (stmt.startsWith("RSInterface ")) {
            interpretIfaceAssign(stmt.substring("RSInterface ".length()).trim());
            return;
        }
        if (stmt.startsWith("int[]") || stmt.startsWith("int []")) {
            interpretArrayDecl(stmt);
            return;
        }
        if (stmt.startsWith("int ") && !stmt.contains("[")) {
            interpretIntAssign(stmt.substring(4).trim());
            return;
        }
        if (stmt.contains(".child(") && !stmt.contains("totalChildren") && !stmt.contains("setChildren")) {
            interpretChildCall(stmt);
            return;
        }
        if (stmt.startsWith("setBounds(") && stmt.endsWith(")")) {
            interpretSetBounds(insideCall(stmt, "setBounds"));
            return;
        }
        if (stmt.startsWith("addRectangle(")) {
            interpretAddRectangle(insideCall(stmt, "addRectangle"));
            return;
        }
        if (stmt.startsWith("addHoverText(")) {
            interpretAddHoverText(insideCall(stmt, "addHoverText"));
            return;
        }
        if (stmt.startsWith("addPosItemSlot(")) {
            interpretAddBankItem(insideCall(stmt, "addPosItemSlot"));
            return;
        }
        if (stmt.startsWith("addSprite(")) {
            interpretAddSprite(insideCall(stmt, "addSprite"));
            return;
        }
        if (stmt.startsWith("addHoverButton(")) {
            interpretAddHoverButton(insideCall(stmt, "addHoverButton"));
            return;
        }
        if (stmt.startsWith("addHoveredButton(")) {
            interpretAddHoveredButton(insideCall(stmt, "addHoveredButton"));
            return;
        }
        if (stmt.startsWith("addConfigButton(")) {
            interpretAddConfigButton(insideCall(stmt, "addConfigButton"));
            return;
        }
        if (stmt.startsWith("addHover(") && !stmt.startsWith("addHoverButton") && !stmt.startsWith("addHoverText")
            && !stmt.startsWith("addHoverBox") && !stmt.startsWith("addHovered")) {
            interpretAddHover(insideCall(stmt, "addHover"));
            return;
        }
        if (stmt.startsWith("addHovered(")) {
            interpretAddHovered(insideCall(stmt, "addHovered"));
            return;
        }
        if (stmt.startsWith("addButton(")) {
            interpretAddButton(insideCall(stmt, "addButton"));
            return;
        }
        if (stmt.startsWith("addText(")) {
            interpretAddText(insideCall(stmt, "addText"));
            return;
        }
        if (stmt.startsWith("addBankItem(")) {
            interpretAddBankItem(insideCall(stmt, "addBankItem"));
            return;
        }

        int dot = stmt.indexOf('.');
        int eq = stmt.indexOf('=');
        if (stmt.contains("interfaceCache[") && eq > 0) {
            interpretCacheField(stmt);
            return;
        }
        if (dot > 0 && eq > dot) {
            String var = stmt.substring(0, dot).trim();
            String field = stmt.substring(dot + 1, eq).trim();
            String value = stmt.substring(eq + 1).trim();
            if (field.startsWith("children[") || field.startsWith("childX[") || field.startsWith("childY[")) {
                interpretIndexedChildField(var, field, value);
                return;
            }
            Integer id = ifaceVars.get(var);
            if (id != null) {
                InterfaceComponent component = widgets.get(id);
                if (component == null) {
                    component = placeholder(id);
                    widgets.put(id, component);
                }
                try {
                    int n = evalExpr(value);
                    if ("width".equals(field)) {
                        component.setWidth(n);
                    } else if ("height".equals(field)) {
                        component.setHeight(n);
                    } else if ("scrollMax".equals(field)) {
                        component.setScrollMax(n);
                        if (component.getType() != ComponentType.CONTAINER) {
                            component.setType(ComponentType.CONTAINER);
                        }
                    }
                } catch (RuntimeException ignored) {
                }
            }
            return;
        }

        if (eq > 0) {
            String left = stmt.substring(0, eq).trim();
            String right = stmt.substring(eq + 1).trim();
            if (left.contains("[") && left.endsWith("]")) {
                interpretArrayIndexAssign(left, right);
                return;
            }
            if (left.endsWith("+")) {
                String name = left.substring(0, left.length() - 1).trim();
                try {
                    intVars.put(name, getInt(name) + evalExpr(right));
                } catch (RuntimeException ignored) {
                }
            } else if (intVars.containsKey(left) || looksLikeIdent(left)) {
                try {
                    intVars.put(left, evalExpr(right));
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    private void interpretIfaceAssign(String stmt) {
        int eq = stmt.indexOf('=');
        if (eq < 0) {
            return;
        }
        String var = stmt.substring(0, eq).trim();
        String rhs = stmt.substring(eq + 1).trim();
        Integer id = null;
        if (rhs.startsWith("addInterface(")) {
            id = evalExpr(insideCall(rhs, "addInterface"));
            if (rootInterfaceId < 0) {
                rootInterfaceId = id;
            }
        } else if (rhs.startsWith("addTabInterface(")) {
            id = evalExpr(insideCall(rhs, "addTabInterface"));
            if (rootInterfaceId < 0) {
                rootInterfaceId = id;
            }
        } else if (rhs.startsWith("addTab(")) {
            id = evalExpr(insideCall(rhs, "addTab"));
            if (rootInterfaceId < 0) {
                rootInterfaceId = id;
            }
        } else if (rhs.startsWith("interfaceCache[")) {
            int open = rhs.indexOf('[');
            int close = rhs.indexOf(']');
            if (open >= 0 && close > open) {
                id = evalExpr(rhs.substring(open + 1, close));
            }
        }
        if (id != null) {
            ifaceVars.put(var, id);
            varNames.put(id, var);
            if (id != rootInterfaceId && !widgets.containsKey(id)) {
                InterfaceComponent container = new InterfaceComponent(ComponentType.CONTAINER);
                container.setId(id);
                container.setName(var.substring(0, 1).toUpperCase(Locale.ROOT) + var.substring(1) + "_" + id);
                container.setWidth(100);
                container.setHeight(100);
                container.setSourceVarName(var);
                widgets.put(id, container);
            } else if (widgets.containsKey(id)) {
                widgets.get(id).setSourceVarName(var);
            }
        }
    }

    private void interpretIntAssign(String stmt) {
        int eq = stmt.indexOf('=');
        if (eq < 0) {
            return;
        }
        String name = stmt.substring(0, eq).trim();
        try {
            intVars.put(name, evalExpr(stmt.substring(eq + 1).trim()));
        } catch (RuntimeException ignored) {
        }
    }

    private void interpretArrayDecl(String stmt) {
        int eq = stmt.indexOf('=');
        if (eq < 0) {
            return;
        }
        String left = stmt.substring(0, eq).trim();
        String name = left.replace("int[]", "").replace("int []", "").trim();
        String rhs = stmt.substring(eq + 1).trim();
        try {
            if (rhs.startsWith("{") && rhs.endsWith("}")) {
                List<String> parts = splitArgs(rhs.substring(1, rhs.length() - 1));
                int[] values = new int[parts.size()];
                for (int i = 0; i < parts.size(); i++) {
                    values[i] = evalExpr(parts.get(i));
                }
                arrays.put(name, values);
            } else if (rhs.contains("new int[")) {
                int open = rhs.indexOf('[');
                int close = rhs.lastIndexOf(']');
                int n = evalExpr(rhs.substring(open + 1, close));
                arrays.put(name, new int[Math.max(0, Math.min(n, 512))]);
            }
        } catch (RuntimeException e) {
            System.err.println("Skipping array " + name + ": " + e.getMessage());
        }
    }

    private void interpretArrayIndexAssign(String left, String right) {
        int open = left.indexOf('[');
        String name = left.substring(0, open).trim();
        try {
            int index = evalExpr(left.substring(open + 1, left.lastIndexOf(']')));
            int[] arr = arrays.get(name);
            if (arr == null || index < 0 || index >= arr.length) {
                return;
            }
            arr[index] = evalExpr(right);
        } catch (RuntimeException ignored) {
        }
    }

    private void interpretChildCall(String stmt) {
        int dot = stmt.indexOf(".child(");
        if (dot < 0) {
            return;
        }
        String var = stmt.substring(0, dot).trim();
        interpretSetBoundsLike(insideCall(stmt.substring(dot + 1), "child"), var, true);
    }

    private void interpretSetBoundsLike(String inside, String parentVar, boolean childOrder) {
        List<String> args = splitArgs(inside);
        if (args.size() != 4 && args.size() != 5) {
            return;
        }
        try {
            Placement placement = new Placement();
            if (childOrder) {
                placement.frame = evalExpr(args.get(0));
                placement.id = evalExpr(args.get(1));
                placement.x = evalExpr(args.get(2));
                placement.y = evalExpr(args.get(3));
                placement.parentVar = parentVar;
            } else {
                placement.id = evalExpr(args.get(0));
                placement.x = evalExpr(args.get(1));
                placement.y = evalExpr(args.get(2));
                placement.frame = evalExpr(args.get(3));
                placement.parentVar = args.size() > 4 ? args.get(4).trim() : parentVar;
            }
            Integer parentId = ifaceVars.get(placement.parentVar);
            placement.parentId = parentId == null ? rootInterfaceId : parentId;
            placements.add(placement);
        } catch (RuntimeException ignored) {
        }
    }

    private void interpretAddRectangle(String inside) {
        List<String> args = splitArgs(inside);
        if (args.size() < 6) {
            return;
        }
        int id = evalExpr(args.get(0));
        InterfaceComponent rect = new InterfaceComponent(ComponentType.RECTANGLE);
        rect.setId(id);
        rect.setWidth(evalExpr(args.get(1)));
        rect.setHeight(evalExpr(args.get(2)));
        rect.setFillColor(parseColor(args.get(3)));
        rect.setFilled(Boolean.parseBoolean(args.get(5).trim()));
        rect.setName("Rect_" + id);
        widgets.put(id, rect);
    }

    private void interpretAddHoverText(String inside) {
        List<String> args = splitArgs(inside);
        if (args.size() < 9) {
            return;
        }
        int id = evalExpr(args.get(0));
        TextComponent text = new TextComponent();
        text.setId(id);
        text.setText(unquote(args.get(1)));
        text.setTooltip(unquote(args.get(2)));
        text.setFontIndex(evalExpr(args.get(4)));
        text.setTextColor(parseColor(args.get(5)));
        text.setCentered(Boolean.parseBoolean(args.get(6).trim()));
        text.setHasShadow(Boolean.parseBoolean(args.get(7).trim()));
        text.setWidth(evalExpr(args.get(8)));
        text.setHeight(16);
        text.setName(labelFor(text.getText(), "Text_" + id));
        widgets.put(id, text);
    }

    private void interpretCacheField(String stmt) {
        int open = stmt.indexOf('[');
        int close = stmt.indexOf(']');
        int eq = stmt.indexOf('=');
        if (open < 0 || close < open || eq < close) {
            return;
        }
        try {
            int id = evalExpr(stmt.substring(open + 1, close));
            String field = stmt.substring(close + 1, eq).replace(".", "").trim();
            int n = evalExpr(stmt.substring(eq + 1).trim());
            InterfaceComponent component = widgets.get(id);
            if (component == null) {
                return;
            }
            if ("width".equals(field)) {
                component.setWidth(n);
            } else if ("height".equals(field)) {
                component.setHeight(n);
            } else if ("scrollMax".equals(field)) {
                component.setScrollMax(n);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void interpretIndexedChildField(String var, String field, String value) {
        int open = field.indexOf('[');
        int close = field.lastIndexOf(']');
        if (open < 0 || close < open) {
            return;
        }
        try {
            int index = evalExpr(field.substring(open + 1, close));
            ChildSlot slot = childSlots.computeIfAbsent(var, k -> new HashMap<>())
                .computeIfAbsent(index, k -> new ChildSlot());
            int n = evalExpr(value);
            if (field.startsWith("children[")) {
                slot.id = n;
            } else if (field.startsWith("childX[")) {
                slot.x = n;
            } else if (field.startsWith("childY[")) {
                slot.y = n;
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void flushChildSlots() {
        for (Map.Entry<String, Map<Integer, ChildSlot>> parent : childSlots.entrySet()) {
            String var = parent.getKey();
            Integer parentId = ifaceVars.get(var);
            if (parentId == null) {
                parentId = rootInterfaceId;
            }
            for (Map.Entry<Integer, ChildSlot> entry : parent.getValue().entrySet()) {
                ChildSlot slot = entry.getValue();
                if (slot.id == Integer.MIN_VALUE) {
                    continue;
                }
                Placement placement = new Placement();
                placement.id = slot.id;
                placement.x = slot.x == Integer.MIN_VALUE ? 0 : slot.x;
                placement.y = slot.y == Integer.MIN_VALUE ? 0 : slot.y;
                placement.frame = entry.getKey();
                placement.parentVar = var;
                placement.parentId = parentId;
                placements.add(placement);
            }
        }
    }

    private void interpretSetBounds(String inside) {
        interpretSetBoundsLike(inside, "", false);
    }

    private void interpretAddSprite(String inside) {
        List<String> args = splitArgs(inside);
        if (args.size() < 3) {
            return;
        }
        int id = evalExpr(args.get(0));
        SpriteComponent sprite = new SpriteComponent();
        sprite.setType(ComponentType.SPRITE);
        sprite.setId(id);
        sprite.setSpriteId(evalExpr(args.get(1)));
        sprite.setSpritePath(unquote(args.get(2)));
        sprite.setName("Sprite_" + id);
        sprite.setWidth(32);
        sprite.setHeight(32);
        widgets.put(id, sprite);
    }

    private void interpretAddHoverButton(String inside) {
        List<String> args = splitArgs(inside);
        if (args.size() < 9) {
            return;
        }
        int id = evalExpr(args.get(0));
        ButtonComponent button = baseButton(id, ComponentType.HOVER_BUTTON);
        button.setNormalSpritePath(unquote(args.get(1)));
        button.setNormalSpriteId(evalExpr(args.get(2)));
        button.setWidth(evalExpr(args.get(3)));
        button.setHeight(evalExpr(args.get(4)));
        button.setTooltip(unquote(args.get(5)));
        button.setName(labelFor(button.getTooltip(), "Button_" + id));
        applyCloseType(button);
        widgets.put(id, button);
    }

    private void interpretAddHoveredButton(String inside) {
        List<String> args = splitArgs(inside);
        if (args.size() < 6) {
            return;
        }
        int id = evalExpr(args.get(0));
        ButtonComponent button = baseButton(id, ComponentType.HOVERED_BUTTON);
        button.setNormalSpritePath(unquote(args.get(1)));
        button.setNormalSpriteId(evalExpr(args.get(2)));
        button.setWidth(evalExpr(args.get(3)));
        button.setHeight(evalExpr(args.get(4)));
        button.setName("Hover_" + id);
        widgets.put(id, button);
    }

    private void interpretAddConfigButton(String inside) {
        List<String> args = splitArgs(inside);
        if (args.size() < 11) {
            return;
        }
        int id = evalExpr(args.get(0));
        ButtonComponent button = baseButton(id, ComponentType.HOVER_BUTTON);
        button.setNormalSpriteId(evalExpr(args.get(2)));
        button.setHoveredSpriteId(evalExpr(args.get(3)));
        button.setNormalSpritePath(unquote(args.get(4)));
        button.setHoveredSpritePath(button.getNormalSpritePath());
        button.setWidth(evalExpr(args.get(5)));
        button.setHeight(evalExpr(args.get(6)));
        button.setTooltip(unquote(args.get(7)));
        button.setName(labelFor(button.getTooltip(), "Config_" + id));
        widgets.put(id, button);
    }

    private void interpretAddHover(String inside) {
        List<String> args = splitArgs(inside);
        if (args.size() < 9) {
            return;
        }
        int id = evalExpr(args.get(0));
        ButtonComponent button = baseButton(id, ComponentType.HOVER_BUTTON);
        button.setNormalSpriteId(evalExpr(args.get(4)));
        button.setNormalSpritePath(unquote(args.get(5)));
        button.setWidth(evalExpr(args.get(6)));
        button.setHeight(evalExpr(args.get(7)));
        button.setTooltip(unquote(args.get(8)));
        button.setName(labelFor(button.getTooltip(), "Hover_" + id));
        applyCloseType(button);
        widgets.put(id, button);
    }

    private void interpretAddHovered(String inside) {
        List<String> args = splitArgs(inside);
        if (args.size() < 6) {
            return;
        }
        int id = evalExpr(args.get(0));
        ButtonComponent button = baseButton(id, ComponentType.HOVERED_BUTTON);
        button.setNormalSpriteId(evalExpr(args.get(1)));
        button.setNormalSpritePath(unquote(args.get(2)));
        button.setWidth(evalExpr(args.get(3)));
        button.setHeight(evalExpr(args.get(4)));
        button.setName("Hover_" + id);
        widgets.put(id, button);
    }

    private void interpretAddButton(String inside) {
        List<String> args = splitArgs(inside);
        if (args.size() < 4) {
            return;
        }
        int id = evalExpr(args.get(0));
        ButtonComponent button = baseButton(id, ComponentType.HOVER_BUTTON);
        button.setNormalSpriteId(evalExpr(args.get(1)));
        button.setNormalSpritePath(unquote(args.get(2)));
        button.setTooltip(unquote(args.get(3)));
        if (args.size() >= 8) {
            button.setWidth(evalExpr(args.get(6)));
            button.setHeight(evalExpr(args.get(7)));
        } else {
            button.setWidth(48);
            button.setHeight(38);
        }
        button.setName(labelFor(button.getTooltip(), "Button_" + id));
        applyCloseType(button);
        widgets.put(id, button);
    }

    private void interpretAddText(String inside) {
        List<String> args = splitArgs(inside);
        if (args.size() < 5) {
            return;
        }
        int id = evalExpr(args.get(0));
        TextComponent text = new TextComponent();
        text.setId(id);
        text.setText(unquote(args.get(1)));
        text.setFontIndex(evalExpr(args.get(3)));
        text.setTextColor(parseColor(args.get(4)));
        if (args.size() >= 7) {
            text.setCentered(Boolean.parseBoolean(args.get(5).trim()));
            text.setHasShadow(Boolean.parseBoolean(args.get(6).trim()));
        }
        text.setName("Text_" + id);
        text.setWidth(Math.max(40, text.getText().length() * 7));
        text.setHeight(16);
        widgets.put(id, text);
    }

    private void interpretAddBankItem(String inside) {
        List<String> args = splitArgs(inside);
        if (args.isEmpty()) {
            return;
        }
        int id = evalExpr(args.get(0));
        InterfaceComponent slot = new InterfaceComponent(ComponentType.ITEM_SLOT);
        slot.setId(id);
        slot.setName("TabSlot_" + id);
        slot.setWidth(32);
        slot.setHeight(32);
        widgets.put(id, slot);
    }

    private InterfaceComponent placeholder(int id) {
        if (id == 5385) {
            InterfaceComponent container = new InterfaceComponent(ComponentType.CONTAINER);
            container.setId(id);
            container.setName("Scroll_" + id);
            container.setWidth(454);
            container.setHeight(206);
            return container;
        }
        SpriteComponent sprite = new SpriteComponent();
        sprite.setId(id);
        sprite.setName("Cache_" + id);
        sprite.setWidth(120);
        sprite.setHeight(16);
        return sprite;
    }

    private ButtonComponent baseButton(int id, ComponentType type) {
        ButtonComponent button = new ButtonComponent();
        button.setType(type);
        button.setId(id);
        return button;
    }

    private static void applyCloseType(ButtonComponent button) {
        if (button.getTooltip() != null && button.getTooltip().toLowerCase(Locale.ROOT).contains("close")) {
            button.setType(ComponentType.CLOSE_BUTTON);
        }
    }

    private static String loopIndexName(String init) {
        String s = init.trim();
        if (s.startsWith("int ")) {
            s = s.substring(4).trim();
        }
        int eq = s.indexOf('=');
        if (eq > 0) {
            s = s.substring(0, eq).trim();
        }
        return looksLikeIdent(s) ? s : null;
    }

    private static String labelFor(String tooltip, String fallback) {
        if (tooltip == null || tooltip.isEmpty()) {
            return fallback;
        }
        return tooltip.length() > 28 ? tooltip.substring(0, 28) : tooltip;
    }

    private boolean evalCondition(String cond) {
        cond = cond.trim();
        int lt = cond.indexOf("<=");
        if (lt >= 0) {
            return evalExpr(cond.substring(0, lt)) <= evalExpr(cond.substring(lt + 2));
        }
        lt = cond.indexOf('<');
        if (lt >= 0) {
            return evalExpr(cond.substring(0, lt)) < evalExpr(cond.substring(lt + 1));
        }
        int gt = cond.indexOf(">=");
        if (gt >= 0) {
            return evalExpr(cond.substring(0, gt)) >= evalExpr(cond.substring(gt + 2));
        }
        gt = cond.indexOf('>');
        if (gt >= 0) {
            return evalExpr(cond.substring(0, gt)) > evalExpr(cond.substring(gt + 1));
        }
        return false;
    }

    private int evalExpr(String expr) {
        expr = expr.trim();
        if (expr.endsWith("++") && looksLikeIdent(expr.substring(0, expr.length() - 2).trim())) {
            String name = expr.substring(0, expr.length() - 2).trim();
            int value = getInt(name);
            intVars.put(name, value + 1);
            return value;
        }
        if (expr.startsWith("++")) {
            String name = expr.substring(2).trim();
            int value = getInt(name) + 1;
            intVars.put(name, value);
            return value;
        }
        if (expr.endsWith("--") && looksLikeIdent(expr.substring(0, expr.length() - 2).trim())) {
            String name = expr.substring(0, expr.length() - 2).trim();
            int value = getInt(name);
            intVars.put(name, value - 1);
            return value;
        }
        if (expr.startsWith("--")) {
            String name = expr.substring(2).trim();
            int value = getInt(name) - 1;
            intVars.put(name, value);
            return value;
        }
        if (expr.contains("&&") || expr.contains("||")) {
            throw new RuntimeException("complex");
        }
        return evalAdd(expr);
    }

    private int evalAdd(String expr) {
        List<String> parts = new ArrayList<>();
        List<Character> ops = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
            }
            if (depth == 0 && (c == '+' || (c == '-' && i > 0 && cur.length() > 0))) {
                parts.add(cur.toString());
                ops.add(c);
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        int value = evalMul(parts.get(0));
        for (int i = 0; i < ops.size(); i++) {
            int rhs = evalMul(parts.get(i + 1));
            value = ops.get(i) == '+' ? value + rhs : value - rhs;
        }
        return value;
    }

    private int evalMul(String expr) {
        expr = expr.trim();
        List<String> parts = new ArrayList<>();
        List<Character> ops = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
            }
            if (depth == 0 && (c == '*' || c == '/' || c == '%')) {
                parts.add(cur.toString());
                ops.add(c);
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        int value = evalAtom(parts.get(0));
        for (int i = 0; i < ops.size(); i++) {
            int rhs = evalAtom(parts.get(i + 1));
            char op = ops.get(i);
            if (op == '*') {
                value *= rhs;
            } else if (op == '/') {
                value = rhs == 0 ? 0 : value / rhs;
            } else {
                value = rhs == 0 ? 0 : value % rhs;
            }
        }
        return value;
    }

    private int evalAtom(String atom) {
        atom = atom.trim();
        if (atom.isEmpty()) {
            return 0;
        }
        if (atom.startsWith("(") && atom.endsWith(")")) {
            return evalExpr(atom.substring(1, atom.length() - 1));
        }
        if (atom.endsWith(".length")) {
            String name = atom.substring(0, atom.length() - 7).trim();
            int[] arr = arrays.get(name);
            if (arr != null) {
                return arr.length;
            }
            if (name.equals("KeyRemapper.NAMES") || name.endsWith("KeyRemapper.NAMES")) {
                return 13;
            }
            throw new RuntimeException("unknown length " + name);
        }
        int bracket = atom.indexOf('[');
        if (bracket > 0 && atom.endsWith("]")) {
            String name = atom.substring(0, bracket).trim();
            int index = evalExpr(atom.substring(bracket + 1, atom.length() - 1));
            int[] arr = arrays.get(name);
            if (arr == null || index < 0 || index >= arr.length) {
                throw new RuntimeException("bad array access " + atom);
            }
            return arr[index];
        }
        if (looksLikeNumber(atom)) {
            return parseColor(atom);
        }
        return getInt(atom);
    }

    private int getInt(String name) {
        Integer value = intVars.get(name);
        if (value == null) {
            throw new RuntimeException("unknown var " + name);
        }
        return value;
    }

    private static int parseColor(String raw) {
        String s = raw.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return (int) Long.parseLong(s.substring(2), 16);
        }
        return Integer.parseInt(s);
    }

    private static boolean looksLikeNumber(String s) {
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return true;
        }
        if (s.isEmpty()) {
            return false;
        }
        int i = s.charAt(0) == '-' ? 1 : 0;
        if (i >= s.length()) {
            return false;
        }
        for (; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeIdent(String s) {
        if (s.isEmpty() || !Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static MethodSpan findMethod(String src, String name) {
        Matcher matcher = Pattern.compile(
            "(?:public|private|protected)?\\s*static\\s+void\\s+" + Pattern.quote(name) + "\\s*\\(")
            .matcher(src);
        if (!matcher.find()) {
            return null;
        }
        int parenOpen = src.indexOf('(', matcher.start());
        int parenClose = matchParen(src, parenOpen);
        if (parenClose < 0) {
            return null;
        }
        int brace = skipWsComments(src, parenClose + 1);
        if (brace >= src.length() || src.charAt(brace) != '{') {
            return null;
        }
        int close = matchBrace(src, brace);
        if (close < 0) {
            return null;
        }
        MethodSpan span = new MethodSpan();
        span.start = matcher.start();
        span.openBrace = brace;
        span.end = close + 1;
        span.body = src.substring(brace + 1, close);
        span.full = src.substring(matcher.start(), close + 1);
        return span;
    }

    static int skipWsComments(String src, int i) {
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                i += 2;
                while (i < n && src.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(src.charAt(i) == '*' && src.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(n, i + 2);
                continue;
            }
            break;
        }
        return i;
    }

    private static boolean keywordAt(String src, int i, String word) {
        if (i + word.length() > src.length() || !src.regionMatches(i, word, 0, word.length())) {
            return false;
        }
        if (i > 0 && Character.isJavaIdentifierPart(src.charAt(i - 1))) {
            return false;
        }
        int after = i + word.length();
        return after >= src.length() || !Character.isJavaIdentifierPart(src.charAt(after));
    }

    private int skipControl(String src, int start) {
        int i = start;
        while (i < src.length() && Character.isJavaIdentifierPart(src.charAt(i))) {
            i++;
        }
        i = skipWsComments(src, i);
        if (i < src.length() && src.charAt(i) == '(') {
            int close = matchParen(src, i);
            if (close < 0) {
                return start + 1;
            }
            i = skipWsComments(src, close + 1);
        }
        if (i < src.length() && src.charAt(i) == '{') {
            int close = matchBrace(src, i);
            return close < 0 ? start + 1 : close + 1;
        }
        int end = findStatementEnd(src, i);
        return end < src.length() && src.charAt(end) == ';' ? end + 1 : Math.max(end, i + 1);
    }

    private static int findStatementEnd(String src, int start) {
        boolean inStr = false;
        int depth = 0;
        for (int i = start; i < src.length(); i++) {
            char c = src.charAt(i);
            if (inStr) {
                if (c == '\\' && i + 1 < src.length()) {
                    i++;
                    continue;
                }
                if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                inStr = true;
                continue;
            }
            if (c == '/' && i + 1 < src.length() && src.charAt(i + 1) == '/') {
                while (i < src.length() && src.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ';' && depth <= 0) {
                return i;
            }
        }
        return src.length();
    }

    static int matchParen(String src, int open) {
        return matchPair(src, open, '(', ')');
    }

    static int matchBrace(String src, int open) {
        return matchPair(src, open, '{', '}');
    }

    private static int matchPair(String src, int open, char openCh, char closeCh) {
        boolean inStr = false;
        int depth = 0;
        for (int i = open; i < src.length(); i++) {
            char c = src.charAt(i);
            if (inStr) {
                if (c == '\\' && i + 1 < src.length()) {
                    i++;
                    continue;
                }
                if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                inStr = true;
                continue;
            }
            if (c == '/' && i + 1 < src.length() && src.charAt(i + 1) == '/') {
                while (i < src.length() && src.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < src.length() && src.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < src.length() && !(src.charAt(i) == '*' && src.charAt(i + 1) == '/')) {
                    i++;
                }
                i++;
                continue;
            }
            if (c == openCh) {
                depth++;
            } else if (c == closeCh) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String insideCall(String stmt, String name) {
        int open = stmt.indexOf('(', stmt.indexOf(name));
        int close = stmt.lastIndexOf(')');
        if (open < 0 || close <= open) {
            return "";
        }
        return stmt.substring(open + 1, close);
    }

    static List<String> splitArgs(String inside) {
        List<String> args = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inStr = false;
        int depth = 0;
        for (int i = 0; i < inside.length(); i++) {
            char c = inside.charAt(i);
            if (inStr) {
                cur.append(c);
                if (c == '\\' && i + 1 < inside.length()) {
                    cur.append(inside.charAt(++i));
                } else if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                inStr = true;
                cur.append(c);
                continue;
            }
            if (c == '(' || c == '[') {
                depth++;
                cur.append(c);
            } else if (c == ')' || c == ']') {
                depth--;
                cur.append(c);
            } else if (c == ',' && depth == 0) {
                args.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            args.add(cur.toString().trim());
        }
        return args;
    }

    private static String[] splitTop(String src, char sep) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            if (c == sep && depth == 0) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    static String unquote(String raw) {
        String s = raw.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    static String readFile(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    private static class ChildSlot {
        int id = Integer.MIN_VALUE;
        int x = Integer.MIN_VALUE;
        int y = Integer.MIN_VALUE;
    }

    private static class Placement {
        int id;
        int x;
        int y;
        int frame;
        int parentId;
        String parentVar = "";
        boolean fromLoop;
        String loopGroup = "";
    }

    static class MethodSpan {
        int start;
        int openBrace;
        int end;
        String body;
        String full;
    }

    public static void main(String[] args) throws Exception {
        ClientWorkspace workspace = ClientWorkspace.detect();
        if (workspace.getInterfacesJava() == null) {
            System.err.println("Interfaces.java not found");
            System.exit(1);
        }
        String path = workspace.getInterfacesJava().getAbsolutePath();
        String method = args.length > 0 ? args[0] : "bank";
        InterfaceProject project = parseInterfaceMethod(path, method);
        if (project == null) {
            System.exit(1);
        }
        for (InterfaceComponent component : project.getComponents()) {
            System.out.println(component.getChildIndex() + "\t" + component.getId() + "\t"
                + component.getX() + "," + component.getY() + "\t" + component.getType()
                + "\t" + component.getName() + (component.isFromLoop() ? " [loop]" : ""));
        }
        if (args.length > 1 && "roundtrip".equals(args[1])) {
            File tmp = new File("target/Interfaces.roundtrip.java");
            Files.copy(workspace.getInterfacesJava().toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            InterfaceProject copy = parseInterfaceMethod(tmp.getAbsolutePath(), method);
            for (InterfaceComponent component : copy.getComponents()) {
                if (component.getId() == 10325) {
                    component.setX(component.getX() + 1);
                    component.setY(component.getY() + 1);
                }
                if (component.getId() == 5294) {
                    component.setY(component.getY() - 2);
                }
            }
            JavaInterfaceWriter.saveToClient(copy);
            String out = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);
            System.out.println(out.contains("setBounds(10325, 71, 37, 20, bank)") ? "LOOP EXPAND OK" : "LOOP FAIL");
            System.out.println(out.contains("setBounds(5294, 110, 283, 5, bank)") ? "LITERAL OK" : "LITERAL FAIL");
            System.out.println(out.contains("setBounds(10335 + i") ? "UNTOUCHED LOOP KEPT" : "UNTOUCHED LOOP LOST");
            tmp.delete();
        }
    }
}
