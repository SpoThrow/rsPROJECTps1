package game;

// Phase 3.2.1: the settings/options subsystem lifted out of client.java.
// State still lives on client; this class reaches it through the receiver it is
// constructed with - NOT client.instance, because Jframe calls loadClientSettings()
// from inside its own constructor (Jframe.java:79), before instance is assigned.

import java.applet.AppletContext;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import sign.signlink;import node.Node;
import node.NodeList;import cache.CacheDownloader;
import cache.Decompressor;
import cache.FileOperations;
import cache.StreamLoader;
import def.Animation;
import def.CurseData667;
import def.EntityDef;
import def.Flo;
import def.IDK;
import def.ItemDef;
import def.ItemStats;
import def.ObjectDef;
import def.SpotAnim;
import def.VarBit;
import def.Varp;
import model.Animable;
import model.Animable_Sub3;
import model.Animable_Sub4;
import model.Animable_Sub5;
import model.Frames;
import model.Model;
import model.Texture;
import net.CreateUID;
import net.ISAACRandomGen;
import net.OnDemandData;
import net.OnDemandFetcher;
import net.RSSocket;
import net.Stream;
import scene.CollisionMap;
import scene.Fog;
import scene.GroundItemLists;
import scene.Object1;
import scene.Object2;
import scene.Object3;
import scene.Object5;
import scene.ObjectCollisionSizes;
import scene.ObjectManager;
import scene.WorldController;
import ui.AmmoOverlay;
import ui.AntiDrag;
import ui.AttackStyleWarn;
import ui.Background;
import ui.BarrowsPlugin;
import ui.BossTimers;
import ui.CannonOverlay;
import ui.Censor;
import ui.ChatChannels;
import ui.ChatHistory;
import ui.ChatboxItemSearch;
import ui.CombatLevelPlugin;
import ui.DrawingArea;
import ui.FriendListPlugin;
import ui.FriendNotes;
import ui.GlPresent;
import ui.GroundMarkers;
import ui.HiscoresPanel;
import ui.HudEditor;
import ui.HudLayout;
import ui.ImplingsPlugin;
import ui.InfoBoxes;
import ui.InventoryTags;
import ui.Jframe;
import ui.KeyRemapper;
import ui.LootBeams;
import ui.LootTracker;
import ui.LwjglPresent;
import ui.MenuEntrySwapper;
import ui.MouseDetection;
import ui.MouseTooltips;
import ui.NpcIndicators;
import ui.ObjectMarkers;
import ui.OverlayManager;
import ui.OverlayRefresh;
import ui.PlayerIndicators;
import ui.PluginSidebar;
import ui.PoisonPlugin;
import ui.RSApplet;
import ui.RSFont;
import ui.RSImageProducer;
import ui.RSInterface;
import ui.RegenMeter;
import ui.RendererConfig;
import ui.SavedCharacters;
import ui.SlayerTracker;
import ui.Sprite;
import ui.StatusBars;
import ui.StatusTimers;
import ui.TextClass;
import ui.TextDrawingArea;
import ui.TextInput;
import static game.client.*;

public final class ClientSettings {

	private final client c;

	ClientSettings(client c) {
		this.c = c;
	}

