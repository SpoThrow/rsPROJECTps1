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
 * Login screen: rendering, the volume/music panel and saved-character
 * selection, lifted out of client.java in Phase 3.2.3.
 *
 * This deliberately does NOT contain the login() handshake, which is a
 * frozen wire-protocol contract and stays in client. State stays on client
 * too; it is reached through the receiver this class is constructed with.
 */
public final class LoginScreen {
	private final client owner;

	public LoginScreen(client owner) {
		this.owner = owner;
	}

	public void processLoginScreenInput() {
		if (owner.myUsername == null) {
			owner.myUsername = "";
		}
		if (owner.myPassword == null) {
			owner.myPassword = "";
		}
		int ox = loginOffsetX();
		int oy = loginOffsetY();
		if (processLoginVolumeClicks(ox, oy)) {
			return;
		}
		if (processSavedCharacterClicks(ox, oy)) {
			return;
		}
		if (owner.normalLogin == true) {
		if (owner.loginScreenState == 0) {
			int i = ox + 765 / 2 - 80;
			int l = oy + 503 / 2 + 20;
			l += 20;
			if (owner.clickMode3 == 1 && owner.saveClickX >= i - 75
					&& owner.saveClickX <= i + 75 && owner.saveClickY >= l - 20
					&& owner.saveClickY <= l + 20) {
				owner.loginScreenState = 3;
				owner.loginScreenCursorPos = 0;
			}
			i = ox + 765 / 2 + 80;
			if (owner.clickMode3 == 1 && owner.saveClickX >= i - 75
					&& owner.saveClickX <= i + 75 && owner.saveClickY >= l - 20
					&& owner.saveClickY <= l + 20) {
				owner.loginMessage1 = "";
				owner.loginMessage2 = "Enter your username & password.";
				owner.loginScreenState = 2;
				owner.loginScreenCursorPos = 0;
			}
		} else {
			if (owner.loginScreenState == 2) {
				int j = oy + 503 / 2 - 40;
				j += 30;
				j += 25;
				if (owner.clickMode3 == 1 && owner.saveClickY >= j - 15
						&& owner.saveClickY < j)
					owner.loginScreenCursorPos = 0;
				j += 15;
				if (owner.clickMode3 == 1 && owner.saveClickY >= j - 15
						&& owner.saveClickY < j)
					owner.loginScreenCursorPos = 1;
				j += 15;
				if (owner.clickMode3 == 1 && owner.saveClickX >= ox + 765 / 2 - 90
						&& owner.saveClickX <= ox + 765 / 2 + 40
						&& owner.saveClickY >= j - 14 && owner.saveClickY <= j + 4) {
					owner.rememberMe = owner.rememberMe == 1 ? 0 : 1;
					owner.saveClientSettings();
				}
				j += 15;
				int i1 = ox + 765 / 2 - 80;
				int k1 = oy + 503 / 2 + 50;
				k1 += 20;
				if (owner.clickMode3 == 1 && owner.saveClickX >= i1 - 75
						&& owner.saveClickX <= i1 + 75
						&& owner.saveClickY >= k1 - 20
						&& owner.saveClickY <= k1 + 20) {
					owner.loginFailures = 0;
					owner.login(owner.myUsername, owner.myPassword, false);
					if (owner.loggedIn)
						return;
				}
				i1 = ox + 765 / 2 + 80;
				if (owner.clickMode3 == 1 && owner.saveClickX >= i1 - 75
						&& owner.saveClickX <= i1 + 75
						&& owner.saveClickY >= k1 - 20
						&& owner.saveClickY <= k1 + 20) {
					owner.loginScreenState = 0;
					// myUsername = "";
					// myPassword = "";
				}
				do {
					int l1 = owner.readChar(-796);
					if (l1 == -1)
						break;
					boolean flag1 = false;
					for (int i2 = 0; i2 < validUserPassChars.length(); i2++) {
						if (l1 != validUserPassChars.charAt(i2))
							continue;
						flag1 = true;
						break;
					}

					if (owner.loginScreenCursorPos == 0) {
						if (l1 == 8 && owner.myUsername.length() > 0)
							owner.myUsername = owner.myUsername.substring(0,
									owner.myUsername.length() - 1);
						if (l1 == 9 || l1 == 10 || l1 == 13)
							owner.loginScreenCursorPos = 1;
						if (flag1)
							owner.myUsername += (char) l1;
						if (owner.myUsername.length() > 12)
							owner.myUsername = owner.myUsername.substring(0, 12);
					} else if (owner.loginScreenCursorPos == 1) {
						if (l1 == 8 && owner.myPassword.length() > 0)
							owner.myPassword = owner.myPassword.substring(0,
									owner.myPassword.length() - 1);
						if (l1 == 9 || l1 == 10 || l1 == 13)
							owner.loginScreenCursorPos = 0;
						if (flag1)
							owner.myPassword += (char) l1;
						if (owner.myPassword.length() > 20)
							owner.myPassword = owner.myPassword.substring(0, 20);
					}
				} while (true);
				return;
			}
			if (owner.loginScreenState == 3) {
				int k = ox + 765 / 2;
				int j1 = oy + 503 / 2 + 50;
				j1 += 20;
				if (owner.clickMode3 == 1 && owner.saveClickX >= k - 75
						&& owner.saveClickX <= k + 75
						&& owner.saveClickY >= j1 - 20
						&& owner.saveClickY <= j1 + 20)
					owner.loginScreenState = 0;
			}
		}
		} else if(owner.normalLogin == false) {
			//Username Clicking area
			if(owner.clickMode3 == 1 && owner.saveClickX >= ox + 145 && owner.saveClickX <= ox + 321 && owner.saveClickY >= oy + 261 && owner.saveClickY <= oy + 288)
				owner.loginScreenCursorPos = 0;
			//Password Clicking area
			if(owner.clickMode3 == 1 && owner.saveClickX >= ox + 331 && owner.saveClickX <= ox + 505 && owner.saveClickY >= oy + 261 && owner.saveClickY <= oy + 288)
				owner.loginScreenCursorPos = 1;
			//Username hover
			if(owner.mouseX >= ox + 145 && owner.mouseX <= ox + 321 && owner.mouseY >= oy + 261 && owner.mouseY <= oy + 288) {
				owner.textbox = 1;
			} else {
				owner.textbox = 0;
			}
			if(owner.mouseX >= ox + 331 && owner.mouseX <= ox + 505 && owner.mouseY >= oy + 261 && owner.mouseY <= oy + 288) {
				owner.textbox1 = 1;
			} else {
				owner.textbox1 = 0;
			}
			//LoginBox clicking area
			if(owner.clickMode3 == 1 && owner.saveClickX >= ox + 515 && owner.saveClickX <= ox + 608 && owner.saveClickY >= oy + 261 && owner.saveClickY <= oy + 292) {
				owner.loginFailures = 0;
				owner.login((owner.myUsername), owner.myPassword, false);
				if(owner.loggedIn)
					return;
			}
			//LoginBox hover
        			if (owner.mouseX >= ox + 515 && owner.mouseX <= ox + 608
                			&& owner.mouseY >= oy + 261 && owner.mouseY <= oy + 292) {
            					owner.loginButtonint = 1;
        				} else {
            					owner.loginButtonint = 0;
			}
			if (owner.clickMode3 == 1 && owner.saveClickX >= ox + 146 && owner.saveClickX <= ox + 280
					&& owner.saveClickY >= oy + 298 && owner.saveClickY <= oy + 320) {
				owner.rememberMe = owner.rememberMe == 1 ? 0 : 1;
				owner.saveClientSettings();
			}
			//Username and password crap
			do {
				int l1 = owner.readChar(-796);
				if(l1 == -1)
					break;
				boolean flag1 = false;
				for(int i2 = 0; i2 < validUserPassChars.length(); i2++) {
					if(l1 != validUserPassChars.charAt(i2))
						continue;
					flag1 = true;
					break;
				} if(owner.loginScreenCursorPos == 0) {
					if(l1 == 8 && owner.myUsername.length() > 0)
						owner.myUsername = owner.myUsername.substring(0, owner.myUsername.length() - 1);
					if(l1 == 9 || l1 == 10 || l1 == 13)
						owner.loginScreenCursorPos = 1;
					if(flag1)
						owner.myUsername += (char)l1;
					if(owner.myUsername.length() > 12)
						owner.myUsername = (owner.myUsername.substring(0, 12));
				} else if(owner.loginScreenCursorPos == 1) {
					if(l1 == 8 && owner.myPassword.length() > 0)
						owner.myPassword = owner.myPassword.substring(0, owner.myPassword.length() - 1);
					if (l1 == 9 || l1 == 10 || l1 == 13)
                    						if (owner.myUsername == "")
                        							owner.loginScreenCursorPos = 0;
                    						else if (owner.myPassword == "") {
                    					} else
                        						owner.login(owner.myUsername, owner.myPassword, false);
					if(flag1)
						owner.myPassword += (char)l1;
					if(owner.myPassword.length() > 20)
						owner.myPassword = owner.myPassword.substring(0, 20);
				}
			} while(true);
			return;
}
	}

