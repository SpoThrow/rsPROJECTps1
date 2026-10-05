package game;

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
import ui.SavedCharacters;
import ui.SlayerTracker;
import ui.Sprite;
import ui.StatusBars;
import ui.StatusTimers;
import ui.TextClass;
import ui.TextDrawingArea;
import ui.TextInput;

import static game.client.*;

/**
 * Right-click menu system: the world/scene menu, the NPC and player menus, the
 * HUD/minimap/friends-list menus, and the menu geometry (menuWidth/Height,
 * menuOffsetX/Y) and attack-option state, lifted out of client.java in 3.2.6.
 *
 * Deliberately does NOT include buildChatAreaMenu/buildSplitPrivateChatMenu,
 * which were already extracted to ChatArea in 3.2.5 and remain facades in
 * client. State stays on client and is reached through the receiver this class
 * is built with.
 */
public final class MenuSystem {
	private final client owner;

	public MenuSystem(client owner) {
		this.owner = owner;
	}

	void build3dScreenMenu() {
		if (owner.itemSelected == 0 && owner.spellSelected == 0) {
			owner.menuActionName[owner.menuActionRow] = "Walk here";
			owner.menuActionID[owner.menuActionRow] = 516;
			owner.menuActionCmd2[owner.menuActionRow] = owner.mouseX;
			owner.menuActionCmd3[owner.menuActionRow] = owner.mouseY;
			owner.menuActionRow++;
		}
		int j = -1;
		for (int k = 0; k < Model.anInt1687; k++) {
			int l = Model.anIntArray1688[k];
			int i1 = l & 0x7f;
			int j1 = l >> 7 & 0x7f;
			int k1 = l >> 29 & 3;
			int l1 = l >> 14 & 0x7fff;
			if (l == j)
				continue;
			j = l;
			if (k1 == 2 && owner.worldController.method304(owner.plane, i1, j1, l) >= 0) {
				ObjectDef class46 = ObjectDef.forID(l1);
				if (class46.childrenIDs != null)
					class46 = class46.method580();
				if (class46 == null)
					continue;
				if (owner.itemSelected == 1) {
					owner.menuActionName[owner.menuActionRow] = "Use " + owner.selectedItemName
							+ " with @cya@" + class46.name;
					owner.menuActionID[owner.menuActionRow] = 62;
					owner.menuActionCmd1[owner.menuActionRow] = l;
					owner.menuActionCmd2[owner.menuActionRow] = i1;
					owner.menuActionCmd3[owner.menuActionRow] = j1;
					owner.menuActionRow++;
				} else if (owner.spellSelected == 1) {
					if ((owner.spellUsableOn & 4) == 4) {
						owner.menuActionName[owner.menuActionRow] = owner.spellTooltip + " @cya@"
								+ class46.name;
						owner.menuActionID[owner.menuActionRow] = 956;
						owner.menuActionCmd1[owner.menuActionRow] = l;
						owner.menuActionCmd2[owner.menuActionRow] = i1;
						owner.menuActionCmd3[owner.menuActionRow] = j1;
						owner.menuActionRow++;
					}
				} else {
					if (class46.actions != null) {
						for (int i2 = 4; i2 >= 0; i2--)
							if (class46.actions[i2] != null) {
								owner.menuActionName[owner.menuActionRow] = class46.actions[i2]
										+ " @cya@" + class46.name;
								if (i2 == 0)
									owner.menuActionID[owner.menuActionRow] = 502;
								if (i2 == 1)
									owner.menuActionID[owner.menuActionRow] = 900;
								if (i2 == 2)
									owner.menuActionID[owner.menuActionRow] = 113;
								if (i2 == 3)
									owner.menuActionID[owner.menuActionRow] = 872;
								if (i2 == 4)
									owner.menuActionID[owner.menuActionRow] = 1062;
								owner.menuActionCmd1[owner.menuActionRow] = l;
								owner.menuActionCmd2[owner.menuActionRow] = i1;
								owner.menuActionCmd3[owner.menuActionRow] = j1;
								owner.menuActionRow++;
							}

					}
					owner.menuActionName[owner.menuActionRow] = "Examine @cya@"
							+ class46.name + " @gre@(@whi@" + l1
							+ "@gre@) (@whi@" + (i1 + baseX) + ","
							+ (j1 + baseY) + "@gre@)";
					// menuActionName[menuActionRow] = "Examine @cya@" +
					// class46.name;
					owner.menuActionID[owner.menuActionRow] = 1226;
					owner.menuActionCmd1[owner.menuActionRow] = class46.type << 14;
					owner.menuActionCmd2[owner.menuActionRow] = i1;
					owner.menuActionCmd3[owner.menuActionRow] = j1;
					owner.menuActionRow++;
				}
			}
			if (k1 == 1) {
				NPC npc = owner.npcArray[l1];
				if (npc.desc.aByte68 == 1 && (npc.x & 0x7f) == 64
						&& (npc.y & 0x7f) == 64) {
					for (int j2 = 0; j2 < owner.npcCount; j2++) {
						NPC npc2 = owner.npcArray[owner.npcIndices[j2]];
						if (npc2 != null && npc2 != npc
								&& npc2.desc.aByte68 == 1 && npc2.x == npc.x
								&& npc2.y == npc.y)
							buildAtNPCMenu(npc2.desc, owner.npcIndices[j2], j1, i1);
					}

					for (int l2 = 0; l2 < owner.playerCount; l2++) {
						Player player = owner.playerArray[owner.playerIndices[l2]];
						if (player != null && player.x == npc.x
								&& player.y == npc.y)
							buildAtPlayerMenu(i1, owner.playerIndices[l2], player, j1);
					}

				}
				buildAtNPCMenu(npc.desc, l1, j1, i1);
			}
			if (k1 == 0) {
				Player player = owner.playerArray[l1];
				if ((player.x & 0x7f) == 64 && (player.y & 0x7f) == 64) {
					for (int k2 = 0; k2 < owner.npcCount; k2++) {
						NPC class30_sub2_sub4_sub1_sub1_2 = owner.npcArray[owner.npcIndices[k2]];
						if (class30_sub2_sub4_sub1_sub1_2 != null
								&& class30_sub2_sub4_sub1_sub1_2.desc.aByte68 == 1
								&& class30_sub2_sub4_sub1_sub1_2.x == player.x
								&& class30_sub2_sub4_sub1_sub1_2.y == player.y)
							buildAtNPCMenu(class30_sub2_sub4_sub1_sub1_2.desc,
									owner.npcIndices[k2], j1, i1);
					}

					for (int i3 = 0; i3 < owner.playerCount; i3++) {
						Player class30_sub2_sub4_sub1_sub2_2 = owner.playerArray[owner.playerIndices[i3]];
						if (class30_sub2_sub4_sub1_sub2_2 != null
								&& class30_sub2_sub4_sub1_sub2_2 != player
								&& class30_sub2_sub4_sub1_sub2_2.x == player.x
								&& class30_sub2_sub4_sub1_sub2_2.y == player.y)
							buildAtPlayerMenu(i1, owner.playerIndices[i3],
									class30_sub2_sub4_sub1_sub2_2, j1);
					}

				}
				buildAtPlayerMenu(i1, l1, player, j1);
			}
			if (k1 == 3) {
				NodeList class19 = owner.groundArray[owner.plane][i1][j1];
				if (class19 != null) {
					for (Item item = (Item) class19.getFirst(); item != null; item = (Item) class19
							.getNext()) {
						ItemDef itemDef = ItemDef.forID(item.ID);
						if (owner.itemSelected == 1) {
							owner.menuActionName[owner.menuActionRow] = "Use "
									+ owner.selectedItemName + " with @lre@"
									+ itemDef.name;
							owner.menuActionID[owner.menuActionRow] = 511;
							owner.menuActionCmd1[owner.menuActionRow] = item.ID;
							owner.menuActionCmd2[owner.menuActionRow] = i1;
							owner.menuActionCmd3[owner.menuActionRow] = j1;
							owner.menuActionRow++;
						} else if (owner.spellSelected == 1) {
							if ((owner.spellUsableOn & 1) == 1) {
								owner.menuActionName[owner.menuActionRow] = owner.spellTooltip
										+ " @lre@" + itemDef.name;
								owner.menuActionID[owner.menuActionRow] = 94;
								owner.menuActionCmd1[owner.menuActionRow] = item.ID;
								owner.menuActionCmd2[owner.menuActionRow] = i1;
								owner.menuActionCmd3[owner.menuActionRow] = j1;
								owner.menuActionRow++;
							}
						} else {
							for (int j3 = 4; j3 >= 0; j3--)
								if (itemDef.groundActions != null
										&& itemDef.groundActions[j3] != null) {
									owner.menuActionName[owner.menuActionRow] = itemDef.groundActions[j3]
											+ " @lre@" + itemDef.name;
									if (j3 == 0)
										owner.menuActionID[owner.menuActionRow] = 652;
									if (j3 == 1)
										owner.menuActionID[owner.menuActionRow] = 567;
									if (j3 == 2)
										owner.menuActionID[owner.menuActionRow] = 234;
									if (j3 == 3)
										owner.menuActionID[owner.menuActionRow] = 244;
									if (j3 == 4)
										owner.menuActionID[owner.menuActionRow] = 213;
									owner.menuActionCmd1[owner.menuActionRow] = item.ID;
									owner.menuActionCmd2[owner.menuActionRow] = i1;
									owner.menuActionCmd3[owner.menuActionRow] = j1;
									owner.menuActionRow++;
								} else if (j3 == 2) {
									owner.menuActionName[owner.menuActionRow] = "Take @lre@"
											+ itemDef.name;
									owner.menuActionID[owner.menuActionRow] = 234;
									owner.menuActionCmd1[owner.menuActionRow] = item.ID;
									owner.menuActionCmd2[owner.menuActionRow] = i1;
									owner.menuActionCmd3[owner.menuActionRow] = j1;
									owner.menuActionRow++;
								}

							// menuActionName[menuActionRow] = "Examine @lre@" +
							// itemDef.name + " @gre@(@whi@" + item.ID +
							// "@gre@)";
							owner.menuActionName[owner.menuActionRow] = "Examine @lre@"
									+ itemDef.name;
							owner.menuActionID[owner.menuActionRow] = 1448;
							owner.menuActionCmd1[owner.menuActionRow] = item.ID;
							owner.menuActionCmd2[owner.menuActionRow] = i1;
							owner.menuActionCmd3[owner.menuActionRow] = j1;
							owner.menuActionRow++;
						}
					}
				}
			}
		}
	}

