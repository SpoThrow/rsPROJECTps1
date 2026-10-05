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
import ui.Renderer;
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
 * Tab area / sidebar: the side icons, the red-stone level markers, skill
 * tooltips and the quick-prayer overlay, lifted out of client.java in 3.2.4.
 *
 * Deliberately NARROWED: the generic interface renderer (drawInterface,
 * drawFriendsListOrWelcomeScreen) and the interface-value / number helpers
 * stay in client, because they are not tab-specific. State stays on client
 * and is reached through the receiver this class is constructed with.
 */
public final class TabArea {
	private final client owner;

	public TabArea(client owner) {
		this.owner = owner;
	}

	public void processTabClick() {
		if (owner.clickMode3 != 1) {
			return;
		}
		int tx = owner.tabDrawX();
		int ty = owner.tabDrawY();
		if (owner.saveClickX >= tx + 5 && owner.saveClickX <= tx + 42
				&& owner.saveClickY >= ty + 1 && owner.saveClickY < ty + 37
				&& tabInterfaceIDs[0] != -1) {
			needDrawTabArea = true;
			tabID = 0;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 43 && owner.saveClickX <= tx + 75
				&& owner.saveClickY >= ty && owner.saveClickY < ty + 37
				&& tabInterfaceIDs[1] != -1) {
			needDrawTabArea = true;
			tabID = 1;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 76 && owner.saveClickX <= tx + 107
				&& owner.saveClickY >= ty && owner.saveClickY < ty + 37
				&& tabInterfaceIDs[2] != -1) {
			needDrawTabArea = true;
			tabID = 2;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 108 && owner.saveClickX <= tx + 141
				&& owner.saveClickY >= ty && owner.saveClickY < ty + 35
				&& tabInterfaceIDs[3] != -1) {
			needDrawTabArea = true;
			tabID = 3;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 142 && owner.saveClickX <= tx + 174
				&& owner.saveClickY >= ty && owner.saveClickY < ty + 37
				&& tabInterfaceIDs[4] != -1) {
			needDrawTabArea = true;
			tabID = 4;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 175 && owner.saveClickX <= tx + 206
				&& owner.saveClickY >= ty && owner.saveClickY < ty + 37
				&& tabInterfaceIDs[5] != -1) {
			needDrawTabArea = true;
			tabID = 5;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 207 && owner.saveClickX <= tx + 246
				&& owner.saveClickY >= ty + 1 && owner.saveClickY < ty + 37
				&& tabInterfaceIDs[6] != -1) {
			needDrawTabArea = true;
			tabID = 6;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 5 && owner.saveClickX <= tx + 42
				&& owner.saveClickY >= ty + 298 && owner.saveClickY < ty + 335
				&& tabInterfaceIDs[7] != -1) {
			needDrawTabArea = true;
			tabID = 7;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 43 && owner.saveClickX <= tx + 75
				&& owner.saveClickY >= ty + 298 && owner.saveClickY < ty + 335
				&& tabInterfaceIDs[8] != -1) {
			needDrawTabArea = true;
			tabID = 8;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 76 && owner.saveClickX <= tx + 108
				&& owner.saveClickY >= ty + 298 && owner.saveClickY < ty + 335
				&& tabInterfaceIDs[9] != -1) {
			needDrawTabArea = true;
			tabID = 9;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 108 && owner.saveClickX <= tx + 145
				&& owner.saveClickY >= ty + 298 && owner.saveClickY < ty + 335
				&& tabInterfaceIDs[10] != -1) {
			needDrawTabArea = true;
			tabID = 10;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 142 && owner.saveClickX <= tx + 175
				&& owner.saveClickY >= ty + 298 && owner.saveClickY < ty + 335
				&& tabInterfaceIDs[11] != -1) {
			needDrawTabArea = true;
			tabID = 11;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 176 && owner.saveClickX <= tx + 206
				&& owner.saveClickY >= ty + 298 && owner.saveClickY < ty + 335
				&& tabInterfaceIDs[12] != -1) {
			needDrawTabArea = true;
			tabID = 12;
			tabAreaAltered = true;
		}
		if (owner.saveClickX >= tx + 207 && owner.saveClickX <= tx + 246
				&& owner.saveClickY >= ty + 298 && owner.saveClickY < ty + 334
				&& tabInterfaceIDs[13] != -1) {
			needDrawTabArea = true;
			tabID = 13;
			tabAreaAltered = true;
		}
	}

