package server.game.players;

import server.Server;
import server.Config;
import server.game.npcs.NPCHandler;
import server.game.npcs.WorldAdventurer;
import server.game.players.actions.dialogues.DialogueRegistry;
import core.util.Misc;

public class DialogueHandler {

	private Client c;
	
	public DialogueHandler(Client client) {
		this.c = client;
	}
	
	/**
	 * Handles all talking
	 * @param dialogue The dialogue you want to use
	 * @param npcId The npc id that the chat will focus on during the chat
	 */
	public void sendDialogues(int dialogue, int npcId) {
		c.talkingNpc = npcId;
		if (DialogueRegistry.dispatch(this, c, dialogue)) {
			return;
		}
		switch(dialogue) {

		/**
		 * rank switcher
		 */
		case 8800:
			c.npcType = WorldAdventurer.NPC_ID;
			sendNpcChat2(WorldAdventurer.greetingLine(c),
					"Need something, or just watching?", WorldAdventurer.NPC_ID, WorldAdventurer.NAME);
			c.nextChat = 8801;
			break;
		case 8802:
			c.npcType = WorldAdventurer.NPC_ID;
			sendNpcChat2(WorldAdventurer.doingLine(c),
					"Talk if you want. I don't mind the company.", WorldAdventurer.NPC_ID, WorldAdventurer.NAME);
			c.nextChat = 8801;
			break;
		case 8803:
			c.npcType = WorldAdventurer.NPC_ID;
			sendNpcChat2(WorldAdventurer.headingLine(),
					"I don't sit still for long.", WorldAdventurer.NPC_ID, WorldAdventurer.NAME);
			c.nextChat = 8801;
			break;
		case 8804:
			c.npcType = WorldAdventurer.NPC_ID;
			sendNpcChat2(WorldAdventurer.tipLine(c),
					"That's free. The rest you earn.", WorldAdventurer.NPC_ID, WorldAdventurer.NAME);
			c.nextChat = 8801;
			break;
		case 8805:
			c.npcType = WorldAdventurer.NPC_ID;
			sendNpcChat2("Yeah. I'll be around.",
					"Try not to die somewhere stupid.", WorldAdventurer.NPC_ID, WorldAdventurer.NAME);
			c.nextChat = 0;
			c.dialogueAction = -1;
			break;

		/**
		 * travelers
		 */

		/**
		 * reset
		 */
		case 553:
			sendOption2("Construction", "Hunter");
			if(c.rubbedLamp)
				c.getPA().sendFrame126("Level up:", 2460);
			else
				c.getPA().sendFrame126("Reset:", 2460);
			c.dialogueAction = 553;
			break;

		/**
		 * Bounty Hunter
		 */
		case 511:
			sendNpcChat1("You need "+((c.bountyHunter.killsMultiplier *10) - c.bountyHunter.bountyKills)+" more kills before you can claim your prize.", c.talkingNpc, "Veteran Hervi");
			c.nextChat = 0;
			break;
		/*
		 * Quests
		 */

		 /*
			 * Cooks Assistant

			 *
			 *Made by Liam`
			 *
			 */

			case 609:
				sendNpcChat2("Oh thank you, thank you. I need milk, an egg, and", "flour. I'd be very grateful if you can get them for me.", c.talkingNpc, "Cook");
				c.cookAss = 1;
				c.nextChat = 610;	
				break;
			case 616:
				if(c.getItems().playerHasItem(1944, 1) && c.getItems().playerHasItem(1927, 1) && c.getItems().playerHasItem(1933, 1)) {
				sendPlayerChat1("Here's all the items!");
				c.nextChat = 618;
				} else {
				sendPlayerChat1("I don't have all the items yet.");
				c.nextChat = 608;
				}
				break;
			case 618:
				c.getItems().deleteItem(1944, 1);
				c.getItems().deleteItem(1927, 1);
				c.getItems().deleteItem(1933, 1);
				c.cookAss = 2;
				sendNpcChat2("You brought me everything I need! I'm saved!", "Thank you!", c.talkingNpc, "Cook");
				c.nextChat = 619;
				break;
			case 623:
				c.getPA().cookFinish();
				break;
				/*
				*Rune Mysteries
				*@author Liam aka Insidia X On Rune-server
				*/

				case 1107:
					c.dialogueAction = 570;
					sendOption2("Okay"," ");
					break;
				case 1110:
					sendNpcChat1("Thank you very much, stranger.", c.talkingNpc, "Duke Horacio");
					c.sendMessage("The Duke hands you a talisman.");
					c.getItems().addItem(1438, 1);
					c.RuneMysteries = 1;
					c.nextChat = 0;
					break;
				case 1153:
					sendPlayerChat1("Okay, here you are.");
					c.sendMessage("You hand the Talisman over to the Head Wizard");
					c.getItems().deleteItem(1438, 1);
					c.nextChat = 1155;
					break;
				case 1158:
					sendNpcChat1("Give this package to Aubury, the Shop owner.", c.talkingNpc, "Head Wizard");
					c.sendMessage("The Head wizard gives you a Research Package");
					c.getItems().addItem(290, 1);
					c.RuneMysteries = 2;
					c.nextChat = 0;
					break;

				case 1174:
					if (c.RuneMysteries == 2) {
						sendPlayerChat1("No, I have a package for you.");
						c.nextChat = 1176;
						} else {
							sendPlayerChat1("Yes");
							c.getShops().openShop(30);
						}
					break;
				case 1180:
					sendPlayerChat1("Here");
					c.sendMessage("You hand Aubury the research package.");
					c.getItems().deleteItem(290, 1);
					c.nextChat = 1181;
					break;
				case 1182:
					sendNpcChat1("Take my notes to the Head Wizard please.", c.talkingNpc, "Aubury");
					c.sendMessage("Aubury hands you his notes.");
					c.getItems().addItem(291, 1);
					c.RuneMysteries = 3;
					c.nextChat = 0;
					break;
				case 1195:
					sendPlayerChat1("Sure. I have them here.");
					c.sendMessage("You hand the research notes to the Head Wizard");
					c.getItems().deleteItem(291, 1);
					c.nextChat = 1196;
					break;
				case 1196:
					sendNpcChat1("Thank you adventurer you have been nothing but helpful, take this Air Talisman.", c.talkingNpc, "Head Wizard");
				c.getItems().addItem(1438, 1);
				c.getPA().RuneMysteriesFinish();
				break;

		case 503: //interface
			c.dialogueAction = -1;
			c.teleAction = -1;
			c.isResetting = true;
			c.getDH().sendDialogues(554, -1);
			return;
		//ranging guild

		//skills

			//monsters

		//Slayer

		case 403:
			c.getSlayer().handleInterface("buy");
			c.nextChat = 0;
			break;

		case 405:
			c.getSlayer().generateTask();
			//c.nextChat = 0;
			break;

		case 406:
			sendNpcChat("Your new task is to kill "+c.taskAmount+" "+c.getSlayer().getTaskName(c.slayerTask)+". Good luck "+c.playerName+".", 602, "Vannaka");
			c.nextChat = 0;
			break;

		case 407:
			sendNpcChat("You currently have "+c.taskAmount+" "+c.getSlayer().getTaskName(c.slayerTask)+" to kill.", "If you would like I could give you an easier task.", "Although if I do this, you won't recieve as many points.", ANGRY_1, "Vannaka");
			c.nextChat = 408;
			break;

		case 410:
			sendNpcChat("Your task can be found in the "+c.getSlayer().getLocation(c.slayerTask)+"", ANNOYED, "Vannaka");
			c.nextChat = 411;
			break;

			//skill teleports

			//Skill Master dialogues
			//HUNTER

			//CONSTRUCTION

			//agility

			//herblore

			//thieving

			//Runecrafting

			//Crafting

			//Fletching

			//Mining

			//Smithing

			//Fishing

			//Cooking

			//firemaking

			//Woodcutting

			//farming

			//slayer

			//strength

			//attack

			//hp

			//Defence

			//Prayer

			//Ranging

			//Magic

		case 0:
			c.talkingNpc = -1;
			c.getPA().removeAllWindows();
			c.nextChat = 0;
			break;
		case 489:
			sendNpcChat1("You currently have "+c.pcPoints+" Pest Points", c.talkingNpc, "Void Knight");
			break;
		case 673:
			sendNpcChat4("Greetings, "+c.playerName+", you haven't faced the KBD yet, have you?", "He is the strongest dragon in "+Config.SERVER_NAME+"!", "But good news!", "He drops dragon bones and the rare Dragonfire Shield!", c.talkingNpc, "Squire");
			break;
		case 310:
			if(c.lockedEXP == 1) {
			sendNpcChat1("Your EXP has been unlocked!",c.talkingNpc, "XP Lock");
		c.lockedEXP = 0;
		} else {
	sendNpcChat1("Your EXP is already unlocked!",c.talkingNpc, "XP Lock");
	}
			c.nextChat = 0;
		break;
		case 320:
		if(c.lockedEXP == 0) {
			sendNpcChat1("Your EXP has been locked!",c.talkingNpc, "XP Lock");
	c.lockedEXP = 1;
		} else {
	sendNpcChat1("Your EXP is already locked!",c.talkingNpc, "XP Lock");
	}
			c.nextChat = 0;
		break;
        /*
        * Slayer Gem
        */
        case 784:
        sendStatement("I currently have " + c.taskAmount + " " + Server.npcHandler.getNpcListName(c.slayerTask) + " to kill.");
        c.nextChat = 0;
        break;

		case 57:
			c.getPA().sendFrame126("Teleport to shops?", 2460);
			c.getPA().sendFrame126("Yes.", 2461);
			c.getPA().sendFrame126("No.", 2462);
			c.getPA().sendFrame164(2459);
			c.dialogueAction = 27;
		break;
		case 599:
			c.fadeStarterTele(3031, 3236, 0);
			//c.nextChat = 597;
			break;
		case 700:
			c.fadeStarterTele2(3025, 3218, 0);
			//c.nextChat = 598;
			break;
		case 701:
			c.fadeStarterTele3(3087, 3505, 0);
			//c.nextChat = 471;
			break;
		case 471:
			sendNpcChat3("I want to thank you for listening to my tutorial!", "I will give you a starter pack just for that!", "Good luck, "+c.playerName+"!", 2244, "Lumbridge Guide"); //goodluck  
			c.nextChat = 472;
		break;
		case 473:
			sendPlayerChat1("Thank you sire!");
			c.nextChat = 516;
			c.completedTut = true;
			break;
		case 517:
			c.getPA().showInterface(3559); 
			c.appearance.canChangeAppearance = true;
			c.nextChat = 0;
			break;
		case 518:
			sendNpcChat1("Goodluck, "+c.playerName+".", 599, "Make-over mage");
			c.nextChat = 519;
			break;
		case 523:
			if(!c.canWalk)
				c.canWalk = true;
			c.getPA().setSidebarInterfaces(c, true);
			sendNpcChat1("Goodluck, "+c.playerName+".", 2244, "Lumbridge Guide");
			c.sendMessage("Good luck, "+c.playerName+"!");
			c.nextChat = 0;
			break;
		case 1699:
			sendNpcChat1("You don't have enough vote points!", c.npcType, "Vote Master");
			c.nextChat = 0;
			break;
		case 1700:
			sendNpcChat1("There's 500000 coins!", c.npcType, "Vote Master");
			c.nextChat = 0;
			break;
		case 73:
			sendNpcChat1("Hello " + Misc.capitalize(c.playerName) + ", take a look at my herblore supplies!", c.talkingNpc, "Kaqemeex");
			c.nextChat = 74;
			break;
		case 74:
			c.getShops().openShop(12);
			break;
	/*
	 * Banker dialogues
	 */
		case 1003:
		c.sendMessage("The banker opens up your bank account.");
		c.getPA().openUpBank();
		c.nextChat = 0;
		c.dialogueAction = -1;
		c.teleAction = -1;
		break;

	/*
	 * Zaff dialogues
	 */
		case 1004:
		sendNpcChat1("Hello "+Misc.capitalize(c.playerName)+"!", c.talkingNpc, "Zaff");
		c.nextChat = 1005;
		break;

	/*
	 * Thessalia dialogues
	 */

	/*
	 * Random.
	 */
		}
	}
	
	

	
	/*
	 * Information Box
	 */
	