	public void saveClientSettings() {
		if (applyingClientSettings) {
			return;
		}
		try {
			captureOptionFields();
			Properties props = new Properties();
			props.setProperty("midiVolume", Integer.toString(c.midiVolume));
			props.setProperty("musicEnabled", Boolean.toString(c.musicEnabled));
			props.setProperty("brightness", Integer.toString(optionBrightness));
			props.setProperty("music", Integer.toString(optionMusic));
			props.setProperty("sound", Integer.toString(optionSound));
			props.setProperty("mouseButtons", Integer.toString(optionMouse));
			props.setProperty("chatEffects", Integer.toString(optionChatEffects));
			props.setProperty("splitPrivateChat", Integer.toString(optionSplitChat));
			props.setProperty("acceptAid", Integer.toString(optionAcceptAid));
			props.setProperty("resizable", Boolean.toString(frameMode == ScreenMode.RESIZABLE));
			props.setProperty("windowWidth", Integer.toString(savedResizeWidth));
			props.setProperty("windowHeight", Integer.toString(savedResizeHeight));
			props.setProperty("fogStrength", Integer.toString(fogStrength));
			props.setProperty("aaStrength", Integer.toString(aaStrength));
			props.setProperty("drawDistance", Integer.toString(drawDistance));
			props.setProperty("tweening", Boolean.toString(tweeningEnabled));
			props.setProperty("tileBlending", Boolean.toString(tileBlending));
			props.setProperty("hideRoofs", Boolean.toString(hideRoofs));
			props.setProperty("tileMarkers", Boolean.toString(tileMarkers));
			props.setProperty("groundItemNames", Boolean.toString(groundItemNames));
			props.setProperty("npcHealthOverlay", Boolean.toString(npcHealthOverlay));
			props.setProperty("boostedStatOverlay", Boolean.toString(boostedStatOverlay));
			props.setProperty("xpDrops", Boolean.toString(xpDrops));
			props.setProperty("xpDropSpeed", Integer.toString(xpDropSpeed));
			props.setProperty("xpDropGrouped", Boolean.toString(xpDropGrouped));
			props.setProperty("boostedPlusDisplay", Boolean.toString(boostedPlusDisplay));
			props.setProperty("boostedInfoBox", Boolean.toString(boostedInfoBox));
			props.setProperty("attackStyleOverlay", Boolean.toString(attackStyleOverlay));
			props.setProperty("npcAttackOption", Integer.toString(npcAttackOption));
			props.setProperty("playerAttackOption", Integer.toString(playerAttackOption));
			props.setProperty("menuEntrySwapper", Boolean.toString(menuEntrySwapper));
			props.setProperty("keyRemapping", Boolean.toString(keyRemapping));
			props.setProperty("enterToChat", Boolean.toString(enterToChat));
			props.setProperty("wasdCamera", Boolean.toString(wasdCamera));
			props.setProperty("spaceContinue", Boolean.toString(spaceContinue));
			props.setProperty("performanceStats", Boolean.toString(performanceStats));
			props.setProperty("showPing", Boolean.toString(showPing));
			props.setProperty("openGl", Boolean.toString(openGlEnabled));
			props.setProperty(RendererConfig.PROPERTY, RendererConfig.requestedName());
			props.setProperty("fpsUnlocked", Boolean.toString(fpsUnlocked));
			props.setProperty("zoomSensitivity", Integer.toString(zoomSensitivity));
			props.setProperty("shiftClickDrop", Boolean.toString(shiftClickDrop));
			props.setProperty("shiftClickWalkHere", Boolean.toString(shiftClickWalkHere));
			props.setProperty("middleClickWear", Boolean.toString(middleClickWear));
			props.setProperty("specOrb", Boolean.toString(specOrb));
			props.setProperty("groundHideValue", Integer.toString(groundHideValue));
			props.setProperty("lootBeamValue", Integer.toString(lootBeamValue));
			props.setProperty("groundItemTextShadow", Integer.toString(groundItemTextShadow));
			props.setProperty("groundItemTextSize", Integer.toString(groundItemTextSize));
			GroundItemLists.save(props);
			LootBeams.save(props);
			props.setProperty("destTile", Boolean.toString(destTile));
			props.setProperty("trueTile", Boolean.toString(trueTile));
			props.setProperty("chatTimestamps", Boolean.toString(chatTimestamps));
			props.setProperty("silentScreenshots", Boolean.toString(silentScreenshots));
			props.setProperty("xpTracker", Boolean.toString(c.hasTrackedSkill()));
			props.setProperty("xpTrackSkills", Integer.toString(trackedSkillBits()));
			props.setProperty("xpCounterOpen", Boolean.toString(xpCounterOpen));
			props.setProperty("xpTrackerFixedX", Integer.toString(xpTrackerFixedX));
			props.setProperty("xpTrackerFixedY", Integer.toString(xpTrackerFixedY));
			props.setProperty("xpTrackerResizeX", Integer.toString(xpTrackerResizeX));
			props.setProperty("xpTrackerResizeY", Integer.toString(xpTrackerResizeY));
			props.setProperty("statusTimers", Boolean.toString(statusTimers));
			props.setProperty("orbFlash", Boolean.toString(orbFlash));
			props.setProperty("resizableInvTransparent", Boolean.toString(resizableInvTransparent));
			props.setProperty("resizableChatTransparent", Boolean.toString(resizableChatTransparent));
			props.setProperty("chatScrollbarLeft", Boolean.toString(chatScrollbarLeft));
			props.setProperty("chatClickThrough", Boolean.toString(chatClickThrough));
			props.setProperty("rememberMe", Boolean.toString(c.rememberMe == 1));
			if (c.rememberMe == 1) {
				props.setProperty("rememberUser", c.myUsername == null ? "" : c.myUsername);
				props.setProperty("rememberPass", c.myPassword == null ? "" : c.myPassword);
			} else {
				props.setProperty("rememberUser", "");
				props.setProperty("rememberPass", "");
			}
			SavedCharacters.save(props);
			MenuEntrySwapper.save(props);
			GroundMarkers.save(props);
			NpcIndicators.save(props);
			SlayerTracker.save(props);
			AmmoOverlay.save(props);
			AntiDrag.save(props);
			AttackStyleWarn.save(props);
			InventoryTags.save(props);
			MouseTooltips.save(props);
			ObjectMarkers.save(props);
			PlayerIndicators.save(props);
			ItemStats.save(props);
			BossTimers.save(props);
			KeyRemapper.save(props);
			CannonOverlay.save(props);
			ChatChannels.save(props);
			ChatHistory.save(props);
			CombatLevelPlugin.save(props);
			FriendListPlugin.save(props);
			FriendNotes.save(props);
			ImplingsPlugin.save(props);
			PoisonPlugin.save(props);
			RegenMeter.save(props);
			StatusBars.save(props);
			BarrowsPlugin.save(props);
			OverlayManager.save(props);
			LootTracker.save(props);
			props.setProperty("pluginSidebar", Boolean.toString(PluginSidebar.open));
			props.setProperty("pluginSidebarBar", Boolean.toString(PluginSidebar.sidebarOut));
			props.setProperty("pluginSidebarTab", Integer.toString(PluginSidebar.selectedTab));
			int quickBits = 0;
			for (int i = 0; i < c.quickPrayers.length; i++) {
				if (c.quickPrayers[i]) {
					quickBits |= 1 << i;
				}
			}
			props.setProperty("quickPrayers", Integer.toString(quickBits));
			int curseBits = 0;
			for (int i = 0; i < c.quickCurses.length; i++) {
				if (c.quickCurses[i]) {
					curseBits |= 1 << i;
				}
			}
			props.setProperty("quickCurses", Integer.toString(curseBits));
			FileOutputStream out = new FileOutputStream(signlink.findcachedir() + "client_settings.properties");
			props.store(out, "Soul-Trail client settings");
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void captureOptionFields() {
		if (c.variousSettings == null) {
			return;
		}
		if (c.variousSettings[166] > 0) {
			optionBrightness = c.variousSettings[166];
		}
		optionMusic = c.variousSettings[168];
		optionSound = c.variousSettings[169];
		optionMouse = c.variousSettings[170];
		optionChatEffects = c.variousSettings[171];
		optionSplitChat = c.splitPrivateChat != 0 || c.variousSettings[287] != 0 ? 1 : 0;
		c.splitPrivateChat = optionSplitChat;
		optionAcceptAid = c.variousSettings[427];
		if (frameMode == ScreenMode.RESIZABLE) {
			savedResizeWidth = Math.max(765, frameWidth);
			savedResizeHeight = Math.max(503, frameHeight);
		}
	}

	private void applyConfig(int id, int state) {
		if (c.variousSettings == null || c.anIntArray1045 == null) {
			return;
		}
		c.anIntArray1045[id] = state;
		if (c.variousSettings[id] != state) {
			c.variousSettings[id] = state;
			if (Varp.cache != null && id >= 0 && id < Varp.cache.length && Varp.cache[id] != null) {
				c.method33(id);
			}
			needDrawTabArea = true;
		}
	}

	public void applySavedOptionSettings() {
		if (c.variousSettings == null) {
			return;
		}
		applyingClientSettings = true;
		try {
			applyConfig(166, optionBrightness);
			applyConfig(168, optionMusic);
			applyConfig(169, optionSound);
			applyConfig(170, optionMouse);
			applyConfig(171, optionChatEffects);
			applyConfig(287, optionSplitChat);
			c.splitPrivateChat = optionSplitChat;
			applyConfig(427, optionAcceptAid);
			applyConfig(876, frameMode == ScreenMode.RESIZABLE ? 1 : 0);
			refreshClientSettingsInterface();
		} finally {
			applyingClientSettings = false;
		}
	}

	public void refreshClientSettingsInterface() {
		if (RSInterface.interfaceCache == null) {
			PluginSidebar.refresh();
			return;
		}
		rebuildSettingsList();
		setCategoryTab(24249, "Display", 0);
		setCategoryTab(24250, "Gameplay", 1);
		setCategoryTab(24251, "Interface", 2);
		setCategoryTab(24252, "Controls", 3);
		setSettingLine(24210, "Resizable client: " + (c.isFixed() ? "Off" : "On"));
		setSettingLine(24248, "Inventory tab (resizable): " + (resizableInvTransparent ? "Transparent" : "Solid"));
		setSettingLine(24245, "Chat box (resizable): " + (resizableChatTransparent ? "Transparent" : "Solid"));
		setSettingLine(24431, "Chat scrollbar: " + (chatScrollbarLeft ? "Left" : "Right"));
		setSettingLine(24432, "Chat click-through: " + (chatClickThrough ? "On" : "Off"));
		setSettingLine(24211, "Distance fog: " + strengthLabel(fogStrength));
		setSettingLine(24212, "Anti-aliasing: " + c.aaLabel(aaStrength));
		setSettingLine(24213, "Animation smoothing: " + (tweeningEnabled ? "On" : "Off"));
		setSettingLine(24215, "Draw distance: " + drawDistance + " tiles");
		setSettingLine(24216, "Ground blending: " + (tileBlending ? "On" : "Off"));
		setSettingLine(24217, "Hide roofs: " + (hideRoofs ? "On" : "Off"));
		setSettingLine(24233, "OpenGL acceleration: " + (openGlEnabled ? "On" : "Off"));
		setSettingLine(24234, "FPS cap: " + (fpsUnlocked ? "Unlocked" : "50"));
		setSettingLine(24434, "Zoom sensitivity: " + c.zoomSensitivityLabel());
		setSettingLine(24435, "Reset camera zoom");
		setSettingLine(24231, "Performance stats: " + (performanceStats ? "On" : "Off"));
		setSettingLine(24232, "Show ping: " + (showPing ? "On" : "Off"));
		setSettingLine(24225, "NPC attack: " + attackOptionLabel(npcAttackOption));
		setSettingLine(24226, "Player attack: " + attackOptionLabel(playerAttackOption));
		setSettingLine(24227, "Menu entry swapper: " + (menuEntrySwapper ? "On" : "Off"));
		setSettingLine(24236, "Shift-click drop: " + (shiftClickDrop ? "On" : "Off"));
		setSettingLine(24433, "Shift-click walk here: " + (shiftClickWalkHere ? "On" : "Off"));
		setSettingLine(24237, "Middle-click wear: " + (middleClickWear ? "On" : "Off"));
		setSettingLine(24239, "Hide loot below: " + valueThresholdLabel(groundHideValue));
		setSettingLine(24240, "Loot beams: " + valueThresholdLabel(lootBeamValue));
		setSettingLine(24426, "Ground name shadow: " + c.groundItemShadowLabel(groundItemTextShadow));
		setSettingLine(24427, "Ground name size: " + c.groundItemSizeLabel(groundItemTextSize));
		setSettingLine(24428, "Ground whitelist: " + GroundItemLists.whitelistCount() + " items");
		setSettingLine(24429, "Ground blacklist: " + GroundItemLists.blacklistCount() + " items");
		setSettingLine(24241, "Destination tile: " + (destTile ? "On" : "Off"));
		setSettingLine(24242, "True tile: " + (trueTile ? "On" : "Off"));
		setSettingLine(24218, "Tile markers: " + (tileMarkers ? "On" : "Off"));
		setSettingLine(24229, "Ground markers: " + (GroundMarkers.enabled ? "On" : "Off"));
		setSettingLine(24219, "Ground item names: " + (groundItemNames ? "On" : "Off"));
		setSettingLine(24253, "NPC indicators: " + NpcIndicators.modeLabel());
		setSettingLine(24254, "NPC hull: " + (NpcIndicators.hull ? "On" : "Off"));
		setSettingLine(24255, "NPC tile: " + (NpcIndicators.tile ? "On" : "Off"));
		setSettingLine(24256, "NPC true tile: " + (NpcIndicators.trueTile ? "On" : "Off"));
		setSettingLine(24257, "NPC south-west tile: " + (NpcIndicators.southWestTile ? "On" : "Off"));
		setSettingLine(24258, "NPC highlight colour: " + NpcIndicators.colorLabel());
		setSettingLine(24259, "NPC names: " + (NpcIndicators.names ? "On" : "Off"));
		setSettingLine(24260, "NPC minimap names: " + (NpcIndicators.minimapNames ? "On" : "Off"));
		setSettingLine(24261, "Slayer overlay: " + (SlayerTracker.enabled ? "On" : "Off"));
		setSettingLine(24262, "Slayer highlight: " + (SlayerTracker.highlight ? "On" : "Off"));
		setSettingLine(24281, "Slayer count on gem: " + (SlayerTracker.countOnItems ? "On" : "Off"));
		setSettingLine(24276, "Boss timers: " + (BossTimers.enabled ? "On" : "Off"));
		setSettingLine(24270, "Object markers: " + (ObjectMarkers.enabled ? "On" : "Off"));
		setSettingLine(24268, "Inventory tags: " + (InventoryTags.enabled ? "On" : "Off"));
		setSettingLine(24423, "Inventory tag style: " + InventoryTags.styleLabel());
		setSettingLine(24424, "Inventory tag opacity: " + InventoryTags.opacity + "%");
		setSettingLine(24271, "Player indicators: " + (PlayerIndicators.enabled ? "On" : "Off"));
		setSettingLine(24272, "Player names: " + (PlayerIndicators.names ? "On" : "Off"));
		setSettingLine(24273, "Player tiles: " + (PlayerIndicators.tiles ? "On" : "Off"));
		setSettingLine(24274, "Player minimap names: " + (PlayerIndicators.minimapNames ? "On" : "Off"));
		setSettingLine(24277, "Highlight friends: " + (PlayerIndicators.friends ? "On" : "Off"));
		setSettingLine(24278, "Highlight team: " + (PlayerIndicators.team ? "On" : "Off"));
		setSettingLine(24279, "Highlight others: " + (PlayerIndicators.others ? "On" : "Off"));
		setSettingLine(24280, "Highlight self: " + (PlayerIndicators.ownPlayer ? "On" : "Off"));
		setSettingLine(24220, "NPC health overlay: " + (npcHealthOverlay ? "On" : "Off"));
		setSettingLine(24221, "Boosted stat overlay: " + (boostedStatOverlay ? "On" : "Off"));
		setSettingLine(24223, "Boosted stats as +N: " + (boostedPlusDisplay ? "On" : "Off"));
		setSettingLine(24425, "Boosted info boxes: " + (boostedInfoBox ? "On" : "Off"));
		setSettingLine(24224, "Attack style box: " + (attackStyleOverlay ? "On" : "Off"));
		setSettingLine(24267, "Attack style warn: " + AttackStyleWarn.label());
		setSettingLine(24238, "Special attack orb: " + (specOrb ? "On" : "Off"));
		setSettingLine(24247, "Low HP/prayer flash: " + (orbFlash ? "On" : "Off"));
		setSettingLine(24246, "Status timers: " + (statusTimers ? "On" : "Off"));
		setSettingLine(24263, "Ammo overlay: " + (AmmoOverlay.enabled ? "On" : "Off"));
		setSettingLine(24275, "Item stats: " + (ItemStats.enabled ? "On" : "Off"));
		setSettingLine(24269, "Mouse tooltips: " + (MouseTooltips.enabled ? "On" : "Off"));
		setSettingLine(24243, "Chat timestamps: " + (chatTimestamps ? "On" : "Off"));
		setSettingLine(24244, "Silent screenshots: " + (silentScreenshots ? "On" : "Off"));
		setSettingLine(24222, "XP drops: " + (xpDrops ? "On" : "Off"));
		setSettingLine(24235, "XP drop speed: " + xpDropSpeedLabel(xpDropSpeed));
		setSettingLine(24422, "Group XP drops: " + (xpDropGrouped ? "On" : "Off"));
		setSettingLine(24228, "Key remapping: " + (keyRemapping ? "On" : "Off") + " (setup)");
		setSettingLine(24264, "Anti-drag: " + (AntiDrag.enabled ? "On" : "Off"));
		setSettingLine(24265, "Anti-drag shift only: " + (AntiDrag.shiftOnly ? "On" : "Off"));
		setSettingLine(24266, "Anti-drag delay: " + AntiDrag.delay);
		setSettingLine(24400, "Cannon plugin: " + (CannonOverlay.enabled ? "On" : "Off"));
		setSettingLine(24401, "Cannon infobox: " + (CannonOverlay.infobox ? "On" : "Off"));
		setSettingLine(24402, "Cannon warning: " + CannonOverlay.warningLabel());
		setSettingLine(24403, "Cannon double-hit tiles: " + (CannonOverlay.doubleHit ? "On" : "Off"));
		setSettingLine(24404, "Cannon spots: " + (CannonOverlay.spots ? "On" : "Off"));
		setSettingLine(24405, "Implings: " + (ImplingsPlugin.enabled ? "On" : "Off"));
		setSettingLine(24406, "Impling names: " + (ImplingsPlugin.names ? "On" : "Off"));
		setSettingLine(24407, "Impling notify: " + (ImplingsPlugin.notify ? "On" : "Off"));
		setSettingLine(24408, "Barrows brothers: " + (BarrowsPlugin.enabled ? "On" : "Off"));
		setSettingLine(24410, "Chat history: " + (ChatHistory.enabled ? "On" : "Off"));
		setSettingLine(24411, "Chat channels: " + (ChatChannels.enabled ? "On" : "Off"));
		setSettingLine(24412, "Clan join/leave: " + (ChatChannels.joinLeave ? "On" : "Off"));
		setSettingLine(24413, "Friend list counts: " + (FriendListPlugin.enabled ? "On" : "Off"));
		setSettingLine(24414, "Friend notes: " + (FriendNotes.enabled ? "On" : "Off"));
		setSettingLine(24415, "Poison: " + (PoisonPlugin.enabled ? "On" : "Off"));
		setSettingLine(24416, "Regeneration meter: " + (RegenMeter.enabled ? "On" : "Off"));
		setSettingLine(24417, "Status bars: " + (StatusBars.enabled ? "On" : "Off"));
		setSettingLine(24418, "Status bar numbers: " + (StatusBars.numbers ? "On" : "Off"));
		setSettingLine(24419, "Status bar heal preview: " + (StatusBars.healPreview ? "On" : "Off"));
		setSettingLine(24214, "Right-click a skill on the Skills tab to start or stop XP tracking.");
		PluginSidebar.refresh();
	}

	public boolean applyClientSetting(int k) {
		switch (k) {
				case 24210:
					if (c.isFixed()) {
						c.setScreenMode(ScreenMode.RESIZABLE);
						c.pushMessage("Resizable mode on. Drag the window to resize.", 0, "");
					} else {
						c.setScreenMode(ScreenMode.FIXED);
						c.pushMessage("Fixed 765x503 mode restored.", 0, "");
					}
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24248:
					resizableInvTransparent = !resizableInvTransparent;
					needDrawTabArea = true;
					refreshClientSettingsInterface();
					saveClientSettings();
					if (c.isFixed()) {
						c.pushMessage("Inventory tab skins apply in resizable mode.", 0, "");
					} else {
						c.pushMessage(resizableInvTransparent
								? "Inventory tab is now transparent."
								: "Inventory tab is now the solid board.", 0, "");
					}
					break;
				case 24245:
					resizableChatTransparent = !resizableChatTransparent;
					inputTaken = true;
					refreshClientSettingsInterface();
					saveClientSettings();
					if (c.isFixed()) {
						c.pushMessage("Chat box skins apply in resizable mode.", 0, "");
					} else {
						c.pushMessage(resizableChatTransparent
								? "Chat box is now transparent."
								: "Chat box is now the solid board.", 0, "");
					}
					break;
				case 24431:
					chatScrollbarLeft = !chatScrollbarLeft;
					inputTaken = true;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Chat scrollbar moved to the " + (chatScrollbarLeft ? "left" : "right") + ".", 0, "");
					break;
				case 24432:
					chatClickThrough = !chatClickThrough;
					refreshClientSettingsInterface();
					saveClientSettings();
					if (c.isFixed() || !resizableChatTransparent) {
						c.pushMessage("Chat click-through applies in resizable transparent chat mode.", 0, "");
					} else {
						c.pushMessage("Chat click-through " + (chatClickThrough ? "on" : "off") + ".", 0, "");
					}
					break;
				case 24211:
					fogStrength = (fogStrength + 1) % 4;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24212:
					if (pluginValueSet) {
						aaStrength = aaFromInput(pluginValue);
						pluginValueSet = false;
					} else {
						aaStrength = (aaStrength + 1) % 5;
					}
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Anti-aliasing: " + c.aaLabel(aaStrength)
							+ " (crisp edges only, no blur).", 0, "");
					break;
				case 24213:
					tweeningEnabled = !tweeningEnabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24215:
					if (pluginValueSet) {
						drawDistance = clamp(pluginValue, 5, 90);
						pluginValueSet = false;
					} else {
						drawDistance += 15;
						if (drawDistance > 90) {
							drawDistance = 25;
						}
					}
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24216:
					tileBlending = !tileBlending;
					rebuildLoadedScene();
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Ground blending " + (tileBlending ? "on" : "off") + ".", 0, "");
					break;
				case 24217:
					hideRoofs = !hideRoofs;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Roofs " + (hideRoofs ? "hidden" : "shown") + ".", 0, "");
					break;
				case 24218:
					tileMarkers = !tileMarkers;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24219:
					groundItemNames = !groundItemNames;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24220:
					npcHealthOverlay = !npcHealthOverlay;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24221:
					boostedStatOverlay = !boostedStatOverlay;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24222:
					xpDrops = !xpDrops;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24235:
					xpDropSpeed = (xpDropSpeed + 1) % 5;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24422:
					xpDropGrouped = !xpDropGrouped;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Group XP drops " + (xpDropGrouped ? "on" : "off") + ".", 0, "");
					break;
				case 24223:
					boostedPlusDisplay = !boostedPlusDisplay;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24425:
					boostedInfoBox = !boostedInfoBox;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24224:
					attackStyleOverlay = !attackStyleOverlay;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24225:
					npcAttackOption = (npcAttackOption + 1) % 3;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24226:
					playerAttackOption = (playerAttackOption + 1) % 3;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24227:
					menuEntrySwapper = !menuEntrySwapper;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Menu entry swapper " + (menuEntrySwapper ? "on" : "off") + ". Shift-right-click an option to set left-click.", 0, "");
					break;
				case 24228:
					openInterfaceID = KeyRemapper.INTERFACE_ID;
					KeyRemapper.refreshInterface();
					break;
				case 24229:
					GroundMarkers.enabled = !GroundMarkers.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Ground markers " + (GroundMarkers.enabled ? "on. Shift-right-click a tile to mark it" : "off") + ".", 0, "");
					break;
				case 24253:
					NpcIndicators.cycleMode();
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage(NpcIndicators.mode == 1
							? "NPC indicators on tagged NPCs. Hold Ctrl and right-click to Tag hull or Tag tile."
							: NpcIndicators.mode == 2 ? "NPC indicators on all NPCs." : "NPC indicators off.", 0, "");
					break;
				case 24254:
					NpcIndicators.hull = !NpcIndicators.hull;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24255:
					NpcIndicators.tile = !NpcIndicators.tile;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24256:
					NpcIndicators.trueTile = !NpcIndicators.trueTile;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24257:
					NpcIndicators.southWestTile = !NpcIndicators.southWestTile;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24258:
					if (pluginValueSet) {
						NpcIndicators.setRgb(pluginValue);
						pluginValueSet = false;
					} else {
						NpcIndicators.cycleColor();
					}
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24259:
					NpcIndicators.names = !NpcIndicators.names;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24260:
					NpcIndicators.minimapNames = !NpcIndicators.minimapNames;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24261:
					SlayerTracker.enabled = !SlayerTracker.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24262:
					SlayerTracker.highlight = !SlayerTracker.highlight;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24281:
					SlayerTracker.countOnItems = !SlayerTracker.countOnItems;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24276:
					BossTimers.enabled = !BossTimers.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24270:
					ObjectMarkers.enabled = !ObjectMarkers.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Object markers " + (ObjectMarkers.enabled ? "on. Shift or Alt-right-click an object to mark it" : "off") + ".", 0, "");
					break;
				case 24268:
					InventoryTags.enabled = !InventoryTags.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Inventory tags " + (InventoryTags.enabled ? "on. Shift-right-click an item to tag it" : "off") + ".", 0, "");
					break;
				case 24423:
					InventoryTags.cycleStyle();
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Inventory tag style: " + InventoryTags.styleLabel() + ".", 0, "");
					break;
				case 24424:
					if (pluginValueSet) {
						InventoryTags.setOpacity(pluginValue);
						pluginValueSet = false;
					} else {
						int next = InventoryTags.opacity + 15;
						if (next > 100) {
							next = 25;
						}
						InventoryTags.setOpacity(next);
					}
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24271:
					PlayerIndicators.enabled = !PlayerIndicators.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24272:
					PlayerIndicators.names = !PlayerIndicators.names;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24273:
					PlayerIndicators.tiles = !PlayerIndicators.tiles;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24274:
					PlayerIndicators.minimapNames = !PlayerIndicators.minimapNames;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24277:
					PlayerIndicators.friends = !PlayerIndicators.friends;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24278:
					PlayerIndicators.team = !PlayerIndicators.team;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24279:
					PlayerIndicators.others = !PlayerIndicators.others;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24280:
					PlayerIndicators.ownPlayer = !PlayerIndicators.ownPlayer;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24263:
					AmmoOverlay.enabled = !AmmoOverlay.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24275:
					ItemStats.enabled = !ItemStats.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24269:
					MouseTooltips.enabled = !MouseTooltips.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24267:
					AttackStyleWarn.cycle();
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24264:
					AntiDrag.enabled = !AntiDrag.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Anti-drag " + (AntiDrag.enabled ? "on" : "off") + ".", 0, "");
					break;
				case 24265:
					AntiDrag.shiftOnly = !AntiDrag.shiftOnly;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24266:
					if (pluginValueSet) {
						AntiDrag.setDelay(pluginValue);
						pluginValueSet = false;
					} else {
						AntiDrag.cycleDelay();
					}
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24400:
					CannonOverlay.enabled = !CannonOverlay.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24401:
					CannonOverlay.infobox = !CannonOverlay.infobox;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24402:
					if (pluginValueSet) {
						CannonOverlay.setWarning(pluginValue);
						pluginValueSet = false;
					} else {
						CannonOverlay.cycleWarning();
					}
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24403:
					CannonOverlay.doubleHit = !CannonOverlay.doubleHit;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24404:
					CannonOverlay.spots = !CannonOverlay.spots;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24405:
					ImplingsPlugin.enabled = !ImplingsPlugin.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24406:
					ImplingsPlugin.names = !ImplingsPlugin.names;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24407:
					ImplingsPlugin.notify = !ImplingsPlugin.notify;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24408:
					BarrowsPlugin.enabled = !BarrowsPlugin.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24410:
					ChatHistory.enabled = !ChatHistory.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24411:
					ChatChannels.enabled = !ChatChannels.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24412:
					ChatChannels.joinLeave = !ChatChannels.joinLeave;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24413:
					FriendListPlugin.enabled = !FriendListPlugin.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24414:
					FriendNotes.enabled = !FriendNotes.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24415:
					PoisonPlugin.enabled = !PoisonPlugin.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24416:
					RegenMeter.enabled = !RegenMeter.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24417:
					StatusBars.enabled = !StatusBars.enabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24418:
					StatusBars.numbers = !StatusBars.numbers;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24419:
					StatusBars.healPreview = !StatusBars.healPreview;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24231:
					performanceStats = !performanceStats;
					fpsOn = performanceStats;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24232:
					showPing = !showPing;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24233:
					openGlEnabled = !openGlEnabled;
					refreshClientSettingsInterface();
					saveClientSettings();
					if (openGlEnabled) {
						c.pushMessage("OpenGL on. Restart the client so the GPU presents frames.", 0, "");
						c.pushMessage("If the game looks blurry, turn NVIDIA FXAA off and set AA to Application-controlled.", 0, "");
					} else {
						c.pushMessage("OpenGL off. Restart the client to fully disable it.", 0, "");
					}
					break;
				case 24234:
					fpsUnlocked = !fpsUnlocked;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage(fpsUnlocked ? "FPS cap unlocked. Game logic still runs at 50 ticks." : "FPS capped at 50.", 0, "");
					break;
				case 24434:
					if (pluginValueSet) {
						zoomSensitivity = clamp(pluginValue, ZOOM_SENSITIVITY_MIN, ZOOM_SENSITIVITY_MAX);
						pluginValueSet = false;
					} else {
						zoomSensitivity += 25;
						if (zoomSensitivity > ZOOM_SENSITIVITY_MAX) {
							zoomSensitivity = ZOOM_SENSITIVITY_MIN;
						}
					}
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Zoom sensitivity: " + c.zoomSensitivityLabel() + ".", 0, "");
					break;
				case 24435:
					resetCameraZoom();
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Camera zoom reset to default closeness (" + CAMERA_ZOOM_DEFAULT + ").", 0, "");
					break;
				case 24236:
					shiftClickDrop = !shiftClickDrop;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Shift-click drop " + (shiftClickDrop ? "on" : "off") + ".", 0, "");
					break;
				case 24433:
					shiftClickWalkHere = !shiftClickWalkHere;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Shift-click walk here " + (shiftClickWalkHere ? "on" : "off") + ".", 0, "");
					break;
				case 24237:
					middleClickWear = !middleClickWear;
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Middle-click wear " + (middleClickWear ? "on" : "off") + ".", 0, "");
					break;
				case 24238:
					specOrb = !specOrb;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24239:
					groundHideValue = nextValueThreshold(groundHideValue);
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24240:
					lootBeamValue = nextValueThreshold(lootBeamValue);
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24420:
					LootBeams.cycleStyle();
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Loot beam style: " + LootBeams.styleName() + ".", 0, "");
					break;
				case 24421:
					LootBeams.cycleFanfare();
					refreshClientSettingsInterface();
					saveClientSettings();
					c.pushMessage("Loot beam fanfare: " + LootBeams.fanfareName() + ".", 0, "");
					break;
				case 24426:
					groundItemTextShadow++;
					if (groundItemTextShadow > 2) {
						groundItemTextShadow = 0;
					}
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24427:
					groundItemTextSize++;
					if (groundItemTextSize > 2) {
						groundItemTextSize = 0;
					}
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24428:
					GroundItemLists.openManageWhitelist();
					refreshClientSettingsInterface();
					break;
				case 24429:
					GroundItemLists.openManageBlacklist();
					refreshClientSettingsInterface();
					break;
				case 24241:
					destTile = !destTile;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24242:
					trueTile = !trueTile;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24243:
					chatTimestamps = !chatTimestamps;
					inputTaken = true;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24244:
					silentScreenshots = !silentScreenshots;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24246:
					statusTimers = !statusTimers;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
				case 24247:
					orbFlash = !orbFlash;
					refreshClientSettingsInterface();
					saveClientSettings();
					break;
		default:
			return KeyRemapper.handleClick(k);
		}
		return true;
	}

	private void setCategoryTab(int id, String name, int category) {
		boolean selected = c.settingsCategory == category;
		setSettingLine(id, selected ? "> " + name : name);
		if (RSInterface.interfaceCache[id] != null) {
			RSInterface.interfaceCache[id].textColor = selected ? 0xffff00 : 0xff981f;
		}
	}

	private void rebuildSettingsList() {
		RSInterface list = RSInterface.interfaceCache[24230];
		if (list == null) {
			return;
		}
		int[] ids;
		switch (c.settingsCategory) {
		case 1:
			ids = new int[] {
				24253, 24254, 24255, 24256, 24257, 24258, 24259, 24260,
				24261, 24262, 24281, 24276, 24270, 24268, 24423, 24424, 24271, 24272, 24273, 24274, 24277, 24278, 24279, 24280,
				24400, 24401, 24402, 24403, 24404, 24405, 24406, 24407, 24408,
				24225, 24226, 24227, 24236, 24433, 24237, 24239, 24240, 24426, 24427, 24428, 24429, 24241, 24242, 24218, 24229, 24219
			};
			break;
		case 2:
			ids = new int[] {
				24220, 24221, 24223, 24425, 24224, 24267, 24238, 24247, 24246, 24263, 24275, 24269,
				24410, 24411, 24412, 24413, 24414, 24415, 24416, 24417, 24418, 24419,
				24243, 24244, 24222, 24235, 24422, 24214
			};
			break;
		case 3:
			ids = new int[] { 24228, 24264, 24265, 24266 };
			break;
		default:
			ids = new int[] {
				24210, 24248, 24245, 24431, 24432, 24211, 24212, 24213, 24215, 24216, 24217, 24233, 24234, 24434, 24435, 24231, 24232
			};
			break;
		}
		RSInterface.setChildren(ids.length, list);
		for (int i = 0; i < ids.length; i++) {
			RSInterface.setBounds(ids[i], 4, i * 20, i, list);
			if (RSInterface.interfaceCache[ids[i]] != null) {
				RSInterface.interfaceCache[ids[i]].height = 16;
			}
		}
		list.scrollMax = Math.max(list.height + 1, ids.length * 20 + 4);
		boolean categoryChanged = c.lastSettingsCategory != c.settingsCategory;
		c.lastSettingsCategory = c.settingsCategory;
		if (categoryChanged) {
			list.scrollPosition = 0;
		} else {
			int maxScroll = list.scrollMax - list.height;
			if (maxScroll < 0) {
				maxScroll = 0;
			}
			if (list.scrollPosition > maxScroll) {
				list.scrollPosition = maxScroll;
			}
			if (list.scrollPosition < 0) {
				list.scrollPosition = 0;
			}
		}
	}

	private String valueThresholdLabel(int value) {
		if (value <= 0) {
			return "Off";
		}
		if (value >= 1000000) {
			return "1m";
		}
		if (value >= 100000) {
			return "100k";
		}
		if (value >= 10000) {
			return "10k";
		}
		if (value >= 1000) {
			return "1k";
		}
		return "100";
	}

	private int nextValueThreshold(int value) {
		if (value <= 0) {
			return 100;
		}
		if (value < 1000) {
			return 1000;
		}
		if (value < 10000) {
			return 10000;
		}
		if (value < 100000) {
			return 100000;
		}
		if (value < 1000000) {
			return 1000000;
		}
		return 0;
	}

	private String xpDropSpeedLabel(int speed) {
		switch (speed) {
		case 0:
			return "Very slow";
		case 1:
			return "Slow";
		case 3:
			return "Fast";
		case 4:
			return "Very fast";
		default:
			return "Normal";
		}
	}

	private String attackOptionLabel(int mode) {
		if (mode == 1) {
			return "Right click";
		}
		if (mode == 2) {
			return "Hidden";
		}
		return "Left click";
	}

	private String strengthLabel(int value) {
		if (value <= 0) {
			return "Off";
		}
		if (value == 1) {
			return "Low";
		}
		if (value == 2) {
			return "Medium";
		}
		return "High";
	}

	int aaFromInput(int n) {
		if (n <= 0) {
			return 0;
		}
		if (n <= 4) {
			return n;
		}
		if (n <= 6) {
			return 3;
		}
		return 4;
	}

	void setSettingLine(int id, String text) {
		if (id < 0 || id >= RSInterface.interfaceCache.length || RSInterface.interfaceCache[id] == null) {
			return;
		}
		RSInterface.interfaceCache[id].message = text;
	}

	public void loadClientSettings() {
		try {
			File propsFile = new File(signlink.findcachedir() + "client_settings.properties");
			if (propsFile.exists()) {
				Properties props = new Properties();
				FileInputStream in = new FileInputStream(propsFile);
				props.load(in);
				in.close();
				c.midiVolume = readIntProp(props, "midiVolume", c.midiVolume);
				c.musicEnabled = readBoolProp(props, "musicEnabled", c.musicEnabled);
				optionBrightness = clamp(readIntProp(props, "brightness", 3), 1, 4);
				optionMusic = clamp(readIntProp(props, "music", 0), 0, 4);
				optionSound = clamp(readIntProp(props, "sound", 0), 0, 4);
				optionMouse = clamp(readIntProp(props, "mouseButtons", 0), 0, 1);
				optionChatEffects = clamp(readIntProp(props, "chatEffects", 0), 0, 1);
				optionSplitChat = clamp(readIntProp(props, "splitPrivateChat", 0), 0, 1);
				optionAcceptAid = clamp(readIntProp(props, "acceptAid", 0), 0, 1);
				savedResizeWidth = Math.max(765, readIntProp(props, "windowWidth", 765));
				savedResizeHeight = Math.max(503, readIntProp(props, "windowHeight", 503));
				fogStrength = clamp(readIntProp(props, "fogStrength", readBoolProp(props, "fog", true) ? 3 : 0), 0, 3);
				aaStrength = clamp(readIntProp(props, "aaStrength", readBoolProp(props, "antiAlias", true) ? 2 : 0), 0, 4);
				drawDistance = clamp(readIntProp(props, "drawDistance", 75), 5, 90);
				tweeningEnabled = readBoolProp(props, "tweening", true);
				tileBlending = readBoolProp(props, "tileBlending", true);
				hideRoofs = readBoolProp(props, "hideRoofs", false);
				tileMarkers = readBoolProp(props, "tileMarkers", false);
				groundItemNames = readBoolProp(props, "groundItemNames", false);
				npcHealthOverlay = readBoolProp(props, "npcHealthOverlay", false);
				boostedStatOverlay = readBoolProp(props, "boostedStatOverlay", false);
				xpDrops = readBoolProp(props, "xpDrops", false);
				xpDropSpeed = clamp(readIntProp(props, "xpDropSpeed", 1), 0, 4);
				xpDropGrouped = readBoolProp(props, "xpDropGrouped", false);
				boostedPlusDisplay = readBoolProp(props, "boostedPlusDisplay", false);
				boostedInfoBox = readBoolProp(props, "boostedInfoBox", true);
				attackStyleOverlay = readBoolProp(props, "attackStyleOverlay", false);
				npcAttackOption = clamp(readIntProp(props, "npcAttackOption", 0), 0, 2);
				playerAttackOption = clamp(readIntProp(props, "playerAttackOption", 0), 0, 2);
				menuEntrySwapper = readBoolProp(props, "menuEntrySwapper", false);
				keyRemapping = readBoolProp(props, "keyRemapping", false);
				enterToChat = readBoolProp(props, "enterToChat", true);
				wasdCamera = readBoolProp(props, "wasdCamera", false);
				spaceContinue = readBoolProp(props, "spaceContinue", true);
				chatTypeFocused = !keyRemapping;
				performanceStats = readBoolProp(props, "performanceStats", false);
				showPing = readBoolProp(props, "showPing", false);
				openGlEnabled = readBoolProp(props, "openGl", false);
				fpsUnlocked = readBoolProp(props, "fpsUnlocked", true);
				zoomSensitivity = clamp(readIntProp(props, "zoomSensitivity", ZOOM_SENSITIVITY_DEFAULT),
						ZOOM_SENSITIVITY_MIN, ZOOM_SENSITIVITY_MAX);
				shiftClickDrop = readBoolProp(props, "shiftClickDrop", true);
				shiftClickWalkHere = readBoolProp(props, "shiftClickWalkHere", true);
				middleClickWear = readBoolProp(props, "middleClickWear", true);
				specOrb = readBoolProp(props, "specOrb", true);
				groundHideValue = readIntProp(props, "groundHideValue", 0);
				lootBeamValue = readIntProp(props, "lootBeamValue", 0);
				groundItemTextShadow = clamp(readIntProp(props, "groundItemTextShadow", 1), 0, 2);
				groundItemTextSize = clamp(readIntProp(props, "groundItemTextSize", 0), 0, 2);
				GroundItemLists.load(props);
				LootBeams.load(props);
				destTile = readBoolProp(props, "destTile", true);
				trueTile = readBoolProp(props, "trueTile", false);
				chatTimestamps = readBoolProp(props, "chatTimestamps", true);
				silentScreenshots = readBoolProp(props, "silentScreenshots", true);
				xpTracker = readBoolProp(props, "xpTracker", false);
				loadTrackedSkills(readIntProp(props, "xpTrackSkills", 0));
				xpCounterOpen = readBoolProp(props, "xpCounterOpen", true);
				xpTrackerFixedX = readIntProp(props, "xpTrackerFixedX", -1);
				xpTrackerFixedY = readIntProp(props, "xpTrackerFixedY", -1);
				xpTrackerResizeX = readIntProp(props, "xpTrackerResizeX", -1);
				xpTrackerResizeY = readIntProp(props, "xpTrackerResizeY", -1);
				statusTimers = readBoolProp(props, "statusTimers", false);
				orbFlash = readBoolProp(props, "orbFlash", true);
				resizableInvTransparent = readBoolProp(props, "resizableInvTransparent", false);
				resizableChatTransparent = readBoolProp(props, "resizableChatTransparent", false);
				chatScrollbarLeft = readBoolProp(props, "chatScrollbarLeft", false);
				chatClickThrough = readBoolProp(props, "chatClickThrough", false);
				c.rememberMe = readBoolProp(props, "rememberMe", false) ? 1 : 0;
				if (c.rememberMe == 1) {
					String savedUser = props.getProperty("rememberUser", "");
					String savedPass = props.getProperty("rememberPass", "");
					if (savedUser != null) {
						c.myUsername = savedUser;
					}
					if (savedPass != null) {
						c.myPassword = savedPass;
					}
					c.loginScreenState = 2;
					c.loginMessage2 = "Enter your username & password.";
				}
				SavedCharacters.load(props);
				fpsOn = performanceStats;
				MenuEntrySwapper.load(props);
				GroundMarkers.load(props);
				NpcIndicators.load(props);
				SlayerTracker.load(props);
				AmmoOverlay.load(props);
				AntiDrag.load(props);
				AttackStyleWarn.load(props);
				InventoryTags.load(props);
				MouseTooltips.load(props);
				ObjectMarkers.load(props);
				PlayerIndicators.load(props);
				ItemStats.load(props);
				BossTimers.load(props);
				KeyRemapper.load(props);
				CannonOverlay.load(props);
				ChatChannels.load(props);
				ChatHistory.load(props);
				CombatLevelPlugin.load(props);
				FriendListPlugin.load(props);
				FriendNotes.load(props);
				ImplingsPlugin.load(props);
				PoisonPlugin.load(props);
				RegenMeter.load(props);
				StatusBars.load(props);
				BarrowsPlugin.load(props);
				OverlayManager.load(props);
				LootTracker.load(props);
				PluginSidebar.open = readBoolProp(props, "pluginSidebar", false);
				PluginSidebar.sidebarOut = readBoolProp(props, "pluginSidebarBar", true);
				PluginSidebar.selectedTab = readIntProp(props, "pluginSidebarTab", PluginSidebar.TAB_CONFIG);
				if (PluginSidebar.selectedTab != PluginSidebar.TAB_LOOT
						&& PluginSidebar.selectedTab != PluginSidebar.TAB_HISCORE) {
					PluginSidebar.selectedTab = PluginSidebar.TAB_CONFIG;
				}
				GroundMarkers.enabled = readBoolProp(props, "groundMarkers", false);
				GroundMarkers.minimap = readBoolProp(props, "groundMarkersMinimap", true);
				int quickBits = readIntProp(props, "quickPrayers", 0);
				for (int i = 0; i < c.quickPrayers.length; i++) {
					c.quickPrayers[i] = (quickBits & (1 << i)) != 0;
				}
				int curseBits = readIntProp(props, "quickCurses", 0);
				for (int i = 0; i < c.quickCurses.length; i++) {
					c.quickCurses[i] = (curseBits & (1 << i)) != 0;
				}
				if (readBoolProp(props, "resizable", false)) {
					frameMode = ScreenMode.RESIZABLE;
					frameWidth = savedResizeWidth;
					frameHeight = savedResizeHeight;
					screenAreaWidth = savedResizeWidth;
					screenAreaHeight = savedResizeHeight;
				} else {
					frameMode = ScreenMode.FIXED;
					frameWidth = 765;
					frameHeight = 503;
					screenAreaWidth = 512;
					screenAreaHeight = 334;
				}
			} else {
				String settingsFile = signlink.findcachedir() + "client_settings.dat";
				File file = new File(settingsFile);
				if (file.exists()) {
					DataInputStream dis = new DataInputStream(new FileInputStream(file));
					c.midiVolume = dis.readInt();
					c.musicEnabled = dis.readBoolean();
					dis.close();
				}
			}
			if (c.midiPlayer != null && c.midiPlayer.playing()) {
				c.midiPlayer.setVolume(0, c.midiVolume);
			}
			if (!c.musicEnabled) {
				optionMusic = 4;
			}
			if (optionMusic == 4) {
				c.loginMusicRestore = 0;
			} else {
				c.loginMusicRestore = optionMusic;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int clamp(int value, int min, int max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}

	private int readIntProp(Properties props, String key, int fallback) {
		try {
			String value = props.getProperty(key);
			if (value != null) {
				return Integer.parseInt(value.trim());
			}
		} catch (Exception e) {
		}
		return fallback;
	}

	private boolean readBoolProp(Properties props, String key, boolean fallback) {
		String value = props.getProperty(key);
		if (value != null) {
			return Boolean.parseBoolean(value.trim());
		}
		return fallback;
	}

	private void rebuildLoadedScene() {
		if (c.loggedIn && c.loadingStage == 2) {
			c.loadingStage = 1;
			c.aLong824 = System.currentTimeMillis();
		}
	}

	public void resetCameraZoom() {
		cameraZoom = CAMERA_ZOOM_DEFAULT;
		c.clampCameraZoom();
		if (instance != null) {
			instance.markSceneDirty();
		}
	}

	private int trackedSkillBits() {
		c.ensureTrackingArray();
		int bits = 0;
		for (int i = 0; i < c.trackingSkill.length && i < 31; i++) {
			if (c.trackingSkill[i]) {
				bits |= 1 << i;
			}
		}
		return bits;
	}

	private void loadTrackedSkills(int bits) {
		c.ensureTrackingArray();
		for (int i = 0; i < c.trackingSkill.length && i < 31; i++) {
			c.trackingSkill[i] = (bits & (1 << i)) != 0;
		}
	}}
