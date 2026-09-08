package server.content.quests;

import server.game.players.Client;

/*
 * @author Liam Aka Insidia X (R-S Name).
 */

public class RuneMysteries {

	public static void showInformation(Client c) {
		for (int i = 8144; i < 8195; i++) {
			c.getPA().sendFrame126("", i);
		
		}
		c.getPA().sendFrame126("@dre@Rune Mysteries", 8144);
		c.getPA().sendFrame126("", 8145);
		if(c.RuneMysteries == 0) {
			c.getPA().sendFrame126("Rune Mysteries", 8144);
			c.getPA().sendFrame126("Go to the second floor of Lumbridge Castle,", 8147);
			c.getPA().sendFrame126("and talk to the Duke Horacio.", 8148);
			c.getPA().sendFrame126("", 8149);
			c.getPA().sendFrame126("There are no requirments.", 8150);
		
		} else if(c.RuneMysteries == 1) {
			c.getPA().sendFrame126("Rune Mysteries", 8144);
			c.getPA().sendFrame126("@str@I've talked to Duke Horacio, and he gave me.", 8147);
			c.getPA().sendFrame126("a Talisman. :", 8148);
			c.getPA().sendFrame126("He wants me to proceed to the Widards Tower to find, :", 8149);
			c.getPA().sendFrame126("Wizard Sedridor located in the basement of the Wizards Tower.", 8150);
			
		} else if(c.RuneMysteries == 2) {
			c.getPA().sendFrame126("Rune Mysteries", 8144);
			c.getPA().sendFrame126("@str@I've talked to Wizard Sedridor,he gave me a package to give to", 8147);
			c.getPA().sendFrame126("Aubury in the Rune Shop, located in the south-east of Varrock. ", 8148);
			
			
		} else if(c.RuneMysteries == 3) {
			c.getPA().sendFrame126("Rune Mysteries", 8144);
			c.getPA().sendFrame126("@str@I talked to Aubury,.", 8147);
			c.getPA().sendFrame126("@str@He Gave me some Research papers for Sedridor.", 8148);
			c.getPA().sendFrame126("@str@I must take them to him..", 8150);
		
		} else if(c.RuneMysteries == 4) {
			c.getPA().sendFrame126("Rune Mysteries", 8144);
			c.getPA().sendFrame126("I took the Research papers to Sedior, and completed the quest.", 8145);
			c.getPA().sendFrame126("@red@QUEST COMPLETE", 8146);
			c.getPA().sendFrame126("As a reward, I gained Access to the rune mines, and an Air Talisman", 8146);
		}
		c.getPA().showInterface(8134);
	}
}
	