	public void sendStartInfo(String text, String text1, String text2, String text3, String title) {
		c.getPA().sendFrame126(title, 6180);
		c.getPA().sendFrame126(text, 6181);
		c.getPA().sendFrame126(text1, 6182);
		c.getPA().sendFrame126(text2, 6183);
		c.getPA().sendFrame126(text3, 6184);
		c.getPA().sendFrame164(6179);
	}
	
	/*
	 * Item chat
	 */
	
	public void sendItemChat1(String header, String one, int item, int zoom) {
		c.getPA().sendFrame246(4883, zoom, item);
		c.getPA().sendFrame126(header, 4884);
		c.getPA().sendFrame126(one, 4885);
		c.getPA().sendFrame164(4882);
	}

	public void sendItemChat2(String header, String one, String two, int item, int zoom) {
		c.getPA().sendFrame246(4888, zoom, item);
		c.getPA().sendFrame126(header, 4889);
		c.getPA().sendFrame126(one, 4890);
		c.getPA().sendFrame126(two, 4891);
		c.getPA().sendFrame164(4887);
	}

	public void sendItemChat3(String header, String one, String two, String three, int item, int zoom) {
		c.getPA().sendFrame246(4894, zoom, item);
		c.getPA().sendFrame126(header, 4895);
		c.getPA().sendFrame126(one, 4896);
		c.getPA().sendFrame126(two, 4897);
		c.getPA().sendFrame126(three, 4898);
		c.getPA().sendFrame164(4893);
	}