	public void determineMenuSize() {
		int i = owner.chatTextDrawingArea.getTextWidth("Choose Option");
		for (int j = 0; j < owner.menuActionRow; j++) {
			int k = owner.chatTextDrawingArea.getTextWidth(owner.menuActionName[j]);
			if (k > i)
				i = k;
		}

		i += 8;
		int l = 15 * owner.menuActionRow + 21;
		int clickMx = owner.saveClickX - owner.minimapDrawX();
		if (!isFixed()) {
			int i1 = owner.saveClickX - i / 2;
			if (i1 + i > frameWidth)
				i1 = frameWidth - i;
			if (i1 < 0)
				i1 = 0;
			int l1 = owner.saveClickY;
			if (l1 + l > frameHeight)
				l1 = frameHeight - l;
			if (l1 < 0)
				l1 = 0;
			owner.menuOpen = true;
			owner.menuScreenArea = 0;
			owner.menuOffsetX = i1;
			owner.menuOffsetY = l1;
			owner.menuWidth = i;
			owner.menuHeight = 15 * owner.menuActionRow + 22;
			return;
		}
		if (owner.overXpCounter(clickMx, owner.saveClickY)) {
			int i1 = owner.saveClickX - owner.gameDrawX() - i / 2;
			if (i1 + i > screenAreaWidth)
				i1 = screenAreaWidth - i;
			if (i1 < 0)
				i1 = 0;
			int l1 = owner.saveClickY - owner.gameDrawY();
			if (l1 + l > screenAreaHeight)
				l1 = screenAreaHeight - l;
			if (l1 < 0)
				l1 = 0;
			owner.menuOpen = true;
			owner.menuScreenArea = 0;
			owner.menuOffsetX = i1;
			owner.menuOffsetY = l1;
			owner.menuWidth = i;
			owner.menuHeight = 15 * owner.menuActionRow + 22;
			return;
		}
		if (owner.saveClickX > owner.tabDrawX() && owner.saveClickY > owner.tabDrawY()
				&& owner.saveClickX < owner.tabDrawX() + 246 && owner.saveClickY < owner.tabDrawY() + 335) {
			int j1 = owner.saveClickX - owner.tabDrawX() - i / 2;
			if (j1 < 0)
				j1 = 0;
			else if (j1 + i > 245)
				j1 = 245 - i;
			int i2 = owner.saveClickY - owner.tabDrawY();
			if (i2 < 0)
				i2 = 0;
			else if (i2 + l > 333)
				i2 = 333 - l;
			owner.menuOpen = true;
			owner.menuScreenArea = 1;
			owner.menuOffsetX = j1;
			owner.menuOffsetY = i2;
			owner.menuWidth = i;
			owner.menuHeight = 15 * owner.menuActionRow + 22;
		} else if (owner.saveClickX > 0 && owner.saveClickY > owner.chatDrawY()
				&& owner.saveClickX < 516 && owner.saveClickY < owner.chatDrawY() + 165
				&& !(owner.chatClickThroughActive() && owner.saveClickY < owner.chatDrawY() + 142
						&& !(owner.saveClickX >= (chatScrollbarLeft ? 2 : 496)
								&& owner.saveClickX < (chatScrollbarLeft ? 2 : 496) + 16))) {
			int k1 = owner.saveClickX - 0 - i / 2;
			if (k1 < 0)
				k1 = 0;
			else if (k1 + i > 516)
				k1 = 516 - i;
			int j2 = owner.saveClickY - owner.chatDrawY();
			if (j2 < 0)
				j2 = 0;
			else if (j2 + l > 165)
				j2 = 165 - l;
			owner.menuOpen = true;
			owner.menuScreenArea = 2;
			owner.menuOffsetX = k1;
			owner.menuOffsetY = j2;
			owner.menuWidth = i;
			owner.menuHeight = 15 * owner.menuActionRow + 22;
		} else if (owner.isOverHudControls(owner.saveClickX, owner.saveClickY)) {
			int j1 = owner.saveClickX - owner.minimapDrawX() - i / 2;
			int minX = hudMenuMinX();
			if (j1 + i > 246)
				j1 = 246 - i;
			if (j1 < minX)
				j1 = minX;
			int i2 = owner.saveClickY;
			if (i2 + l > 168)
				i2 = 168 - l;
			if (i2 < 0)
				i2 = 0;
			owner.menuOpen = true;
			owner.menuScreenArea = 3;
			owner.menuOffsetX = j1;
			owner.menuOffsetY = i2;
			owner.menuWidth = i;
			owner.menuHeight = 15 * owner.menuActionRow + 22;
		} else {
			int i1 = owner.saveClickX - owner.gameDrawX() - i / 2;
			if (i1 + i > screenAreaWidth)
				i1 = screenAreaWidth - i;
			if (i1 < 0)
				i1 = 0;
			int l1 = owner.saveClickY - owner.gameDrawY();
			if (l1 + l > screenAreaHeight)
				l1 = screenAreaHeight - l;
			if (l1 < 0)
				l1 = 0;
			owner.menuOpen = true;
			owner.menuScreenArea = 0;
			owner.menuOffsetX = i1;
			owner.menuOffsetY = l1;
			owner.menuWidth = i;
			owner.menuHeight = 15 * owner.menuActionRow + 22;
		}
	}

