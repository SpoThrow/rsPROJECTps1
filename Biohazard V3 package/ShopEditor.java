import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopEditor extends JFrame {
    private File shopsFile;
    private File spawnConfigFile;
    private File npcShopsFile;
    private File npcDropsFile;
    private File npcStatsFile;
    private File teleportsFile;
    private File spawnPointsFile;
    private File objectsFile;
    private File configFile;
    private File startingInventoryFile;
    private File startingBankFile;
    private File playerRightsFile;
    private File doorsFile;
    private File objectSizeFile;
    private List<Shop> shops;
    private List<NpcSpawn> npcSpawns;
    private List<TeleportLocation> teleports = new ArrayList<>();
    private List<SpawnPoint> spawnPoints = new ArrayList<>();
    private List<ObjectSpawn> objectSpawns = new ArrayList<>();
    private List<StarterItem> startingInventory = new ArrayList<>();
    private List<StarterItem> startingBank = new ArrayList<>();
    private List<PlayerRight> playerRights = new ArrayList<>();
    private List<Door> doors = new ArrayList<>();
    private List<ObjectSize> objectSizes = new ArrayList<>();
    private Map<Integer, Integer> npcShopMap;
    private Map<Integer, NpcStats> npcStatsMap;
    private Map<Integer, List<NpcDrop>> npcDropsMap;
    private JList<Shop> shopList;
    private JList<NpcSpawn> npcList;
    private JList<NpcSpawn> dropsNpcList;
    private JList<NpcSpawn> statsNpcList;
    private JList<TeleportLocation> teleportList;
    private JList<SpawnPoint> spawnPointList;
    private JList<ObjectSpawn> objectList;
    private JList<StarterItem> inventoryList;
    private JList<StarterItem> bankList;
    private JList<PlayerRight> rightsList;
    private JList<Door> doorList;
    private JList<ObjectSize> objectSizeList;
    private DefaultListModel<NpcSpawn> npcListModel;
    private DefaultListModel<NpcSpawn> dropsNpcListModel;
    private DefaultListModel<NpcSpawn> statsNpcListModel;
    private DefaultListModel<TeleportLocation> teleportListModel;
    private DefaultListModel<SpawnPoint> spawnPointListModel;
    private DefaultListModel<ObjectSpawn> objectListModel;
    private DefaultListModel<StarterItem> inventoryListModel;
    private DefaultListModel<StarterItem> bankListModel;
    private DefaultListModel<PlayerRight> rightsListModel;
    private DefaultListModel<Door> doorListModel;
    private DefaultListModel<ObjectSize> objectSizeListModel;
    private DefaultTableModel itemTableModel;
    private DefaultTableModel dropTableModel;
    private JTable itemTable;
    private JTable dropTable;
    private JTextField shopIdField;
    private JTextField shopNameField;
    private JTextField sellModifierField;
    private JTextField buyModifierField;
    private JTextField itemIdField;
    private JTextField itemAmountField;
    private JTextField npcIdField;
    private JTextField npcXField;
    private JTextField npcYField;
    private JTextField npcHeightField;
    private JTextField npcShopIdField;
    private JTextField npcCombatField;
    private JTextField npcHealthField;
    private JTextField npcAttackField;
    private JTextField npcDefenseField;
    private JTextField npcAttackSpeedField;
    private JTextField statsNpcIdField;
    private JTextField dropItemIdField;
    private JTextField dropAmountField;
    private JComboBox<String> rarityCombo;
    private JCheckBox autoSaveCheckbox;
    private JTextField teleportCategoryField;
    private JTextField teleportNameField;
    private JTextField teleportXField;
    private JTextField teleportYField;
    private JTextField spawnPointNameField;
    private JTextField spawnPointXField;
    private JTextField spawnPointYField;
    private JTextField objectIdField;
    private JTextField objectXField;
    private JTextField objectYField;
    private JTextField objectHeightField;
    private JTextField objectFaceField;
    private JTextField objectTypeField;
    private JTextField woodcuttingExpField;
    private JTextField miningExpField;
    private JTextField smithingExpField;
    private JTextField farmingExpField;
    private JTextField firemakingExpField;
    private JTextField herbloreExpField;
    private JTextField fishingExpField;
    private JTextField agilityExpField;
    private JTextField prayerExpField;
    private JTextField runecraftingExpField;
    private JTextField craftingExpField;
    private JTextField thievingExpField;
    private JTextField slayerExpField;
    private JTextField cookingExpField;
    private JTextField fletchingExpField;
    private JTextField constructionExpField;
    private JTextField hunterExpField;
    private JTextField meleeExpField;
    private JTextField rangeExpField;
    private JTextField magicExpField;
    private JTextField serverExpBonusField;
    private JTextField inventoryItemIdField;
    private JTextField inventoryAmountField;
    private JTextField bankItemIdField;
    private JTextField bankAmountField;
    private JTextField rightLevelField;
    private JTextField rightNameField;
    private JTextField rightPrefixField;
    private JTextField rightColorField;
    private JTextField doorXField;
    private JTextField doorYField;
    private JTextField doorHeightField;
    private JTextField doorFaceField;
    private JTextField doorStateField;
    private JTextField objectSizeIdField;
    private JTextField objectSizeNameField;
    private JTextField objectSizeSizeField;
    private JTextField objectSizeExamField;
    private Map<Integer, String> itemNames = new HashMap<>();
    private Map<Integer, String> npcNames = new HashMap<>();
    private List<String> undoStack = new ArrayList<>();
    private List<String> redoStack = new ArrayList<>();

    public ShopEditor() {
        setTitle("Biohazard Shop Editor");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        shops = new ArrayList<>();
        npcSpawns = new ArrayList<>();
        teleports = new ArrayList<>();
        spawnPoints = new ArrayList<>();
        npcShopMap = new HashMap<>();
        npcStatsMap = new HashMap<>();
        npcDropsMap = new HashMap<>();

        // Top panel - file operations
        JPanel topPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Save shops.cfg");
        JButton saveNpcButton = new JButton("Save NPC Files");
        JButton saveTeleportButton = new JButton("Save Teleport Files");
        topPanel.add(saveButton);
        topPanel.add(saveNpcButton);
        topPanel.add(saveTeleportButton);
        add(topPanel, BorderLayout.NORTH);

        saveButton.addActionListener(e -> saveShopsFile());
        saveNpcButton.addActionListener(e -> saveNpcFiles());
        saveTeleportButton.addActionListener(e -> saveTeleportFiles());

        // Center panel - tabbed interface
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Shop Editor Tab
        JPanel shopEditorPanel = new JPanel(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Left panel - shop list
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Shops"));
        
        shopList = new JList<>();
        JScrollPane shopScrollPane = new JScrollPane(shopList);
        leftPanel.add(shopScrollPane, BorderLayout.CENTER);
        
        JPanel shopButtonPanel = new JPanel(new FlowLayout());
        JButton addShopButton = new JButton("Add Shop");
        JButton duplicateShopButton = new JButton("Duplicate Shop");
        JButton deleteShopButton = new JButton("Delete Shop");
        shopButtonPanel.add(addShopButton);
        shopButtonPanel.add(duplicateShopButton);
        shopButtonPanel.add(deleteShopButton);
        leftPanel.add(shopButtonPanel, BorderLayout.SOUTH);
        
        addShopButton.addActionListener(e -> addShop());
        duplicateShopButton.addActionListener(e -> duplicateShop());
        deleteShopButton.addActionListener(e -> deleteShop());
        
        // Right panel - shop details
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Shop Details"));
        
        JPanel shopDetailsPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        shopDetailsPanel.add(new JLabel("Shop ID:"));
        shopIdField = new JTextField();
        shopDetailsPanel.add(shopIdField);
        shopDetailsPanel.add(new JLabel("Shop Name:"));
        shopNameField = new JTextField();
        shopDetailsPanel.add(shopNameField);
        shopDetailsPanel.add(new JLabel("Sell Modifier:"));
        sellModifierField = new JTextField();
        shopDetailsPanel.add(sellModifierField);
        shopDetailsPanel.add(new JLabel("Buy Modifier:"));
        buyModifierField = new JTextField();
        shopDetailsPanel.add(buyModifierField);
        
        JButton updateDetailsButton = new JButton("Update Shop Details");
        shopDetailsPanel.add(updateDetailsButton);
        
        rightPanel.add(shopDetailsPanel, BorderLayout.NORTH);
        
        // Item table container with search
        JPanel itemTableContainer = new JPanel(new BorderLayout());
        
        // Item search/filter panel
        JPanel itemSearchPanel = new JPanel(new FlowLayout());
        itemSearchPanel.add(new JLabel("Filter Items:"));
        JTextField itemFilterField = new JTextField(15);
        itemSearchPanel.add(itemFilterField);
        itemTableContainer.add(itemSearchPanel, BorderLayout.NORTH);
        
        // Item table
        String[] itemColumns = {"Item ID", "Item Name", "Amount"};
        itemTableModel = new DefaultTableModel(itemColumns, 0);
        itemTable = new JTable(itemTableModel);
        itemTable.setAutoCreateRowSorter(true);
        itemTable.getColumnModel().getColumn(0).setCellRenderer(new ItemNameRenderer());
        JScrollPane itemScrollPane = new JScrollPane(itemTable);
        itemTableContainer.add(itemScrollPane, BorderLayout.CENTER);
        
        rightPanel.add(itemTableContainer, BorderLayout.CENTER);
        
        // Item controls
        JPanel itemControlPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        itemControlPanel.add(new JLabel("Item ID:"));
        itemIdField = new JTextField();
        itemControlPanel.add(itemIdField);
        JButton searchItemButton = new JButton("Search");
        itemControlPanel.add(searchItemButton);
        itemControlPanel.add(new JLabel("Amount:"));
        itemAmountField = new JTextField();
        itemControlPanel.add(itemAmountField);
        JButton addItemButton = new JButton("Add Item");
        JButton removeItemButton = new JButton("Remove Item");
        itemControlPanel.add(addItemButton);
        itemControlPanel.add(removeItemButton);
        rightPanel.add(itemControlPanel, BorderLayout.SOUTH);
        
        addItemButton.addActionListener(e -> addItem());
        removeItemButton.addActionListener(e -> removeItem());
        updateDetailsButton.addActionListener(e -> updateShopDetails());
        searchItemButton.addActionListener(e -> searchItemByName());
        
        // Item filter listener
        itemFilterField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterItems(itemFilterField.getText());
            }
        });
        
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        splitPane.setDividerLocation(250);
        
        shopEditorPanel.add(splitPane, BorderLayout.CENTER);
        tabbedPane.addTab("Shop Editor", shopEditorPanel);
        
        // NPC Spawn Editor Tab
        JPanel npcSpawnPanel = new JPanel(new BorderLayout());
        JPanel npcLeftPanel = new JPanel(new BorderLayout());
        npcLeftPanel.setBorder(BorderFactory.createTitledBorder("NPC Spawns"));
        
        // NPC search field
        JPanel npcSearchPanel = new JPanel(new FlowLayout());
        npcSearchPanel.add(new JLabel("Search:"));
        JTextField npcSearchField = new JTextField(15);
        npcSearchPanel.add(npcSearchField);
        npcLeftPanel.add(npcSearchPanel, BorderLayout.NORTH);
        
        npcListModel = new DefaultListModel<>();
        npcList = new JList<>(npcListModel);
        npcList.setCellRenderer(new NpcNameRenderer());
        JScrollPane npcScrollPane = new JScrollPane(npcList);
        npcLeftPanel.add(npcScrollPane, BorderLayout.CENTER);
        
        JPanel npcButtonPanel = new JPanel(new FlowLayout());
        JButton addNpcButton = new JButton("Add NPC Spawn");
        JButton duplicateNpcButton = new JButton("Duplicate NPC Spawn");
        JButton deleteNpcButton = new JButton("Delete NPC Spawn");
        JButton bulkDeleteButton = new JButton("Bulk Delete");
        JButton undoButton = new JButton("Undo");
        JButton redoButton = new JButton("Redo");
        npcButtonPanel.add(addNpcButton);
        npcButtonPanel.add(duplicateNpcButton);
        npcButtonPanel.add(deleteNpcButton);
        npcButtonPanel.add(bulkDeleteButton);
        npcButtonPanel.add(undoButton);
        npcButtonPanel.add(redoButton);
        npcLeftPanel.add(npcButtonPanel, BorderLayout.SOUTH);
        
        JPanel npcRightPanel = new JPanel(new BorderLayout());
        npcRightPanel.setBorder(BorderFactory.createTitledBorder("NPC Spawn Details"));
        
        // Position section
        JPanel positionPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        positionPanel.setBorder(BorderFactory.createTitledBorder("Position"));
        positionPanel.add(new JLabel("NPC ID:"));
        JPanel npcIdPanel = new JPanel(new BorderLayout());
        npcIdField = new JTextField();
        JButton searchNpcButton = new JButton("Search");
        npcIdPanel.add(npcIdField, BorderLayout.CENTER);
        npcIdPanel.add(searchNpcButton, BorderLayout.EAST);
        positionPanel.add(npcIdPanel);
        positionPanel.add(new JLabel("X Position:"));
        npcXField = new JTextField();
        positionPanel.add(npcXField);
        positionPanel.add(new JLabel("Y Position:"));
        npcYField = new JTextField();
        positionPanel.add(npcYField);
        positionPanel.add(new JLabel("Height:"));
        npcHeightField = new JTextField();
        positionPanel.add(npcHeightField);
        
        // Shop assignment section
        JPanel shopAssignPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        shopAssignPanel.setBorder(BorderFactory.createTitledBorder("Shop Assignment"));
        shopAssignPanel.add(new JLabel("Shop ID:"));
        npcShopIdField = new JTextField();
        shopAssignPanel.add(npcShopIdField);
        shopAssignPanel.add(new JLabel(""));
        JButton updateNpcButton = new JButton("Update NPC Spawn");
        shopAssignPanel.add(updateNpcButton);
        
        JPanel npcDetailsPanel = new JPanel(new BorderLayout());
        npcDetailsPanel.add(positionPanel, BorderLayout.NORTH);
        npcDetailsPanel.add(shopAssignPanel, BorderLayout.CENTER);
        
        // Auto-save checkbox
        JPanel autoSavePanel = new JPanel(new FlowLayout());
        autoSaveCheckbox = new JCheckBox("Auto-save NPC files");
        autoSavePanel.add(autoSaveCheckbox);
        npcDetailsPanel.add(autoSavePanel, BorderLayout.SOUTH);
        
        npcRightPanel.add(npcDetailsPanel, BorderLayout.NORTH);
        
        JSplitPane npcSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        npcSplitPane.setLeftComponent(npcLeftPanel);
        npcSplitPane.setRightComponent(npcRightPanel);
        npcSplitPane.setDividerLocation(300);
        
        npcSpawnPanel.add(npcSplitPane, BorderLayout.CENTER);
        tabbedPane.addTab("NPC Spawns", npcSpawnPanel);
        
        // NPC Drops Tab
        JPanel npcDropsPanel = new JPanel(new BorderLayout());
        JPanel dropsLeftPanel = new JPanel(new BorderLayout());
        dropsLeftPanel.setBorder(BorderFactory.createTitledBorder("NPCs"));
        
        // NPC search field for drops
        JPanel dropsSearchPanel = new JPanel(new FlowLayout());
        dropsSearchPanel.add(new JLabel("Search:"));
        JTextField dropsSearchField = new JTextField(15);
        dropsSearchPanel.add(dropsSearchField);
        dropsLeftPanel.add(dropsSearchPanel, BorderLayout.NORTH);
        
        dropsNpcListModel = new DefaultListModel<>();
        dropsNpcList = new JList<>(dropsNpcListModel);
        dropsNpcList.setCellRenderer(new NpcNameRenderer());
        JScrollPane dropsNpcScrollPane = new JScrollPane(dropsNpcList);
        dropsLeftPanel.add(dropsNpcScrollPane, BorderLayout.CENTER);
        
        JPanel dropsRightPanel = new JPanel(new BorderLayout());
        dropsRightPanel.setBorder(BorderFactory.createTitledBorder("NPC Drops"));
        
        // Drop table
        String[] dropColumns = {"Item ID", "Item Name", "Amount", "Rarity"};
        dropTableModel = new DefaultTableModel(dropColumns, 0);
        dropTable = new JTable(dropTableModel);
        dropTable.setAutoCreateRowSorter(true);
        dropTable.getColumnModel().getColumn(0).setCellRenderer(new ItemNameRenderer());
        JScrollPane dropScrollPane = new JScrollPane(dropTable);
        dropsRightPanel.add(dropScrollPane, BorderLayout.CENTER);
        
        // Drop controls
        JPanel dropControlPanel = new JPanel(new GridLayout(4, 3, 5, 5));
        dropControlPanel.add(new JLabel("Item ID:"));
        dropItemIdField = new JTextField();
        dropControlPanel.add(dropItemIdField);
        JButton searchDropItemButton = new JButton("Search");
        dropControlPanel.add(searchDropItemButton);
        dropControlPanel.add(new JLabel("Amount:"));
        dropAmountField = new JTextField();
        dropControlPanel.add(dropAmountField);
        dropControlPanel.add(new JLabel(""));
        dropControlPanel.add(new JLabel("Rarity:"));
        String[] rarities = {"ALWAYS", "COMMON", "UNCOMMON", "RARE", "VERY_RARE", "SUPER_RARE"};
        JComboBox<String> rarityCombo = new JComboBox<>(rarities);
        dropControlPanel.add(rarityCombo);
        JButton addDropButton = new JButton("Add Drop");
        JButton removeDropButton = new JButton("Remove Drop");
        dropControlPanel.add(addDropButton);
        dropControlPanel.add(removeDropButton);
        dropsRightPanel.add(dropControlPanel, BorderLayout.SOUTH);
        
        JSplitPane dropsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        dropsSplitPane.setLeftComponent(dropsLeftPanel);
        dropsSplitPane.setRightComponent(dropsRightPanel);
        dropsSplitPane.setDividerLocation(250);
        
        npcDropsPanel.add(dropsSplitPane, BorderLayout.CENTER);
        tabbedPane.addTab("NPC Drops", npcDropsPanel);
        
        // NPC Stats Tab
        JPanel npcStatsPanel = new JPanel(new BorderLayout());
        JPanel statsLeftPanel = new JPanel(new BorderLayout());
        statsLeftPanel.setBorder(BorderFactory.createTitledBorder("NPCs"));
        
        // NPC search field for stats
        JPanel statsSearchPanel = new JPanel(new FlowLayout());
        statsSearchPanel.add(new JLabel("Search:"));
        JTextField statsSearchField = new JTextField(15);
        statsSearchPanel.add(statsSearchField);
        statsLeftPanel.add(statsSearchPanel, BorderLayout.NORTH);
        
        statsNpcListModel = new DefaultListModel<>();
        statsNpcList = new JList<>(statsNpcListModel);
        statsNpcList.setCellRenderer(new NpcNameRenderer());
        JScrollPane statsNpcScrollPane = new JScrollPane(statsNpcList);
        statsLeftPanel.add(statsNpcScrollPane, BorderLayout.CENTER);
        
        JPanel statsRightPanel = new JPanel(new BorderLayout());
        statsRightPanel.setBorder(BorderFactory.createTitledBorder("NPC Stats"));
        
        JPanel statsDetailsPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        statsDetailsPanel.add(new JLabel("NPC ID:"));
        statsNpcIdField = new JTextField();
        statsNpcIdField.setEditable(false);
        statsDetailsPanel.add(statsNpcIdField);
        statsDetailsPanel.add(new JLabel("Combat Level:"));
        npcCombatField = new JTextField();
        statsDetailsPanel.add(npcCombatField);
        statsDetailsPanel.add(new JLabel("Health:"));
        npcHealthField = new JTextField();
        statsDetailsPanel.add(npcHealthField);
        statsDetailsPanel.add(new JLabel("Attack:"));
        npcAttackField = new JTextField();
        statsDetailsPanel.add(npcAttackField);
        statsDetailsPanel.add(new JLabel("Defense:"));
        npcDefenseField = new JTextField();
        statsDetailsPanel.add(npcDefenseField);
        statsDetailsPanel.add(new JLabel("Attack Speed:"));
        npcAttackSpeedField = new JTextField();
        statsDetailsPanel.add(npcAttackSpeedField);
        
        JButton updateStatsButton = new JButton("Update NPC Stats");
        statsDetailsPanel.add(updateStatsButton);
        
        statsRightPanel.add(statsDetailsPanel, BorderLayout.NORTH);
        
        JSplitPane statsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        statsSplitPane.setLeftComponent(statsLeftPanel);
        statsSplitPane.setRightComponent(statsRightPanel);
        statsSplitPane.setDividerLocation(250);
        
        npcStatsPanel.add(statsSplitPane, BorderLayout.CENTER);
        tabbedPane.addTab("NPC Stats", npcStatsPanel);
        
        // Teleport Editor Tab
        JPanel teleportPanel = new JPanel(new BorderLayout());
        JPanel teleportLeftPanel = new JPanel(new BorderLayout());
        teleportLeftPanel.setBorder(BorderFactory.createTitledBorder("Teleports"));
        
        teleportListModel = new DefaultListModel<>();
        teleportList = new JList<>(teleportListModel);
        JScrollPane teleportScrollPane = new JScrollPane(teleportList);
        teleportLeftPanel.add(teleportScrollPane, BorderLayout.CENTER);
        
        JPanel teleportButtonPanel = new JPanel(new FlowLayout());
        JButton addTeleportButton = new JButton("Add Teleport");
        JButton deleteTeleportButton = new JButton("Delete Teleport");
        teleportButtonPanel.add(addTeleportButton);
        teleportButtonPanel.add(deleteTeleportButton);
        teleportLeftPanel.add(teleportButtonPanel, BorderLayout.SOUTH);
        
        JPanel teleportRightPanel = new JPanel(new BorderLayout());
        teleportRightPanel.setBorder(BorderFactory.createTitledBorder("Teleport Details"));
        
        JPanel teleportDetailsPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        teleportDetailsPanel.add(new JLabel("Category:"));
        teleportCategoryField = new JTextField();
        teleportDetailsPanel.add(teleportCategoryField);
        teleportDetailsPanel.add(new JLabel("Name:"));
        teleportNameField = new JTextField();
        teleportDetailsPanel.add(teleportNameField);
        teleportDetailsPanel.add(new JLabel("X:"));
        teleportXField = new JTextField();
        teleportDetailsPanel.add(teleportXField);
        teleportDetailsPanel.add(new JLabel("Y:"));
        teleportYField = new JTextField();
        teleportDetailsPanel.add(teleportYField);
        
        JButton updateTeleportButton = new JButton("Update Teleport");
        teleportDetailsPanel.add(updateTeleportButton);
        
        teleportRightPanel.add(teleportDetailsPanel, BorderLayout.NORTH);
        
        JSplitPane teleportSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        teleportSplitPane.setLeftComponent(teleportLeftPanel);
        teleportSplitPane.setRightComponent(teleportRightPanel);
        teleportSplitPane.setDividerLocation(250);
        
        teleportPanel.add(teleportSplitPane, BorderLayout.CENTER);
        tabbedPane.addTab("Teleport Editor", teleportPanel);
        
        // Spawn Point Editor Tab
        JPanel spawnPointPanel = new JPanel(new BorderLayout());
        JPanel spawnLeftPanel = new JPanel(new BorderLayout());
        spawnLeftPanel.setBorder(BorderFactory.createTitledBorder("Spawn Points"));
        
        spawnPointListModel = new DefaultListModel<>();
        spawnPointList = new JList<>(spawnPointListModel);
        JScrollPane spawnScrollPane = new JScrollPane(spawnPointList);
        spawnLeftPanel.add(spawnScrollPane, BorderLayout.CENTER);
        
        JPanel spawnButtonPanel = new JPanel(new FlowLayout());
        JButton addSpawnPointButton = new JButton("Add Spawn Point");
        JButton deleteSpawnPointButton = new JButton("Delete Spawn Point");
        spawnButtonPanel.add(addSpawnPointButton);
        spawnButtonPanel.add(deleteSpawnPointButton);
        spawnLeftPanel.add(spawnButtonPanel, BorderLayout.SOUTH);
        
        JPanel spawnRightPanel = new JPanel(new BorderLayout());
        spawnRightPanel.setBorder(BorderFactory.createTitledBorder("Spawn Point Details"));
        
        JPanel spawnDetailsPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        spawnDetailsPanel.add(new JLabel("Name:"));
        spawnPointNameField = new JTextField();
        spawnDetailsPanel.add(spawnPointNameField);
        spawnDetailsPanel.add(new JLabel("X:"));
        spawnPointXField = new JTextField();
        spawnDetailsPanel.add(spawnPointXField);
        spawnDetailsPanel.add(new JLabel("Y:"));
        spawnPointYField = new JTextField();
        spawnDetailsPanel.add(spawnPointYField);
        
        JButton updateSpawnPointButton = new JButton("Update Spawn Point");
        spawnDetailsPanel.add(updateSpawnPointButton);
        
        spawnRightPanel.add(spawnDetailsPanel, BorderLayout.NORTH);
        
        JSplitPane spawnSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        spawnSplitPane.setLeftComponent(spawnLeftPanel);
        spawnSplitPane.setRightComponent(spawnRightPanel);
        spawnSplitPane.setDividerLocation(250);
        
        spawnPointPanel.add(spawnSplitPane, BorderLayout.CENTER);
        tabbedPane.addTab("Spawn Point Editor", spawnPointPanel);
        
        // Object Spawns Tab
        JPanel objectPanel = new JPanel(new BorderLayout());
        JPanel objectLeftPanel = new JPanel(new BorderLayout());
        objectLeftPanel.setBorder(BorderFactory.createTitledBorder("Object Spawns"));
        
        objectListModel = new DefaultListModel<>();
        objectList = new JList<>(objectListModel);
        JScrollPane objectScrollPane = new JScrollPane(objectList);
        objectLeftPanel.add(objectScrollPane, BorderLayout.CENTER);
        
        JPanel objectButtonPanel = new JPanel(new FlowLayout());
        JButton addObjectButton = new JButton("Add Object");
        JButton deleteObjectButton = new JButton("Delete Object");
        objectButtonPanel.add(addObjectButton);
        objectButtonPanel.add(deleteObjectButton);
        objectLeftPanel.add(objectButtonPanel, BorderLayout.SOUTH);
        
        JPanel objectRightPanel = new JPanel(new BorderLayout());
        objectRightPanel.setBorder(BorderFactory.createTitledBorder("Object Details"));
        
        JPanel objectDetailsPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        objectDetailsPanel.add(new JLabel("Object ID:"));
        objectIdField = new JTextField();
        objectDetailsPanel.add(objectIdField);
        objectDetailsPanel.add(new JLabel("X:"));
        objectXField = new JTextField();
        objectDetailsPanel.add(objectXField);
        objectDetailsPanel.add(new JLabel("Y:"));
        objectYField = new JTextField();
        objectDetailsPanel.add(objectYField);
        objectDetailsPanel.add(new JLabel("Height:"));
        objectHeightField = new JTextField();
        objectDetailsPanel.add(objectHeightField);
        objectDetailsPanel.add(new JLabel("Face:"));
        objectFaceField = new JTextField();
        objectDetailsPanel.add(objectFaceField);
        objectDetailsPanel.add(new JLabel("Type:"));
        objectTypeField = new JTextField();
        objectDetailsPanel.add(objectTypeField);
        
        JButton updateObjectButton = new JButton("Update Object");
        objectDetailsPanel.add(updateObjectButton);
        
        objectRightPanel.add(objectDetailsPanel, BorderLayout.NORTH);
        
        JSplitPane objectSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        objectSplitPane.setLeftComponent(objectLeftPanel);
        objectSplitPane.setRightComponent(objectRightPanel);
        objectSplitPane.setDividerLocation(250);
        
        objectPanel.add(objectSplitPane, BorderLayout.CENTER);
        tabbedPane.addTab("Object Spawns", objectPanel);
        
        // Exp Rates Tab
        JPanel expRatesPanel = new JPanel(new BorderLayout());
        
        // Info panel explaining exp calculation
        JPanel expInfoPanel = new JPanel(new BorderLayout());
        expInfoPanel.setBorder(BorderFactory.createTitledBorder("Experience Calculation Info"));
        JTextArea expInfoText = new JTextArea();
        expInfoText.setEditable(false);
        expInfoText.setBackground(expInfoPanel.getBackground());
        expInfoText.setText(
            "Experience Calculation Formula:\n" +
            "Final Experience = Base Experience × Skill Multiplier × Server Exp Bonus\n\n" +
            "• Base Experience: The standard RuneScape XP for the action\n" +
            "• Skill Multiplier: The specific rate for each skill (e.g., Woodcutting Exp)\n" +
            "• Server Exp Bonus: Global multiplier applied to all XP (decimal value, 1.0 = normal)\n\n" +
            "Example: If base XP is 100, Woodcutting Exp is 15, and Server Exp Bonus is 2.0:\n" +
            "Final XP = 100 × 15 × 2.0 = 3000 experience\n\n" +
            "Combat XP (Melee/Range/Magic) uses separate rate multipliers."
        );
        expInfoPanel.add(expInfoText, BorderLayout.CENTER);
        expRatesPanel.add(expInfoPanel, BorderLayout.NORTH);
        
        JPanel expRatesContent = new JPanel(new GridLayout(0, 2, 10, 10));
        expRatesContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        expRatesContent.add(new JLabel("Woodcutting Exp:"));
        woodcuttingExpField = new JTextField();
        expRatesContent.add(woodcuttingExpField);
        
        expRatesContent.add(new JLabel("Mining Exp:"));
        miningExpField = new JTextField();
        expRatesContent.add(miningExpField);
        
        expRatesContent.add(new JLabel("Smithing Exp:"));
        smithingExpField = new JTextField();
        expRatesContent.add(smithingExpField);
        
        expRatesContent.add(new JLabel("Farming Exp:"));
        farmingExpField = new JTextField();
        expRatesContent.add(farmingExpField);
        
        expRatesContent.add(new JLabel("Firemaking Exp:"));
        firemakingExpField = new JTextField();
        expRatesContent.add(firemakingExpField);
        
        expRatesContent.add(new JLabel("Herblore Exp:"));
        herbloreExpField = new JTextField();
        expRatesContent.add(herbloreExpField);
        
        expRatesContent.add(new JLabel("Fishing Exp:"));
        fishingExpField = new JTextField();
        expRatesContent.add(fishingExpField);
        
        expRatesContent.add(new JLabel("Agility Exp:"));
        agilityExpField = new JTextField();
        expRatesContent.add(agilityExpField);
        
        expRatesContent.add(new JLabel("Prayer Exp:"));
        prayerExpField = new JTextField();
        expRatesContent.add(prayerExpField);
        
        expRatesContent.add(new JLabel("Runecrafting Exp:"));
        runecraftingExpField = new JTextField();
        expRatesContent.add(runecraftingExpField);
        
        expRatesContent.add(new JLabel("Crafting Exp:"));
        craftingExpField = new JTextField();
        expRatesContent.add(craftingExpField);
        
        expRatesContent.add(new JLabel("Thieving Exp:"));
        thievingExpField = new JTextField();
        expRatesContent.add(thievingExpField);
        
        expRatesContent.add(new JLabel("Slayer Exp:"));
        slayerExpField = new JTextField();
        expRatesContent.add(slayerExpField);
        
        expRatesContent.add(new JLabel("Cooking Exp:"));
        cookingExpField = new JTextField();
        expRatesContent.add(cookingExpField);
        
        expRatesContent.add(new JLabel("Fletching Exp:"));
        fletchingExpField = new JTextField();
        expRatesContent.add(fletchingExpField);
        
        expRatesContent.add(new JLabel("Construction Exp:"));
        constructionExpField = new JTextField();
        expRatesContent.add(constructionExpField);
        
        expRatesContent.add(new JLabel("Hunter Exp:"));
        hunterExpField = new JTextField();
        expRatesContent.add(hunterExpField);
        
        expRatesContent.add(new JLabel("Melee Exp Rate:"));
        meleeExpField = new JTextField();
        expRatesContent.add(meleeExpField);
        
        expRatesContent.add(new JLabel("Range Exp Rate:"));
        rangeExpField = new JTextField();
        expRatesContent.add(rangeExpField);
        
        expRatesContent.add(new JLabel("Magic Exp Rate:"));
        magicExpField = new JTextField();
        expRatesContent.add(magicExpField);
        
        expRatesContent.add(new JLabel("Server Exp Bonus:"));
        serverExpBonusField = new JTextField();
        expRatesContent.add(serverExpBonusField);
        
        JButton saveExpRatesButton = new JButton("Save Exp Rates");
        expRatesContent.add(new JLabel(""));
        expRatesContent.add(saveExpRatesButton);
        
        JScrollPane expRatesScrollPane = new JScrollPane(expRatesContent);
        expRatesPanel.add(expRatesScrollPane, BorderLayout.CENTER);
        tabbedPane.addTab("Exp Rates", expRatesPanel);
        
        // Starting Inventory Tab
        JPanel inventoryPanel = new JPanel(new BorderLayout());
        JPanel inventoryLeftPanel = new JPanel(new BorderLayout());
        inventoryLeftPanel.setBorder(BorderFactory.createTitledBorder("Starting Inventory"));
        
        inventoryListModel = new DefaultListModel<>();
        inventoryList = new JList<>(inventoryListModel);
        JScrollPane inventoryScrollPane = new JScrollPane(inventoryList);
        inventoryLeftPanel.add(inventoryScrollPane, BorderLayout.CENTER);
        
        JPanel inventoryButtonPanel = new JPanel(new FlowLayout());
        JButton addInventoryItemButton = new JButton("Add Item");
        JButton deleteInventoryItemButton = new JButton("Delete Item");
        inventoryButtonPanel.add(addInventoryItemButton);
        inventoryButtonPanel.add(deleteInventoryItemButton);
        inventoryLeftPanel.add(inventoryButtonPanel, BorderLayout.SOUTH);
        
        JPanel inventoryRightPanel = new JPanel(new BorderLayout());
        inventoryRightPanel.setBorder(BorderFactory.createTitledBorder("Item Details"));
        
        JPanel inventoryDetailsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inventoryDetailsPanel.add(new JLabel("Item ID:"));
        inventoryItemIdField = new JTextField();
        inventoryDetailsPanel.add(inventoryItemIdField);
        inventoryDetailsPanel.add(new JLabel("Amount:"));
        inventoryAmountField = new JTextField();
        inventoryDetailsPanel.add(inventoryAmountField);
        
        JButton updateInventoryButton = new JButton("Update Item");
        inventoryDetailsPanel.add(updateInventoryButton);
        
        inventoryRightPanel.add(inventoryDetailsPanel, BorderLayout.NORTH);
        
        JSplitPane inventorySplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        inventorySplitPane.setLeftComponent(inventoryLeftPanel);
        inventorySplitPane.setRightComponent(inventoryRightPanel);
        inventorySplitPane.setDividerLocation(250);
        
        inventoryPanel.add(inventorySplitPane, BorderLayout.CENTER);
        tabbedPane.addTab("Starting Inventory", inventoryPanel);
        
        // Starting Bank Tab
        JPanel bankPanel = new JPanel(new BorderLayout());
        JPanel bankLeftPanel = new JPanel(new BorderLayout());
        bankLeftPanel.setBorder(BorderFactory.createTitledBorder("Starting Bank"));
        
        bankListModel = new DefaultListModel<>();
        bankList = new JList<>(bankListModel);
        JScrollPane bankScrollPane = new JScrollPane(bankList);
        bankLeftPanel.add(bankScrollPane, BorderLayout.CENTER);
        
        JPanel bankButtonPanel = new JPanel(new FlowLayout());
        JButton addBankItemButton = new JButton("Add Item");
        JButton deleteBankItemButton = new JButton("Delete Item");
        bankButtonPanel.add(addBankItemButton);
        bankButtonPanel.add(deleteBankItemButton);
        bankLeftPanel.add(bankButtonPanel, BorderLayout.SOUTH);
        
        JPanel bankRightPanel = new JPanel(new BorderLayout());
        bankRightPanel.setBorder(BorderFactory.createTitledBorder("Item Details"));
        
        JPanel bankDetailsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        bankDetailsPanel.add(new JLabel("Item ID:"));
        bankItemIdField = new JTextField();
        bankDetailsPanel.add(bankItemIdField);
        bankDetailsPanel.add(new JLabel("Amount:"));
        bankAmountField = new JTextField();
        bankDetailsPanel.add(bankAmountField);
        
        JButton updateBankButton = new JButton("Update Item");
        bankDetailsPanel.add(updateBankButton);
        
        bankRightPanel.add(bankDetailsPanel, BorderLayout.NORTH);
        
        JSplitPane bankSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bankSplitPane.setLeftComponent(bankLeftPanel);
        bankSplitPane.setRightComponent(bankRightPanel);
        bankSplitPane.setDividerLocation(250);
        
        bankPanel.add(bankSplitPane, BorderLayout.CENTER);
        tabbedPane.addTab("Starting Bank", bankPanel);
        
        // Player Rights Tab
        JPanel rightsPanel = new JPanel(new BorderLayout());
        JPanel rightsLeftPanel = new JPanel(new BorderLayout());
        rightsLeftPanel.setBorder(BorderFactory.createTitledBorder("Player Rights"));
        
        rightsListModel = new DefaultListModel<>();
        rightsList = new JList<>(rightsListModel);
        JScrollPane rightsScrollPane = new JScrollPane(rightsList);
        rightsLeftPanel.add(rightsScrollPane, BorderLayout.CENTER);
        
        JPanel rightsButtonPanel = new JPanel(new FlowLayout());
        JButton addRightButton = new JButton("Add Right");
        JButton deleteRightButton = new JButton("Delete Right");
        rightsButtonPanel.add(addRightButton);
        rightsButtonPanel.add(deleteRightButton);
        rightsLeftPanel.add(rightsButtonPanel, BorderLayout.SOUTH);
        
        JPanel rightsRightPanel = new JPanel(new BorderLayout());
        rightsRightPanel.setBorder(BorderFactory.createTitledBorder("Right Details"));
        
        JPanel rightsDetailsPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        rightsDetailsPanel.add(new JLabel("Level:"));
        rightLevelField = new JTextField();
        rightsDetailsPanel.add(rightLevelField);
        rightsDetailsPanel.add(new JLabel("Name:"));
        rightNameField = new JTextField();
        rightsDetailsPanel.add(rightNameField);
        rightsDetailsPanel.add(new JLabel("Prefix:"));
        rightPrefixField = new JTextField();
        rightsDetailsPanel.add(rightPrefixField);
        rightsDetailsPanel.add(new JLabel("Color:"));
        rightColorField = new JTextField();
        rightsDetailsPanel.add(rightColorField);
        
        JButton updateRightButton = new JButton("Update Right");
        rightsDetailsPanel.add(updateRightButton);
        
        rightsRightPanel.add(rightsDetailsPanel, BorderLayout.NORTH);
        
        JSplitPane rightsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightsSplitPane.setLeftComponent(rightsLeftPanel);
        rightsSplitPane.setRightComponent(rightsRightPanel);
        rightsSplitPane.setDividerLocation(250);
        
        rightsPanel.add(rightsSplitPane, BorderLayout.CENTER);
        tabbedPane.addTab("Player Rights", rightsPanel);
        
        // Doors Tab
        JPanel doorsPanel = new JPanel(new BorderLayout());
        JPanel doorsLeftPanel = new JPanel(new BorderLayout());
        doorsLeftPanel.setBorder(BorderFactory.createTitledBorder("Doors"));
        
        doorListModel = new DefaultListModel<>();
        doorList = new JList<>(doorListModel);
        JScrollPane doorScrollPane = new JScrollPane(doorList);
        doorsLeftPanel.add(doorScrollPane, BorderLayout.CENTER);
        
        JPanel doorsButtonPanel = new JPanel(new FlowLayout());
        JButton addDoorButton = new JButton("Add Door");
        JButton deleteDoorButton = new JButton("Delete Door");
        doorsButtonPanel.add(addDoorButton);
        doorsButtonPanel.add(deleteDoorButton);
        doorsLeftPanel.add(doorsButtonPanel, BorderLayout.SOUTH);
        
        JPanel doorsRightPanel = new JPanel(new BorderLayout());
        doorsRightPanel.setBorder(BorderFactory.createTitledBorder("Door Details"));
        
        JPanel doorsDetailsPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        doorsDetailsPanel.add(new JLabel("X:"));
        doorXField = new JTextField();
        doorsDetailsPanel.add(doorXField);
        doorsDetailsPanel.add(new JLabel("Y:"));
        doorYField = new JTextField();
        doorsDetailsPanel.add(doorYField);
        doorsDetailsPanel.add(new JLabel("Height:"));
        doorHeightField = new JTextField();
        doorsDetailsPanel.add(doorHeightField);
        doorsDetailsPanel.add(new JLabel("Face:"));
        doorFaceField = new JTextField();
        doorsDetailsPanel.add(doorFaceField);
        doorsDetailsPanel.add(new JLabel("State:"));
        doorStateField = new JTextField();
        doorsDetailsPanel.add(doorStateField);
        
        JButton updateDoorButton = new JButton("Update Door");
        doorsDetailsPanel.add(updateDoorButton);
        
        doorsRightPanel.add(doorsDetailsPanel, BorderLayout.NORTH);
        
        JSplitPane doorsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        doorsSplitPane.setLeftComponent(doorsLeftPanel);
        doorsSplitPane.setRightComponent(doorsRightPanel);
        doorsSplitPane.setDividerLocation(250);
        
        doorsPanel.add(doorsSplitPane, BorderLayout.CENTER);
        tabbedPane.addTab("Doors", doorsPanel);
        
        // Object Size Tab
        JPanel objectSizePanel = new JPanel(new BorderLayout());
        JPanel objectSizeLeftPanel = new JPanel(new BorderLayout());
        objectSizeLeftPanel.setBorder(BorderFactory.createTitledBorder("Object Sizes"));
        
        objectSizeListModel = new DefaultListModel<>();
        objectSizeList = new JList<>(objectSizeListModel);
        JScrollPane objectSizeScrollPane = new JScrollPane(objectSizeList);
        objectSizeLeftPanel.add(objectSizeScrollPane, BorderLayout.CENTER);
        
        JPanel objectSizeButtonPanel = new JPanel(new FlowLayout());
        JButton addObjectSizeButton = new JButton("Add Object Size");
        JButton deleteObjectSizeButton = new JButton("Delete Object Size");
        objectSizeButtonPanel.add(addObjectSizeButton);
        objectSizeButtonPanel.add(deleteObjectSizeButton);
        objectSizeLeftPanel.add(objectSizeButtonPanel, BorderLayout.SOUTH);
        
        JPanel objectSizeRightPanel = new JPanel(new BorderLayout());
        objectSizeRightPanel.setBorder(BorderFactory.createTitledBorder("Object Size Details"));
        
        JPanel objectSizeDetailsPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        objectSizeDetailsPanel.add(new JLabel("Object ID:"));
        objectSizeIdField = new JTextField();
        objectSizeDetailsPanel.add(objectSizeIdField);
        objectSizeDetailsPanel.add(new JLabel("Name:"));
        objectSizeNameField = new JTextField();
        objectSizeDetailsPanel.add(objectSizeNameField);
        objectSizeDetailsPanel.add(new JLabel("Size:"));
        objectSizeSizeField = new JTextField();
        objectSizeDetailsPanel.add(objectSizeSizeField);
        objectSizeDetailsPanel.add(new JLabel("Examine:"));
        objectSizeExamField = new JTextField();
        objectSizeDetailsPanel.add(objectSizeExamField);
        
        JButton updateObjectSizeButton = new JButton("Update Object Size");
        objectSizeDetailsPanel.add(updateObjectSizeButton);
        
        objectSizeRightPanel.add(objectSizeDetailsPanel, BorderLayout.NORTH);
        
        JSplitPane objectSizeSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        objectSizeSplitPane.setLeftComponent(objectSizeLeftPanel);
        objectSizeSplitPane.setRightComponent(objectSizeRightPanel);
        objectSizeSplitPane.setDividerLocation(250);
        
        objectSizePanel.add(objectSizeSplitPane, BorderLayout.CENTER);
        tabbedPane.addTab("Object Sizes", objectSizePanel);
        
        add(tabbedPane, BorderLayout.CENTER);

        // Shop list selection listener
        shopList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedShopChanged();
            }
        });
        
        // NPC list selection listener
        npcList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedNpcChanged();
            }
        });
        
        // Drops NPC list selection listener
        dropsNpcList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedDropsNpcChanged();
            }
        });
        
        // Stats NPC list selection listener
        statsNpcList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedStatsNpcChanged();
            }
        });
        
        // Teleport list selection listener
        teleportList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedTeleportChanged();
            }
        });
        
        // Spawn point list selection listener
        spawnPointList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedSpawnPointChanged();
            }
        });
        
        // Object list selection listener
        objectList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedObjectChanged();
            }
        });
        
        // Inventory list selection listener
        inventoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedInventoryItemChanged();
            }
        });
        
        // Bank list selection listener
        bankList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedBankItemChanged();
            }
        });
        
        // Rights list selection listener
        rightsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedRightChanged();
            }
        });
        
        // Door list selection listener
        doorList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedDoorChanged();
            }
        });
        
        // Object Size list selection listener
        objectSizeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedObjectSizeChanged();
            }
        });
        
        addTeleportButton.addActionListener(e -> addTeleport());
        deleteTeleportButton.addActionListener(e -> deleteTeleport());
        updateTeleportButton.addActionListener(e -> updateTeleport());
        addSpawnPointButton.addActionListener(e -> addSpawnPoint());
        deleteSpawnPointButton.addActionListener(e -> deleteSpawnPoint());
        updateSpawnPointButton.addActionListener(e -> updateSpawnPoint());
        addObjectButton.addActionListener(e -> addObject());
        deleteObjectButton.addActionListener(e -> deleteObject());
        updateObjectButton.addActionListener(e -> updateObject());
        saveExpRatesButton.addActionListener(e -> saveExpRates());
        addInventoryItemButton.addActionListener(e -> addInventoryItem());
        deleteInventoryItemButton.addActionListener(e -> deleteInventoryItem());
        updateInventoryButton.addActionListener(e -> updateInventoryItem());
        addBankItemButton.addActionListener(e -> addBankItem());
        deleteBankItemButton.addActionListener(e -> deleteBankItem());
        updateBankButton.addActionListener(e -> updateBankItem());
        addRightButton.addActionListener(e -> addRight());
        deleteRightButton.addActionListener(e -> deleteRight());
        updateRightButton.addActionListener(e -> updateRight());
        addDoorButton.addActionListener(e -> addDoor());
        deleteDoorButton.addActionListener(e -> deleteDoor());
        updateDoorButton.addActionListener(e -> updateDoor());
        addObjectSizeButton.addActionListener(e -> addObjectSize());
        deleteObjectSizeButton.addActionListener(e -> deleteObjectSize());
        updateObjectSizeButton.addActionListener(e -> updateObjectSize());
        
        addDropButton.addActionListener(e -> addDrop());
        removeDropButton.addActionListener(e -> removeDrop());
        searchDropItemButton.addActionListener(e -> searchItemByName());
        updateStatsButton.addActionListener(e -> updateNpcStats());
        dropsSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterNpcs(dropsSearchField.getText(), dropsNpcListModel);
            }
        });
        statsSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterNpcs(statsSearchField.getText(), statsNpcListModel);
            }
        });
        
        addNpcButton.addActionListener(e -> addNpcSpawn());
        duplicateNpcButton.addActionListener(e -> duplicateNpcSpawn());
        deleteNpcButton.addActionListener(e -> deleteNpcSpawn());
        bulkDeleteButton.addActionListener(e -> bulkDeleteNpcSpawns());
        undoButton.addActionListener(e -> undo());
        redoButton.addActionListener(e -> redo());
        updateNpcButton.addActionListener(e -> updateNpcSpawn());
        searchNpcButton.addActionListener(e -> searchNpcByName());
        npcSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterNpcs(npcSearchField.getText());
            }
        });

        // Auto-load on startup
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("Biohazard v3 server client cache/Proxy Server/Data/cfg"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            shopsFile = fileChooser.getSelectedFile();
            loadShops();
            loadItemNames();
            
            // Auto-load related files
            File cfgDir = shopsFile.getParentFile();
            spawnConfigFile = new File(cfgDir, "spawn-config.cfg");
            npcShopsFile = new File(cfgDir, "npc-shops.cfg");
            npcDropsFile = new File(cfgDir, "npc_drops.cfg");
            npcStatsFile = new File(cfgDir, "npc.cfg");
            teleportsFile = new File(cfgDir, "teleports.cfg");
            spawnPointsFile = new File(cfgDir, "spawn-points.cfg");
            objectsFile = new File(cfgDir, "global-objects.cfg");
            configFile = new File("Biohazard v3 server client cache/Proxy Server/src/server/Config.java");
            startingInventoryFile = new File(cfgDir, "starting-inventory.cfg");
            startingBankFile = new File(cfgDir, "starting-bank.cfg");
            playerRightsFile = new File(cfgDir, "player-rights.cfg");
            doorsFile = new File(cfgDir, "doors.cfg");
            objectSizeFile = new File("Biohazard v3 server client cache/Proxy Server/Data/objectSize.cfg");
            loadNpcSpawns();
            loadNpcNames();
            loadNpcShops();
            loadNpcDrops();
            loadNpcStats();
            loadTeleports();
            loadSpawnPoints();
            loadObjectSpawns();
            loadExpRates();
            loadStartingInventory();
            loadStartingBank();
            loadPlayerRights();
            loadDoors();
            loadObjectSizes();
        }
    }

    private void loadShops() {
        shops.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(shopsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("shop = ") || line.startsWith("shop=")) {
                    Shop shop = parseShopLine(line);
                    if (shop != null) {
                        shops.add(shop);
                    }
                }
            }
            updateShopList();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadItemNames() {
        itemNames.clear();
        File itemCfgFile = new File(shopsFile.getParentFile(), "item.cfg");
        if (!itemCfgFile.exists()) {
            itemCfgFile = new File("Biohazard v3 server client cache/Proxy Server/Data/cfg/item.cfg");
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(itemCfgFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("item = ") || line.startsWith("item=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int itemId = Integer.parseInt(parts[0]);
                            String itemName = parts[1].replace("_", " ");
                            itemNames.put(itemId, itemName);
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not load item names: " + e.getMessage());
        }
    }

    private void loadNpcSpawns() {
        npcSpawns.clear();
        if (spawnConfigFile == null || !spawnConfigFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(spawnConfigFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("spawn = ")) {
                    NpcSpawn spawn = parseSpawnLine(line);
                    if (spawn != null) {
                        npcSpawns.add(spawn);
                    }
                }
            }
            updateNpcList();
        } catch (IOException e) {
            System.out.println("Could not load NPC spawns: " + e.getMessage());
        }
    }

    private void loadNpcNames() {
        npcNames.clear();
        File npcCfgFile = new File(shopsFile.getParentFile(), "npc.cfg");
        if (!npcCfgFile.exists()) {
            npcCfgFile = new File("Biohazard v3 server client cache/Proxy Server/Data/cfg/npc.cfg");
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(npcCfgFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("npc = ") || line.startsWith("npc=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int npcId = Integer.parseInt(parts[0]);
                            String npcName = parts[1].replace("_", " ");
                            npcNames.put(npcId, npcName);
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not load NPC names: " + e.getMessage());
        }
    }

    private void loadNpcShops() {
        npcShopMap.clear();
        if (npcShopsFile == null || !npcShopsFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(npcShopsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("npc-shop = ") || line.startsWith("npc-shop=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int npcId = Integer.parseInt(parts[0]);
                            int shopId = Integer.parseInt(parts[1]);
                            npcShopMap.put(npcId, shopId);
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not load NPC shop mappings: " + e.getMessage());
        }
    }

    private void loadNpcDrops() {
        npcDropsMap.clear();
        if (npcDropsFile == null || !npcDropsFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(npcDropsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("drop = ") || line.startsWith("drop=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            String[] npcIds = parts[0].split("/");
                            for (String npcIdStr : npcIds) {
                                int npcId = Integer.parseInt(npcIdStr);
                                List<NpcDrop> drops = npcDropsMap.computeIfAbsent(npcId, k -> new ArrayList<>());
                                
                                for (int i = 1; i < parts.length; i++) {
                                    String dropStr = parts[i];
                                    if (dropStr.contains(":")) {
                                        String[] dropParts = dropStr.split(":");
                                        if (dropParts.length >= 3) {
                                            int itemId = Integer.parseInt(dropParts[0]);
                                            int amount = Integer.parseInt(dropParts[1]);
                                            String rarity = dropParts[2];
                                            drops.add(new NpcDrop(itemId, amount, rarity));
                                        }
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not load NPC drops: " + e.getMessage());
        }
    }

    private void loadNpcStats() {
        npcStatsMap.clear();
        if (npcStatsFile == null || !npcStatsFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(npcStatsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("npc = ") || line.startsWith("npc=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int npcId = Integer.parseInt(parts[0]);
                            int combat = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                            int health = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                            int attack = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
                            int defense = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
                            int attackSpeed = parts.length > 5 ? Integer.parseInt(parts[5]) : 0;
                            npcStatsMap.put(npcId, new NpcStats(npcId, combat, health, attack, defense, attackSpeed));
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not load NPC stats: " + e.getMessage());
        }
    }

    private void loadTeleports() {
        teleports.clear();
        if (teleportsFile == null || !teleportsFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(teleportsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("teleport = ") || line.startsWith("teleport=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 4) {
                        try {
                            String category = parts[0].replace("_", " ");
                            String name = parts[1].replace("_", " ");
                            int x = Integer.parseInt(parts[2]);
                            int y = Integer.parseInt(parts[3]);
                            teleports.add(new TeleportLocation(category, name, x, y));
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
            updateTeleportList();
        } catch (IOException e) {
            System.out.println("Could not load teleports: " + e.getMessage());
        }
    }

    private void loadObjectSpawns() {
        objectSpawns.clear();
        if (objectsFile == null || !objectsFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(objectsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("object = ") || line.startsWith("object=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 6) {
                        try {
                            int objectId = Integer.parseInt(parts[0]);
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            int height = Integer.parseInt(parts[3]);
                            int face = Integer.parseInt(parts[4]);
                            int type = Integer.parseInt(parts[5]);
                            objectSpawns.add(new ObjectSpawn(objectId, x, y, height, face, type));
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
            updateObjectList();
        } catch (IOException e) {
            System.out.println("Could not load object spawns: " + e.getMessage());
        }
    }

    private void loadExpRates() {
        if (configFile == null || !configFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("WOODCUTTING_EXPERIENCE =")) {
                    woodcuttingExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("MINING_EXPERIENCE =")) {
                    miningExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("SMITHING_EXPERIENCE =")) {
                    smithingExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("FARMING_EXPERIENCE =")) {
                    farmingExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("FIREMAKING_EXPERIENCE =")) {
                    firemakingExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("HERBLORE_EXPERIENCE =")) {
                    herbloreExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("FISHING_EXPERIENCE =")) {
                    fishingExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("AGILITY_EXPERIENCE =")) {
                    agilityExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("PRAYER_EXPERIENCE =")) {
                    prayerExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("RUNECRAFTING_EXPERIENCE =")) {
                    runecraftingExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("CRAFTING_EXPERIENCE =")) {
                    craftingExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("THIEVING_EXPERIENCE =")) {
                    thievingExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("SLAYER_EXPERIENCE =")) {
                    slayerExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("COOKING_EXPERIENCE =")) {
                    cookingExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("FLETCHING_EXPERIENCE =")) {
                    fletchingExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("CONSTRUCTION_EXPERIENCE =")) {
                    constructionExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("HUNTER_EXPERIENCE =")) {
                    hunterExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("MELEE_EXP_RATE =")) {
                    meleeExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("RANGE_EXP_RATE =")) {
                    rangeExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("MAGIC_EXP_RATE =")) {
                    magicExpField.setText(line.split("=")[1].trim().replace(";", ""));
                } else if (line.contains("SERVER_EXP_BONUS =")) {
                    serverExpBonusField.setText(line.split("=")[1].trim().replace(";", ""));
                }
            }
        } catch (IOException e) {
            System.out.println("Could not load exp rates: " + e.getMessage());
        }
    }

    private void loadSpawnPoints() {
        spawnPoints.clear();
        if (spawnPointsFile == null || !spawnPointsFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(spawnPointsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("spawn = ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 5) {
                        try {
                            String name = parts[2].replace("_", " ");
                            int x = Integer.parseInt(parts[3]);
                            int y = Integer.parseInt(parts[4]);
                            spawnPoints.add(new SpawnPoint(name, x, y));
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
            updateSpawnPointList();
        } catch (IOException e) {
            System.out.println("Could not load spawn points: " + e.getMessage());
        }
    }

    private Shop parseShopLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 4) return null;

        try {
            int shopId = Integer.parseInt(parts[2]);
            String shopName = parts[3].replace("_", " ");
            int sellModifier = Integer.parseInt(parts[4]);
            int buyModifier = Integer.parseInt(parts[5]);

            Shop shop = new Shop(shopId, shopName, sellModifier, buyModifier);

            for (int i = 6; i < parts.length; i += 2) {
                if (i + 1 < parts.length) {
                    int itemId = Integer.parseInt(parts[i]);
                    int amount = Integer.parseInt(parts[i + 1]);
                    shop.addItem(itemId, amount);
                }
            }
            return shop;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private NpcSpawn parseSpawnLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 5) return null;

        try {
            int npcId = Integer.parseInt(parts[2]);
            int x = Integer.parseInt(parts[3]);
            int y = Integer.parseInt(parts[4]);
            int height = Integer.parseInt(parts[5]);
            return new NpcSpawn(npcId, x, y, height);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateShopList() {
        DefaultListModel<Shop> model = new DefaultListModel<>();
        for (Shop shop : shops) {
            model.addElement(shop);
        }
        shopList.setModel(model);
    }

    private void updateNpcList() {
        npcListModel.clear();
        for (NpcSpawn spawn : npcSpawns) {
            npcListModel.addElement(spawn);
        }
        // Update other NPC lists too
        dropsNpcListModel.clear();
        statsNpcListModel.clear();
        for (NpcSpawn spawn : npcSpawns) {
            dropsNpcListModel.addElement(spawn);
            statsNpcListModel.addElement(spawn);
        }
    }

    private void updateTeleportList() {
        teleportListModel.clear();
        for (TeleportLocation teleport : teleports) {
            teleportListModel.addElement(teleport);
        }
    }

    private void updateSpawnPointList() {
        spawnPointListModel.clear();
        for (SpawnPoint spawnPoint : spawnPoints) {
            spawnPointListModel.addElement(spawnPoint);
        }
    }

    private void updateObjectList() {
        objectListModel.clear();
        for (ObjectSpawn object : objectSpawns) {
            objectListModel.addElement(object);
        }
    }

    private void selectedShopChanged() {
        Shop selected = shopList.getSelectedValue();
        if (selected != null) {
            shopIdField.setText(String.valueOf(selected.id));
            shopNameField.setText(selected.name);
            sellModifierField.setText(String.valueOf(selected.sellModifier));
            buyModifierField.setText(String.valueOf(selected.buyModifier));
            updateItemTable(selected);
        }
    }

    private void filterItems(String searchText) {
        Shop selected = shopList.getSelectedValue();
        if (selected != null) {
            updateItemTable(selected, searchText);
        }
    }

    private void selectedNpcChanged() {
        NpcSpawn selected = npcList.getSelectedValue();
        if (selected != null) {
            npcIdField.setText(String.valueOf(selected.npcId));
            npcXField.setText(String.valueOf(selected.x));
            npcYField.setText(String.valueOf(selected.y));
            npcHeightField.setText(String.valueOf(selected.height));
            Integer shopId = npcShopMap.get(selected.npcId);
            npcShopIdField.setText(shopId != null ? String.valueOf(shopId) : "");
        }
    }

    private void selectedDropsNpcChanged() {
        NpcSpawn selected = dropsNpcList.getSelectedValue();
        if (selected != null) {
            updateDropTable(selected.npcId);
        }
    }

    private void selectedStatsNpcChanged() {
        NpcSpawn selected = statsNpcList.getSelectedValue();
        if (selected != null) {
            statsNpcIdField.setText(String.valueOf(selected.npcId));
            NpcStats stats = npcStatsMap.get(selected.npcId);
            if (stats != null) {
                npcCombatField.setText(String.valueOf(stats.combat));
                npcHealthField.setText(String.valueOf(stats.health));
                npcAttackField.setText(String.valueOf(stats.attack));
                npcDefenseField.setText(String.valueOf(stats.defense));
                npcAttackSpeedField.setText(String.valueOf(stats.attackSpeed));
            } else {
                npcCombatField.setText("");
                npcHealthField.setText("");
                npcAttackField.setText("");
                npcDefenseField.setText("");
                npcAttackSpeedField.setText("");
            }
        }
    }

    private void selectedTeleportChanged() {
        TeleportLocation selected = teleportList.getSelectedValue();
        if (selected != null) {
            teleportCategoryField.setText(selected.category);
            teleportNameField.setText(selected.name);
            teleportXField.setText(String.valueOf(selected.x));
            teleportYField.setText(String.valueOf(selected.y));
        }
    }

    private void selectedSpawnPointChanged() {
        SpawnPoint selected = spawnPointList.getSelectedValue();
        if (selected != null) {
            spawnPointNameField.setText(selected.name);
            spawnPointXField.setText(String.valueOf(selected.x));
            spawnPointYField.setText(String.valueOf(selected.y));
        }
    }

    private void selectedObjectChanged() {
        ObjectSpawn selected = objectList.getSelectedValue();
        if (selected != null) {
            objectIdField.setText(String.valueOf(selected.objectId));
            objectXField.setText(String.valueOf(selected.x));
            objectYField.setText(String.valueOf(selected.y));
            objectHeightField.setText(String.valueOf(selected.height));
            objectFaceField.setText(String.valueOf(selected.face));
            objectTypeField.setText(String.valueOf(selected.type));
        }
    }

    private void updateDropTable(int npcId) {
        dropTableModel.setRowCount(0);
        List<NpcDrop> drops = npcDropsMap.get(npcId);
        if (drops != null) {
            for (NpcDrop drop : drops) {
                String itemName = itemNames.get(drop.itemId);
                dropTableModel.addRow(new Object[]{drop.itemId, itemName != null ? itemName : "", drop.amount, drop.rarity});
            }
        }
    }

    private void addDrop() {
        NpcSpawn selected = dropsNpcList.getSelectedValue();
        if (selected != null) {
            try {
                int itemId = Integer.parseInt(dropItemIdField.getText());
                int amount = Integer.parseInt(dropAmountField.getText());
                String rarity = (String) rarityCombo.getSelectedItem();
                
                List<NpcDrop> drops = npcDropsMap.computeIfAbsent(selected.npcId, k -> new ArrayList<>());
                drops.add(new NpcDrop(itemId, amount, rarity));
                updateDropTable(selected.npcId);
                
                if (autoSaveCheckbox.isSelected()) {
                    saveNpcFiles();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid numeric value", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeDrop() {
        int selectedRow = dropTable.getSelectedRow();
        if (selectedRow != -1) {
            NpcSpawn selected = dropsNpcList.getSelectedValue();
            if (selected != null) {
                List<NpcDrop> drops = npcDropsMap.get(selected.npcId);
                if (drops != null && selectedRow < drops.size()) {
                    drops.remove(selectedRow);
                    updateDropTable(selected.npcId);
                    
                    if (autoSaveCheckbox.isSelected()) {
                        saveNpcFiles();
                    }
                }
            }
        }
    }

    private void updateNpcStats() {
        NpcSpawn selected = statsNpcList.getSelectedValue();
        if (selected != null) {
            try {
                int combat = Integer.parseInt(npcCombatField.getText());
                int health = Integer.parseInt(npcHealthField.getText());
                int attack = Integer.parseInt(npcAttackField.getText());
                int defense = Integer.parseInt(npcDefenseField.getText());
                int attackSpeed = Integer.parseInt(npcAttackSpeedField.getText());
                
                npcStatsMap.put(selected.npcId, new NpcStats(selected.npcId, combat, health, attack, defense, attackSpeed));
                
                if (autoSaveCheckbox.isSelected()) {
                    saveNpcFiles();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid numeric value", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addTeleport() {
        TeleportLocation newTeleport = new TeleportLocation("Modern", "New Teleport", 3222, 3222);
        teleports.add(newTeleport);
        updateTeleportList();
        teleportList.setSelectedIndex(teleports.size() - 1);
    }

    private void deleteTeleport() {
        TeleportLocation selected = teleportList.getSelectedValue();
        if (selected != null) {
            teleports.remove(selected);
            updateTeleportList();
        }
    }

    private void updateTeleport() {
        TeleportLocation selected = teleportList.getSelectedValue();
        if (selected != null) {
            try {
                selected.category = teleportCategoryField.getText();
                selected.name = teleportNameField.getText();
                selected.x = Integer.parseInt(teleportXField.getText());
                selected.y = Integer.parseInt(teleportYField.getText());
                updateTeleportList();
                
                if (autoSaveCheckbox.isSelected()) {
                    saveTeleportFiles();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid numeric value", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addObject() {
        ObjectSpawn newObject = new ObjectSpawn(9981, 3254, 3429, 0, -1, 10);
        objectSpawns.add(newObject);
        updateObjectList();
        objectList.setSelectedIndex(objectSpawns.size() - 1);
    }

    private void deleteObject() {
        ObjectSpawn selected = objectList.getSelectedValue();
        if (selected != null) {
            objectSpawns.remove(selected);
            updateObjectList();
            saveObjectFile();
        }
    }

    private void updateObject() {
        ObjectSpawn selected = objectList.getSelectedValue();
        if (selected != null) {
            try {
                selected.objectId = Integer.parseInt(objectIdField.getText());
                selected.x = Integer.parseInt(objectXField.getText());
                selected.y = Integer.parseInt(objectYField.getText());
                selected.height = Integer.parseInt(objectHeightField.getText());
                selected.face = Integer.parseInt(objectFaceField.getText());
                selected.type = Integer.parseInt(objectTypeField.getText());
                updateObjectList();
                
                if (autoSaveCheckbox.isSelected()) {
                    saveObjectFile();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid numeric value", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveObjectFile() {
        if (objectsFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(objectsFile))) {
                writer.println("// objectId\tX\tY\tH\tFace\tobjectType");
                for (ObjectSpawn object : objectSpawns) {
                    writer.println("object = " + object.objectId + "\t" + object.x + "\t" + object.y + "\t" + object.height + "\t" + object.face + "\t" + object.type);
                }
                writer.println("[ENDOFOBJECTLIST]");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving global-objects.cfg: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveExpRates() {
        try {
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(configFile))) {
                for (String line : lines) {
                    if (line.contains("WOODCUTTING_EXPERIENCE")) {
                        writer.println("    public static final int WOODCUTTING_EXPERIENCE = " + woodcuttingExpField.getText() + ";");
                    } else if (line.contains("MINING_EXPERIENCE")) {
                        writer.println("    public static final int MINING_EXPERIENCE = " + miningExpField.getText() + ";");
                    } else if (line.contains("SMITHING_EXPERIENCE")) {
                        writer.println("    public static final int SMITHING_EXPERIENCE = " + smithingExpField.getText() + ";");
                    } else if (line.contains("FARMING_EXPERIENCE")) {
                        writer.println("    public static final int FARMING_EXPERIENCE = " + farmingExpField.getText() + ";");
                    } else if (line.contains("FIREMAKING_EXPERIENCE")) {
                        writer.println("    public static final int FIREMAKING_EXPERIENCE = " + firemakingExpField.getText() + ";");
                    } else if (line.contains("HERBLORE_EXPERIENCE")) {
                        writer.println("    public static final int HERBLORE_EXPERIENCE = " + herbloreExpField.getText() + ";");
                    } else if (line.contains("FISHING_EXPERIENCE")) {
                        writer.println("    public static final int FISHING_EXPERIENCE = " + fishingExpField.getText() + ";");
                    } else if (line.contains("AGILITY_EXPERIENCE")) {
                        writer.println("    public static final int AGILITY_EXPERIENCE = " + agilityExpField.getText() + ";");
                    } else if (line.contains("PRAYER_EXPERIENCE")) {
                        writer.println("    public static final int PRAYER_EXPERIENCE = " + prayerExpField.getText() + ";");
                    } else if (line.contains("RUNECRAFTING_EXPERIENCE")) {
                        writer.println("    public static final int RUNECRAFTING_EXPERIENCE = " + runecraftingExpField.getText() + ";");
                    } else if (line.contains("CRAFTING_EXPERIENCE")) {
                        writer.println("    public static final int CRAFTING_EXPERIENCE = " + craftingExpField.getText() + ";");
                    } else if (line.contains("THIEVING_EXPERIENCE")) {
                        writer.println("    public static final int THIEVING_EXPERIENCE = " + thievingExpField.getText() + ";");
                    } else if (line.contains("SLAYER_EXPERIENCE")) {
                        writer.println("    public static final int SLAYER_EXPERIENCE = " + slayerExpField.getText() + ";");
                    } else if (line.contains("COOKING_EXPERIENCE")) {
                        writer.println("    public static final int COOKING_EXPERIENCE = " + cookingExpField.getText() + ";");
                    } else if (line.contains("FLETCHING_EXPERIENCE")) {
                        writer.println("    public static final int FLETCHING_EXPERIENCE = " + fletchingExpField.getText() + ";");
                    } else if (line.contains("CONSTRUCTION_EXPERIENCE")) {
                        writer.println("    public static final int CONSTRUCTION_EXPERIENCE = " + constructionExpField.getText() + ";");
                    } else if (line.contains("HUNTER_EXPERIENCE")) {
                        writer.println("    public static final int HUNTER_EXPERIENCE = " + hunterExpField.getText() + ";");
                    } else if (line.contains("MELEE_EXP_RATE")) {
                        writer.println("    public static final int MELEE_EXP_RATE = " + meleeExpField.getText() + ";");
                    } else if (line.contains("RANGE_EXP_RATE")) {
                        writer.println("    public static final int RANGE_EXP_RATE = " + rangeExpField.getText() + ";");
                    } else if (line.contains("MAGIC_EXP_RATE")) {
                        writer.println("    public static final int MAGIC_EXP_RATE = " + magicExpField.getText() + ";");
                    } else if (line.contains("SERVER_EXP_BONUS")) {
                        writer.println("    public static double SERVER_EXP_BONUS = " + serverExpBonusField.getText() + ";");
                    } else {
                        writer.println(line);
                    }
                }
            }
            
            JOptionPane.showMessageDialog(this, "Experience rates saved successfully!\nPlease recompile the server for changes to take effect.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving experience rates: " + e.getMessage());
        }
    }

    private void addSpawnPoint() {
        SpawnPoint newSpawnPoint = new SpawnPoint("New Spawn Point", 3222, 3222);
        spawnPoints.add(newSpawnPoint);
        updateSpawnPointList();
        spawnPointList.setSelectedIndex(spawnPoints.size() - 1);
    }

    private void deleteSpawnPoint() {
        SpawnPoint selected = spawnPointList.getSelectedValue();
        if (selected != null) {
            spawnPoints.remove(selected);
            updateSpawnPointList();
        }
    }

    private void updateSpawnPoint() {
        SpawnPoint selected = spawnPointList.getSelectedValue();
        if (selected != null) {
            try {
                selected.name = spawnPointNameField.getText();
                selected.x = Integer.parseInt(spawnPointXField.getText());
                selected.y = Integer.parseInt(spawnPointYField.getText());
                updateSpawnPointList();
                
                if (autoSaveCheckbox.isSelected()) {
                    saveTeleportFiles();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid numeric value", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateItemTable(Shop shop) {
        updateItemTable(shop, "");
    }

    private void updateItemTable(Shop shop, String filter) {
        itemTableModel.setRowCount(0);
        String lowerFilter = filter.toLowerCase();
        for (ShopItem item : shop.items) {
            String itemName = itemNames.get(item.id);
            boolean matches = filter.isEmpty();
            if (!matches && itemName != null) {
                matches = itemName.toLowerCase().contains(lowerFilter) || String.valueOf(item.id).contains(lowerFilter);
            }
            if (matches) {
                itemTableModel.addRow(new Object[]{item.id, itemName != null ? itemName : "", item.amount});
            }
        }
    }

    private void addShop() {
        Shop newShop = new Shop(getNextShopId(), "New Shop", 2, 2);
        shops.add(newShop);
        updateShopList();
        shopList.setSelectedIndex(shops.size() - 1);
    }

    private void deleteShop() {
        Shop selected = shopList.getSelectedValue();
        if (selected != null) {
            shops.remove(selected);
            updateShopList();
            itemTableModel.setRowCount(0);
        }
    }

    private void addNpcSpawn() {
        NpcSpawn newSpawn = new NpcSpawn(0, 3222, 3222, 0);
        npcSpawns.add(newSpawn);
        updateNpcList();
        npcList.setSelectedIndex(npcSpawns.size() - 1);
    }

    private void deleteNpcSpawn() {
        NpcSpawn selected = npcList.getSelectedValue();
        if (selected != null) {
            npcSpawns.remove(selected);
            npcShopMap.remove(selected.npcId);
            updateNpcList();
        }
    }

    private void updateNpcSpawn() {
        NpcSpawn selected = npcList.getSelectedValue();
        if (selected != null) {
            try {
                // Coordinate validation
                int x = Integer.parseInt(npcXField.getText());
                int y = Integer.parseInt(npcYField.getText());
                int height = Integer.parseInt(npcHeightField.getText());
                
                if (x < 0 || x > 10000 || y < 0 || y > 10000) {
                    JOptionPane.showMessageDialog(this, "Coordinates must be between 0 and 10000", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (height < 0 || height > 4) {
                    JOptionPane.showMessageDialog(this, "Height must be between 0 and 4", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Save state for undo
                saveStateForUndo();
                
                int oldNpcId = selected.npcId;
                selected.npcId = Integer.parseInt(npcIdField.getText());
                selected.x = x;
                selected.y = y;
                selected.height = height;
                
                // Update shop mapping
                String shopIdText = npcShopIdField.getText();
                if (!shopIdText.isEmpty()) {
                    int shopId = Integer.parseInt(shopIdText);
                    npcShopMap.put(selected.npcId, shopId);
                } else {
                    npcShopMap.remove(selected.npcId);
                }
                
                // Remove old mapping if NPC ID changed
                if (oldNpcId != selected.npcId) {
                    npcShopMap.remove(oldNpcId);
                }
                
                updateNpcList();
                
                if (autoSaveCheckbox.isSelected()) {
                    saveNpcFiles();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid numeric value", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void duplicateNpcSpawn() {
        NpcSpawn selected = npcList.getSelectedValue();
        if (selected != null) {
            saveStateForUndo();
            NpcSpawn newSpawn = new NpcSpawn(selected.npcId, selected.x + 1, selected.y + 1, selected.height);
            npcSpawns.add(newSpawn);
            // Copy shop assignment if exists
            if (npcShopMap.containsKey(selected.npcId)) {
                npcShopMap.put(newSpawn.npcId, npcShopMap.get(selected.npcId));
            }
            updateNpcList();
            npcList.setSelectedIndex(npcSpawns.size() - 1);
            
            if (autoSaveCheckbox.isSelected()) {
                saveNpcFiles();
            }
        }
    }

    private void bulkDeleteNpcSpawns() {
        int[] selectedIndices = npcList.getSelectedIndices();
        if (selectedIndices.length > 0) {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Delete " + selectedIndices.length + " NPC spawns?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                saveStateForUndo();
                // Sort indices in descending order to avoid index shifting issues
                java.util.Arrays.sort(selectedIndices);
                for (int i = selectedIndices.length - 1; i >= 0; i--) {
                    NpcSpawn spawn = npcSpawns.get(selectedIndices[i]);
                    npcShopMap.remove(spawn.npcId);
                    npcSpawns.remove(selectedIndices[i]);
                }
                updateNpcList();
                
                if (autoSaveCheckbox.isSelected()) {
                    saveNpcFiles();
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select NPCs to delete", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void filterNpcs(String searchText, DefaultListModel<NpcSpawn> model) {
        model.clear();
        String lowerSearch = searchText.toLowerCase();
        for (NpcSpawn spawn : npcSpawns) {
            String npcName = npcNames.get(spawn.npcId);
            boolean matches = String.valueOf(spawn.npcId).contains(lowerSearch);
            if (npcName != null) {
                matches = matches || npcName.toLowerCase().contains(lowerSearch);
            }
            if (matches || searchText.isEmpty()) {
                model.addElement(spawn);
            }
        }
    }

    private void filterNpcs(String searchText) {
        filterNpcs(searchText, npcListModel);
    }

    private void searchNpcByName() {
        JDialog dialog = new JDialog(this, "Search NPC by Name", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new BorderLayout());
        
        JTextField searchField = new JTextField();
        DefaultListModel<NpcSpawn> searchModel = new DefaultListModel<>();
        JList<NpcSpawn> searchList = new JList<>(searchModel);
        searchList.setCellRenderer(new NpcNameRenderer());
        
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                searchModel.clear();
                String search = searchField.getText().toLowerCase();
                if (!search.isEmpty()) {
                    for (NpcSpawn spawn : npcSpawns) {
                        String npcName = npcNames.get(spawn.npcId);
                        if (npcName != null && npcName.toLowerCase().contains(search)) {
                            searchModel.addElement(spawn);
                        }
                    }
                }
            }
        });
        
        searchList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                NpcSpawn selected = searchList.getSelectedValue();
                if (selected != null) {
                    npcIdField.setText(String.valueOf(selected.npcId));
                    dialog.dispose();
                }
            }
        });
        
        dialog.add(new JLabel("Search NPC name:"), BorderLayout.NORTH);
        dialog.add(searchField, BorderLayout.CENTER);
        dialog.add(new JScrollPane(searchList), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void saveStateForUndo() {
        // Simple state saving - serialize current state
        StringBuilder state = new StringBuilder();
        for (NpcSpawn spawn : npcSpawns) {
            state.append(spawn.npcId).append(",").append(spawn.x).append(",").append(spawn.y).append(",").append(spawn.height).append(";");
        }
        undoStack.add(state.toString());
        if (undoStack.size() > 50) undoStack.remove(0); // Limit stack size
        redoStack.clear();
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            String state = undoStack.remove(undoStack.size() - 1);
            redoStack.add(serializeCurrentState());
            restoreState(state);
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            String state = redoStack.remove(redoStack.size() - 1);
            undoStack.add(serializeCurrentState());
            restoreState(state);
        }
    }

    private String serializeCurrentState() {
        StringBuilder state = new StringBuilder();
        for (NpcSpawn spawn : npcSpawns) {
            state.append(spawn.npcId).append(",").append(spawn.x).append(",").append(spawn.y).append(",").append(spawn.height).append(";");
        }
        return state.toString();
    }

    private void restoreState(String state) {
        npcSpawns.clear();
        String[] spawns = state.split(";");
        for (String spawnStr : spawns) {
            if (!spawnStr.isEmpty()) {
                String[] parts = spawnStr.split(",");
                if (parts.length == 4) {
                    try {
                        int npcId = Integer.parseInt(parts[0]);
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int height = Integer.parseInt(parts[3]);
                        npcSpawns.add(new NpcSpawn(npcId, x, y, height));
                    } catch (NumberFormatException e) {
                        // Skip invalid entries
                    }
                }
            }
        }
        updateNpcList();
    }

    private void duplicateShop() {
        Shop selected = shopList.getSelectedValue();
        if (selected != null) {
            Shop newShop = new Shop(getNextShopId(), selected.name + " (Copy)", selected.sellModifier, selected.buyModifier);
            for (ShopItem item : selected.items) {
                newShop.addItem(item.id, item.amount);
            }
            shops.add(newShop);
            updateShopList();
            shopList.setSelectedIndex(shops.size() - 1);
        }
    }

    private void filterShops(String searchText) {
        DefaultListModel<Shop> model = (DefaultListModel<Shop>) shopList.getModel();
        model.clear();
        
        if (searchText.isEmpty()) {
            for (Shop shop : shops) {
                model.addElement(shop);
            }
        } else {
            searchText = searchText.toLowerCase();
            for (Shop shop : shops) {
                if (String.valueOf(shop.id).contains(searchText) || 
                    shop.name.toLowerCase().contains(searchText)) {
                    model.addElement(shop);
                }
            }
        }
    }


    private void searchItemByName() {
        JDialog searchDialog = new JDialog(this, "Search Items", true);
        searchDialog.setSize(400, 300);
        searchDialog.setLayout(new BorderLayout());
        
        JPanel searchPanel = new JPanel(new FlowLayout());
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        DefaultListModel<String> searchResultsModel = new DefaultListModel<>();
        JList<String> searchResultsList = new JList<>(searchResultsModel);
        JScrollPane scrollPane = new JScrollPane(searchResultsList);
        
        JButton selectButton = new JButton("Select");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);
        
        searchDialog.add(searchPanel, BorderLayout.NORTH);
        searchDialog.add(scrollPane, BorderLayout.CENTER);
        searchDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        searchButton.addActionListener(e -> {
            String searchText = searchField.getText().toLowerCase();
            searchResultsModel.clear();
            for (Map.Entry<Integer, String> entry : itemNames.entrySet()) {
                if (entry.getValue().toLowerCase().contains(searchText) || 
                    String.valueOf(entry.getKey()).contains(searchText)) {
                    searchResultsModel.addElement(entry.getKey() + " - " + entry.getValue());
                }
            }
        });
        
        selectButton.addActionListener(e -> {
            String selected = searchResultsList.getSelectedValue();
            if (selected != null) {
                String[] parts = selected.split(" - ");
                if (parts.length > 0) {
                    itemIdField.setText(parts[0]);
                    searchDialog.dispose();
                }
            }
        });
        
        cancelButton.addActionListener(e -> searchDialog.dispose());
        
        searchField.addActionListener(e -> searchButton.doClick());
        searchResultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectButton.doClick();
                }
            }
        });
        
        searchDialog.setLocationRelativeTo(this);
        searchDialog.setVisible(true);
    }

    private void updateShopDetails() {
        Shop selected = shopList.getSelectedValue();
        if (selected != null) {
            try {
                selected.id = Integer.parseInt(shopIdField.getText());
                selected.name = shopNameField.getText();
                selected.sellModifier = Integer.parseInt(sellModifierField.getText());
                selected.buyModifier = Integer.parseInt(buyModifierField.getText());
                updateShopList();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid numeric value", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addItem() {
        Shop selected = shopList.getSelectedValue();
        if (selected != null) {
            try {
                int itemId = Integer.parseInt(itemIdField.getText());
                int amount = Integer.parseInt(itemAmountField.getText());
                selected.addItem(itemId, amount);
                updateItemTable(selected);
                itemIdField.setText("");
                itemAmountField.setText("");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid item ID or amount", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeItem() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow >= 0) {
            Shop shop = shopList.getSelectedValue();
            if (shop != null) {
                shop.items.remove(selectedRow);
                updateItemTable(shop);
            }
        }
    }

    private int getNextShopId() {
        int maxId = 0;
        for (Shop shop : shops) {
            if (shop.id > maxId) {
                maxId = shop.id;
            }
        }
        return maxId + 1;
    }

    private void saveShopsFile() {
        if (shopsFile == null) {
            JOptionPane.showMessageDialog(this, "No file loaded", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(shopsFile))) {
            writer.println("//-----ShopID---ShopName----------------------------------------Sell----Buy-----Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount--Item----Amount");
            for (Shop shop : shops) {
                writer.print("shop = " + shop.id + "\t" + shop.name.replace(" ", "_") + "\t" + shop.sellModifier + "\t" + shop.buyModifier);
                for (ShopItem item : shop.items) {
                    writer.print("\t" + item.id + "\t" + item.amount);
                }
                writer.println();
            }
            writer.println("[ENDOFSHOPLIST]");
            JOptionPane.showMessageDialog(this, "Shops saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveNpcFiles() {
        if (spawnConfigFile == null || npcShopsFile == null) {
            JOptionPane.showMessageDialog(this, "No files loaded", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Save spawn-config.cfg
        try (PrintWriter writer = new PrintWriter(new FileWriter(spawnConfigFile))) {
            for (NpcSpawn spawn : npcSpawns) {
                writer.println("spawn = " + spawn.npcId + "\t" + spawn.x + "\t" + spawn.y + "\t" + spawn.height + "\t1\t0\t0\t0");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving spawn-config.cfg: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Save npc-shops.cfg
        try (PrintWriter writer = new PrintWriter(new FileWriter(npcShopsFile))) {
            writer.println("// NPC to Shop Assignment Configuration");
            writer.println("// Format: npc-shop = npcId shopId");
            for (Map.Entry<Integer, Integer> entry : npcShopMap.entrySet()) {
                writer.println("npc-shop = " + entry.getKey() + "\t" + entry.getValue());
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving npc-shops.cfg: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Save npc_drops.cfg
        if (npcDropsFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(npcDropsFile))) {
                writer.println("################################");
                writer.println("#  NPC DROPS LIST FOR BIOHAZARD  #");
                writer.println("################################");
                writer.println("#  GUIDELINES FOR FORMATTING   #");
                writer.println("#       ID:AMOUNT:RARITY       #");
                writer.println("################################");
                writer.println();
                writer.println("# BEGIN DROP TABLE");
                writer.println();
                
                // Group NPCs by their drops to avoid duplicates
                Map<String, List<Integer>> dropToNpcs = new HashMap<>();
                for (Map.Entry<Integer, List<NpcDrop>> entry : npcDropsMap.entrySet()) {
                    String dropKey = entry.getValue().stream()
                        .map(d -> d.itemId + ":" + d.amount + ":" + d.rarity)
                        .reduce((a, b) -> a + "\t" + b)
                        .orElse("");
                    dropToNpcs.computeIfAbsent(dropKey, k -> new ArrayList<>()).add(entry.getKey());
                }
                
                for (Map.Entry<String, List<Integer>> entry : dropToNpcs.entrySet()) {
                    String npcIds = entry.getValue().stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + "/" + b)
                        .orElse("");
                    writer.println("drop = " + npcIds + "\t" + entry.getKey());
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving npc_drops.cfg: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // Save npc.cfg
        if (npcStatsFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(npcStatsFile))) {
                writer.println("//    NpcID     NpcName                         combat  health  attack  defense  attackSpeed");
                for (Map.Entry<Integer, NpcStats> entry : npcStatsMap.entrySet()) {
                    NpcStats stats = entry.getValue();
                    String npcName = npcNames.get(stats.npcId);
                    if (npcName != null) {
                        writer.println("npc = " + stats.npcId + "\t" + npcName.replace(" ", "_") + "\t" + stats.combat + "\t" + stats.health + "\t" + stats.attack + "\t" + stats.defense + "\t" + stats.attackSpeed);
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving npc.cfg: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        JOptionPane.showMessageDialog(this, "NPC files saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveTeleportFiles() {
        // Save teleports.cfg
        if (teleportsFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(teleportsFile))) {
                writer.println("// Player Teleport Locations");
                writer.println("// Format: teleport = Category Name X Y");
                for (TeleportLocation teleport : teleports) {
                    writer.println("teleport = " + teleport.category.replace(" ", "_") + " " + teleport.name.replace(" ", "_") + " " + teleport.x + " " + teleport.y);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving teleports.cfg: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // Save spawn-points.cfg
        if (spawnPointsFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(spawnPointsFile))) {
                writer.println("// Player Spawn Points");
                writer.println("// Format: spawn = Name X Y");
                for (SpawnPoint spawnPoint : spawnPoints) {
                    writer.println("spawn = " + spawnPoint.name.replace(" ", "_") + " " + spawnPoint.x + " " + spawnPoint.y);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving spawn-points.cfg: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        JOptionPane.showMessageDialog(this, "Teleport and spawn point files saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ShopEditor().setVisible(true);
        });
    }

    static class Shop {
        int id;
        String name;
        int sellModifier;
        int buyModifier;
        List<ShopItem> items = new ArrayList<>();

        Shop(int id, String name, int sellModifier, int buyModifier) {
            this.id = id;
            this.name = name;
            this.sellModifier = sellModifier;
            this.buyModifier = buyModifier;
        }

        void addItem(int itemId, int amount) {
            items.add(new ShopItem(itemId, amount));
        }

        @Override
        public String toString() {
            return id + " - " + name;
        }
    }

    static class ShopItem {
        int id;
        int amount;

        ShopItem(int id, int amount) {
            this.id = id;
            this.amount = amount;
        }
    }

    static class NpcSpawn {
        int npcId;
        int x;
        int y;
        int height;

        NpcSpawn(int npcId, int x, int y, int height) {
            this.npcId = npcId;
            this.x = x;
            this.y = y;
            this.height = height;
        }

        @Override
        public String toString() {
            return npcId + " (" + x + ", " + y + ", " + height + ")";
        }
    }

    static class NpcStats {
        int npcId;
        int combat;
        int health;
        int attack;
        int defense;
        int attackSpeed;

        NpcStats(int npcId, int combat, int health) {
            this.npcId = npcId;
            this.combat = combat;
            this.health = health;
            this.attack = 0;
            this.defense = 0;
            this.attackSpeed = 0;
        }

        NpcStats(int npcId, int combat, int health, int attack, int defense, int attackSpeed) {
            this.npcId = npcId;
            this.combat = combat;
            this.health = health;
            this.attack = attack;
            this.defense = defense;
            this.attackSpeed = attackSpeed;
        }

        @Override
        public String toString() {
            return npcId + " - Combat: " + combat + " HP: " + health + " Atk: " + attack + " Def: " + defense + " Spd: " + attackSpeed;
        }
    }

    static class NpcDrop {
        int itemId;
        int amount;
        String rarity;

        NpcDrop(int itemId, int amount, String rarity) {
            this.itemId = itemId;
            this.amount = amount;
            this.rarity = rarity;
        }
    }

    static class TeleportLocation {
        String category;
        String name;
        int x;
        int y;

        TeleportLocation(String category, String name, int x, int y) {
            this.category = category;
            this.name = name;
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "[" + category + "] " + name + " (" + x + ", " + y + ")";
        }
    }

    static class SpawnPoint {
        String name;
        int x;
        int y;

        SpawnPoint(String name, int x, int y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return name + " (" + x + ", " + y + ")";
        }
    }

    static class ObjectSpawn {
        int objectId;
        int x;
        int y;
        int height;
        int face;
        int type;

        ObjectSpawn(int objectId, int x, int y, int height, int face, int type) {
            this.objectId = objectId;
            this.x = x;
            this.y = y;
            this.height = height;
            this.face = face;
            this.type = type;
        }

        @Override
        public String toString() {
            return "Object " + objectId + " at (" + x + ", " + y + ", " + height + ") Face: " + face + " Type: " + type;
        }
    }

    static class StarterItem {
        int itemId;
        int amount;

        StarterItem(int itemId, int amount) {
            this.itemId = itemId;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "Item " + itemId + " x" + amount;
        }
    }

    static class PlayerRight {
        int level;
        String name;
        String prefix;
        String color;

        PlayerRight(int level, String name, String prefix, String color) {
            this.level = level;
            this.name = name;
            this.prefix = prefix;
            this.color = color;
        }

        @Override
        public String toString() {
            return level + " - " + name + " [" + prefix + "]";
        }
    }

    static class Door {
        int x;
        int y;
        int height;
        int face;
        int state;

        Door(int x, int y, int height, int face, int state) {
            this.x = x;
            this.y = y;
            this.height = height;
            this.face = face;
            this.state = state;
        }

        @Override
        public String toString() {
            return "Door at (" + x + ", " + y + ", " + height + ") Face: " + face + " State: " + state;
        }
    }

    static class ObjectSize {
        int objectId;
        String name;
        String size;
        String examine;

        ObjectSize(int objectId, String name, String size, String examine) {
            this.objectId = objectId;
            this.name = name;
            this.size = size;
            this.examine = examine;
        }

        @Override
        public String toString() {
            return objectId + " - " + name + " (" + size + ")";
        }
    }

    private void loadStartingInventory() {
        startingInventory.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(startingInventoryFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("item = ") || line.startsWith("item=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 2) {
                        int itemId = Integer.parseInt(parts[0]);
                        int amount = Integer.parseInt(parts[1]);
                        startingInventory.add(new StarterItem(itemId, amount));
                    }
                }
            }
            updateInventoryList();
        } catch (IOException e) {
            // File might not exist yet
        }
    }

    private void loadStartingBank() {
        startingBank.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(startingBankFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("item = ") || line.startsWith("item=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 2) {
                        int itemId = Integer.parseInt(parts[0]);
                        int amount = Integer.parseInt(parts[1]);
                        startingBank.add(new StarterItem(itemId, amount));
                    }
                }
            }
            updateBankList();
        } catch (IOException e) {
            // File might not exist yet
        }
    }

    private void loadPlayerRights() {
        playerRights.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(playerRightsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("right = ") || line.startsWith("right=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 4) {
                        int level = Integer.parseInt(parts[0]);
                        String name = parts[1];
                        String prefix = parts[2];
                        String color = parts[3];
                        playerRights.add(new PlayerRight(level, name, prefix, color));
                    }
                }
            }
            updateRightsList();
        } catch (IOException e) {
            // File might not exist yet
        }
    }

    private void saveStartingInventory() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(startingInventoryFile))) {
            writer.println("// Starting Inventory Configuration");
            writer.println("// Format: item = itemId amount");
            writer.println("// These items are given to new players on first login");
            writer.println();
            for (StarterItem item : startingInventory) {
                writer.println("item = " + item.itemId + " " + item.amount);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving starting inventory: " + e.getMessage());
        }
    }

    private void saveStartingBank() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(startingBankFile))) {
            writer.println("// Starting Bank Configuration");
            writer.println("// Format: item = itemId amount");
            writer.println("// These items are placed in new players' bank on first login");
            writer.println();
            for (StarterItem item : startingBank) {
                writer.println("item = " + item.itemId + " " + item.amount);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving starting bank: " + e.getMessage());
        }
    }

    private void savePlayerRights() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(playerRightsFile))) {
            writer.println("// Player Rights Configuration");
            writer.println("// Format: right = level name prefix color");
            writer.println("// Level: 0=Player, 1=Moderator, 2=Administrator, 3=Owner, 4-9=Donator tiers");
            writer.println();
            for (PlayerRight right : playerRights) {
                writer.println("right = " + right.level + " " + right.name + " " + right.prefix + " " + right.color);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving player rights: " + e.getMessage());
        }
    }

    private void selectedInventoryItemChanged() {
        StarterItem selected = inventoryList.getSelectedValue();
        if (selected != null) {
            inventoryItemIdField.setText(String.valueOf(selected.itemId));
            inventoryAmountField.setText(String.valueOf(selected.amount));
        }
    }

    private void selectedBankItemChanged() {
        StarterItem selected = bankList.getSelectedValue();
        if (selected != null) {
            bankItemIdField.setText(String.valueOf(selected.itemId));
            bankAmountField.setText(String.valueOf(selected.amount));
        }
    }

    private void selectedRightChanged() {
        PlayerRight selected = rightsList.getSelectedValue();
        if (selected != null) {
            rightLevelField.setText(String.valueOf(selected.level));
            rightNameField.setText(selected.name);
            rightPrefixField.setText(selected.prefix);
            rightColorField.setText(selected.color);
        }
    }

    private void updateInventoryList() {
        inventoryListModel.clear();
        for (StarterItem item : startingInventory) {
            inventoryListModel.addElement(item);
        }
    }

    private void updateBankList() {
        bankListModel.clear();
        for (StarterItem item : startingBank) {
            bankListModel.addElement(item);
        }
    }

    private void updateRightsList() {
        rightsListModel.clear();
        for (PlayerRight right : playerRights) {
            rightsListModel.addElement(right);
        }
    }

    private void addInventoryItem() {
        try {
            int itemId = Integer.parseInt(inventoryItemIdField.getText());
            int amount = Integer.parseInt(inventoryAmountField.getText());
            startingInventory.add(new StarterItem(itemId, amount));
            updateInventoryList();
            saveStartingInventory();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid item ID or amount");
        }
    }

    private void deleteInventoryItem() {
        StarterItem selected = inventoryList.getSelectedValue();
        if (selected != null) {
            startingInventory.remove(selected);
            updateInventoryList();
            saveStartingInventory();
        }
    }

    private void updateInventoryItem() {
        StarterItem selected = inventoryList.getSelectedValue();
        if (selected != null) {
            try {
                selected.itemId = Integer.parseInt(inventoryItemIdField.getText());
                selected.amount = Integer.parseInt(inventoryAmountField.getText());
                updateInventoryList();
                saveStartingInventory();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid item ID or amount");
            }
        }
    }

    private void addBankItem() {
        try {
            int itemId = Integer.parseInt(bankItemIdField.getText());
            int amount = Integer.parseInt(bankAmountField.getText());
            startingBank.add(new StarterItem(itemId, amount));
            updateBankList();
            saveStartingBank();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid item ID or amount");
        }
    }

    private void deleteBankItem() {
        StarterItem selected = bankList.getSelectedValue();
        if (selected != null) {
            startingBank.remove(selected);
            updateBankList();
            saveStartingBank();
        }
    }

    private void updateBankItem() {
        StarterItem selected = bankList.getSelectedValue();
        if (selected != null) {
            try {
                selected.itemId = Integer.parseInt(bankItemIdField.getText());
                selected.amount = Integer.parseInt(bankAmountField.getText());
                updateBankList();
                saveStartingBank();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid item ID or amount");
            }
        }
    }

    private void addRight() {
        try {
            int level = Integer.parseInt(rightLevelField.getText());
            String name = rightNameField.getText();
            String prefix = rightPrefixField.getText();
            String color = rightColorField.getText();
            playerRights.add(new PlayerRight(level, name, prefix, color));
            updateRightsList();
            savePlayerRights();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid level");
        }
    }

    private void deleteRight() {
        PlayerRight selected = rightsList.getSelectedValue();
        if (selected != null) {
            playerRights.remove(selected);
            updateRightsList();
            savePlayerRights();
        }
    }

    private void updateRight() {
        PlayerRight selected = rightsList.getSelectedValue();
        if (selected != null) {
            try {
                selected.level = Integer.parseInt(rightLevelField.getText());
                selected.name = rightNameField.getText();
                selected.prefix = rightPrefixField.getText();
                selected.color = rightColorField.getText();
                updateRightsList();
                savePlayerRights();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid level");
            }
        }
    }

    private void loadDoors() {
        doors.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(doorsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("door = ") || line.startsWith("door=")) {
                    String[] parts = line.substring(line.indexOf("=") + 1).trim().split("\\s+");
                    if (parts.length >= 5) {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int height = Integer.parseInt(parts[2]);
                        int face = Integer.parseInt(parts[3]);
                        int state = Integer.parseInt(parts[4]);
                        doors.add(new Door(x, y, height, face, state));
                    }
                }
            }
            updateDoorList();
        } catch (IOException e) {
            // File might not exist yet
        }
    }

    private void saveDoors() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(doorsFile))) {
            writer.println("//the door starts as closed");
            writer.println("// South     0 face =   doorX -1     face = -3");
            writer.println("// East     -1 face =     doorY -1     face = 0");
            writer.println("// North    -2 face =     doorX +1    face = -1");
            writer.println("// West     -3 face =     doorY +1     face = -2");
            writer.println();
            writer.println("//        doorX        doorY     Height       Face    State //0 closed, 1 open");
            for (Door door : doors) {
                writer.println("door = " + door.x + "\t" + door.y + "\t" + door.height + "\t" + door.face + "\t" + door.state);
            }
            writer.println("[ENDOFDOORLIST]");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving doors: " + e.getMessage());
        }
    }

    private void selectedDoorChanged() {
        Door selected = doorList.getSelectedValue();
        if (selected != null) {
            doorXField.setText(String.valueOf(selected.x));
            doorYField.setText(String.valueOf(selected.y));
            doorHeightField.setText(String.valueOf(selected.height));
            doorFaceField.setText(String.valueOf(selected.face));
            doorStateField.setText(String.valueOf(selected.state));
        }
    }

    private void updateDoorList() {
        doorListModel.clear();
        for (Door door : doors) {
            doorListModel.addElement(door);
        }
    }

    private void addDoor() {
        try {
            int x = Integer.parseInt(doorXField.getText());
            int y = Integer.parseInt(doorYField.getText());
            int height = Integer.parseInt(doorHeightField.getText());
            int face = Integer.parseInt(doorFaceField.getText());
            int state = Integer.parseInt(doorStateField.getText());
            doors.add(new Door(x, y, height, face, state));
            updateDoorList();
            saveDoors();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid numeric value");
        }
    }

    private void deleteDoor() {
        Door selected = doorList.getSelectedValue();
        if (selected != null) {
            doors.remove(selected);
            updateDoorList();
            saveDoors();
        }
    }

    private void updateDoor() {
        Door selected = doorList.getSelectedValue();
        if (selected != null) {
            try {
                selected.x = Integer.parseInt(doorXField.getText());
                selected.y = Integer.parseInt(doorYField.getText());
                selected.height = Integer.parseInt(doorHeightField.getText());
                selected.face = Integer.parseInt(doorFaceField.getText());
                selected.state = Integer.parseInt(doorStateField.getText());
                updateDoorList();
                saveDoors();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid numeric value");
            }
        }
    }

    private void loadObjectSizes() {
        objectSizes.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(objectSizeFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("objectId = ")) {
                    String content = line.substring(line.indexOf("=") + 1).trim();
                    // Parse: objectId, name, size, examine (examine is quoted)
                    String[] parts = content.split("\\s+", 4);
                    if (parts.length >= 4) {
                        try {
                            int objectId = Integer.parseInt(parts[0]);
                            String name = parts[1].replace("_", " ");
                            String size = parts[2];
                            String examine = parts[3].replace("\"", "").replace("_", " ");
                            objectSizes.add(new ObjectSize(objectId, name, size, examine));
                        } catch (NumberFormatException e) {
                            // Skip invalid lines
                        }
                    }
                }
            }
            updateObjectSizeList();
        } catch (IOException e) {
            // File might not exist yet
        }
    }

    private void saveObjectSizes() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(objectSizeFile))) {
            writer.println("ID------Name------------------------------------Size----Examin Info");
            writer.println();
            for (ObjectSize obj : objectSizes) {
                String name = obj.name.replace(" ", "_");
                String examine = "\"" + obj.examine + "\"";
                writer.println("objectId = " + obj.objectId + "\t" + name + "\t\t\t" + obj.size + "\t" + examine);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving object sizes: " + e.getMessage());
        }
    }

    private void selectedObjectSizeChanged() {
        ObjectSize selected = objectSizeList.getSelectedValue();
        if (selected != null) {
            objectSizeIdField.setText(String.valueOf(selected.objectId));
            objectSizeNameField.setText(selected.name);
            objectSizeSizeField.setText(selected.size);
            objectSizeExamField.setText(selected.examine);
        }
    }

    private void updateObjectSizeList() {
        objectSizeListModel.clear();
        for (ObjectSize obj : objectSizes) {
            objectSizeListModel.addElement(obj);
        }
    }

    private void addObjectSize() {
        try {
            int objectId = Integer.parseInt(objectSizeIdField.getText());
            String name = objectSizeNameField.getText();
            String size = objectSizeSizeField.getText();
            String examine = objectSizeExamField.getText();
            objectSizes.add(new ObjectSize(objectId, name, size, examine));
            updateObjectSizeList();
            saveObjectSizes();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid object ID");
        }
    }

    private void deleteObjectSize() {
        ObjectSize selected = objectSizeList.getSelectedValue();
        if (selected != null) {
            objectSizes.remove(selected);
            updateObjectSizeList();
            saveObjectSizes();
        }
    }

    private void updateObjectSize() {
        ObjectSize selected = objectSizeList.getSelectedValue();
        if (selected != null) {
            try {
                selected.objectId = Integer.parseInt(objectSizeIdField.getText());
                selected.name = objectSizeNameField.getText();
                selected.size = objectSizeSizeField.getText();
                selected.examine = objectSizeExamField.getText();
                updateObjectSizeList();
                saveObjectSizes();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid object ID");
            }
        }
    }

    class NpcNameRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof NpcSpawn) {
                NpcSpawn spawn = (NpcSpawn) value;
                String npcName = npcNames.get(spawn.npcId);
                boolean hasShop = npcShopMap.containsKey(spawn.npcId);
                
                if (hasShop && !isSelected) {
                    setBackground(new Color(200, 255, 200)); // Light green for NPCs with shops
                    setOpaque(true);
                }
                
                if (npcName != null) {
                    String shopInfo = hasShop ? " [Shop: " + npcShopMap.get(spawn.npcId) + "]" : "";
                    setText(spawn.npcId + " - " + npcName + shopInfo + " (" + spawn.x + ", " + spawn.y + ", " + spawn.height + ")");
                    setToolTipText(npcName + (hasShop ? " - Assigned to shop " + npcShopMap.get(spawn.npcId) : ""));
                } else {
                    String shopInfo = hasShop ? " [Shop: " + npcShopMap.get(spawn.npcId) + "]" : "";
                    setText(spawn.npcId + shopInfo + " (" + spawn.x + ", " + spawn.y + ", " + spawn.height + ")");
                }
            }
            return c;
        }
    }

    class ItemNameRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column == 0 && value instanceof Integer) {
                int itemId = (Integer) value;
                String itemName = itemNames.get(itemId);
                if (itemName != null) {
                    setText(itemId + " - " + itemName);
                    setToolTipText(itemName);
                } else {
                    setText(String.valueOf(itemId));
                    setToolTipText("Unknown item");
                }
            }
            return c;
        }
    }
}