	public void drawLoginScreen(boolean flag) {
		if (owner.normalLogin == true) {
		owner.resetImageProducers();
		owner.aRSImageProducer_1109.initDrawingArea();
		if (owner.aBackground_966 != null) {
			owner.aBackground_966.drawBackground(0, 0);
		}
		char c = '\u0168';
		char c1 = '\310';
		if(owner.musicEnabled && !lowMem) {
			if (owner.date.getMonth() == 10 && owner.date.getDate() >= 28)
				owner.playSong(owner.HWEEN_THEME);
			if (owner.date.getMonth() == 11 || (owner.date.getMonth() == 0 && owner.date.getDate() <= 27))
				owner.playSong(owner.XMAS_THEME);
			else
				owner.playSong(owner.OLD_THEME);
		}
		if (owner.loginScreenState == 0) {
			int i = c1 / 2 + 80;
			owner.smallText.method382(0x75a9a9, c / 2, onDemandFetcher.statusString,
					i, true);
			i = c1 / 2 - 20;
			owner.chatTextDrawingArea.method382(0xffff00, c / 2,
					"Welcome to Soul-Trail", i, true);
			i += 30;
			int l = c / 2 - 80;
			int k1 = c1 / 2 + 20;
			if (owner.aBackground_967 != null) {
				owner.aBackground_967.drawBackground(l - 73, k1 - 20);
			}
			owner.chatTextDrawingArea
					.method382(0xffffff, l, "New User", k1 + 5, true);
			l = c / 2 + 80;
			if (owner.aBackground_967 != null) {
				owner.aBackground_967.drawBackground(l - 73, k1 - 20);
			}
			owner.chatTextDrawingArea.method382(0xffffff, l, "Existing User", k1 + 5,
					true);
		}
		if (owner.loginScreenState == 2) {
			int j = c1 / 2 - 40;
			if (owner.loginMessage1.length() > 0) {
				owner.chatTextDrawingArea.method382(0xffff00, c / 2, owner.loginMessage1,
						j - 15, true);
				owner.chatTextDrawingArea.method382(0xffff00, c / 2, owner.loginMessage2,
						j, true);
				j += 30;
			} else {
				owner.chatTextDrawingArea.method382(0xffff00, c / 2, owner.loginMessage2,
						j - 7, true);
				j += 30;
			}
			owner.chatTextDrawingArea
					.method389(true, c / 2 - 90, 0xffffff, "Username: "
							+ capitalize(owner.myUsername)
							+ ((owner.loginScreenCursorPos == 0)
									& (loopCycle % 40 < 20) ? "@yel@|" : ""), j);
			// chatTextDrawingArea.method389(true, c / 2 - 90, 0xffffff,
			// "Username: " + myUsername + ((loginScreenCursorPos == 0) &
			// (loopCycle % 40 < 20) ? "@yel@|" : ""), j);
			j += 15;
			owner.chatTextDrawingArea
					.method389(true, c / 2 - 88, 0xffffff, "Password: "
							+ TextClass.passwordAsterisks(owner.myPassword)
							+ ((owner.loginScreenCursorPos == 1)
									& (loopCycle % 40 < 20) ? "@yel@|" : ""), j);
			j += 15;
			int boxX = c / 2 - 90;
			int boxY = j - 10;
			DrawingArea.fillPixels(boxX, 12, 12, 0xffffff, boxY);
			DrawingArea.drawPixels(10, boxY + 1, boxX + 1, 0, 10);
			if (owner.rememberMe == 1) {
				DrawingArea.drawPixels(8, boxY + 2, boxX + 2, 0xffff00, 8);
			}
			owner.chatTextDrawingArea.method389(true, boxX + 16, 0xffffff, "Remember me", j);
			j += 15;
			if (!flag) {
				int i1 = c / 2 - 80;
				int l1 = c1 / 2 + 50;
				if (owner.aBackground_967 != null) {
					owner.aBackground_967.drawBackground(i1 - 73, l1 - 20);
				}
				owner.chatTextDrawingArea.method382(0xffffff, i1, "Login", l1 + 5,
						true);
				i1 = c / 2 + 80;
				if (owner.aBackground_967 != null) {
					owner.aBackground_967.drawBackground(i1 - 73, l1 - 20);
				}
				owner.chatTextDrawingArea.method382(0xffffff, i1, "Cancel", l1 + 5,
						true);
			}
		}
		if (owner.loginScreenState == 3) {
			owner.chatTextDrawingArea.method382(0xffff00, c / 2,
					"Create a free account", c1 / 2 - 60, true);
			int k = c1 / 2 - 35;
			owner.chatTextDrawingArea.method382(0xffffff, c / 2,
					"To create a new account you need to", k, true);
			k += 15;
			owner.chatTextDrawingArea.method382(0xffffff, c / 2,
					"go back to the main RuneScape webpage", k, true);
			k += 15;
			owner.chatTextDrawingArea.method382(0xffffff, c / 2,
					"and choose the red 'create account'", k, true);
			k += 15;
			owner.chatTextDrawingArea.method382(0xffffff, c / 2,
					"button at the top right of that page.", k, true);
			k += 15;
			int j1 = c / 2;
			int i2 = c1 / 2 + 50;
			if (owner.aBackground_967 != null) {
				owner.aBackground_967.drawBackground(j1 - 73, i2 - 20);
			}
			owner.chatTextDrawingArea.method382(0xffffff, j1, "Cancel", i2 + 5, true);
		}
			fillLoginBackdrop();
			blitLoginScene();
			owner.blitTitle(owner.aRSImageProducer_1109, 171, 202);
			drawSavedCharacters();
			drawLoginVolume();
			owner.welcomeScreenRaised = false;
		} else if(owner.normalLogin == false) {
		//worldLoginScreen();
			owner.resetImageProducers();
			owner.aRSImageProducer_1109.initDrawingArea();
			Sprite loginTest = new Sprite("Login/login");
			loginTest.drawSprite(0, 0);
			if(owner.loginMessage1.length() > 0) {
				owner.chatTextDrawingArea.method382(0xe0bb00, 255, owner.loginMessage2, 60, true);
			} else {
				owner.chatTextDrawingArea.method382(0xe0bb00, 255, owner.loginMessage2, 60, true);
			}
			if (owner.loginButtonint == 0) {
				Sprite LOGINBUTTON0 = new Sprite("Login/LOGINBUTTON0");
				LOGINBUTTON0.drawSprite(382, 89);
				} else if (owner.loginButtonint == 1) {
				Sprite LOGINBUTTON1 = new Sprite("Login/LOGINBUTTON1");
				LOGINBUTTON1.drawSprite(382, 89);
				}
			if (owner.rememberMe == 0) {
				Sprite unclickedR = new Sprite("Login/unclicked");
				unclickedR.drawSprite(13, 130);
			} else if (owner.rememberMe == 1) {
				Sprite clickedR = new Sprite("Login/clicked");
				clickedR.drawSprite(13, 130);
			}
			owner.aTextDrawingArea_1271.method389(false, 32, 0x000000, "Remember me", 141);
			if (owner.textbox == 0) {
				Sprite textbox = new Sprite("Login/textbox");
				textbox.drawSprite(13, 91);
			} else if (owner.textbox == 1) {
				Sprite textbox1 = new Sprite("Login/textbox1");
				textbox1.drawSprite(13, 91);
			}
			if (owner.textbox1 == 0) {
				Sprite textbox = new Sprite("Login/textbox");
				textbox.drawSprite(197, 91);
			} else if (owner.textbox1 == 1) {
				Sprite textbox1 = new Sprite("Login/textbox1");
				textbox1.drawSprite(197, 91);
			}	
			/** Font types **/
			//chatTextDrawingArea.method389(true, 18, 0x00f0ff, "" + capitalize(myUsername) + ((loginScreenCursorPos == 0) & (loopCycle % 40 < 20) ? "|" : ""), 110);
 			owner.aTextDrawingArea_1271.method389(false,18,0x000000,"" + (owner.myUsername) + ((owner.loginScreenCursorPos == 0) & (loopCycle % 40 < 20) ? "|" : ""), 110);
			//chatTextDrawingArea.method389(true, 203, 0x00f0ff, "" + TextClass.passwordAsterisks(myPassword) + ((loginScreenCursorPos == 1) & (loopCycle % 40 < 20) ? "|" : ""), 110);
 			owner.aTextDrawingArea_1271.method389(false,203,0x000000,"" + TextClass.passwordAsterisks(owner.myPassword) + ((owner.loginScreenCursorPos == 1) & (loopCycle % 40 < 20) ? "|" : ""), 110);
			fillLoginBackdrop();
			blitLoginScene();
			owner.blitTitle(owner.aRSImageProducer_1109, 171, 133);
			drawSavedCharacters();
			drawLoginVolume();
			owner.welcomeScreenRaised = false;
		}
	}

