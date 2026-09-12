# RSPS Interface Maker

A visual interface designer for 317/474-style RuneScape Private Servers (Biohazard V3 and similar sources).

## Features

- **Visual Canvas**: 512x334 fixed mode canvas matching 317 client dimensions
- **Component Types**: Sprites, Hover Buttons, Text, Tooltips, Close Buttons
- **Drag & Drop**: Position components by dragging or using arrow keys (Shift for 10px movement)
- **Multi-Select**: Select multiple components with Ctrl+click for batch operations
- **Alignment Tools**: Align left, center, right, top, middle, bottom
- **Distribution Tools**: Distribute components evenly horizontally or vertically
- **Property Panel**: Edit all component properties in real-time
- **Layer Management**: Reorder components via drag-and-drop list
- **Code Generation**: Export clean Java code matching Biohazard style
- **Live Code Preview**: Real-time Java code preview as you edit
- **Validation**: Check for overlapping IDs, missing sprites, and canvas bounds
- **Quick Templates**: Pre-built interface templates (Empty, Basic Dialog, Shop, Teleport)
- **Project Persistence**: Save and load projects as JSON files
- **Advanced Features**: Load interfaces from cache, automatic ID range suggestion, ZIP export with sprites

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Building

```bash
cd RSPSInterfaceMaker
mvn clean package
```

## Running

```bash
mvn javafx:run
```

Or run the JAR directly:

```bash
java -jar target/interface-maker-1.0.0.jar
```

## Usage

### Creating a New Interface

1. **Add Components**: Use the toolbar buttons to add:
   - **Sprite**: Background images or static graphics
   - **Button**: Interactive buttons with hover states
   - **Text**: Labels and text elements
   - **Close Button**: Pre-configured close button (top-right corner)

2. **Position Components**:
   - Click and drag to move components
   - Use arrow keys for precise positioning (hold Shift for 10px steps)
   - Hold Ctrl + click to select multiple components
   - Use alignment buttons or menu (Align > Left/Center/Right/Top/Middle/Bottom)
   - Use distribution menu (Distribute > Horizontal/Vertical) for even spacing
   - Press Delete to remove selected component(s)

3. **Edit Properties**:
   - Select a component on the canvas or from the component list
   - Modify properties in the right panel (ID, X, Y, Width, Height, Tooltip, etc.)
   - Type-specific properties appear below common properties
   - Live code preview updates automatically as you edit

4. **Manage Layers**:
   - Use the component list (left panel) to view all components
   - Right-click to Move Up/Down or Delete
   - Components are rendered from bottom to top (last = top)

### Saving and Loading Projects

- **File > Save Project**: Save your work as a JSON file
- **File > Open Project**: Load a previously saved project
- **File > New Project**: Start fresh

### Exporting Code

1. Click **File > Export Code**
2. A window will appear with:
   - Complete Java method for Interfaces.java
   - Implementation instructions
   - Required sprite files list
   - ClickingButtons.java stub code
   - Command to open the interface

3. Copy the generated code and follow the instructions

### Exporting as ZIP Package

1. Click **File > Export as ZIP Package**
2. Optionally select a sprite source directory (if you want sprite files included)
3. Choose the output location for the ZIP file
4. The ZIP will contain:
   - `[InterfaceName].java` - Complete interface method
   - `Implementation_Guide.txt` - Setup instructions
   - `sprites/` - Sprite files (if source directory provided)
   - `EXPORT_SUMMARY.txt` - Project information and component list

### Advanced Features

#### Loading Interface from Cache

1. Set your cache path: **Advanced > Set Cache Path**
2. Click **Advanced > Load Interface from Cache**
3. Enter the interface ID you want to reference
4. View the interface structure (read-only reference)

#### Automatic ID Range Suggestion

1. Set your cache path: **Advanced > Set Cache Path**
2. Click **Advanced > Suggest Free ID Range**
3. The tool will scan the cache and suggest a free ID range
4. Apply the suggestion to automatically assign IDs to your interface

## Generated Code Structure

The tool generates code matching the Biohazard V3 style:

### Interfaces.java Method

```java
public static void myNewInterface(TextDrawingArea[] tda) {
    RSInterface inter = addInterface(45000);
    setChildren(N, inter);

    addSprite(...);
    addHoverButton(...);
    addHoveredButton(...);
    addText(...);

    setBounds(...);
    setBounds(...);
}
```

### Implementation Steps

1. **Add Sprites**: Place PNG files in your client cache at the specified folder
2. **Add Method**: Paste the generated method into Interfaces.java
3. **Register Interface**: Add the method call in loadInterfaces()
4. **Handle Clicks**: Add button cases in ClickingButtons.java
5. **Open Interface**: Use the provided command or method

## Component Types

### Sprite
- Static image component
- Supports optional disabled state
- Properties: sprite path, disabled sprite path

### Hover Button
- Interactive button with normal and hover states
- Generates both addHoverButton and addHoveredButton calls
- Properties: normal sprite, hovered sprite, action name, action ID

### Text
- Text label with configurable font and color
- Properties: text content, font index, color (hex), shadow, centered

### Tooltip
- Hover text component
- Properties: tooltip text

### Close Button
- Pre-configured close button pattern
- Positioned at top-right by default
- Properties: same as Hover Button

## Project Structure

```
RSPSInterfaceMaker/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/rsps/interfacemaker/
│   │   │       ├── model/          # Data models
│   │   │       ├── view/           # UI components
│   │   │       ├── generator/      # Code generation
│   │   │       └── util/           # Utilities (CacheReader, ZipExporter, etc.)
│   │   └── resources/
│   │       └── styles.css          # Dark theme styling
├── pom.xml                          # Maven configuration
└── README.md
```

## Keyboard Shortcuts

- **Arrow Keys**: Move selected component(s) 1px
- **Shift + Arrow Keys**: Move selected component(s) 10px
- **Ctrl + Click**: Multi-select components for batch operations
- **Delete**: Remove selected component(s)

## Tips

- Use descriptive component names for easier identification
- Keep track of your interface ID range to avoid conflicts
- Test sprite paths match your client cache structure
- Use the component list to verify layer order
- Use multi-select with alignment tools for faster component positioning
- Use distribution tools to evenly space components
- Use the cache path feature to check for ID conflicts before deploying
- Export as ZIP for a complete package with all necessary files
- Save frequently to avoid losing work

## Example Output

See `EXAMPLE_OUTPUT.md` for a complete example of a simple interface with background, close button, title, and action button.

## Troubleshooting

**Sprites not loading**: Ensure sprite paths match your client cache directory structure exactly.

**Interface not appearing**: Verify the interface ID doesn't conflict with existing interfaces and that you've added the loadInterfaces() call.

**Buttons not clicking**: Check that the action IDs in ClickingButtons.java match the generated component IDs.

**Cache reader not working**: Ensure the cache path is set correctly and points to a valid 317 cache directory structure.

**ZIP export missing sprites**: Make sure to select the correct sprite source directory when exporting as ZIP. The tool looks for sprite files matching the paths specified in your components.

## License

This tool is provided as-is for RSPS development purposes.