	private void drawSkillTooltip() {
		if (tabInterfaceIDs[tabID] != 3917) {
			return;
		}
		int mx = owner.mouseX - skillTabOriginX();
		int my = owner.mouseY - skillTabOriginY();
		int skill = owner.hoveredSkill(mx, my);
		if (skill == -2) {
			return;
		}
		String[] lines;
		if (skill == -1) {
			long total = 0;
			for (int i = 0; i < owner.currentExp.length; i++) {
				total += owner.currentExp[i];
			}
			lines = new String[] { "Total XP: " + owner.formatNumber((int) Math.min(total, 2147483647L)) };
		} else {
			int xp = owner.currentExp[skill];
			int level = owner.maxStats[skill];
			int next = level >= 99 ? anIntArray1019[97] : anIntArray1019[level - 1];
			if (level <= 1) {
				next = anIntArray1019[0];
			}
			int remain = next - xp;
			if (remain < 0 || level >= 99) {
				remain = 0;
			}
			lines = new String[] {
				owner.skillDisplayName(skill) + " XP: " + owner.formatNumber(xp),
				"Next level at: " + owner.formatNumber(next),
				"Remaining XP: " + owner.formatNumber(remain)
			};
		}
		int width = 10;
		for (int i = 0; i < lines.length; i++) {
			int w = owner.smallText.getTextWidth(lines[i]) + 8;
			if (w > width) {
				width = w;
			}
		}
		int height = 6 + lines.length * 12;
		int boxX;
		int boxY = my + 2;
		if (mx < 95 && mx + 10 + width <= 190) {
			boxX = mx + 10;
		} else {
			boxX = mx - width - 4;
		}
		if (boxY + height > 250) {
			boxY = my - height;
		}
		if (boxX < 0) {
			boxX = 0;
		}
		if (boxY < 0) {
			boxY = 0;
		}
		DrawingArea.method335(0x000000, boxY, width, height, 180, boxX);
		DrawingArea.fillPixels(boxX, width, height, 0x5A4933, boxY);
		for (int i = 0; i < lines.length; i++) {
			owner.smallText.method385(0xff981f, lines[i], boxY + 12 + i * 12, boxX + 4);
		}
	}

	void drawTabArea() {
		owner.aRSImageProducer_1163.initDrawingArea();
		Texture.anIntArray1472 = owner.anIntArray1181;
		if (!isFixed()) {
			owner.copyWorldUnderHud(owner.aRSImageProducer_1163, owner.tabDrawX(), owner.tabDrawY());
		}
		currentTabArea().drawSprite(0, 0);
		if (owner.invOverlayInterfaceID == -1) {
			drawRedStones();
			drawSideIcons();
		}
		if (owner.invOverlayInterfaceID != -1)
			owner.drawInterface(0, 28,
					RSInterface.interfaceCache[owner.invOverlayInterfaceID], 37);
		else if (tabInterfaceIDs[tabID] != -1)
			owner.drawInterface(0, 28,
					RSInterface.interfaceCache[tabInterfaceIDs[tabID]], 37);
		drawSkillTooltip();
		CombatLevelPlugin.apply(owner.maxStats);
		FriendListPlugin.updateTitle(owner.friendsCount, owner.anInt1046 == 1 ? 200 : 100);
		int hpNow = owner.widgetInt(4016);
		int hpMax = owner.widgetInt(4017);
		int prayNow = owner.widgetInt(4012);
		int prayMax = owner.widgetInt(4013);
		if (hpNow < 0) {
			hpNow = owner.currentStats != null && owner.currentStats.length > 3 ? owner.currentStats[3] : 0;
		}
		if (hpMax < 1) {
			hpMax = owner.maxStats != null && owner.maxStats.length > 3 ? owner.maxStats[3] : 1;
		}
		if (prayNow < 0) {
			prayNow = owner.currentStats != null && owner.currentStats.length > 5 ? owner.currentStats[5] : 0;
		}
		if (prayMax < 1) {
			prayMax = owner.maxStats != null && owner.maxStats.length > 5 ? owner.maxStats[5] : 1;
		}
		Sprite hpIcon = owner.ORBS != null && owner.ORBS.length > 3 ? owner.ORBS[3] : null;
		Sprite prayIcon = owner.ORBS != null && owner.ORBS.length > 6 ? owner.ORBS[6] : null;
		StatusBars.draw(owner.smallText, hpNow, hpMax, prayNow, prayMax, hpIcon, prayIcon);
		StatusBars.hoverHeal = 0;
		if (owner.selectingQuickPrayers && tabID == 5) {
			drawQuickPrayerSelection();
		}
		if (!owner.menuOpen && MouseTooltips.enabled) {
			drawTooltipOn(owner.mouseX - owner.tabDrawX(), owner.mouseY - owner.tabDrawY());
		}
		if (owner.menuOpen && owner.menuScreenArea == 1 && isFixed())
			owner.drawMenu();
		if (isFixed()) {
			Renderer.blit(owner.aRSImageProducer_1163, owner.graphics, owner.tabDrawX(), owner.tabDrawY());
		}
		owner.aRSImageProducer_1165.initDrawingArea();
		Texture.anIntArray1472 = owner.anIntArray1182;
	}

