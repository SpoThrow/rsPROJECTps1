package server.game.players;

/**
* @uthor Jason 
* http://www.rune-server.org/members/jason
*/public class SkillcapeData {
    
    private enum CapeAnimations {
        ATTACK(4959, 823, 9747, 9748, 9749, 4), 
        DEFENCE(4961, 824, 9753, 9754, 9755, 5),
        STRENGTH(4981, 828, 9750, 9751, 9752, 9),
        HEALTH_POINTS(4971, 833, 9768, 9769, 9770, 4),
        RANGE(4973, 832, 9756, 9757, 9758, 4),
        PRAYER(4979, 829, 9759, 9760, 9761, 7),
        MAGIC(4939, 813, 9762, 9763, 9764, 3),
        COOKING(4955, 821, 9801, 9802, 9803, 14),
        WOODCUTTING(4957, 822, 9807, 9808, 9809, 11),
        FLETCHING(4937, 812, 9783, 9784, 9785, 7),
        FISHING(4951, 819, 9798, 9799, 9800, 6),
        FIREMAKING(4975, 831, 9804, 9805, 9806, 4),
        CRAFTING(4949, 818, 9780, 9781, 9782, 7),
        SMITHING(4943, 815, 9795, 9796, 9797, 7),
        MINING(4941, 814, 9792, 9793, 9794, 4),
        HERBLORE(4969, 835, 9774, 9775, 9776, 7),
        AGILITY(4977, 830, 9771, 9772, 9773, 4),
        THIEVING(4965, 826, 9777, 9778, 9779, 3),
        SLAYER(4967, 827, 9786, 9787, 9788, 3),
        FARMING(4963, 825, 9810, 9811, 9812, 6),
        RUNECRAFTING(4947, 817, 9765, 9766, 9767, 6),
        QUEST(4945, 816, 9813, -1, 9814, 9),
        HUNTER(5158, 907, 9948, 9949, 9950, 6),
        CONSTRUCTION(4953, 820, 9789, 9790, 9791, 5);
        
        private int animation, graphics, untrimmed, trimmed, hood, timer;
        private CapeAnimations(int animation, int graphics, int untrimmed, int trimmed, int hood, int timer) {
            this.animation = animation;
            this.graphics = graphics;
            this.untrimmed = untrimmed;
            this.trimmed = trimmed;
            this.hood = hood;
            this.timer = timer;
        }
        
        public int getAnimation() {
            return animation;
        }
        
        public int getGraphics() {
            return graphics;
        }
        
        public int getUntrimmed() {
            return untrimmed;
        }
        
        public int getTrimmed() {
            return trimmed;
        }
        
        public int getHood() {
            return hood;
        }
        
        public int getTimer() {
            return timer;
        }
        
    }
    
    
    public static void handleSkillcape(Client c) {
        if(!isWearingSkillcape(c)) {
            c.sendMessage("You need to be wearing a skillcape and hood to perform this action.");
            return;
        }
        if(c.inWild()) {
            c.sendMessage("You can't perform this emote while in the wilderness.");
            return;
        }
        if(c.playerIndex > 0) {
            c.sendMessage("You can't perform this emote while attacking another player.");
            return;
        }
        if(c.npcIndex > 0) {
            c.sendMessage("You can't perform this emote while attacking an npc.");
            return;
        }
        @SuppressWarnings("unused")
		int capeId = c.playerEquipment[c.playerCape];
        int hoodId = c.playerEquipment[c.playerHat];
        c.isDoingEmote = true;
        for(CapeAnimations cape : CapeAnimations.values()) {
            if(cape.getHood() == hoodId) {
                int timer = cape.getTimer() * 1000;
                if(System.currentTimeMillis() - c.skillcapeDelay < timer) {
                    c.sendMessage("You have to wait a total of "+cape.getTimer()+" seconds to do a skillcape emote.");
                    return;
                }
                c.startAnimation(cape.getAnimation());
                c.gfx0(cape.getGraphics());
                c.skillcapeDelay = System.currentTimeMillis();
                return;
            }
        }
    }
    
    public static boolean isWearingSkillcape(Client c) {
        for(CapeAnimations cape : CapeAnimations.values())
            if(c.playerEquipment[c.playerHat] == cape.getHood())
                if(c.playerEquipment[c.playerCape] == cape.getTrimmed() || c.playerEquipment[c.playerCape] == cape.getUntrimmed())
                    return true;
            return false;
    }


}