	public void sendItemChat4(String header, String one, String two, String three, String four, int item, int zoom) {
		c.getPA().sendFrame246(4901, zoom, item);
		c.getPA().sendFrame126(header, 4902);
		c.getPA().sendFrame126(one, 4903);
		c.getPA().sendFrame126(two, 4904);
		c.getPA().sendFrame126(three, 4905);
		c.getPA().sendFrame126(four, 4906);
		c.getPA().sendFrame164(4900);
	}

	/*
	 * Statements
	 */
	
	public void sendStatement2(String s, String s1) {
		c.getPA().sendFrame126(s, 360);
		c.getPA().sendFrame126(s1, 361);
		c.getPA().sendFrame126("Click here to continue", 362);
		c.getPA().sendFrame164(359);
	}
	
	@SuppressWarnings("unused")
	private void sendStatement3(String s, String s1, String s2) {
		c.getPA().sendFrame126(s, 364);
		c.getPA().sendFrame126(s1, 365);
		c.getPA().sendFrame126(s2, 366);
		c.getPA().sendFrame126("Click here to continue", 367);
		c.getPA().sendFrame164(363);
	}
	
	public void sendStatement4(String s, String s1, String s2, String s3) {
		c.getPA().sendFrame126(s, 369);
		c.getPA().sendFrame126(s1, 370);
		c.getPA().sendFrame126(s2, 371);
		c.getPA().sendFrame126(s3, 372);
		c.getPA().sendFrame126("Click here to continue", 373);
		c.getPA().sendFrame164(368);
	}
	