	public boolean processMenuClick() {
		if (owner.activeInterfaceType != 0)
			return false;
		int j = owner.clickMode3;
		if (owner.spellSelected == 1 && owner.saveClickX >= owner.minimapDrawX()
				&& owner.saveClickY >= 160 && owner.saveClickX <= frameWidth
				&& owner.saveClickY <= 205)
			j = 0;
		if (owner.menuOpen) {
			if (j != 1) {
				int k = owner.mouseX;
				int j1 = owner.mouseY;
				if (owner.menuScreenArea == 0) {
					k -= owner.gameDrawX();
					j1 -= owner.gameDrawY();
				}
				if (owner.menuScreenArea == 1) {
					k -= owner.tabDrawX();
					j1 -= owner.tabDrawY();
				}
				if (owner.menuScreenArea == 2) {
					k -= 17;
					j1 -= owner.chatDrawY();
				}
				if (owner.menuScreenArea == 3) {
					k -= owner.minimapDrawX();
					j1 -= 0;
				}
				if (k < owner.menuOffsetX - 10 || k > owner.menuOffsetX + owner.menuWidth + 10
						|| j1 < owner.menuOffsetY - 10
						|| j1 > owner.menuOffsetY + owner.menuHeight + 10) {
					owner.menuOpen = false;
					if (owner.menuScreenArea == 1)
						needDrawTabArea = true;
					if (owner.menuScreenArea == 2)
						inputTaken = true;
				}
			}
			if (j == 1) {
				int l = owner.menuOffsetX;
				int k1 = owner.menuOffsetY;
				int i2 = owner.menuWidth;
				int k2 = owner.saveClickX;
				int l2 = owner.saveClickY;
				if (owner.menuScreenArea == 0) {
					k2 -= owner.gameDrawX();
					l2 -= owner.gameDrawY();
				}
				if (owner.menuScreenArea == 1) {
					k2 -= owner.tabDrawX();
					l2 -= owner.tabDrawY();
				}
				if (owner.menuScreenArea == 2) {
					k2 -= 17;
					l2 -= owner.chatDrawY();
				}
				if (owner.menuScreenArea == 3) {
					k2 -= owner.minimapDrawX();
					l2 -= 0;
				}
				int i3 = -1;
				for (int j3 = 0; j3 < owner.menuActionRow; j3++) {
					int k3 = k1 + 31 + (owner.menuActionRow - 1 - j3) * 15;
					if (k2 > l && k2 < l + i2 && l2 > k3 - 13 && l2 < k3 + 3)
						i3 = j3;
				}
				if (i3 != -1)
					owner.doAction(i3);
				owner.menuOpen = false;
				if (owner.menuScreenArea == 1)
					needDrawTabArea = true;
				if (owner.menuScreenArea == 2) {
					inputTaken = true;
				}
			}
			return true;
		} else {
			if (j == 1 && owner.menuActionRow > 0) {
				int i1 = owner.menuActionID[owner.menuActionRow - 1];
				if (i1 == 632 || i1 == 78 || i1 == 867 || i1 == 431 || i1 == 53
						|| i1 == 74 || i1 == 454 || i1 == 539 || i1 == 493
						|| i1 == 847 || i1 == 447 || i1 == 1125) {
					int l1 = owner.menuActionCmd2[owner.menuActionRow - 1];
					int j2 = owner.menuActionCmd3[owner.menuActionRow - 1];
					RSInterface class9 = RSInterface.interfaceCache[j2];
					if (class9.aBoolean259 || class9.aBoolean235) {
						owner.aBoolean1242 = false;
						owner.anInt989 = 0;
						owner.anInt1084 = j2;
						owner.anInt1085 = l1;
						owner.activeInterfaceType = 2;
						owner.anInt1087 = owner.saveClickX;
						owner.anInt1088 = owner.saveClickY;
						if (RSInterface.interfaceCache[j2].parentID == openInterfaceID)
							owner.activeInterfaceType = 1;
						if (RSInterface.interfaceCache[j2].parentID == owner.backDialogID)
							owner.activeInterfaceType = 3;
						return true;
					}
				}
			}
			if (j == 1
					&& (owner.anInt1253 == 1 || menuHasAddFriend(owner.menuActionRow - 1))
					&& owner.menuActionRow > 2)
				j = 2;
			if (j == 1 && owner.menuActionRow > 0)
				owner.doAction(owner.menuActionRow - 1);
			if (j == 2 && owner.menuActionRow > 0)
				determineMenuSize();
			return false;
		}
	}