	private void drawLoginVolume() {
		if (owner.aRSImageProducer_1113 == null) {
			return;
		}
		ensureLoginVolumeSprites();
		int muted = loginMusicLevel() >= 4 ? 1 : 0;
		int bars = muted == 1 ? 0 : 4 - loginMusicLevel();
		int mx = owner.mouseX - loginOffsetX() - 562;
		int my = owner.mouseY - loginOffsetY() - 265;
		boolean hover = mx >= LOGIN_VOL_X && mx < LOGIN_VOL_X + LOGIN_VOL_W && my >= LOGIN_VOL_Y && my < LOGIN_VOL_Y + LOGIN_VOL_H;
		owner.aRSImageProducer_1113.initDrawingArea();
		int x = LOGIN_VOL_X;
		int y = LOGIN_VOL_Y;
		DrawingArea.drawPixels(LOGIN_VOL_H, y, x, 0x120e0a, LOGIN_VOL_W);
		DrawingArea.fillPixels(x, LOGIN_VOL_W, LOGIN_VOL_H, hover ? 0xff981f : 0x5A4933, y);
		int iconX = x + 1;
		int iconY = y + 1;
		Sprite icon = muted == 1 ? owner.loginMuteSprite : owner.loginMusicSprite;
		if (icon != null) {
			icon.drawSprite(iconX, iconY);
		} else {
			drawLoginVolumeFallback(iconX + 6, iconY + 6, muted == 1);
		}
		int barX = x + 40;
		int barY = y + 14;
		int barW = 44;
		int barH = 10;
		DrawingArea.drawPixels(barH, barY, barX, 0x1a1610, barW);
		DrawingArea.fillPixels(barX, barW, barH, 0x6a5a40, barY);
		if (bars > 0) {
			int fill = bars * barW / 4;
			DrawingArea.drawPixels(barH - 2, barY + 1, barX + 1, 0xff981f, fill - 2 > 0 ? fill - 2 : fill);
		}
		if (owner.smallText != null) {
			owner.smallText.method382(0xc6b895, barX + barW / 2, muted == 1 ? "Muted" : "Music", y + 11, true);
		}
		owner.blitTitle(owner.aRSImageProducer_1113, 265, 562);
	}