	public void drawRedStones() {
		Sprite[] stones = currentRedStones();
		if (tabInterfaceIDs[tabID] != -1) {
			switch (tabID) {
			case 0:
				stones[0].drawSprite(3, 0);
				break;
			case 1:
				stones[4].drawSprite(41, 0);
				break;
			case 2:
				stones[4].drawSprite(74, 0);
				break;
			case 3:
				stones[4].drawSprite(107, 0);
				break;
			case 4:
				stones[4].drawSprite(140, 0);
				break;
			case 5:
				stones[4].drawSprite(173, 0);
				break;
			case 6:
				stones[1].drawSprite(206, 0);
				break;
			case 7:
				stones[2].drawSprite(3, 298);
				break;
			case 8:
				stones[4].drawSprite(41, 298);
				break;
			case 9:
				stones[4].drawSprite(74, 298);
				break;
			case 10:
				stones[4].drawSprite(107, 298);
				break;
			case 11:
				stones[4].drawSprite(140, 298);
				break;
			case 12:
				stones[4].drawSprite(173, 298);
				break;
			case 13:
				stones[3].drawSprite(206, 298);
				break;
			}
		}
	}

	public void pmTabToReply() {
		if (owner.messagePromptRaised || owner.inputDialogState != 0) {
			return;
		}
		String name = null;
		for (int k = 0; k < 500; k++) {
			if (owner.chatMessages[k] == null) {
				continue;
			}
			int l = owner.chatTypes[k];
			if (l == 3 || l == 7) {
				name = owner.chatNames[k];
				break;
			}
		}

		if (name == null) {
			owner.pushMessage(
					"You haven't received any messages to which you can reply.",
					0, "");
			return;
		}

		if (name.startsWith("@cr")) {
			name = name.substring(5);
		}
		name = name.trim();
		if (name.length() == 0) {
			return;
		}

		inputTaken = true;
		owner.inputDialogState = 0;
		owner.messagePromptRaised = true;
		owner.promptInput = "";
		owner.friendsListAction = 3;
		owner.aLong953 = TextClass.longForName(name);
		owner.aString1121 = "Enter message to send to " + TextClass.fixName(name);
		chatTypeFocused = false;
	}

	private void drawQuickPrayerSelection() {
		ensureQuickPraySprites();
		int ox = 28;
		int oy = 37;
		if (owner.quickPraySprites[0] != null) {
			owner.quickPraySprites[0].drawSprite1(ox, oy + 22);
		} else {
			DrawingArea.method335(0x3a3a3a, oy + 22, 190, 212, 90, ox);
		}
		if (owner.quickPraySprites[3] != null) {
			owner.quickPraySprites[3].drawSprite(ox, oy + 20);
			owner.quickPraySprites[3].drawSprite(ox, oy + 232);
		}
		boolean curses = owner.cursesBook();
		int[] xs = curses ? QUICK_CURSE_X : QUICK_PRAY_X;
		int[] ys = curses ? QUICK_CURSE_Y : QUICK_PRAY_Y;
		boolean[] selected = curses ? owner.quickCurses : owner.quickPrayers;
		for (int i = 0; i < selected.length && i < xs.length; i++) {
			Sprite tick = selected[i] ? owner.quickPraySprites[2] : owner.quickPraySprites[1];
			if (tick != null) {
				tick.drawSprite(ox + xs[i] - 2, oy + ys[i] - 2);
			} else if (selected[i]) {
				DrawingArea.fillPixels(ox + xs[i], 34, 34, 0xE2C04A, oy + ys[i]);
			}
		}
		int cx = ox;
		int cy = oy + 224;
		boolean hover = owner.mouseX >= owner.tabDrawX() + cx && owner.mouseX < owner.tabDrawX() + cx + 190
				&& owner.mouseY >= owner.tabDrawY() + cy && owner.mouseY < owner.tabDrawY() + cy + 24;
		Sprite confirm = hover && owner.quickPraySprites[4] != null ? owner.quickPraySprites[4] : owner.quickPraySprites[5];
		if (confirm == null) {
			confirm = owner.quickPraySprites[4];
		}
		if (confirm != null) {
			confirm.drawSprite(cx, cy);
		}
	}