	public void buildAtNPCMenu(EntityDef entityDef, int i, int j, int k) {
		if (owner.menuActionRow >= 400)
			return;
		if (entityDef.childrenIDs != null)
			entityDef = entityDef.method161();
		if (entityDef == null)
			return;
		if (!entityDef.aBoolean84)
			return;
		String s = entityDef.name;
		if (entityDef.combatLevel != 0)
			s = s
					+ combatDiffColor(myPlayer.combatLevel,
							entityDef.combatLevel) + " (level-"
					+ entityDef.combatLevel + ")";
		if (owner.itemSelected == 1) {
			owner.menuActionName[owner.menuActionRow] = "Use " + owner.selectedItemName
					+ " with @yel@" + s;
			owner.menuActionID[owner.menuActionRow] = 582;
			owner.menuActionCmd1[owner.menuActionRow] = i;
			owner.menuActionCmd2[owner.menuActionRow] = k;
			owner.menuActionCmd3[owner.menuActionRow] = j;
			owner.menuActionRow++;
			return;
		}
		if (owner.spellSelected == 1) {
			if ((owner.spellUsableOn & 2) == 2) {
				owner.menuActionName[owner.menuActionRow] = owner.spellTooltip + " @yel@" + s;
				owner.menuActionID[owner.menuActionRow] = 413;
				owner.menuActionCmd1[owner.menuActionRow] = i;
				owner.menuActionCmd2[owner.menuActionRow] = k;
				owner.menuActionCmd3[owner.menuActionRow] = j;
				owner.menuActionRow++;
			}
		} else {
			if (entityDef.actions != null) {
				boolean rightClickAttack = npcAttackOption == 1;
				if (npcAttackOption != 2 && rightClickAttack) {
					addNpcAttackOptions(entityDef, i, j, k, s);
				}
				for (int l = 4; l >= 0; l--)
					if (entityDef.actions[l] != null
							&& !entityDef.actions[l].equalsIgnoreCase("attack")) {
						owner.menuActionName[owner.menuActionRow] = entityDef.actions[l]
								+ " @yel@" + s;
						if (l == 0)
							owner.menuActionID[owner.menuActionRow] = 20;
						if (l == 1)
							owner.menuActionID[owner.menuActionRow] = 412;
						if (l == 2)
							owner.menuActionID[owner.menuActionRow] = 225;
						if (l == 3)
							owner.menuActionID[owner.menuActionRow] = 965;
						if (l == 4)
							owner.menuActionID[owner.menuActionRow] = 478;
						owner.menuActionCmd1[owner.menuActionRow] = i;
						owner.menuActionCmd2[owner.menuActionRow] = k;
						owner.menuActionCmd3[owner.menuActionRow] = j;
						owner.menuActionRow++;
					}
				if (npcAttackOption != 2 && !rightClickAttack) {
					addNpcAttackOptions(entityDef, i, j, k, s);
				}
			}
			// menuActionName[menuActionRow] = "Examine @yel@" + s +
			// " @gre@(@whi@" + entityDef.type + "@gre@)";
			owner.menuActionName[owner.menuActionRow] = "Examine @yel@" + s;
			owner.menuActionID[owner.menuActionRow] = 1025;
			owner.menuActionCmd1[owner.menuActionRow] = i;
			owner.menuActionCmd2[owner.menuActionRow] = k;
			owner.menuActionCmd3[owner.menuActionRow] = j;
			owner.menuActionRow++;
		}
	}

