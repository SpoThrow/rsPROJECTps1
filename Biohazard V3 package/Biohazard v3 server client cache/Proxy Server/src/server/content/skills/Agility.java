package server.content.skills;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.players.Client;
import server.game.players.Player;
/**
 * Agility.java
 * 
 * @author Acquittal
 *
 *
 **/
 
public class Agility {

	public Client client;
	public int agtimer = 10;
	public boolean bonus = false;

	public Agility(Client c) {
		client = c;
	}	
	
	public void brimhavenMonkeyBars(Client c, String Object, int level, int x, int y, int a, int b, int xp)
	{
		if (c.playerLevel[Player.playerAgility] < level) {
			c.sendMessage("You need a Agility level of "+ level +" to pass this " + Object + ".");
			return;
		}
		if (c.absX == a && c.absY == b) { 
			c.getPA().walkTo3(x, y);
			
			c.getPA().addSkillXP(xp, Player.playerAgility);
			
			c.getPA().refreshSkill(Player.playerAgility);
		}
	}
	
	/*
	 * Wilderness course
	 */
	
	public void wildernessEntrance(Client c, String Object, int level, int x, int y, int a, int b, int xp) {
		if (c.playerLevel[Player.playerAgility] < level) {
			c.sendMessage("You need a Agility level of "+ level +" to pass this " + Object + ".");
			return;
		}
		if (c.absX == a && c.absY == b) { 
			c.getPA().walkTo3(x, y);
			
			c.getPA().addSkillXP(xp, Player.playerAgility);
			c.getPA().refreshSkill(Player.playerAgility);
			
		}
	}
	
	public void doWildernessEntrance(final Client c) {
		if (System.currentTimeMillis() - c.foodDelay < 2000) {
			return;
		}
			c.stopMovement();
			c.freezeTimer = 16;
			c.playerWalkIndex = 762;
			c.updateRequired = true;
			c.appearanceUpdateRequired = true;
			c.getAgil().wildernessEntrance(c, "Door", 1, 0, +15, 2998, 3917, 40 * Config.AGILITY_EXPERIENCE);
			c.foodDelay = System.currentTimeMillis();
			CycleEventHandler.addEvent(c, new CycleEvent() {
				@Override
				public void execute(CycleEventContainer container) {
					c.playerStandIndex = 0x328;
					c.playerTurnIndex = 0x337;
					c.playerWalkIndex = 0x333;
					c.playerTurn180Index = 0x334;
					c.playerTurn90CWIndex = 0x335;
					c.playerTurn90CCWIndex = 0x336;
					c.playerRunIndex = 0x338;
					c.updateRequired = true;
					c.appearanceUpdateRequired = true;
					container.stop();
				}
				@Override
				public void stop() {
				}
			}, 14);
	}
	
	
	/*
	 * Gnome course
	 */
	
	public void gnomeLog(Client c, String Object, int level, int x, int y, int a, int b, int xp)
	{
		if (c.playerLevel[Player.playerAgility] < level) {
			c.sendMessage("You need a Agility level of "+ level +" to pass this " + Object + ".");
			return;
		}
		if (c.absX == a && c.absY == b) { 
			c.getPA().walkTo3(x, y);
			
			c.getPA().addSkillXP(xp, Player.playerAgility);
			c.getPA().refreshSkill(Player.playerAgility);
			
		}
	}

	public void gnomeNet(Client c, String net, int level, int a, int b, int h, int x, int y, int emote, int xp)
	{
		if (c.playerLevel[Player.playerAgility] < level) {
			c.sendMessage("You need a Agility level of "+ level +" to pass this " + net + ".");
			return;
		}
		if (c.absX == a && c.absY == b) {
			c.teleportToX = x;
			c.teleportToY = y;
			c.heightLevel = h;
			c.updateRequired = true;
			c.appearanceUpdateRequired = true;
			
			c.getPA().addSkillXP(xp, Player.playerAgility);
			c.getPA().refreshSkill(Player.playerAgility);
			
			c.turnPlayerTo(c.getX()- 1, c.getY());
		}
	}

	public void gnomeBranch(Client c, String branch, int level, int x, int y, int h, int a, int b, int emote, int xp)
	{
		if (c.playerLevel[Player.playerAgility] < level) {
			c.sendMessage("You need a Agility level of "+ level +" to pass this " + branch + ".");
			return;
		}
		if (c.absX == a && c.absY == b) {
			c.teleportToX = x;
			c.teleportToY = y;
			c.heightLevel = h;
			
			c.getPA().addSkillXP(xp, Player.playerAgility);
			c.getPA().refreshSkill(Player.playerAgility);
			
		}
	}



