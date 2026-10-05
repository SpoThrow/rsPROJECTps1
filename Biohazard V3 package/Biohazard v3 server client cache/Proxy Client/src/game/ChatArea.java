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
 * Chat: the chat area, split private chat, the channel buttons, the
 * right-click chat menus and the chat's own text helpers, lifted out of
 * client.java in 3.2.5.
 *
 * Deliberately NARROWED: the generic interface renderer (drawInterface) and
 * the shared geometry / messaging helpers (gameDrawX/Y, chatDrawY, isFixed,
 * pushMessage) stay in client, because they are not chat-specific. State
 * stays on client and is reached through the receiver this class is built
 * with.
 */
public final class ChatArea {
	private final client owner;

	public ChatArea(client owner) {
		this.owner = owner;
	}

	void drawChatArea() {
		owner.aRSImageProducer_1166.initDrawingArea();
		Texture.anIntArray1472 = owner.anIntArray1180;
		boolean hideChat = owner.chatBoxHidden && !isFixed() && !owner.messagePromptRaised
				&& owner.inputDialogState == 0 && owner.aString844 == null && owner.backDialogID == -1 && owner.dialogID == -1
				&& !ChatboxItemSearch.open;
		boolean transparentChat = !isFixed() && resizableChatTransparent && !hideChat;
		if (transparentChat) {
			copyWorldUnderHud(owner.aRSImageProducer_1166, 0, owner.chatDrawY());
			// Frosted glass ~5–8% opacity over the message area (filter row stays solid below).
			DrawingArea.method335(0xD8DCE4, 0, 519, 142, 14, 0);
			DrawingArea.method335(0xF0F2F6, 1, 517, 1, 28, 1);
			DrawingArea.method335(0xF0F2F6, 140, 517, 1, 28, 1);
			DrawingArea.method335(0xB0B8C4, 1, 1, 140, 32, 1);
			DrawingArea.method335(0xB0B8C4, 1, 1, 140, 32, 517);
			DrawingArea.method335(0x9098A4, 0, 519, 1, 36, 0);
			DrawingArea.method335(0x9098A4, 141, 519, 1, 36, 0);
			// Opaque filter-row strip from the solid chat sprite.
			Sprite filterBg = owner.chatAreaResizable != null ? owner.chatAreaResizable : owner.chatArea;
			if (filterBg != null) {
				DrawingArea.setDrawingArea(filterBg.myHeight, 0, filterBg.myWidth, 142);
				filterBg.drawSprite(0, 0);
				DrawingArea.defaultDrawingAreaSize();
			}
		}
		if (hideChat) {
			DrawingArea.setAllPixelsToZero();
			Sprite chatBg = owner.chatAreaResizable != null ? owner.chatAreaResizable : owner.chatArea;
			if (chatBg != null) {
				int top = chatBg.myHeight - 28;
				if (top < 0) {
					top = 0;
				}
				DrawingArea.setDrawingArea(chatBg.myHeight, 0, chatBg.myWidth, top);
				chatBg.drawSprite(0, 0);
				DrawingArea.defaultDrawingAreaSize();
			}
		} else if (!transparentChat) {
			Sprite chatBg = currentChatArea();
			if (chatBg != null) {
				chatBg.drawSprite(0, 0);
			}
		}
		drawChannelButtons();
		if (hideChat) {
			if (owner.menuOpen && owner.menuScreenArea == 2 && isFixed()) {
				owner.drawMenu();
			}
			if (isFixed()) {
				owner.aRSImageProducer_1166.drawGraphics(owner.chatDrawY(), owner.graphics, 0);
			}
			owner.aRSImageProducer_1165.initDrawingArea();
			Texture.anIntArray1472 = owner.anIntArray1182;
			return;
		}
		TextDrawingArea textDrawingArea = owner.aTextDrawingArea_1271;
		if (ChatboxItemSearch.open) {
			ChatboxItemSearch.draw(owner);
		} else if (owner.messagePromptRaised) {
			owner.newBoldFont.drawCenteredString(owner.aString1121, 259, 60, 0, -1);
			owner.newBoldFont.drawCenteredString(owner.promptInput + "*", 259, 80, 128, -1);
		} else if (owner.inputDialogState == 1) {
			owner.newBoldFont.drawCenteredString("Enter amount:", 259, 60, 0, -1);
			owner.newBoldFont.drawCenteredString(owner.amountOrNameInput + "*", 259, 80,
					128, -1);
		} else if (owner.inputDialogState == 2) {
			owner.newBoldFont.drawCenteredString(openInterfaceID == 5292 ? "Enter item name:" : "Enter name:", 259, 60, 0, -1);
			owner.newBoldFont.drawCenteredString(owner.amountOrNameInput + "*", 259, 80,
					128, -1);
		} else if (owner.aString844 != null) {
			owner.newBoldFont.drawCenteredString(owner.aString844, 259, 60, 0, -1);
			owner.newBoldFont.drawCenteredString("Click to continue", 259, 80, 128,
					-1);
		} else if (owner.backDialogID != -1) {
			owner.drawInterface(0, 20, RSInterface.interfaceCache[owner.backDialogID], 20);
		} else if (owner.dialogID != -1) {
			owner.drawInterface(0, 20, RSInterface.interfaceCache[owner.dialogID], 20);
		} else {
			int j77 = -3;
			int j = 0;
			int chatMsgX = chatScrollbarLeft ? 28 : 11;
			int chatTextLeft = chatScrollbarLeft ? 26 : 8;
			int chatTextRight = chatScrollbarLeft ? 506 : 497;
			DrawingArea.setDrawingArea(122, chatTextLeft, chatTextRight, 7);
			for (int k = 0; k < 500; k++)
				if (owner.chatMessages[k] != null) {
					int chatType = owner.chatTypes[k];
					int yPos = (70 - j77 * 14) + anInt1089 + 5;
					String s1 = owner.chatNames[k];
					byte byte0 = 0;
					if (s1 != null && s1.startsWith("@cr1@")) {
						s1 = s1.substring(5);
						byte0 = 1;
					} else if (s1 != null && s1.startsWith("@cr2@")) {
						s1 = s1.substring(5);
						byte0 = 2;
					} else if (s1 != null && s1.startsWith("@cr3@")) {
						s1 = s1.substring(5);
						byte0 = 3;
					} else if (s1 != null && s1.startsWith("@cr4@")) {
						s1 = s1.substring(5);
						byte0 = 4;
					} else if (s1 != null && s1.startsWith("@cr5@")) {
						s1 = s1.substring(5);
						byte0 = 5;
					} else if (s1 != null && s1.startsWith("@cr6@")) {
						s1 = s1.substring(5);
						byte0 = 6;
					} else if (s1 != null && s1.startsWith("@cr7@")) {
						s1 = s1.substring(5);
						byte0 = 7;
					} else if (s1 != null && s1.startsWith("@cr8@")) {
						s1 = s1.substring(5);
						byte0 = 8;
					} else if (s1 != null && s1.startsWith("@cr9@")) {
						s1 = s1.substring(5);
						byte0 = 9;
					}
					if (chatType == 0) {
						if (owner.chatTypeView == 5 || owner.chatTypeView == 0) {
							if (yPos > 0 && yPos < 210) {
								int x0 = chatMsgX;
								String stamp0 = chatTimePrefix(k);
								if (stamp0.length() > 0) {
									owner.newRegularFont.drawBasicString(stamp0, x0, yPos,
											chatTimestampColor(), chatShadow());
									x0 += owner.newRegularFont.getTextWidth(stamp0);
								}
								owner.newRegularFont.drawBasicString(chatBody(k),
										x0, yPos, chatInk(ChatChannels.gameColor(owner, chatBody(k))), chatShadow());
							}
							j++;
							j77++;
						}
					}
					if ((chatType == 1 || chatType == 2)
							&& (chatType == 1 || owner.publicChatMode == 0 || owner.publicChatMode == 1
									&& owner.isFriendOrSelf(s1))) {
						if (owner.chatTypeView == 1 || owner.chatTypeView == 0) {
							if (yPos > 0 && yPos < 210) {
								int xPos = chatMsgX;
								String stamp = chatTimePrefix(k);
								if (stamp.length() > 0) {
									owner.newRegularFont.drawBasicString(stamp, xPos, yPos, chatTimestampColor(), chatShadow());
									xPos += owner.newRegularFont.getTextWidth(stamp);
								}
								if (byte0 == 1) {
									owner.modIcons[0].drawSprite(xPos + 1, yPos - 11);
									xPos += 14;
								} else if (byte0 == 2) {
									owner.modIcons[2].drawSprite(xPos, yPos - 13);
									xPos += 14;
								} else if (byte0 == 3) {
									owner.modIcons[1].drawSprite(xPos + 1, yPos - 11);
									xPos += 14;
								} else if (byte0 == 4) {
									owner.modIcons[3].drawSprite(xPos + 1, yPos - 10);
									xPos += 14;
								} else if (byte0 == 5) {
									owner.modIcons[4].drawSprite(xPos + 1, yPos - 10);
									xPos += 14;
								} else if (byte0 == 6) {
									owner.modIcons[5].drawSprite(xPos + 1, yPos - 10);
									xPos += 14;
								} else if (byte0 == 7) {
									owner.modIcons[6].drawSprite(xPos + 1, yPos - 10);
									xPos += 14;
								} else if (byte0 == 8) {
									owner.modIcons[7].drawSprite(xPos + 1, yPos - 10);
									xPos += 14;
								} else if (byte0 == 9) {
									owner.modIcons[8].drawSprite(xPos + 1, yPos - 10);
									xPos += 14;
								}
								owner.newRegularFont.drawBasicString(s1 + ":", xPos,
										yPos, chatInk(0), chatShadow());
								xPos += owner.newRegularFont.getTextWidth(s1) + 8;
								owner.newRegularFont.drawBasicString(chatBody(k),
										xPos, yPos, chatPublicBodyColor(), chatShadow());
							}
							j++;
							j77++;
						}
					}
					if ((chatType == 3 || chatType == 7)
							&& (owner.splitPrivateChat == 0 || owner.chatTypeView == 2)
							&& (chatType == 7 || owner.privateChatMode == 0 || owner.privateChatMode == 1
									&& owner.isFriendOrSelf(s1))) {
						if (owner.chatTypeView == 2 || owner.chatTypeView == 0) {
							if (yPos > 0 && yPos < 210) {
								int k1 = chatMsgX;
								// textDrawingArea.method385(0, "From", yPos,
								// k1);
								// k1 += textDrawingArea.getTextWidth("From ");

								owner.newRegularFont.drawBasicString("From", k1,
										yPos, chatInk(0), chatShadow());
								if (byte0 == 3 || byte0 == 2 || byte0 == 1
										|| byte0 == 0) {
									k1 += textDrawingArea.getTextWidth("From ");
								} else if (byte0 == 6 || byte0 == 5
										|| byte0 == 4) {
									k1 += textDrawingArea.getTextWidth("From");
								}
								if (byte0 == 1) {
									owner.modIcons[0].drawSprite(k1 - 1, yPos - 11);
									k1 += 12;
								} else if (byte0 == 2) {
									owner.modIcons[2].drawSprite(k1 - 2, yPos - 13);
									k1 += 12;
								} else if (byte0 == 3) {
									owner.modIcons[1].drawSprite(k1 - 1, yPos - 11);
									k1 += 12;
								} else if (byte0 == 4) {
									owner.modIcons[3].drawSprite(k1, yPos - 10);
									k1 += 12;
								} else if (byte0 == 5) {
									owner.modIcons[4].drawSprite(k1, yPos - 10);
									k1 += 12;
								} else if (byte0 == 6) {
									owner.modIcons[5].drawSprite(k1, yPos - 10);
									k1 += 12;
								} else if (byte0 == 7) {
									owner.modIcons[6].drawSprite(k1, yPos - 10);
									k1 += 12;
								} else if (byte0 == 8) {
									owner.modIcons[7].drawSprite(k1, yPos - 10);
									k1 += 12;
								} else if (byte0 == 9) {
									owner.modIcons[8].drawSprite(k1, yPos - 10);
									k1 += 12;
								}
								// textDrawingArea.method385(0, s1 + ":", yPos,
								// k1);
								// k1 += textDrawingArea.getTextWidth(s1) + 8;
								// textDrawingArea.method385(0x800000,
								// chatMessages[k], yPos, k1);

								owner.newRegularFont.drawBasicString(s1 + ":", k1,
										yPos, chatInk(0), chatShadow());
								k1 += owner.newRegularFont.getTextWidth(s1) + 8;
								String privStamp = chatTimePrefix(k);
								if (privStamp.length() > 0) {
									owner.newRegularFont.drawBasicString(privStamp, k1, yPos,
											chatTimestampColor(), chatShadow());
									k1 += owner.newRegularFont.getTextWidth(privStamp);
								}
								owner.newRegularFont.drawBasicString(chatBody(k),
										k1, yPos, 0x800000, chatShadow());
							}
							j++;
							j77++;
						}
					}
					if (chatType == 4
							&& (owner.tradeMode == 0 || owner.tradeMode == 1
									&& owner.isFriendOrSelf(s1))) {
						if (owner.chatTypeView == 3 || owner.chatTypeView == 0) {
							if (yPos > 0 && yPos < 210)
								// textDrawingArea.method385(0x800080, s1 + " "
								// + chatMessages[k], yPos, 11);
								owner.newRegularFont.drawBasicString(s1 + " "
										+ stampedChat(k), chatMsgX, yPos, 0x800080,
										-1);
							j++;
							j77++;
						}
					}
					if (chatType == 5 && owner.splitPrivateChat == 0
							&& owner.privateChatMode < 2) {
						if (owner.chatTypeView == 2 || owner.chatTypeView == 0) {
							if (yPos > 0 && yPos < 210) {
								int x5 = chatMsgX;
								String stamp5 = chatTimePrefix(k);
								if (stamp5.length() > 0) {
									owner.newRegularFont.drawBasicString(stamp5, x5, yPos,
											chatTimestampColor(), chatShadow());
									x5 += owner.newRegularFont.getTextWidth(stamp5);
								}
								owner.newRegularFont.drawBasicString(chatBody(k),
										x5, yPos, 0x800000, chatShadow());
							}
							j++;
							j77++;
						}
					}
					if (chatType == 6
							&& (owner.splitPrivateChat == 0 || owner.chatTypeView == 2)
							&& owner.privateChatMode < 2) {
						if (owner.chatTypeView == 2 || owner.chatTypeView == 0) {
							if (yPos > 0 && yPos < 210) {
								owner.newRegularFont.drawBasicString(
										"To " + s1 + ":", chatMsgX, yPos, chatInk(0), chatShadow());
								int x6 = chatMsgX + 4 + owner.newRegularFont.getTextWidth("To :" + s1);
								String stamp6 = chatTimePrefix(k);
								if (stamp6.length() > 0) {
									owner.newRegularFont.drawBasicString(stamp6, x6, yPos,
											chatTimestampColor(), chatShadow());
									x6 += owner.newRegularFont.getTextWidth(stamp6);
								}
								owner.newRegularFont.drawBasicString(chatBody(k),
										x6, yPos, 0x800000, chatShadow());
							}
							j++;
							j77++;
						}
					}
					if (chatType == 8
							&& (owner.tradeMode == 0 || owner.tradeMode == 1
									&& owner.isFriendOrSelf(s1))) {
						if (owner.chatTypeView == 3 || owner.chatTypeView == 0) {
							if (yPos > 0 && yPos < 210)
								owner.newRegularFont.drawBasicString(s1 + " "
										+ stampedChat(k), chatMsgX, yPos, 0x7e3200,
										-1);
							j++;
							j77++;
						}
						if (chatType == 11 && (owner.clanChatMode == 0)) {
							if (owner.chatTypeView == 11) {
								if (yPos > 0 && yPos < 110)
									owner.newRegularFont.drawBasicString(s1 + " "
											+ stampedChat(k), 19, yPos,
											0x7e3200, chatShadow());
								j++;
								j77++;
							}
						}
						if (chatType == 12) {
							if (owner.chatTypeView == 11 || owner.chatTypeView == 0) {							
								if (yPos > 3 && yPos < 130) {
									String title = "<col=0000FF>" + owner.clanTitles[k]
											+ "</col>";
									String username = (owner.chatRights[k] > 0 ? "<img="
											+ (owner.chatRights[k] - 1) + ">" : "")
											+ TextClass.fixName(owner.chatNames[k]);
									String message = "<col=800000>"
											+ stampedChat(k) + "</col>";
									owner.newRegularFont.drawBasicString("[" + title + "] "
											+ username + ": " + message, chatMsgX, yPos,
											chatInk(0), chatShadow());
								}
								j++;
								j77++;
							}
						}
					}
					if (chatType == 16) {
						int j2 = chatMsgX + 40;
						int clanNameWidth = textDrawingArea
								.getTextWidth(owner.clanname);
						if (owner.chatTypeView == 11 || owner.chatTypeView == 0) {
							if (yPos > 0 && yPos < 110)
								switch (owner.chatRights[k]) {
								case 1:
									j2 += clanNameWidth;
									owner.modIcons[0].drawSprite(j2 - 18, yPos - 12);
									j2 += 15;
									break;
								case 2:
									j2 += clanNameWidth;
									owner.modIcons[2].drawSprite(j2 - 18, yPos - 12);
									j2 += 15;
									break;
								case 3:
									j2 += clanNameWidth;
									owner.modIcons[1].drawSprite(j2 - 18, yPos - 12);
									j2 += 15;
									break;
								case 4:
									j2 += clanNameWidth;
									owner.modIcons[3].drawSprite(j2 - 18, yPos - 12);
									j2 += 15;
									break;
								case 5:
									j2 += clanNameWidth;
									owner.modIcons[4].drawSprite(j2 - 18, yPos - 12);
									j2 += 15;
									break;
								case 6:
									j2 += clanNameWidth;
									owner.modIcons[5].drawSprite(j2 - 18, yPos - 12);
									j2 += 15;
									break;
								case 7:
									j2 += clanNameWidth;
									owner.modIcons[6].drawSprite(j2 - 18, yPos - 12);
									j2 += 15;
									break;
								case 8:
									j2 += clanNameWidth;
									owner.modIcons[7].drawSprite(j2 - 18, yPos - 12);
									j2 += 15;
									break;
								case 9:
									j2 += clanNameWidth;
									owner.modIcons[8].drawSprite(j2 - 18, yPos - 12);
									j2 += 15;
									break;
								default:
									j2 += clanNameWidth;
									break;
								}
							owner.newRegularFont
									.drawBasicString("[", chatMsgX + 8, yPos, chatInk(0), chatShadow());
							owner.newRegularFont.drawBasicString("]",
									clanNameWidth + chatMsgX + 5, yPos, chatInk(0), chatShadow());
							owner.newRegularFont.drawBasicString(""
									+ capitalize(owner.clanname) + "", chatMsgX + 14, yPos, 255,
									-1);
							owner.newRegularFont.drawBasicString(
									capitalize(owner.chatNames[k]) + ":", j2 - 17,
									yPos);
							j2 += owner.newRegularFont.getTextWidth(owner.chatNames[k]) + 7;
							owner.newRegularFont.drawBasicString(
									capitalize(stampedChat(k)), j2 - 16, yPos,
									0x800000, chatShadow());

							j++;
							j77++;
						}
					}
				}
			DrawingArea.defaultDrawingAreaSize();
			anInt1211 = j * 14 + 7 + 5;
			if (anInt1211 < 111)
				anInt1211 = 111;
			int scrollX = chatScrollbarLeft ? 2 : 496;
			drawChatScrollbar(114, anInt1211 - anInt1089 - 113, 7, scrollX, anInt1211);
			String s;
			if (myPlayer != null && myPlayer.name != null)
				s = myPlayer.name;
			else
				s = TextClass.fixName(owner.myUsername);
			int nameX = chatMsgX;
			textDrawingArea.method389(transparentChatActive(), nameX, chatInk(0), s + ":", 133);
			int inputX = nameX + 1 + textDrawingArea.getTextWidth(s + ": ");
			if (keyRemapping && !enterToChat) {
				textDrawingArea.method385(0x808080, "Chat disabled", 133, inputX);
			} else if (keyRemapping && enterToChat && !chatTypeFocused) {
				textDrawingArea.method385(0x808080, "Press Enter to chat", 133, inputX);
			} else {
				textDrawingArea.drawChatInput(chatInputColor(), inputX, owner.inputString + "*", 133, transparentChatActive());
			}
			DrawingArea.method339(121, 0x807660, 506, 7);
		}
		if (owner.menuOpen && owner.menuScreenArea == 2 && isFixed()) {
			owner.drawMenu();
		}
		if (isFixed()) {
			owner.aRSImageProducer_1166.drawGraphics(owner.chatDrawY(), owner.graphics, 0);
		}
		owner.aRSImageProducer_1165.initDrawingArea();
		Texture.anIntArray1472 = owner.anIntArray1182;
	}