	public void buildAtPlayerMenu(int i, int j, Player player, int k) {
		if (player == myPlayer)
			return;
		if (owner.menuActionRow >= 400)
			return;
		String s;
		if (player.skill == 0)
			s = player.name
					+ combatDiffColor(myPlayer.combatLevel, player.combatLevel)
					+ " (level-" + player.combatLevel + ")";
		else
			s = player.name + " (skill-" + player.skill + ")";
		String col = PlayerIndicators.menuPrefix(owner, player);
		if (owner.itemSelected == 1) {
			owner.menuActionName[owner.menuActionRow] = "Use " + owner.selectedItemName
					+ " with " + col + s;
			owner.menuActionID[owner.menuActionRow] = 491;
			owner.menuActionCmd1[owner.menuActionRow] = j;
			owner.menuActionCmd2[owner.menuActionRow] = i;
			owner.menuActionCmd3[owner.menuActionRow] = k;
			owner.menuActionRow++;
		} else if (owner.spellSelected == 1) {
			if ((owner.spellUsableOn & 8) == 8) {
				owner.menuActionName[owner.menuActionRow] = owner.spellTooltip + " " + col + s;
				owner.menuActionID[owner.menuActionRow] = 365;
				owner.menuActionCmd1[owner.menuActionRow] = j;
				owner.menuActionCmd2[owner.menuActionRow] = i;
				owner.menuActionCmd3[owner.menuActionRow] = k;
				owner.menuActionRow++;
			}
		} else {
			boolean rightClickAttack = playerAttackOption == 1;
			if (rightClickAttack) {
				addPlayerAttackOptions(i, j, player, k, s);
			}
			for (int l = 4; l >= 0; l--)
				if (owner.atPlayerActions[l] != null) {
					if (owner.atPlayerActions[l].equalsIgnoreCase("attack")) {
						continue;
					}
					owner.menuActionName[owner.menuActionRow] = owner.atPlayerActions[l]
							+ " " + col + s;
					char c = '\0';
					if (owner.atPlayerArray[l])
						c = '\u07D0';
					if (l == 0)
						owner.menuActionID[owner.menuActionRow] = 561 + c;
					if (l == 1)
						owner.menuActionID[owner.menuActionRow] = 779 + c;
					if (l == 2)
						owner.menuActionID[owner.menuActionRow] = 27 + c;
					if (l == 3)
						owner.menuActionID[owner.menuActionRow] = 577 + c;
					if (l == 4)
						owner.menuActionID[owner.menuActionRow] = 729 + c;
					owner.menuActionCmd1[owner.menuActionRow] = j;
					owner.menuActionCmd2[owner.menuActionRow] = i;
					owner.menuActionCmd3[owner.menuActionRow] = k;
					owner.menuActionRow++;
				}
			if (!rightClickAttack) {
				addPlayerAttackOptions(i, j, player, k, s);
			}

		}
		for (int i1 = 0; i1 < owner.menuActionRow; i1++)
			if (owner.menuActionID[i1] == 516) {
				owner.menuActionName[i1] = "Walk here " + col + s;
				return;
			}

	}