	public void gnomePipe(Client c, String pipe, int level, int a, int b, int x, int y, int add, int amount, int xp)
	{

		if (c.playerLevel[Player.playerAgility] < level) {
			c.sendMessage("You need a Agility level of "+ level +" to pass this " + pipe + ".");
			return;
		}
		if (c.absX == a && c.absY == b)
		{
			if (bonus && c.ag1 == 1 && c.ag2 >= 1 
					&& c.ag3 >= 1 && c.ag4 >= 1 
					&& c.ag5 >= 1 && c.ag6 >= 1)
			{
				c.getPA().walkTo3(x, y);
				c.turnPlayerTo(c.getX(), c.getY()+ 1);
				
				c.getPA().addSkillXP(360, Player.playerAgility);
				c.getPA().refreshSkill(Player.playerAgility);
				
				c.getItems().addItem(add, amount);
				c.sendMessage("Congratulations, you have been awarded for completing the course, you...");
				c.sendMessage("... receive "+ amount +" tickets, and 360 experience!");
				bonus = false;
				c.ag1 = 0;
				c.ag2 = 0;
				c.ag3 = 0;
				c.ag4 = 0;
				c.ag5 = 0;
				c.ag6 = 0;
			} else {
				c.getPA().walkTo3(x, y);
				
				c.getPA().addSkillXP(xp, Player.playerAgility);
				c.getPA().refreshSkill(Player.playerAgility);
				
				c.getItems().addItem(add, 1);
				c.turnPlayerTo(c.getX(), c.getY()+ 1);
				bonus = false;
				c.sendMessage("You did not complete the full course, you only receive one agility ticket.");
				c.ag1 = 0;
				c.ag2 = 0;
				c.ag3 = 0;
				c.ag4 = 0;
				c.ag5 = 0;
				c.ag6 = 0;
			}
		}
	}
	