	void drawSplitPrivateChat() {
		if (owner.splitPrivateChat == 0)
			return;
		TextDrawingArea textDrawingArea = owner.aTextDrawingArea_1271;
		int i = 0;
		if (owner.anInt1104 != 0)
			i = 1;
		for (int j = 0; j < 100; j++)
			if (owner.chatMessages[j] != null) {
				int k = owner.chatTypes[j];
				String s = owner.chatNames[j];
				byte byte1 = 0;
				if (s != null && s.startsWith("@cr1@")) {
					s = s.substring(5);
					byte1 = 1;
				}
				if (s != null && s.startsWith("@cr2@")) {
					s = s.substring(5);
					byte1 = 2;
				}
				if (s != null && s.startsWith("@cr3@")) {
					s = s.substring(5);
					byte1 = 3;
				}
				if (s != null && s.startsWith("@cr4@")) {
					s = s.substring(5);
					byte1 = 4;
				}
				if (s != null && s.startsWith("@cr5@")) {
					s = s.substring(5);
					byte1 = 5;
				}
				if (s != null && s.startsWith("@cr6@")) {
					s = s.substring(5);
					byte1 = 6;
				}
				if (s != null && s.startsWith("@cr7@")) {
					s = s.substring(5);
					byte1 = 7;
				}
				if (s != null && s.startsWith("@cr8@")) {
					s = s.substring(5);
					byte1 = 8;
				}
				if (s != null && s.startsWith("@cr9@")) {
					s = s.substring(5);
					byte1 = 9;
				}

				if ((k == 3 || k == 7)
						&& (k == 7 || owner.privateChatMode == 0 || owner.privateChatMode == 1
								&& owner.isFriendOrSelf(s))) {
					int l = splitPrivateMessageY(i);
					int k1 = 4;
					textDrawingArea.method385(0, "From", l, k1);
					textDrawingArea.method385(65535, "From", l - 1, k1);
					if (byte1 == 3 || byte1 == 2 || byte1 == 1 || byte1 == 0) {
						k1 += textDrawingArea.getTextWidth("From ");
					} else if (byte1 == 6 || byte1 == 5 || byte1 == 4) {
						k1 += textDrawingArea.getTextWidth("From");
					}
					if (byte1 == 1) {
						owner.modIcons[0].drawSprite(k1 - 2, l - 12);
						k1 += 12;
					}
					if (byte1 == 2) {
						owner.modIcons[2].drawSprite(k1 - 2, l - 13);
						k1 += 12;
					}
					if (byte1 == 3) {
						owner.modIcons[1].drawSprite(k1 - 2, l - 12);
						k1 += 12;
					}
					if (byte1 == 4) {
						owner.modIcons[3].drawSprite(k1, l - 11);
						k1 += 12;
					}
					if (byte1 == 5) {
						owner.modIcons[4].drawSprite(k1, l - 11);
						k1 += 12;
					}
					if (byte1 == 6) {
						owner.modIcons[5].drawSprite(k1, l - 11);
						k1 += 12;
					}
					if (byte1 == 7) {
						owner.modIcons[6].drawSprite(k1, l - 11);
						k1 += 12;
					}
					if (byte1 == 8) {
						owner.modIcons[7].drawSprite(k1, l - 11);
						k1 += 12;
					}
					if (byte1 == 9) {
						owner.modIcons[8].drawSprite(k1, l - 11);
						k1 += 12;
					}
					String stampSplit = chatTimePrefix(j);
					if (stampSplit.length() > 0) {
						textDrawingArea.method385(0, stampSplit, l, k1);
						textDrawingArea.method385(chatTimestampColor(), stampSplit, l - 1, k1);
						k1 += textDrawingArea.getTextWidth(stampSplit);
					}
					String splitLine = s + ": " + chatBody(j);
					textDrawingArea.method385(0, splitLine, l, k1);
					textDrawingArea.method385(65535, splitLine, l - 1, k1);
					if (++i >= 5)
						return;
				}
				if (k == 5 && owner.privateChatMode < 2) {
					int i1 = splitPrivateMessageY(i);
					int x5 = 4;
					String stamp5 = chatTimePrefix(j);
					if (stamp5.length() > 0) {
						textDrawingArea.method385(0, stamp5, i1, x5);
						textDrawingArea.method385(chatTimestampColor(), stamp5, i1 - 1, x5);
						x5 += textDrawingArea.getTextWidth(stamp5);
					}
					textDrawingArea.method385(0, chatBody(j), i1, x5);
					textDrawingArea.method385(65535, chatBody(j), i1 - 1, x5);
					if (++i >= 5)
						return;
				}
				if (k == 6 && owner.privateChatMode < 2) {
					int j1 = splitPrivateMessageY(i);
					String toPrefix = "To " + s + ": ";
					textDrawingArea.method385(0, toPrefix, j1, 4);
					textDrawingArea.method385(65535, toPrefix, j1 - 1, 4);
					int x6 = 4 + textDrawingArea.getTextWidth(toPrefix);
					String stamp6 = chatTimePrefix(j);
					if (stamp6.length() > 0) {
						textDrawingArea.method385(0, stamp6, j1, x6);
						textDrawingArea.method385(chatTimestampColor(), stamp6, j1 - 1, x6);
						x6 += textDrawingArea.getTextWidth(stamp6);
					}
					textDrawingArea.method385(0, chatBody(j), j1, x6);
					textDrawingArea.method385(65535, chatBody(j), j1 - 1, x6);
					if (++i >= 5)
						return;
				}
			}

	}