	private boolean processLoginVolumeClicks(int ox, int oy) {
		if (owner.clickMode3 != 1) {
			return false;
		}
		int mx = owner.saveClickX - ox - 562;
		int my = owner.saveClickY - oy - 265;
		if (mx < LOGIN_VOL_X || mx >= LOGIN_VOL_X + LOGIN_VOL_W || my < LOGIN_VOL_Y || my >= LOGIN_VOL_Y + LOGIN_VOL_H) {
			return false;
		}
		int barX = LOGIN_VOL_X + 40;
		if (mx < barX) {
			if (loginMusicLevel() >= 4) {
				applyLoginMusicLevel(owner.loginMusicRestore);
			} else {
				applyLoginMusicLevel(4);
			}
		} else {
			int barW = 44;
			int rel = mx - barX;
			if (rel < 0) {
				rel = 0;
			}
			if (rel >= barW) {
				rel = barW - 1;
			}
			int bars = 1 + (rel * 4) / barW;
			if (bars > 4) {
				bars = 4;
			}
			applyLoginMusicLevel(4 - bars);
		}
		return true;
	}

	private void drawLoginVolumeFallback(int x, int y, boolean muted) {
		DrawingArea.drawPixels(16, y + 6, x + 2, 0xffd37a, 6);
		DrawingArea.drawPixels(10, y + 2, x + 8, 0xffd37a, 4);
		DrawingArea.drawPixels(6, y + 14, x + 12, 0xffd37a, 6);
		if (muted) {
			DrawingArea.method339(y + 4, 0xff3030, 18, x);
			DrawingArea.method339(y + 20, 0xff3030, 18, x);
		}
	}