	@SuppressWarnings("unused")
	private void sendStatement5(String s, String s1, String s2, String s3, String s4) {
		c.getPA().sendFrame126(s, 375);
		c.getPA().sendFrame126(s1, 376);
		c.getPA().sendFrame126(s2, 377);
		c.getPA().sendFrame126(s3, 378);
		c.getPA().sendFrame126(s4, 379);
		c.getPA().sendFrame126("Click here to continue", 380);
		c.getPA().sendFrame164(374);
	}
	
	/*
	 * Npc Chatting
	 */
	
	public void sendNpcChat1(String s, int ChatNpc, String name) {
		c.getPA().sendFrame200(4883, 591);
		c.getPA().sendFrame126(name, 4884);
		c.getPA().sendFrame126(s, 4885);
		c.getPA().sendFrame75(ChatNpc, 4883);
		c.getPA().sendFrame164(4882);
	}
	
	public void sendNpcChat2(String s, String s1, int ChatNpc, String name) {
		c.getPA().sendFrame200(4888, 591);
		c.getPA().sendFrame126(name, 4889);
		c.getPA().sendFrame126(s, 4890);
		c.getPA().sendFrame126(s1, 4891);
		c.getPA().sendFrame75(ChatNpc, 4888);
		c.getPA().sendFrame164(4887);
	}