	void buildChatAreaMenu(int j) {
		int l = 0;
		for (int i1 = 0; i1 < 500; i1++) {
			if (owner.chatMessages[i1] == null)
				continue;
			int j1 = owner.chatTypes[i1];
			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;
			if (k1 < -23)
				break;
			String s = owner.chatNames[i1];
			if (owner.chatTypeView == 1) {
				buildPublicChat(j);
				break;
			}
			if (owner.chatTypeView == 2) {
				buildFriendChat(j);
				break;
			}
			if (owner.chatTypeView == 3 || owner.chatTypeView == 4) {
				buildDuelorTrade(j);
				break;
			}
			if (owner.chatTypeView == 5) {
				break;
			}
			if (s != null && s.startsWith("@cr1@")) {
				s = s.substring(5);
			}
			if (s != null && s.startsWith("@cr2@")) {
				s = s.substring(5);
			}
			if (s != null && s.startsWith("@cr3@")) {
				s = s.substring(5);
			}
			if (s != null && s.startsWith("@cr4@")) {
				s = s.substring(5);
			}
			if (s != null && s.startsWith("@cr5@")) {
				s = s.substring(5);
			}
			if (s != null && s.startsWith("@cr6@")) {
				s = s.substring(5);
			}
			if (s != null && s.startsWith("@cr7@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr8@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr9@"))
				s = s.substring(5);
			if (j1 == 0)
				l++;
			if ((j1 == 1 || j1 == 2)
					&& (j1 == 1 || owner.publicChatMode == 0 || owner.publicChatMode == 1
							&& owner.isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1 && !s.equals(myPlayer.name)) {
					if (owner.myPrivilege >= 1) {
						owner.menuActionName[owner.menuActionRow] = "Report abuse @whi@"
								+ s;
						owner.menuActionID[owner.menuActionRow] = 606;
						owner.menuActionRow++;
					}
					owner.menuActionName[owner.menuActionRow] = "Add ignore @whi@" + s;
					owner.menuActionID[owner.menuActionRow] = 42;
					owner.menuActionRow++;
					owner.menuActionName[owner.menuActionRow] = "Add friend @whi@" + s;
					owner.menuActionID[owner.menuActionRow] = 337;
					owner.menuActionRow++;
				}
				l++;
			}
			if ((j1 == 3 || j1 == 7)
					&& owner.splitPrivateChat == 0
					&& (j1 == 7 || owner.privateChatMode == 0 || owner.privateChatMode == 1
							&& owner.isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					if (owner.myPrivilege >= 1) {
						owner.menuActionName[owner.menuActionRow] = "Report abuse @whi@"
								+ s;
						owner.menuActionID[owner.menuActionRow] = 606;
						owner.menuActionRow++;
					}
					owner.menuActionName[owner.menuActionRow] = "Add ignore @whi@" + s;
					owner.menuActionID[owner.menuActionRow] = 42;
					owner.menuActionRow++;
					owner.menuActionName[owner.menuActionRow] = "Add friend @whi@" + s;
					owner.menuActionID[owner.menuActionRow] = 337;
					owner.menuActionRow++;
				}
				l++;
			}
			if (j1 == 4
					&& (owner.tradeMode == 0 || owner.tradeMode == 1 && owner.isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					owner.menuActionName[owner.menuActionRow] = "Accept trade @whi@" + s;
					owner.menuActionID[owner.menuActionRow] = 484;
					owner.menuActionRow++;
				}
				l++;
			}
			if ((j1 == 5 || j1 == 6) && owner.splitPrivateChat == 0
					&& owner.privateChatMode < 2)
				l++;
			if (j1 == 8
					&& (owner.tradeMode == 0 || owner.tradeMode == 1 && owner.isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					owner.menuActionName[owner.menuActionRow] = "Accept challenge @whi@"
							+ s;
					owner.menuActionID[owner.menuActionRow] = 6;
					owner.menuActionRow++;
				}
				l++;
			}
		}
	}