	private void applyLoginMusicLevel(int level) {
		if (level < 0) {
			level = 0;
		}
		if (level > 4) {
			level = 4;
		}
		int previous = loginMusicLevel();
		boolean wasOn = owner.musicEnabled;
		optionMusic = level;
		if (level >= 4) {
			if (previous < 4) {
				owner.loginMusicRestore = previous;
			}
			owner.musicEnabled = false;
			owner.stopMidi();
		} else {
			owner.loginMusicRestore = level;
			int[] volumes = { 256, 192, 128, 64 };
			owner.musicEnabled = true;
			owner.setMidiVolume(volumes[level]);
			if (!wasOn || owner.midiPlayer == null || !owner.midiPlayer.playing()) {
				owner.currentSong = -1;
				owner.prevSong = 0;
				playTitleMusic();
			}
		}
		if (owner.variousSettings != null) {
			owner.variousSettings[168] = optionMusic;
		}
		owner.saveClientSettings();
	}

	private void ensureLoginVolumeSprites() {
		if (owner.loginMusicSprite == null) {
			try {
				Sprite sprite = new Sprite("Login/music");
				if (sprite.myWidth > 0 && sprite.myHeight > 0) {
					owner.loginMusicSprite = sprite;
				}
			} catch (Exception ignored) {
			}
		}
		if (owner.loginMuteSprite == null) {
			try {
				Sprite sprite = new Sprite("Login/mute");
				if (sprite.myWidth > 0 && sprite.myHeight > 0) {
					owner.loginMuteSprite = sprite;
				}
			} catch (Exception ignored) {
			}
		}
	}