	void buildMinimapHudMenu() {
		int mx = owner.mouseX - owner.minimapDrawX();
		int my = owner.mouseY;
		HudLayout hud = HudLayout.get();
		if (owner.hudHit(mx, my, hud.compassX, hud.compassY, hud.compassW > 0 ? hud.compassW : 33, hud.compassH > 0 ? hud.compassH : 33)) {
			owner.menuActionName[owner.menuActionRow] = "Face North";
			owner.menuActionID[owner.menuActionRow] = 1503;
			owner.menuActionRow++;
			return;
		}
		if (owner.hudHit(mx, my, hud.runOrbX, hud.runOrbY, hud.runOrbW, hud.runOrbH)) {
			owner.menuActionName[owner.menuActionRow] = "Toggle Run";
			owner.menuActionID[owner.menuActionRow] = 1050;
			owner.menuActionRow++;
			return;
		}
		if (owner.hudHit(mx, my, hud.prayerOrbX, hud.prayerOrbY, hud.prayerOrbW, hud.prayerOrbH)) {
			if (owner.selectingQuickPrayers) {
				owner.menuActionName[owner.menuActionRow] = "Confirm Quick Prayers";
				owner.menuActionID[owner.menuActionRow] = 1507;
				owner.menuActionRow++;
			} else {
				owner.menuActionName[owner.menuActionRow] = "Setup Quick Prayers";
				owner.menuActionID[owner.menuActionRow] = 1506;
				owner.menuActionRow++;
				owner.menuActionName[owner.menuActionRow] = "Toggle Quick Prayers";
				owner.menuActionID[owner.menuActionRow] = 1505;
				owner.menuActionRow++;
			}
			return;
		}
		if (specOrb && owner.hudHit(mx, my, hud.specOrbX, hud.specOrbY, hud.specOrbW, hud.specOrbH)) {
			owner.menuActionName[owner.menuActionRow] = "Use Special Attack";
			owner.menuActionID[owner.menuActionRow] = 1509;
			owner.menuActionRow++;
			return;
		}
		if (owner.hudHit(mx, my, hud.hpOrbX, hud.hpOrbY, hud.hpOrbW, hud.hpOrbH)) {
			String poisonTip = PoisonPlugin.orbTooltip();
			if (poisonTip != null) {
				owner.menuActionName[owner.menuActionRow] = poisonTip;
				owner.menuActionID[owner.menuActionRow] = 1515;
				owner.menuActionRow++;
			}
			return;
		}
		if (owner.overXpCounter(mx, my)) {
			owner.menuActionName[owner.menuActionRow] = "Reset XP tracker";
			owner.menuActionID[owner.menuActionRow] = 1510;
			owner.menuActionRow++;
			owner.menuActionName[owner.menuActionRow] = (xpDrops ? "Turn XP drops off" : "Turn XP drops on");
			owner.menuActionID[owner.menuActionRow] = 1512;
			owner.menuActionRow++;
		}
	}