	public void drawChannelButtons() {
		String text[] = { "On", "Friends", "Off", "Hide" };
		int textColor[] = { 65280, 0xffff00, 0xff0000, 65535 };
		switch (owner.cButtonCPos) {
		case 0:
			owner.chatButtons[1].drawSprite(5, 142);
			break;
		case 1:
			owner.chatButtons[1].drawSprite(71, 142);
			break;
		case 2:
			owner.chatButtons[1].drawSprite(137, 142);
			break;
		case 3:
			owner.chatButtons[1].drawSprite(203, 142);
			break;
		case 4:
			owner.chatButtons[1].drawSprite(269, 142);
			break;
		case 5:
			owner.chatButtons[1].drawSprite(335, 142);
			break;
		}
		if (owner.cButtonHPos == owner.cButtonCPos) {
			switch (owner.cButtonHPos) {
			case 0:
				owner.chatButtons[2].drawSprite(5, 142);
				break;
			case 1:
				owner.chatButtons[2].drawSprite(71, 142);
				break;
			case 2:
				owner.chatButtons[2].drawSprite(137, 142);
				break;
			case 3:
				owner.chatButtons[2].drawSprite(203, 142);
				break;
			case 4:
				owner.chatButtons[2].drawSprite(269, 142);
				break;
			case 5:
				owner.chatButtons[2].drawSprite(335, 142);
				break;
			case 6:
				owner.chatButtons[3].drawSprite(404, 142);
				break;
			}
		} else {
			switch (owner.cButtonHPos) {
			case 0:
				owner.chatButtons[0].drawSprite(5, 142);
				break;
			case 1:
				owner.chatButtons[0].drawSprite(71, 142);
				break;
			case 2:
				owner.chatButtons[0].drawSprite(137, 142);
				break;
			case 3:
				owner.chatButtons[0].drawSprite(203, 142);
				break;
			case 4:
				owner.chatButtons[0].drawSprite(269, 142);
				break;
			case 5:
				owner.chatButtons[0].drawSprite(335, 142);
				break;
			case 6:
				owner.chatButtons[3].drawSprite(404, 142);
				break;
			}
		}
		owner.smallText.method389(true, 425, 0xffffff, "  Screenshot", 157);
		owner.smallText.method389(true, 26, 0xffffff, "All", 157);
		owner.smallText.method389(true, 86, 0xffffff, "Game", 157);
		owner.smallText.method389(true, 150, 0xffffff, "Public", 152);
		owner.smallText.method389(true, 212, 0xffffff, "Private", 152);
		owner.smallText.method389(true, 286, 0xffffff, "Clan", 152);
		owner.smallText.method389(true, 349, 0xffffff, "Trade", 152);
		owner.smallText.method382(textColor[owner.publicChatMode], 164,
				text[owner.publicChatMode], 163, true);
		owner.smallText.method382(textColor[owner.privateChatMode], 230,
				text[owner.privateChatMode], 163, true);
		owner.smallText.method382(textColor[owner.clanChatMode], 296, text[owner.clanChatMode],
				163, true);
		owner.smallText.method382(textColor[owner.tradeMode], 362, text[owner.tradeMode], 163,
				true);
	}