	public void sendNpcChat3(String s, String s1, String s2, int ChatNpc, String name) {
		c.getPA().sendFrame200(4894, 591);
		c.getPA().sendFrame126(name, 4895);
		c.getPA().sendFrame126(s, 4896);
		c.getPA().sendFrame126(s1, 4897);
		c.getPA().sendFrame126(s2, 4898);
		c.getPA().sendFrame75(ChatNpc, 4894);
		c.getPA().sendFrame164(4893);
	}
	
	public void sendNpcChat4(String s, String s1, String s2, String s3, int ChatNpc, String name) {
		c.getPA().sendFrame200(4901, 591);
		c.getPA().sendFrame126(name, 4902);
		c.getPA().sendFrame126(s, 4903);
		c.getPA().sendFrame126(s1, 4904);
		c.getPA().sendFrame126(s2, 4905);
		c.getPA().sendFrame126(s3, 4906);
		c.getPA().sendFrame75(ChatNpc, 4901);
		c.getPA().sendFrame164(4900);
	}
	
	/*
	 * Player Chating Back
	 */
	
	public void sendPlayerChat1(String s) {
		c.getPA().sendFrame200(969, 591);
		c.getPA().sendFrame126(Misc.capitalize(c.playerName), 970);
		c.getPA().sendFrame126(s, 971);
		c.getPA().sendFrame185(969);
		c.getPA().sendFrame164(968);
	}
	
	public void sendPlayerChat2(String s, String s1) {
		c.getPA().sendFrame200(974, 591);
		c.getPA().sendFrame126(Misc.capitalize(c.playerName), 975);
		c.getPA().sendFrame126(s, 976);
		c.getPA().sendFrame126(s1, 977);
		c.getPA().sendFrame185(974);
		c.getPA().sendFrame164(973);
	}
	
	public void sendPlayerChat3(String s, String s1, String s2) {
		c.getPA().sendFrame200(980, 591);
		c.getPA().sendFrame126(Misc.capitalize(c.playerName), 981);
		c.getPA().sendFrame126(s, 982);
		c.getPA().sendFrame126(s1, 983);
		c.getPA().sendFrame126(s2, 984);
		c.getPA().sendFrame185(980);
		c.getPA().sendFrame164(979);
	}
	
	@SuppressWarnings("unused")
	private void sendPlayerChat4(String s, String s1, String s2, String s3) {
		c.getPA().sendFrame200(987, 591);
		c.getPA().sendFrame126(Misc.capitalize(c.playerName), 988);
		c.getPA().sendFrame126(s, 989);
		c.getPA().sendFrame126(s1, 990);
		c.getPA().sendFrame126(s2, 991);
		c.getPA().sendFrame126(s3, 992);
		c.getPA().sendFrame185(987);
		c.getPA().sendFrame164(986);
	}
	
	public String npcName(){
	String npcName;
	if(c.talkingNpc <1 ){
	npcName = "";}
	else {
	npcName = NPCHandler.NpcList[c.talkingNpc].npcName;}
	return npcName;
	}
       
