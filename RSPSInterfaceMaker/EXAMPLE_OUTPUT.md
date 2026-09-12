# Example Output: Simple Shop Interface

This example shows the generated output for a simple interface with:
- Background sprite
- Close button
- Title text
- One action button

## Generated Java Method (Interfaces.java)

```java
public static void simpleShop(TextDrawingArea[] tda) {
    RSInterface inter = addInterface(45000);
    setChildren(4, inter);

    addSprite(45001, 1, "Interfaces/SimpleShop/BACKGROUND");
    addHoverButton(45002, "Interfaces/SimpleShop/CLOSE", 2, 20, 20, "Close", 0, 3, 1);
    addHoveredButton(45003, "Interfaces/SimpleShop/CLOSE_HOVER", 2, 20, 20, 4);
    addText(45004, "Shop Name", tda, 0, 0xFFFFFF, false, true);
    addHoverButton(45005, "Interfaces/SimpleShop/BUTTON", 5, 100, 25, "Buy Item", 0, 6, 1);
    addHoveredButton(45006, "Interfaces/SimpleShop/BUTTON_HOVER", 5, 100, 25, 7);

    setBounds(45001, 0, 0, 0, inter);
    setBounds(45002, 475, 10, 1, inter);
    setBounds(45003, 475, 10, 2, inter);
    setBounds(45004, 20, 20, 3, inter);
    setBounds(45005, 50, 100, 4, inter);
    setBounds(45006, 50, 100, 5, inter);
}
```

## Implementation Guide

## Overview
- **Interface ID**: `45000`
- **Component ID Range**: `45001` to `45006`
- **Total Components**: 4

## Step 1: Add Sprite Files

Place the following sprite files in your client cache:

**Folder**: `Interfaces/SimpleShop/`
- `Interfaces/SimpleShop/BACKGROUND.png`
- `Interfaces/SimpleShop/CLOSE.png`
- `Interfaces/SimpleShop/CLOSE_HOVER.png`
- `Interfaces/SimpleShop/BUTTON.png`
- `Interfaces/SimpleShop/BUTTON_HOVER.png`

## Step 2: Add Method to Interfaces.java

Add this method to the `Interfaces` class:

```java
public static void simpleShop(TextDrawingArea[] tda) {
    RSInterface inter = addInterface(45000);
    setChildren(4, inter);

    addSprite(45001, 1, "Interfaces/SimpleShop/BACKGROUND");
    addHoverButton(45002, "Interfaces/SimpleShop/CLOSE", 2, 20, 20, "Close", 0, 3, 1);
    addHoveredButton(45003, "Interfaces/SimpleShop/CLOSE_HOVER", 2, 20, 20, 4);
    addText(45004, "Shop Name", tda, 0, 0xFFFFFF, false, true);
    addHoverButton(45005, "Interfaces/SimpleShop/BUTTON", 5, 100, 25, "Buy Item", 0, 6, 1);
    addHoveredButton(45006, "Interfaces/SimpleShop/BUTTON_HOVER", 5, 100, 25, 7);

    setBounds(45001, 0, 0, 0, inter);
    setBounds(45002, 475, 10, 1, inter);
    setBounds(45003, 475, 10, 2, inter);
    setBounds(45004, 20, 20, 3, inter);
    setBounds(45005, 50, 100, 4, inter);
    setBounds(45006, 50, 100, 5, inter);
}
```

## Step 3: Register in loadInterfaces()

Add this call inside the `loadInterfaces(TextDrawingArea[] tda)` method:

```java
simpleShop(tda);
```

## Step 4: Handle Button Clicks

Add these cases to `ClickingButtons.java` to handle button clicks:

```java
case 45002:
    // TODO: Handle Close action
    break;
case 45005:
    // TODO: Handle Buy Item action
    break;
```

## Step 5: Open the Interface

Use this command to open the interface:

```java
c.getPA().showInterface(45000);
```

## Notes

- Sprite IDs are automatically calculated as `componentId - interfaceId`
- Hover button IDs are `componentId + 1`
- Dummy IDs for hovered buttons are `componentId + 2`
- Ensure sprite paths match your client cache structure exactly
- Test the interface after implementation to verify positioning

## Visual Layout

```
+-------------------------------------+
| Shop Name                    [X]    |  (Title + Close)
|                                     |
|                                     |
|                                     |
|         [ Buy Item ]                 |  (Action button)
|                                     |
|                                     |
|                                     |
|                                     |
|                                     |
+-------------------------------------+
```

## Code Pattern Explanation

### Sprites
```java
addSprite(id, spriteId, "Interfaces/Folder/NAME");
```
- `id`: Component ID (e.g., 45001)
- `spriteId`: Calculated as `id - interfaceId` (e.g., 1)
- Path: Full path to sprite in cache

### Hover Buttons
```java
addHoverButton(id, "path", spriteId, width, height, "Tooltip", 0, hoverId, 1);
addHoveredButton(hoverId, "path", spriteId, width, height, dummyId);
```
- `id`: Component ID (e.g., 45002)
- `spriteId`: Calculated as `id - interfaceId` (e.g., 2)
- `hoverId`: Calculated as `id + 1 - interfaceId` (e.g., 3)
- `dummyId`: Calculated as `id + 2 - interfaceId` (e.g., 4)
- The `0, 1` at the end are fixed parameters for Biohazard

### Text
```java
addText(id, "Text", tda, font, color, centered, shadow);
```
- `id`: Component ID (e.g., 45004)
- `font`: Font index (0-3 typically)
- `color`: Hex color (e.g., 0xFFFFFF for white)
- `centered`: Boolean for text centering
- `shadow`: Boolean for text shadow

### SetBounds
```java
setBounds(childId, x, y, index, inter);
```
- `childId`: Component ID
- `x, y`: Position coordinates
- `index`: Child index (layer order, 0 = bottom)
- `inter`: The interface object

## Customization Tips

1. **Change Interface ID**: If 45000 conflicts, use a different range (e.g., 50000-50999)
2. **Adjust Positions**: Modify X/Y values in setBounds to reposition components
3. **Change Colors**: Edit the hex color value in addText (0xFFFFFF = white)
4. **Add More Buttons**: Add more addHoverButton/addHoveredButton pairs with unique IDs
5. **Resize Components**: Update Width/Height in both addHoverButton and setBounds calls