	public void processChatModeClick() {
		if (owner.mouseX >= 5 && owner.mouseX <= 61 && owner.mouseY >= owner.chatDrawY() + 144
				&& owner.mouseY <= owner.chatDrawY() + 165) {
			owner.cButtonHPos = 0;
			inputTaken = true;
		} else if (owner.mouseX >= 71 && owner.mouseX <= 127
				&& owner.mouseY >= owner.chatDrawY() + 144 && owner.mouseY <= owner.chatDrawY() + 165) {
			owner.cButtonHPos = 1;
			inputTaken = true;
		} else if (owner.mouseX >= 137 && owner.mouseX <= 193
				&& owner.mouseY >= owner.chatDrawY() + 144 && owner.mouseY <= owner.chatDrawY() + 165) {
			owner.cButtonHPos = 2;
			inputTaken = true;
		} else if (owner.mouseX >= 203 && owner.mouseX <= 259
				&& owner.mouseY >= owner.chatDrawY() + 144 && owner.mouseY <= owner.chatDrawY() + 165) {
			owner.cButtonHPos = 3;
			inputTaken = true;
		} else if (owner.mouseX >= 269 && owner.mouseX <= 325
				&& owner.mouseY >= owner.chatDrawY() + 144 && owner.mouseY <= owner.chatDrawY() + 165) {
			owner.cButtonHPos = 4;
			inputTaken = true;
		} else if (owner.mouseX >= 335 && owner.mouseX <= 391
				&& owner.mouseY >= owner.chatDrawY() + 144 && owner.mouseY <= owner.chatDrawY() + 165) {
			owner.cButtonHPos = 5;
			inputTaken = true;
		} else if (owner.mouseX >= 404 && owner.mouseX <= 515
				&& owner.mouseY >= owner.chatDrawY() + 144 && owner.mouseY <= owner.chatDrawY() + 165) {
			owner.cButtonHPos = 6;
			inputTaken = true;
		} else {
			owner.cButtonHPos = -1;
			inputTaken = true;
		}
		if (owner.clickMode3 == 1) {
			if (owner.saveClickY >= owner.chatDrawY() + 122 && owner.saveClickY < owner.chatDrawY() + 144
					&& owner.saveClickX >= 7 && owner.saveClickX < 506) {
				chatTypeFocused = true;
				inputTaken = true;
			}
			if (owner.saveClickX >= 5 && owner.saveClickX <= 61
					&& owner.saveClickY >= owner.chatDrawY() + 144 && owner.saveClickY <= owner.chatDrawY() + 167) {
				owner.cButtonCPos = 0;
				owner.chatTypeView = 0;
				if (!isFixed()) {
					owner.chatBoxHidden = !owner.chatBoxHidden;
				}
				inputTaken = true;
			} else if (owner.saveClickX >= 71 && owner.saveClickX <= 127
					&& owner.saveClickY >= owner.chatDrawY() + 144 && owner.saveClickY <= owner.chatDrawY() + 167) {
				owner.cButtonCPos = 1;
				owner.chatTypeView = 5;
				inputTaken = true;
			} else if (owner.saveClickX >= 137 && owner.saveClickX <= 193
					&& owner.saveClickY >= owner.chatDrawY() + 144 && owner.saveClickY <= owner.chatDrawY() + 167) {
				owner.cButtonCPos = 2;
				owner.chatTypeView = 1;
				inputTaken = true;
			} else if (owner.saveClickX >= 203 && owner.saveClickX <= 259
					&& owner.saveClickY >= owner.chatDrawY() + 144 && owner.saveClickY <= owner.chatDrawY() + 167) {
				owner.cButtonCPos = 3;
				owner.chatTypeView = 2;
				inputTaken = true;
			} else if (owner.saveClickX >= 269 && owner.saveClickX <= 325
					&& owner.saveClickY >= owner.chatDrawY() + 144 && owner.saveClickY <= owner.chatDrawY() + 167) {
				owner.cButtonCPos = 4;
				owner.chatTypeView = 11;
				inputTaken = true;
			} else if (owner.saveClickX >= 335 && owner.saveClickX <= 391
					&& owner.saveClickY >= owner.chatDrawY() + 144 && owner.saveClickY <= owner.chatDrawY() + 167) {
				owner.cButtonCPos = 5;
				owner.chatTypeView = 3;
				inputTaken = true;
			} else if (owner.saveClickX >= 404 && owner.saveClickX <= 515
					&& owner.saveClickY >= owner.chatDrawY() + 144 && owner.saveClickY <= owner.chatDrawY() + 167) {
				Jframe.takeScreenshot();
			}
		}
	}

