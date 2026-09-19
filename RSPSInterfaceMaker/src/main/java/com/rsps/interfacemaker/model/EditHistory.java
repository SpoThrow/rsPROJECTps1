package com.rsps.interfacemaker.model;

import java.util.ArrayList;
import java.util.List;

/** Snapshots component positions and sizes for undo/redo. */
public class EditHistory {
    private final List<List<Snapshot>> undo = new ArrayList<>();
    private final List<List<Snapshot>> redo = new ArrayList<>();

    public void clear() {
        undo.clear();
        redo.clear();
    }

    public void push(InterfaceProject project) {
        undo.add(capture(project));
        redo.clear();
        if (undo.size() > 80) {
            undo.remove(0);
        }
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public void undo(InterfaceProject project) {
        if (undo.isEmpty()) {
            return;
        }
        redo.add(capture(project));
        restore(project, undo.remove(undo.size() - 1));
    }

    public void redo(InterfaceProject project) {
        if (redo.isEmpty()) {
            return;
        }
        undo.add(capture(project));
        restore(project, redo.remove(redo.size() - 1));
    }

    private static List<Snapshot> capture(InterfaceProject project) {
        List<Snapshot> shots = new ArrayList<>();
        for (InterfaceComponent component : project.getComponents()) {
            Snapshot snapshot = new Snapshot();
            snapshot.id = component.getId();
            snapshot.x = component.getX();
            snapshot.y = component.getY();
            snapshot.width = component.getWidth();
            snapshot.height = component.getHeight();
            snapshot.scrollMax = component.getScrollMax();
            snapshot.previewScroll = component.getPreviewScroll();
            shots.add(snapshot);
        }
        return shots;
    }

    private static void restore(InterfaceProject project, List<Snapshot> shots) {
        for (Snapshot snapshot : shots) {
            InterfaceComponent component = project.findById(snapshot.id);
            if (component == null) {
                continue;
            }
            component.setX(snapshot.x);
            component.setY(snapshot.y);
            component.setWidth(snapshot.width);
            component.setHeight(snapshot.height);
            component.setScrollMax(snapshot.scrollMax);
            component.setPreviewScroll(snapshot.previewScroll);
        }
    }

    private static class Snapshot {
        int id;
        int x;
        int y;
        int width;
        int height;
        int scrollMax;
        int previewScroll;
    }
}