	public void drawSideIcons() {
		/* Top sideIcons */
		if (tabInterfaceIDs[0] != -1)// attack
			owner.sideIcons[0].drawSprite(10, 4);
		if (tabInterfaceIDs[1] != -1)// stat
			owner.sideIcons[1].drawSprite(43, 4);
		if (tabInterfaceIDs[2] != -1)// quest
			owner.sideIcons[2].drawSprite(76, 3);
		if (tabInterfaceIDs[3] != -1)// inventory
			owner.sideIcons[3].drawSprite(111, 5);
		if (tabInterfaceIDs[4] != -1)// equipment
			owner.sideIcons[4].drawSprite(140, 1);
		if (tabInterfaceIDs[5] != -1)// prayer
			owner.sideIcons[5].drawSprite(174, 1);
		if (tabInterfaceIDs[6] != -1)// magic
			owner.sideIcons[6].drawSprite(208, 4);
		/* Bottom sideIcons */
		if (tabInterfaceIDs[7] != -1)// clan
			owner.sideIcons[7].drawSprite(11, 303);
		if (tabInterfaceIDs[8] != -1)// friends
			owner.sideIcons[8].drawSprite(46, 306);
		if (tabInterfaceIDs[9] != -1)// ignore
			owner.sideIcons[9].drawSprite(79, 306);
		if (tabInterfaceIDs[10] != -1)// options
			owner.sideIcons[10].drawSprite(113, 300);
		if (tabInterfaceIDs[11] != -1)// options
			owner.sideIcons[11].drawSprite(145, 304);
		if (tabInterfaceIDs[12] != -1)// emotes
			owner.sideIcons[12].drawSprite(181, 302);
		if (tabInterfaceIDs[13] != -1)// music
			owner.sideIcons[13].drawSprite(213, 303);
	}

	private void drawTooltipOn(int mx, int my) {
		if (owner.menuActionRow < 2 && owner.itemSelected == 0 && owner.spellSelected == 0) {
			return;
		}
		String s;
		if (owner.itemSelected == 1 && owner.menuActionRow < 2)
			s = "Use " + owner.selectedItemName + " with...";
		else if (owner.spellSelected == 1 && owner.menuActionRow < 2)
			s = owner.spellTooltip + "...";
		else
			s = owner.menuActionName[owner.menuActionRow - 1];
		if (owner.menuActionRow > 2)
			s = s + "@whi@ / " + (owner.menuActionRow - 2) + " more options";
		if (MouseTooltips.enabled) {
			MouseTooltips.drawAtMouse(owner, s, mx, my);
		} else {
			FriendNotes.drawHover(owner, s, mx, my);
		}
	}

	private void ensureQuickPraySprites() {
		if (owner.quickPraySprites[0] != null) {
			return;
		}
		for (int i = 0; i < owner.quickPraySprites.length; i++) {
			try {
				Sprite sprite = new Sprite("Prayer/Quick/" + i);
				if (sprite.myWidth > 0 && sprite.myHeight > 0) {
					if (i == 1 || i == 2) {
						sprite.setTransparency(0, 0, 0);
					}
					owner.quickPraySprites[i] = sprite;
				}
			} catch (Exception ignored) {
			}
		}
	}

	private Sprite currentTabArea() {
		if (isFixed()) {
			return owner.tabArea;
		}
		if (resizableInvTransparent && owner.tabAreaResizableClear != null) {
			return owner.tabAreaResizableClear;
		}
		return owner.tabAreaResizable != null ? owner.tabAreaResizable : owner.tabArea;
	}

	public static void setTab(int id) {
        needDrawTabArea = true;
        tabID = id;
        tabAreaAltered = true;
    }

	boolean isSkillTabLayer(int id) {
		return id == 3917 || id == 3918 || id == 3925 || id == 3932 || id == 3939
				|| id == 3946 || id == 3953 || id == 4148;
	}

	private Sprite[] currentRedStones() {
		return !isFixed() && owner.redStonesResizable != null ? owner.redStonesResizable : owner.redStones;
	}

	int skillTabOriginX() {
		return owner.tabDrawX() + 28;
	}

	int skillTabOriginY() {
		return owner.tabDrawY() + 37;
	}
}