	void buildSplitPrivateChatMenu() {
		if (owner.splitPrivateChat == 0)
			return;
		int i = 0;
		if (owner.anInt1104 != 0)
			i = 1;
		for (int j = 0; j < 100; j++)
			if (owner.chatMessages[j] != null) {
				int k = owner.chatTypes[j];
				String s = owner.chatNames[j];
				if (s != null && s.startsWith("@cr1@")) {
					s = s.substring(5);
				}
				if (s != null && s.startsWith("@cr2@")) {
					s = s.substring(5);
				}
				if (s != null && s.startsWith("@cr3@")) {
					s = s.substring(5);
				}
				if (s != null && s.startsWith("@cr4@")) {
					s = s.substring(5);
				}
				if (s != null && s.startsWith("@cr5@")) {
					s = s.substring(5);
				}
				if (s != null && s.startsWith("@cr6@")) {
					s = s.substring(5);
				}
				if (s != null && s.startsWith("@cr7@")) {
					s = s.substring(5);
				}
				if (s != null && s.startsWith("@cr8@")) {
					s = s.substring(5);
				}
				if (s != null && s.startsWith("@cr9@")) {
					s = s.substring(5);
				}
				if ((k == 3 || k == 7)
						&& (k == 7 || owner.privateChatMode == 0 || owner.privateChatMode == 1
								&& owner.isFriendOrSelf(s))) {
					int l = splitPrivateMessageY(i);
					int my = owner.mouseY - owner.gameDrawY();
					if (owner.mouseX > owner.gameDrawX() + 4 && my > l - 10
							&& my <= l + 3) {
						int i1 = owner.aTextDrawingArea_1271.getTextWidth("From:  "
								+ s + stampedChat(j)) + 25;
						if (i1 > 450)
							i1 = 450;
						if (owner.mouseX < owner.gameDrawX() + 4 + i1) {
							if (owner.myPrivilege >= 1) {
								owner.menuActionName[owner.menuActionRow] = "Report abuse @whi@"
										+ s;
								owner.menuActionID[owner.menuActionRow] = 2606;
								owner.menuActionRow++;
							}
							owner.menuActionName[owner.menuActionRow] = "Add ignore @whi@"
									+ s;
							owner.menuActionID[owner.menuActionRow] = 2042;
							owner.menuActionRow++;
							owner.menuActionName[owner.menuActionRow] = "Add friend @whi@"
									+ s;
							owner.menuActionID[owner.menuActionRow] = 2337;
							owner.menuActionRow++;
						}
					}
					if (++i >= 5)
						return;
				}
				if ((k == 5 || k == 6) && owner.privateChatMode < 2 && ++i >= 5)
					return;
			}

	}