     public void sendPlayerChat(String[] lineamount, int emote){
     switch(lineamount.length){
     case 1 :
                sendPlayerChat(lineamount[0], emote);
                break;
            case 2 :
                sendPlayerChat(lineamount[0], lineamount[1], emote);
                break;
            case 3 :
                sendPlayerChat(lineamount[0], lineamount[1], lineamount[2], emote);
                break;
            case 4 :
                sendPlayerChat(lineamount[0], lineamount[1], lineamount[2], lineamount[3], emote);
                break;
     }
     }
	 public void sendOption(String[] lineamount){
	 switch(lineamount.length){
     case 2 :
                sendOption2(lineamount[0], lineamount[1]);
                break;
            case 3 :
                sendOption3(lineamount[0], lineamount[1], lineamount[2]);
                break;
            case 4 :
                sendOption4(lineamount[0], lineamount[1], lineamount[2], lineamount[3]);
                break;
            case 5 :
                sendOption5(lineamount[0], lineamount[1], lineamount[2], lineamount[3], lineamount[4]);
                break;
     }
	 }
	
	public void sendNpcChat(String[] lineamount, int emote, String name){
	switch(lineamount.length){
            case 1 :
                sendNpcChat(lineamount[0], emote, name);
                break;
            case 2 :
                sendNpcChat(lineamount[0], lineamount[1], emote, name);
                break;
            case 3 :
                sendNpcChat(lineamount[0], lineamount[1], lineamount[2], emote, name);
                break;
            case 4 :
                sendNpcChat(lineamount[0], lineamount[1], lineamount[2], lineamount[3], emote, name);
                break;
      
     }
	}

	public void sendOption(String s) {
		c.getPA().sendFrame126("Select an Option", 2470);
	 	c.getPA().sendFrame126(s, 2471);
		c.getPA().sendFrame126("Click here to continue", 2473);
		c.getPA().sendFrame164(13758);
	}	
	
	public void sendOption2(String s, String s1) {
		c.getPA().sendFrame126("Select an Option", 2460);
		c.getPA().sendFrame126(s, 2461);
		c.getPA().sendFrame126(s1, 2462);
		c.getPA().sendFrame164(2459);
	}
	
	public void sendOption3(String s, String s1, String s2) {
		c.getPA().sendFrame126("Select an Option", 2470);
		c.getPA().sendFrame126(s, 2471);
		c.getPA().sendFrame126(s1, 2472);
		c.getPA().sendFrame126(s2, 2473);
		c.getPA().sendFrame164(2469);
	}
	
	public void sendOption4(String s, String s1, String s2, String s3) {
		c.getPA().sendFrame126("Select an Option", 2481);
		c.getPA().sendFrame126(s, 2482);
		c.getPA().sendFrame126(s1, 2483);
		c.getPA().sendFrame126(s2, 2484);
		c.getPA().sendFrame126(s3, 2485);
		c.getPA().sendFrame164(2480);
	}
	
	public void sendOption5(String s, String s1, String s2, String s3, String s4) {
		c.getPA().sendFrame126("Select an Option", 2493);
		c.getPA().sendFrame126(s, 2494);
		c.getPA().sendFrame126(s1, 2495);
		c.getPA().sendFrame126(s2, 2496);
		c.getPA().sendFrame126(s3, 2497);
		c.getPA().sendFrame126(s4, 2498);
		c.getPA().sendFrame164(2492);
	}

	public void sendStatement(String s) { // 1 line click here to continue chat box interface
		c.nextChat = 0;
		c.getPA().sendFrame126(s, 357);
		c.getPA().sendFrame126("Click here to continue", 358);
		c.getPA().sendFrame164(356);
	}

	public void sendNpcChat(String s, int emote, String name) {
		c.getPA().sendFrame200(4883, emote);
		c.getPA().sendFrame126(name, 4884);
		c.getPA().sendFrame126(s, 4885);
		c.getPA().sendFrame75(c.npcType, 4883);
		c.getPA().sendFrame164(4882);
	}
	
	public void sendNpcChat(String s, String s1, int emote, String name) {
		c.getPA().sendFrame200(4888, emote);
		c.getPA().sendFrame126(name, 4889);
		c.getPA().sendFrame126(s, 4890);
		c.getPA().sendFrame126(s1, 4891);
		c.getPA().sendFrame75(c.npcType, 4888);
		c.getPA().sendFrame164(4887);
	}
	