	private int loginMusicLevel() {
		if (!owner.musicEnabled) {
			return 4;
		}
		return optionMusic;
	}

	void blitLoginScene() {
		owner.blitTitle(owner.aRSImageProducer_1110, 0, 0);
		owner.blitTitle(owner.aRSImageProducer_1111, 0, 637);
		owner.blitTitle(owner.aRSImageProducer_1107, 0, 128);
		owner.blitTitle(owner.aRSImageProducer_1108, 371, 202);
		owner.blitTitle(owner.aRSImageProducer_1112, 265, 0);
		owner.blitTitle(owner.aRSImageProducer_1113, 265, 562);
		owner.blitTitle(owner.aRSImageProducer_1114, 171, 128);
		owner.blitTitle(owner.aRSImageProducer_1115, 171, 562);
	}

	void fillLoginBackdrop() {
		if (owner.graphics == null || isFixed()) {
			return;
		}
		int ox = loginOffsetX();
		int oy = loginOffsetY();
		owner.graphics.setColor(Color.BLACK);
		if (owner.welcomeScreenRaised) {
			owner.graphics.fillRect(0, 0, frameWidth, frameHeight);
			return;
		}
		if (oy > 0) {
			owner.graphics.fillRect(0, 0, frameWidth, oy);
		}
		if (ox > 0) {
			owner.graphics.fillRect(0, oy, ox, 503);
		}
		int right = ox + 765;
		if (right < frameWidth) {
			owner.graphics.fillRect(right, oy, frameWidth - right, 503);
		}
		int bottom = oy + 503;
		if (bottom < frameHeight) {
			owner.graphics.fillRect(0, bottom, frameWidth, frameHeight - bottom);
		}
	}

	public int loginOffsetX() {
		return isFixed() ? 0 : Math.max(0, (frameWidth - 765) / 2);
	}

	public int loginOffsetY() {
		return isFixed() ? 0 : Math.max(0, (frameHeight - 503) / 2);
	}

	private void playTitleMusic() {
		if (!owner.musicEnabled || lowMem || onDemandFetcher == null) {
			return;
		}
		if (owner.date.getMonth() == 10 && owner.date.getDate() >= 28) {
			owner.playSong(owner.HWEEN_THEME);
		} else if (owner.date.getMonth() == 11 || owner.date.getMonth() == 0 && owner.date.getDate() <= 27) {
			owner.playSong(owner.XMAS_THEME);
		} else {
			owner.playSong(owner.OLD_THEME);
		}
	}