	public void rightClickChatButtons() {
		if (owner.mouseX >= 5 && owner.mouseX <= 61 && owner.mouseY >= owner.chatDrawY() + 144
				&& owner.mouseY <= owner.chatDrawY() + 165) {
			owner.menuActionName[1] = "Clear chat history";
			owner.menuActionID[1] = 1508;
			owner.menuActionName[2] = "View All";
			owner.menuActionID[2] = 999;
			owner.menuActionRow = 3;
		} else if (owner.mouseX >= 71 && owner.mouseX <= 127
				&& owner.mouseY >= owner.chatDrawY() + 144 && owner.mouseY <= owner.chatDrawY() + 165) {
			owner.menuActionName[1] = "View Game";
			owner.menuActionID[1] = 998;
			owner.menuActionRow = 2;
		} else if (owner.mouseX >= 137 && owner.mouseX <= 193
				&& owner.mouseY >= owner.chatDrawY() + 144 && owner.mouseY <= owner.chatDrawY() + 165) {
			owner.menuActionName[1] = "Hide public";
			owner.menuActionID[1] = 997;
			owner.menuActionName[2] = "Off public";
			owner.menuActionID[2] = 996;
			owner.menuActionName[3] = "Friends public";
			owner.menuActionID[3] = 995;
			owner.menuActionName[4] = "On public";
			owner.menuActionID[4] = 994;
			owner.menuActionName[5] = "View public";
			owner.menuActionID[5] = 993;
			owner.menuActionRow = 6;
		} else if (owner.mouseX >= 203 && owner.mouseX <= 259
				&& owner.mouseY >= owner.chatDrawY() + 144 && owner.mouseY <= owner.chatDrawY() + 165) {
			owner.menuActionName[1] = "Off private";
			owner.menuActionID[1] = 992;
			owner.menuActionName[2] = "Friends private";
			owner.menuActionID[2] = 991;
			owner.menuActionName[3] = "On private";
			owner.menuActionID[3] = 990;
			owner.menuActionName[4] = "View private";
			owner.menuActionID[4] = 989;
			owner.menuActionRow = 5;
		} else if (owner.mouseX >= 269 && owner.mouseX <= 325
				&& owner.mouseY >= owner.chatDrawY() + 144 && owner.mouseY <= owner.chatDrawY() + 165) {
			owner.menuActionName[1] = "Off clan chat";
			owner.menuActionID[1] = 1003;
			owner.menuActionName[2] = "Friends clan chat";
			owner.menuActionID[2] = 1002;
			owner.menuActionName[3] = "On clan chat";
			owner.menuActionID[3] = 1001;
			owner.menuActionName[4] = "View clan chat";
			owner.menuActionID[4] = 1000;
			owner.menuActionRow = 5;
		} else if (owner.mouseX >= 335 && owner.mouseX <= 391
				&& owner.mouseY >= owner.chatDrawY() + 144 && owner.mouseY <= owner.chatDrawY() + 165) {
			owner.menuActionName[1] = "Off trade";
			owner.menuActionID[1] = 987;
			owner.menuActionName[2] = "Friends trade";
			owner.menuActionID[2] = 986;
			owner.menuActionName[3] = "On trade";
			owner.menuActionID[3] = 985;
			owner.menuActionName[4] = "View trade";
			owner.menuActionID[4] = 984;
			owner.menuActionRow = 5;
		}
	}

	private void buildDuelorTrade(int j) {
		int l = 0;
		for (int i1 = 0; i1 < 500; i1++) {
			if (owner.chatMessages[i1] == null)
				continue;
			if (owner.chatTypeView != 3 && owner.chatTypeView != 4)
				continue;
			int j1 = owner.chatTypes[i1];
			String s = owner.chatNames[i1];
			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;
			if (k1 < -23)
				break;
			if (s != null && s.startsWith("@cr1@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr2@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr3@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr4@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr5@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr6@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr7@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr8@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr9@"))
				s = s.substring(5);
			if (owner.chatTypeView == 3 && j1 == 4
					&& (owner.tradeMode == 0 || owner.tradeMode == 1 && owner.isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					owner.menuActionName[owner.menuActionRow] = "Accept trade @whi@" + s;
					owner.menuActionID[owner.menuActionRow] = 484;
					owner.menuActionRow++;
				}
				l++;
			}
			if (owner.chatTypeView == 4 && j1 == 8
					&& (owner.tradeMode == 0 || owner.tradeMode == 1 && owner.isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					owner.menuActionName[owner.menuActionRow] = "Accept challenge @whi@"
							+ s;
					owner.menuActionID[owner.menuActionRow] = 6;
					owner.menuActionRow++;
				}
				l++;
			}
			if (j1 == 12) {
				if (j > k1 - 14 && j <= k1) {
					owner.menuActionName[owner.menuActionRow] = "Go-to @blu@" + s;
					owner.menuActionID[owner.menuActionRow] = 915;
					owner.menuActionRow++;
				}
				l++;
			}
		}
	}

	private void buildFriendChat(int j) {
		int l = 0;
		for (int i1 = 0; i1 < 500; i1++) {
			if (owner.chatMessages[i1] == null)
				continue;
			if (owner.chatTypeView != 2)
				continue;
			int j1 = owner.chatTypes[i1];
			String s = owner.chatNames[i1];
			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;
			if (k1 < -23)
				break;
			if (s != null && s.startsWith("@cr1@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr2@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr3@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr4@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr5@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr6@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr7@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr8@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr9@"))
				s = s.substring(5);
			if ((j1 == 5 || j1 == 6)
					&& (owner.splitPrivateChat == 0 || owner.chatTypeView == 2)
					&& (j1 == 6 || owner.privateChatMode == 0 || owner.privateChatMode == 1
							&& owner.isFriendOrSelf(s)))
				l++;
			if ((j1 == 3 || j1 == 7)
					&& (owner.splitPrivateChat == 0 || owner.chatTypeView == 2)
					&& (j1 == 7 || owner.privateChatMode == 0 || owner.privateChatMode == 1
							&& owner.isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					if (owner.myPrivilege >= 1) {
						owner.menuActionName[owner.menuActionRow] = "Report abuse @whi@"
								+ s;
						owner.menuActionID[owner.menuActionRow] = 606;
						owner.menuActionRow++;
					}
					owner.menuActionName[owner.menuActionRow] = "Add ignore @whi@" + s;
					owner.menuActionID[owner.menuActionRow] = 42;
					owner.menuActionRow++;
					owner.menuActionName[owner.menuActionRow] = "Add friend @whi@" + s;
					owner.menuActionID[owner.menuActionRow] = 337;
					owner.menuActionRow++;
				}
				l++;
			}
		}
	}