	public void drawMenu() {
		int i = owner.menuOffsetX;
		int j = owner.menuOffsetY;
		int k = owner.menuWidth;
		int l = owner.menuHeight + 1;
		int i1 = 0x5d5447;
		// DrawingArea.drawPixels(height, yPos, xPos, color, width);
		// DrawingArea.fillPixels(xPos, width, height, color, yPos);
		DrawingArea.drawPixels(l, j, i, i1, k);
		DrawingArea.drawPixels(16, j + 1, i + 1, 0, k - 2);
		DrawingArea.fillPixels(i + 1, k - 2, l - 19, 0, j + 18);
		owner.chatTextDrawingArea.method385(i1, "Choose Option", j + 14, i + 3);
		int j1 = owner.mouseX;
		int k1 = owner.mouseY;
		if (owner.menuScreenArea == 0) {
			j1 -= owner.gameDrawX();
			k1 -= owner.gameDrawY();
		}
		if (owner.menuScreenArea == 1) {
			j1 -= owner.tabDrawX();
			k1 -= owner.tabDrawY();
		}
		if (owner.menuScreenArea == 2) {
			j1 -= 17;
			k1 -= owner.chatDrawY();
		}
		if (owner.menuScreenArea == 3) {
			j1 -= owner.minimapDrawX();
			k1 -= 0;
		}
		for (int l1 = 0; l1 < owner.menuActionRow; l1++) {
			int i2 = j + 31 + (owner.menuActionRow - 1 - l1) * 15;
			int j2 = 0xffffff;
			if (j1 > i && j1 < i + k && k1 > i2 - 13 && k1 < i2 + 3)
				j2 = 0xffff00;
			owner.newBoldFont.drawBasicString(owner.menuActionName[l1], i + 3, i2, j2, 0);
		}
	}

	private void addPlayerAttackOptions(int i, int j, Player player, int k, String s) {
		if (playerAttackOption == 2) {
			return;
		}
		String col = PlayerIndicators.menuPrefix(owner, player);
		for (int l = 4; l >= 0; l--) {
			if (owner.atPlayerActions[l] == null || !owner.atPlayerActions[l].equalsIgnoreCase("attack")) {
				continue;
			}
			char c = '\0';
			if (player.combatLevel > myPlayer.combatLevel)
				c = '\u07D0';
			if (myPlayer.team != 0 && player.team != 0)
				if (myPlayer.team == player.team)
					c = '\u07D0';
				else
					c = '\0';
			owner.menuActionName[owner.menuActionRow] = owner.atPlayerActions[l] + " " + col + s;
			if (l == 0)
				owner.menuActionID[owner.menuActionRow] = 561 + c;
			if (l == 1)
				owner.menuActionID[owner.menuActionRow] = 779 + c;
			if (l == 2)
				owner.menuActionID[owner.menuActionRow] = 27 + c;
			if (l == 3)
				owner.menuActionID[owner.menuActionRow] = 577 + c;
			if (l == 4)
				owner.menuActionID[owner.menuActionRow] = 729 + c;
			owner.menuActionCmd1[owner.menuActionRow] = j;
			owner.menuActionCmd2[owner.menuActionRow] = i;
			owner.menuActionCmd3[owner.menuActionRow] = k;
			owner.menuActionRow++;
		}
	}