	private void drawSavedCharacters() {
		if (owner.aRSImageProducer_1108 == null || owner.smallText == null) {
			return;
		}
		int n = SavedCharacters.count();
		if (n <= 0) {
			return;
		}
		int[] oldScan = Texture.anIntArray1472;
		int oldTx = Texture.textureInt1;
		int oldTy = Texture.textureInt2;
		owner.aRSImageProducer_1108.initDrawingArea();
		Texture.method364();
		int mx = owner.mouseX - loginOffsetX() - SavedCharacters.PANEL_X;
		int my = owner.mouseY - loginOffsetY() - SavedCharacters.PANEL_Y;
		int cardW = SavedCharacters.cardWidth(n);
		int startX = (SavedCharacters.PANEL_W - n * cardW) / 2;
		owner.smallText.method382(0, SavedCharacters.PANEL_W / 2 + 1, "Saved characters", 13, true);
		owner.smallText.method382(0xff981f, SavedCharacters.PANEL_W / 2, "Saved characters", 12, true);
		for (int i = 0; i < n; i++) {
			SavedCharacters slot = SavedCharacters.get(i);
			if (slot == null) {
				continue;
			}
			int x = startX + i * cardW + 3;
			int y = 18;
			int w = cardW - 6;
			int h = 110;
			boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
			DrawingArea.drawPixels(h, y, x, 0x0d0d0d, w);
			DrawingArea.drawPixels(h - 2, y + 1, x + 1, hover ? 0xff981f : 0x6a6a6a, w - 2);
			DrawingArea.drawPixels(h - 4, y + 2, x + 2, 0x2a2218, w - 2);
			int well = 56;
			int wx = x + (w - well) / 2;
			int wy = y + 6;
			DrawingArea.drawPixels(well + 2, wy - 1, wx - 1, 0x111111, well + 2);
			slot.drawPortrait(wx, wy, well, well);
			DrawingArea.fillPixels(wx - 1, well + 2, well + 2, hover ? 0xff981f : 0x4a4a4a, wy - 1);
			String name = slot.name;
			if (name.length() > 11) {
				name = name.substring(0, 11);
			}
			owner.smallText.method382(0, x + w / 2 + 1, name, wy + well + 14, true);
			owner.smallText.method382(0xffffff, x + w / 2, name, wy + well + 13, true);
			owner.smallText.method382(0xff981f, x + w / 2, "Lvl " + slot.combat, wy + well + 25, true);
			owner.smallText.method382(0xc6b895, x + w / 2, slot.totalLevel + " tot  " + compactXp(slot.totalXp) + " xp", wy + well + 37, true);
			int rx = x + w - 8;
			int ry = y + 12;
			boolean overX = mx >= rx - 6 && mx <= rx + 6 && my >= ry - 8 && my <= ry + 4;
			owner.smallText.method382(overX ? 0xff3030 : 0xa07850, rx, "x", ry, true);
		}
		Texture.anIntArray1472 = oldScan;
		Texture.textureInt1 = oldTx;
		Texture.textureInt2 = oldTy;
		owner.blitTitle(owner.aRSImageProducer_1108, SavedCharacters.PANEL_Y, SavedCharacters.PANEL_X);
	}

	private boolean processSavedCharacterClicks(int ox, int oy) {
		if (owner.clickMode3 != 1 || SavedCharacters.count() <= 0) {
			return false;
		}
		int mx = owner.saveClickX - ox - SavedCharacters.PANEL_X;
		int my = owner.saveClickY - oy - SavedCharacters.PANEL_Y;
		int n = SavedCharacters.count();
		int slot = SavedCharacters.hoveredSlot(mx, my);
		if (slot < 0) {
			return false;
		}
		if (SavedCharacters.hitRemove(mx, my, slot, n)) {
			SavedCharacters.remove(slot);
			owner.saveClientSettings();
			return true;
		}
		SavedCharacters chosen = SavedCharacters.get(slot);
		if (chosen == null) {
			return false;
		}
		owner.myUsername = chosen.name;
		if (chosen.password != null) {
			owner.myPassword = chosen.password;
		}
		owner.rememberMe = 1;
		owner.loginScreenState = 2;
		owner.loginFailures = 0;
		owner.login(chosen.name, chosen.password == null ? "" : chosen.password, false);
		return true;
	}

	private String compactXp(int xp) {
		if (xp >= 10000000) {
			return (xp / 1000000) + "m";
		}
		if (xp >= 100000) {
			return (xp / 1000) + "k";
		}
		return owner.formatNumber(xp);
	}
}