	public void doGnomeLog(final Client c) {
			if (System.currentTimeMillis() - c.foodDelay < 2000) {
				return;
			}
				c.stopMovement();
				c.freezeTimer = 8;
				c.playerWalkIndex = 762;
				c.updateRequired = true;
				c.appearanceUpdateRequired = true;
				c.getAgil().gnomeLog(c, "Log", 1, 0, -7, 2474, 3436, 8 * Config.AGILITY_EXPERIENCE);
				c.ag1 = 1;
				c.foodDelay = System.currentTimeMillis();
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						c.playerStandIndex = 0x328;
						c.playerTurnIndex = 0x337;
						c.playerWalkIndex = 0x333;
						c.playerTurn180Index = 0x334;
						c.playerTurn90CWIndex = 0x335;
						c.playerTurn90CCWIndex = 0x336;
						c.playerRunIndex = 0x338;
						c.updateRequired = true;
						c.appearanceUpdateRequired = true;
						container.stop();
					}
					@Override
					public void stop() {
					}
				}, 6);
	}
	
	public void doGnomeNet1(final Client c) {
		if (System.currentTimeMillis() - c.foodDelay < 2000) {
		return;
		}
			c.stopMovement();
			c.startAnimation(828);
			c.getAgil().gnomeNet(c, "Net", 1, 2471, 3426, 1, 2471, 3424, 828, 8 * Config.AGILITY_EXPERIENCE);
			c.getAgil().gnomeNet(c, "Net", 1, 2472, 3426, 1, 2472, 3424, 828, 8 * Config.AGILITY_EXPERIENCE);
			c.getAgil().gnomeNet(c, "Net", 1, 2473, 3426, 1, 2473, 3424, 828, 8 * Config.AGILITY_EXPERIENCE);
			c.getAgil().gnomeNet(c, "Net", 1, 2474, 3426, 1, 2474, 3424, 828, 8 * Config.AGILITY_EXPERIENCE);
			c.getAgil().gnomeNet(c, "Net", 1, 2475, 3426, 1, 2475, 3424, 828,8 * Config.AGILITY_EXPERIENCE);
			c.getAgil().gnomeNet(c, "Net", 1, 2476, 3426, 1, 2476, 3424, 828, 8 * Config.AGILITY_EXPERIENCE);
			c.ag2 = 1;
			c.foodDelay = System.currentTimeMillis();
	}
	
	public void doGnomeBranch1(final Client c) {
		if (System.currentTimeMillis() - c.foodDelay < 2000) {
			return;
			}
				c.stopMovement();
				c.startAnimation(828);
				c.getAgil().gnomeBranch(c, "Branch", 1, 2473, 3420, 2, 2473, 3423, 828, 5 * Config.AGILITY_EXPERIENCE);
				c.getAgil().gnomeBranch(c, "Branch", 1, 2473, 3420, 2, 2474, 3422, 828, 5 * Config.AGILITY_EXPERIENCE);
				c.getAgil().gnomeBranch(c, "Branch", 1, 2473, 3420, 2, 2472, 3422, 828, 5 * Config.AGILITY_EXPERIENCE);
				c.ag3 = 1;
				c.foodDelay = System.currentTimeMillis();
	}
	
	public void doGnomeBranch2(final Client c) {
		if (System.currentTimeMillis() - c.foodDelay < 2000) {
			return;
			}
				c.stopMovement();
				c.startAnimation(828);
				c.sendMessage("You slip and fall down.");
				c.getAgil().gnomeBranch(c, "Branch", 1, 2486, 3420, 0, 2485, 3419, 828, 5 * Config.AGILITY_EXPERIENCE);
				c.getAgil().gnomeBranch(c, "Branch", 1, 2486, 3420, 0, 2485, 3420, 828, 5 * Config.AGILITY_EXPERIENCE);
				c.getAgil().gnomeBranch(c, "Branch", 1, 2486, 3420, 0, 2486, 3420, 828, 5 * Config.AGILITY_EXPERIENCE);
				c.ag5 = 1;
				c.foodDelay = System.currentTimeMillis();
	}
	
	public void doGnomeNet2(final Client c) {
		if (System.currentTimeMillis() - c.foodDelay < 2000) {
			return;
			}
				c.stopMovement();
				c.startAnimation(828);
				c.getAgil().gnomeNet(c, "Net", 1, 2483, 3425, 0, 2483, 3427, 828, 8 * Config.AGILITY_EXPERIENCE);
				c.getAgil().gnomeNet(c, "Net", 1, 2484, 3425, 0, 2484, 3427, 828, 8 * Config.AGILITY_EXPERIENCE);
				c.getAgil().gnomeNet(c, "Net", 1, 2485, 3425, 0, 2485, 3427, 828, 8 * Config.AGILITY_EXPERIENCE);
				c.getAgil().gnomeNet(c, "Net", 1, 2486, 3425, 0, 2486, 3427, 828, 8 * Config.AGILITY_EXPERIENCE);
				c.getAgil().gnomeNet(c, "Net", 1, 2487, 3425, 0, 2487, 3427, 828, 8 * Config.AGILITY_EXPERIENCE);
				c.getAgil().gnomeNet(c, "Net", 1, 2488, 3425, 0, 2488, 3427, 828, 8 * Config.AGILITY_EXPERIENCE);
				c.ag6 = 1;
				c.getAgil().bonus = true;
				c.foodDelay = System.currentTimeMillis();
	}
	
	public void doGnomeRope(final Client c) {
				if (System.currentTimeMillis() - c.foodDelay < 2000) {
					return;
					}
				c.stopMovement();
				c.freezeTimer = 8;
				c.playerWalkIndex = 762;
				c.updateRequired = true;
				c.appearanceUpdateRequired = true;
				c.getAgil().gnomeLog(c, "Log", 1, +6, 0, 2477, 3420, 7 * Config.AGILITY_EXPERIENCE);
				c.ag4 = 1;
				c.foodDelay = System.currentTimeMillis();
				CycleEventHandler.addEvent(c, new CycleEvent() {
					@Override
					public void execute(CycleEventContainer container) {
						c.playerStandIndex = 0x328;
						c.playerTurnIndex = 0x337;
						c.playerWalkIndex = 0x333;
						c.playerTurn180Index = 0x334;
						c.playerTurn90CWIndex = 0x335;
						c.playerTurn90CCWIndex = 0x336;
						c.playerRunIndex = 0x338;
						c.updateRequired = true;
						c.appearanceUpdateRequired = true;
						container.stop();
					}
					@Override
					public void stop() {
					}
				}, 6);
	}
		
	public void doGnomePipe1(final Client c) {
			if (System.currentTimeMillis() - c.foodDelay < 2000) {
				return;
			}
			c.stopMovement();
			c.freezeTimer = 8;
			c.playerWalkIndex = 746;
			c.updateRequired = true;
			c.appearanceUpdateRequired = true;
			c.getAgil().gnomePipe(c, "Pipe", 1, 2484, 3430, 0, +7, 2996, 10, 47 * Config.AGILITY_EXPERIENCE);
			c.foodDelay = System.currentTimeMillis();
			CycleEventHandler.addEvent(c, new CycleEvent() {
				@Override
				public void execute(CycleEventContainer container) {
					c.playerStandIndex = 0x328;
					c.playerTurnIndex = 0x337;
					c.playerWalkIndex = 0x333;
					c.playerTurn180Index = 0x334;
					c.playerTurn90CWIndex = 0x335;
					c.playerTurn90CCWIndex = 0x336;
					c.playerRunIndex = 0x338;
					c.updateRequired = true;
					c.appearanceUpdateRequired = true;
					c.startAnimation(748);
					container.stop();
				}
				@Override
				public void stop() {
				}
			}, 7);
	}
	
	public void doGnomePipe2(final Client c) {
		if (System.currentTimeMillis() - c.foodDelay < 2000) {
			return;
		}
		c.stopMovement();
		c.freezeTimer = 8;
		c.playerWalkIndex = 746;
		c.updateRequired = true;
		c.appearanceUpdateRequired = true;
		c.getAgil().gnomePipe(c, "Pipe", 1, 2487, 3430, 0, +7, 2996, 10, 47 * Config.AGILITY_EXPERIENCE);
		c.foodDelay = System.currentTimeMillis();
		CycleEventHandler.addEvent(c, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				c.playerStandIndex = 0x328;
				c.playerTurnIndex = 0x337;
				c.playerWalkIndex = 0x333;
				c.playerTurn180Index = 0x334;
				c.playerTurn90CWIndex = 0x335;
				c.playerTurn90CCWIndex = 0x336;
				c.playerRunIndex = 0x338;
				c.updateRequired = true;
				c.appearanceUpdateRequired = true;
				c.startAnimation(748);
				container.stop();
			}
			@Override
			public void stop() {
			}
		}, 7);
	}
	

	/*
	 * Rewards.
	 */

	public void gnomeTicketCounter(Client c, String ticket, int remove, int amount, int xp) {
		
		if (c.getItems().playerHasItem(2996, 1)) {
			if (ticket.equals("1"))
			{
				c.getItems().deleteItem2(remove, amount);
				c.getPA().addSkillXP(xp, Player.playerAgility);
				c.getPA().refreshSkill(Player.playerAgility);
				
				c.sendMessage("You've recieved "+ xp + " Agility Experience!");
				return;
			}
		}
		if (c.getItems().playerHasItem(2996, 10)) {
			if (ticket.equals("10"))
			{
				c.getItems().deleteItem2(remove, amount);
				c.getPA().addSkillXP(xp, Player.playerAgility);
				c.getPA().refreshSkill(Player.playerAgility);
				
				c.sendMessage("You've recieved "+ xp + " Agility Experience!");
				return;
			}
		}
		if (c.getItems().playerHasItem(2996, 25)) {
			if (ticket.equals("25"))
			{
				c.getItems().deleteItem2(remove, amount);
				c.getPA().addSkillXP(xp, Player.playerAgility);
				c.getPA().refreshSkill(Player.playerAgility);
				
				c.sendMessage("You've recieved "+ xp + " Agility Experience!");
				return;
			}
		}
		if (c.getItems().playerHasItem(2996, 100)) {
			if (ticket.equals("100"))
			{
				c.getItems().deleteItem2(remove, amount);
				c.getPA().addSkillXP(xp, Player.playerAgility);
				c.getPA().refreshSkill(Player.playerAgility);
				
				c.sendMessage("You've recieved "+ xp + " Agility Experience!");
				return;
			}
		}
		if (c.getItems().playerHasItem(2996, 1000)) {
			if (ticket.equals("1000"))
			{
				c.getItems().deleteItem2(remove, amount);
				c.getPA().addSkillXP(xp, Player.playerAgility);
				c.getPA().refreshSkill(Player.playerAgility);
				
				c.sendMessage("You've recieved "+ xp + " Agility Experience!");
				return;
			}
		}
		else
		{
			c.sendMessage("You need more agility tickets to get this reward!");
			
			return;
		}
		
	}
}