	private void buildPublicChat(int j) {
		int l = 0;
		for (int i1 = 0; i1 < 500; i1++) {
			if (owner.chatMessages[i1] == null)
				continue;
			if (owner.chatTypeView != 1)
				continue;
			int j1 = owner.chatTypes[i1];
			String s = owner.chatNames[i1];
			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;
			if (k1 < -23)
				break;
			if (s != null && s.startsWith("@cr1@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr2@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr3@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr4@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr5@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr6@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr7@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr8@"))
				s = s.substring(5);
			if (s != null && s.startsWith("@cr9@"))
				s = s.substring(5);
			if ((j1 == 1 || j1 == 2)
					&& (j1 == 1 || owner.publicChatMode == 0 || owner.publicChatMode == 1
							&& owner.isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1 && !s.equals(myPlayer.name)) {
					if (owner.myPrivilege >= 1) {
						owner.menuActionName[owner.menuActionRow] = "Report abuse @whi@"
								+ s;
						owner.menuActionID[owner.menuActionRow] = 606;
						owner.menuActionRow++;
					}
					owner.menuActionName[owner.menuActionRow] = "Add ignore @whi@" + s;
					owner.menuActionID[owner.menuActionRow] = 42;
					owner.menuActionRow++;
					owner.menuActionName[owner.menuActionRow] = "Add friend @whi@" + s;
					owner.menuActionID[owner.menuActionRow] = 337;
					owner.menuActionRow++;
				}
				l++;
			}
		}
	}

	void copyWorldUnderHud(RSImageProducer dest, int srcX, int srcY) {
		if (dest == null || owner.aRSImageProducer_1165 == null) {
			return;
		}
		int[] source = owner.aRSImageProducer_1165.anIntArray315;
		int sw = owner.aRSImageProducer_1165.anInt316;
		int sh = owner.aRSImageProducer_1165.anInt317;
		int[] destPix = dest.anIntArray315;
		int dw = dest.anInt316;
		int dh = dest.anInt317;
		for (int y = 0; y < dh; y++) {
			int sy = srcY + y;
			if (sy < 0 || sy >= sh) {
				continue;
			}
			int sx = srcX;
			int dx = 0;
			int copy = dw;
			if (sx < 0) {
				dx -= sx;
				copy += sx;
				sx = 0;
			}
			if (sx + copy > sw) {
				copy = sw - sx;
			}
			if (copy <= 0 || dx >= dw) {
				continue;
			}
			System.arraycopy(source, sy * sw + sx, destPix, y * dw + dx, copy);
		}
	}

	private void drawChatScrollbar(int j, int k, int l, int i1, int j1) {
		if (!isFixed() && resizableChatTransparent) {
			int trackA = 14;
			int thumbA = 40;
			DrawingArea.method335(0xD0D4DC, l, 16, j, trackA, i1);
			DrawingArea.method335(0xA8B0BC, l, 1, j, 28, i1);
			DrawingArea.method335(0xA8B0BC, l, 1, j, 28, i1 + 15);
			int k1 = ((j - 32) * j) / j1;
			if (k1 < 8) {
				k1 = 8;
			}
			int l1 = ((j - 32 - k1) * k) / (j1 - j);
			if (l1 < 0) {
				l1 = 0;
			}
			DrawingArea.method335(0xE8ECF2, l + 16 + l1, 14, k1, thumbA, i1 + 1);
			DrawingArea.method335(0x9098A4, l + 16 + l1, 14, 1, 50, i1 + 1);
			DrawingArea.method335(0x9098A4, l + 15 + l1 + k1, 14, 1, 50, i1 + 1);
			return;
		}
		owner.drawScrollbar(j, k, l, i1, j1);
	}

	public static String capitalize(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (i == 0) {
				s = String.format("%s%s", Character.toUpperCase(s.charAt(0)),
						s.substring(1));
			}
			if (!Character.isLetterOrDigit(s.charAt(i))) {
				if (i + 1 < s.length()) {
					s = String.format("%s%s%s", s.subSequence(0, i + 1),
							Character.toUpperCase(s.charAt(i + 1)),
							s.substring(i + 2));
				}
			}
		}
		return s;
	}

	private void chatJoin(long l) {
		try {
			if (l == 0L)
				return;
			owner.stream.createFrame(60);
			owner.stream.writeQWord(l);
			return;
		} catch (RuntimeException runtimeexception) {
			signlink.reporterror("47229, " + 3 + ", " + l + ", "
					+ runtimeexception.toString());
		}
		throw new RuntimeException();

	}

	void clearChatHistory() {
		for (int i = 0; i < owner.chatMessages.length; i++) {
			owner.chatMessages[i] = null;
			owner.chatNames[i] = null;
			owner.chatTypes[i] = 0;
			owner.chatTimes[i] = 0L;
		}
		inputTaken = true;
		owner.pushMessage("Chat history cleared.", 0, "");
	}

	private Sprite currentChatArea() {
		if (isFixed()) {
			return owner.chatArea;
		}
		if (resizableChatTransparent) {
			return null;
		}
		return owner.chatAreaResizable != null ? owner.chatAreaResizable : owner.chatArea;
	}

	static String timePrefix(long ms) {
		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.setTimeInMillis(ms);
		int h = cal.get(java.util.Calendar.HOUR_OF_DAY);
		int m = cal.get(java.util.Calendar.MINUTE);
		String hh = h < 10 ? "0" + h : Integer.toString(h);
		String mm = m < 10 ? "0" + m : Integer.toString(m);
		return "[" + hh + ":" + mm + "] ";
	}

	private String stampedChat(int index) {
		String msg = chatBody(index);
		String stamp = chatTimePrefix(index);
		if (stamp.length() == 0) {
			return msg;
		}
		return stamp + msg;
	}

	private int chatInk(int color) {
		if (transparentChatActive() && (color == 0 || color == 0x000000)) {
			return 0xffffff;
		}
		return color;
	}

	private String chatTimePrefix(int index) {
		if (!chatTimestamps || owner.chatTimes == null || owner.chatTimes[index] == 0L) {
			return "";
		}
		return timePrefix(owner.chatTimes[index]);
	}

	private String chatBody(int index) {
		String msg = owner.chatMessages[index];
		return msg == null ? "" : msg;
	}

	private boolean transparentChatActive() {
		return !isFixed() && resizableChatTransparent;
	}

	private int chatTimestampColor() {
		return transparentChatActive() ? 0x00FFFA : 0x0000FF;
	}

	private int chatPublicBodyColor() {
		return transparentChatActive() ? 0x9595FF : 255;
	}

	private int chatShadow() {
		return transparentChatActive() ? 0 : -1;
	}

	private int chatInputColor() {
		return transparentChatActive() ? 0xffffff : 255;
	}

	private int splitPrivateMessageY(int line) {
		return owner.chatDrawY() - owner.gameDrawY() - 5 - line * 13;
	}
}