	public void sendNpcChat2(String s, String s1, int emote) {
		c.getPA().sendFrame200(4888, emote);
		c.getPA().sendFrame126("Skill Master", 4889);
		c.getPA().sendFrame126(s, 4890);
		c.getPA().sendFrame126(s1, 4891);
		c.getPA().sendFrame75(c.npcType, 4888);
		c.getPA().sendFrame164(4887);
	}

	public void sendNpcChat(String s, String s1, String s2, int emote, String name) {
		c.getPA().sendFrame200(4894, emote);
		c.getPA().sendFrame126(name, 4895);
		c.getPA().sendFrame126(s, 4896);
		c.getPA().sendFrame126(s1, 4897);
		c.getPA().sendFrame126(s2, 4898);
		c.getPA().sendFrame75(c.npcType, 4894);
		c.getPA().sendFrame164(4893);
	}
	
	public void sendNpcChat(String s, String s1, String s2, String s3, int emote, String name) {
		c.getPA().sendFrame200(4901, emote);
		c.getPA().sendFrame126(name, 4902);
		c.getPA().sendFrame126(s, 4903);
		c.getPA().sendFrame126(s1, 4904);
		c.getPA().sendFrame126(s2, 4905);
		c.getPA().sendFrame126(s3, 4906);
		c.getPA().sendFrame75(c.npcType, 4901);
		c.getPA().sendFrame164(4900);
	}
	
	public void sendPlayerChat(String s, int emote) {
		c.getPA().sendFrame200(969, emote);
		c.getPA().sendFrame126(c.playerName, 970);
		c.getPA().sendFrame126(s, 971);
		c.getPA().sendFrame185(969);
		c.getPA().sendFrame164(968);
	}
	
	private void sendPlayerChat(String s, String s1, int emote) {
		c.getPA().sendFrame200(974, emote);
		c.getPA().sendFrame126(c.playerName, 975);
		c.getPA().sendFrame126(s, 976);
		c.getPA().sendFrame126(s1, 977);
		c.getPA().sendFrame185(974);
		c.getPA().sendFrame164(973);
	}
	
	private void sendPlayerChat(String s, String s1, String s2, int emote) {
		c.getPA().sendFrame200(980, emote);
		c.getPA().sendFrame126(c.playerName, 981);
		c.getPA().sendFrame126(s, 982);
		c.getPA().sendFrame126(s1, 983);
		c.getPA().sendFrame126(s2, 984);
		c.getPA().sendFrame185(980);
		c.getPA().sendFrame164(979);
	}
	
	private void sendPlayerChat(String s, String s1, String s2, String s3, int emote) {
		c.getPA().sendFrame200(987, emote);
		c.getPA().sendFrame126(c.playerName, 988);
		c.getPA().sendFrame126(s, 989);
		c.getPA().sendFrame126(s1, 990);
		c.getPA().sendFrame126(s2, 991);
		c.getPA().sendFrame126(s3, 992);
		c.getPA().sendFrame185(987);
		c.getPA().sendFrame164(986);
	}
	
	public final int 
HAPPY = 588, 
CALM = 589, 
CALM_CONTINUED = 590, 
CONTENT = 591, 
EVIL = 592, 
EVIL_CONTINUED = 593, 
DELIGHTED_EVIL = 594, 
ANNOYED = 595, 
DISTRESSED = 596, 
DISTRESSED_CONTINUED = 597, 
NEAR_TEARS = 598,
SAD = 599, 
DISORIENTED_LEFT = 600, 
DISORIENTED_RIGHT = 601, 
UNINTERESTED = 602, 
SLEEPY = 603, 
PLAIN_EVIL = 604, 
LAUGHING = 605, 
LONGER_LAUGHING = 606, 
LONGER_LAUGHING_2 = 607, 
LAUGHING_2 = 608, 
EVIL_LAUGH_SHORT = 609, 
SLIGHTLY_SAD = 610, 
VERY_SAD = 611, 
OTHER = 612, 
NEAR_TEARS_2 = 613, 
ANGRY_1 = 614, 
ANGRY_2 = 615, 
ANGRY_3 = 616, 
ANGRY_4 = 617;
}
