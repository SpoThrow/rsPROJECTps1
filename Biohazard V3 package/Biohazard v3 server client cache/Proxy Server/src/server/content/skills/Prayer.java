package server.content.skills;


import java.util.HashMap;

import server.Config;
import server.game.minigames.randomevents.Zombie;
import server.game.players.Client;
import core.util.Misc;

/**
 *
 *Prayer
 *@author Acquittal
 *
 */
public class Prayer {
       
        enum Bones {
                REGULAR(526, 5, "Bones"),
                BIG(532, 15, "Big Bones"),
                BABY_DRAG(534, 30, "Baby Dragon Bones"),
                DRAG(536, 72, "Dragon Bones"),
                DAG(6729, 125, "Dagannoth Bones"),
                BAT(530, 5, "Bat Bones"),
                WOLF(2859, 5, "Wolf Bones"),
                MONKEY(3179, 5, "Monkey Bones"),
                JOGRE(3125, 15, "Jogre Bones"),
                RAURG(4832, 150, "Raurg Bones");

               
                static HashMap<Integer, Bones> BoneInfo = new HashMap<Integer, Bones>();

                int ID, XP;
                String Name;

                static {
                        for (final Bones b : BoneInfo.values())
                                Bones.BoneInfo.put(b.XP, b);
                }

                /**
                 *
                 * @param ID
                 * @param XP
                 * @param Name
                 */
                Bones(final int ID, final int XP, final String Name) {
                        this.ID = ID;
                        this.XP = XP;
                        this.Name = Name;
                }

                /**
                 *
                 * @return
                 */
                int getID() {
                        return ID;
                }

                /**
                 *
                 * @return
                 */
                int getXP() {
                        return XP;
                }

                /**
                 *
                 * @return
                 */
                String getName() {
                        return Name;
                }

        }

        /**
         *
         * @param ID The bone Id
         * @return
         */
        public static boolean IsABone(Client c, int ID) {
                for (final Bones b : Bones.values()) {
                        if (c.getItems().playerHasItem(b.getID(), 1)) {
                                if (b.getID() == ID) {
                                        return true;
                                }
                        }
                }
                return false;
        }

        /**
         *
         * @param ID The bone Id
         */
        public static void buryBone(Client c, int ID) {
                if (System.currentTimeMillis() - c.buryDelay > 1500) {
                        for (final Bones b : Bones.values()) {
                                if (ID == b.getID()) {
                                        int doubleExperience = Misc.random(20);
                                        if (doubleExperience >= 1) {
                                                c.getItems().deleteItem(ID, 1);
                                                c.sendMessage("You bury the bones.");
                                                
                                                c.getPA().addSkillXP(b.getXP() * Config.PRAYER_EXPERIENCE, 5);
                                                
                                                c.buryDelay = System.currentTimeMillis();
                                                c.startAnimation(827);
                                				if(Misc.random(250) == 0) {
                                					Zombie.spawnZombie(c);
                                				}
                                        } else if (doubleExperience == 0) {
                                                c.getItems().deleteItem(ID, 1);
                                                c.sendMessage("You bury the bones.");
                                                
                                                c.getPA().addSkillXP(b.getXP() * 2 * Config.PRAYER_EXPERIENCE, 5);
                                                
                                                c.buryDelay = System.currentTimeMillis();
                                                c.startAnimation(827);
                                				if(Misc.random(250) == 0) {
                                					Zombie.spawnZombie(c);
                                				}
                                        }
                                }
                        }
                }
        }

        /**
         *
         * @param ID the bone Id
         */
        public static void bonesOnAltar(Client c, int ID) {
        int     Failure = Misc.random(40);
                for (final Bones b : Bones.values()) {
                        if (ID == b.getID()) {
                                if (Failure >= 1) {
                                        c.getItems().deleteItem(ID, 1);
                                        c.sendMessage("The gods are very pleased with your offering.");
                                        
                                        c.getPA().addSkillXP(b.getXP() * 4 * Config.PRAYER_EXPERIENCE, 5);
                                        
                                        c.startAnimation(896);
                                		c.gfx0(247);
                        				if(Misc.random(80) == 0) {
                        					Zombie.spawnZombie(c);
                        				}
                                } else if (Failure == 0) {
                                        c.gfx0(76);
                                		c.startAnimation(896);
                                        c.sendMessage("The gods are not satisfied with your offering");
                                        c.setHitUpdateRequired2(true);
                                        c.setHitDiff2(1);
                                        c.updateRequired = true;
                                }
                        }
                }
        }
}