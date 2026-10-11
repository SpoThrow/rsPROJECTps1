package server.content.skills;


import java.util.HashMap;

import server.Config;
import server.event.CycleEvent;
import server.event.CycleEventContainer;
import server.event.CycleEventHandler;
import server.game.minigames.randomevents.RandomEventManager;
import server.game.players.Client;
import core.util.Misc;

/**
 *
 *Prayer
 *
 */
public class Prayer {

        /**
         * The altar loop's own event id, so walking away stops the offering without touching any
         * other timed action the player might be running.
         */
        private static final int ALTAR_EVENT = 4630;

        /** Cycles between one bone and the next. Two is fast enough to feel like a batch. */
        private static final int ALTAR_CYCLES = 2;


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
                if (System.currentTimeMillis() - c.timers.buryDelay > 1500) {
                        for (final Bones b : Bones.values()) {
                                if (ID == b.getID()) {
                                        int doubleExperience = Misc.random(20);
                                        if (doubleExperience >= 1) {
                                                c.getItems().deleteItem(ID, 1);
                                                c.sendMessage("You bury the bones.");
                                                
                                                c.getPA().addSkillXP(b.getXP() * Config.PRAYER_EXPERIENCE, 5);
                                                
                                                c.timers.buryDelay = System.currentTimeMillis();
                                                c.startAnimation(827);
                                				if (RandomEventManager.onSkillAction(c)) {
                                					return;
                                				}
                                        } else if (doubleExperience == 0) {
                                                c.getItems().deleteItem(ID, 1);
                                                c.sendMessage("You bury the bones.");
                                                
                                                c.getPA().addSkillXP(b.getXP() * 2 * Config.PRAYER_EXPERIENCE, 5);
                                                
                                                c.timers.buryDelay = System.currentTimeMillis();
                                                c.startAnimation(827);
                                				if (RandomEventManager.onSkillAction(c)) {
                                					return;
                                				}
                                        }
                                }
                        }
                }
        }

        /**
         * Offers {@code ID} on the altar, and keeps offering the same bone until the pack runs out,
         * the player walks, or {@link #cancel} is called.
         *
         * <p><b>What changed.</b> One use used to spend exactly one bone, so offering a full pack
         * was a click per bone. The first bone is still offered on the click itself — the manual
         * path must not feel slower than it did — and the rest follow on a
         * {@link #ALTAR_CYCLES}-tick loop of their own.
         *
         * <p>A click while that loop is running is deliberately ignored rather than served: the
         * loop is already offering bones, and honouring the click as well would offer two per tick
         * for a player who holds the button down. So the loop owns the pace from its first tick, and
         * the rapid manual use that still works is the one that starts it.
         *
         * @param ID the bone Id
         */
        public static void bonesOnAltar(final Client c, final int ID) {
                if (c.playerIsPraying) {
                        // Already offering. See the class comment above: a click here would be a
                        // second offering on top of the loop's, for no extra bone.
                        return;
                }
                if (!IsABone(c, ID) || !c.getItems().playerHasItem(ID, 1)) {
                        return;
                }
                c.playerIsPraying = true;
                // The click's own bone, offered now rather than a tick later, so a single manual
                // offering is answered in the same tick it used to be.
                if (!offer(c, ID)) {
                        cancel(c);
                        return;
                }
                CycleEventHandler.addEvent(ALTAR_EVENT, c, new CycleEvent() {

                        @Override
                        public void execute(CycleEventContainer container) {
                                if (!c.playerIsPraying || !c.getItems().playerHasItem(ID, 1)) {
                                        container.stop();
                                        return;
                                }
                                if (!offer(c, ID)) {
                                        container.stop();
                                }
                        }

                        @Override
                        public void stop() {
                                cancel(c);
                        }
                }, ALTAR_CYCLES);
        }

        /**
         * One bone's worth of offering: the animation, the gfx, the experience and the 1-in-40
         * refusal, all as one unit.
         *
         * @return whether the loop should keep going. False on a random event (which has taken the
         *         player's attention) and on a bone id the table does not know, so the caller stops
         *         rather than ticking on nothing.
         */
        private static boolean offer(Client c, int ID) {
                for (final Bones b : Bones.values()) {
                        if (ID != b.getID()) {
                                continue;
                        }
                        int failure = Misc.random(40);
                        if (failure == 0) {
                                // A refused offering costs the player nothing but the tick: the
                                // bone is kept for the next attempt, which is the behaviour this
                                // branch has always had.
                                c.gfx0(76);
                                c.startAnimation(896);
                                c.sendMessage("The gods are not satisfied with your offering");
                                c.setHitUpdateRequired2(true);
                                c.setHitDiff2(1);
                                c.updateRequired = true;
                                return true;
                        }
                        c.getItems().deleteItem(ID, 1);
                        c.sendMessage("The gods are very pleased with your offering.");
                        c.getPA().addSkillXP(b.getXP() * 4 * Config.PRAYER_EXPERIENCE, 5);
                        c.startAnimation(896);
                        c.gfx0(247);
                        return !RandomEventManager.onSkillAction(c);
                }
                return false;
        }

        /**
         * Stops a running offering, if one is running.
         *
         * <p>The event stop is unconditional and the flag is not, for the reason {@code
         * GemCutting.cancel} documents: the flag means "an offering is running", so whichever of the
         * two cancels runs second must still be able to remove its own event rather than find the
         * flag already cleared.
         */
        public static void cancel(Client c) {
                if (c.playerIsPraying) {
                        c.playerIsPraying = false;
                }
                CycleEventHandler.stopEvents(c, ALTAR_EVENT);
        }
}