	private void addNpcAttackOptions(EntityDef entityDef, int i, int j, int k, String s) {
		if (entityDef.actions == null) {
			return;
		}
		for (int i1 = 4; i1 >= 0; i1--) {
			if (entityDef.actions[i1] == null
					|| !entityDef.actions[i1].equalsIgnoreCase("attack")) {
				continue;
			}
			// Vanilla +2000 demotes Attack vs higher-CB NPCs (Nex etc) so left-click
			// becomes Walk/Examine. Skip that penalty when the NPC-attack plugin is
			// set to Left click.
			int priority = 0;
			if (npcAttackOption != 0 && entityDef.combatLevel > myPlayer.combatLevel) {
				priority = 2000;
			}
			owner.menuActionName[owner.menuActionRow] = entityDef.actions[i1] + " @yel@" + s;
			if (i1 == 0)
				owner.menuActionID[owner.menuActionRow] = 20 + priority;
			if (i1 == 1)
				owner.menuActionID[owner.menuActionRow] = 412 + priority;
			if (i1 == 2)
				owner.menuActionID[owner.menuActionRow] = 225 + priority;
			if (i1 == 3)
				owner.menuActionID[owner.menuActionRow] = 965 + priority;
			if (i1 == 4)
				owner.menuActionID[owner.menuActionRow] = 478 + priority;
			owner.menuActionCmd1[owner.menuActionRow] = i;
			owner.menuActionCmd2[owner.menuActionRow] = k;
			owner.menuActionCmd3[owner.menuActionRow] = j;
			owner.menuActionRow++;
		}
	}

	boolean buildFriendsListMenu(RSInterface class9) {
		int i = class9.contentType;
		if (i >= 1 && i <= 200 || i >= 701 && i <= 900) {
			if (i >= 801)
				i -= 701;
			else if (i >= 701)
				i -= 601;
			else if (i >= 101)
				i -= 101;
			else
				i--;
			owner.menuActionName[owner.menuActionRow] = "Remove @whi@" + owner.friendsList[i];
			owner.menuActionID[owner.menuActionRow] = 792;
			owner.menuActionRow++;
			if (FriendNotes.enabled) {
				owner.menuActionName[owner.menuActionRow] = FriendNotes.menuLabel(owner.friendsList[i]) + " @whi@" + owner.friendsList[i];
				owner.menuActionID[owner.menuActionRow] = FriendNotes.ACTION_NOTE;
				owner.menuActionRow++;
			}
			owner.menuActionName[owner.menuActionRow] = "Message @whi@" + owner.friendsList[i];
			owner.menuActionID[owner.menuActionRow] = 639;
			owner.menuActionRow++;
			return true;
		}
		if (i >= 401 && i <= 500) {
			owner.menuActionName[owner.menuActionRow] = "Remove @whi@" + class9.message;
			owner.menuActionID[owner.menuActionRow] = 322;
			owner.menuActionRow++;
			return true;
		} else {
			return false;
		}
	}

	private int hudMenuMinX() {
		HudLayout hud = HudLayout.get();
		int min = 0;
		if (hud.compassX < min) {
			min = hud.compassX;
		}
		if (hud.hpOrbX < min) {
			min = hud.hpOrbX;
		}
		if (hud.prayerOrbX < min) {
			min = hud.prayerOrbX;
		}
		if (hud.runOrbX < min) {
			min = hud.runOrbX;
		}
		if (hud.specOrbX < min) {
			min = hud.specOrbX;
		}
		int xpLeft = owner.xpHudOffX();
		if (xpLeft < min) {
			min = xpLeft;
		}
		return min - 8;
	}

	static String combatDiffColor(int i, int j) {
		int k = i - j;
		if (k < -9)
			return "@red@";
		if (k < -6)
			return "@or3@";
		if (k < -3)
			return "@or2@";
		if (k < 0)
			return "@or1@";
		if (k > 9)
			return "@gre@";
		if (k > 6)
			return "@gr3@";
		if (k > 3)
			return "@gr2@";
		if (k > 0)
			return "@gr1@";
		else
			return "@yel@";
	}

	boolean menuHasAddFriend(int j) {
		if (j < 0)
			return false;
		int k = owner.menuActionID[j];
		if (k >= 2000)
			k -= 2000;
		return k == 337;
	}
}
