package server.game.npcs;

class NPCDrop {
	public int npcId;
	public int itemId;
	public int itemAmount = -1;
	public int itemAmount2 = -1;
	public String rarity;
	
	public NPCDrop(int npcId, int itemId, int itemAmount, String percentRarity) {
		this.npcId = npcId;
		this.itemId = itemId;
		this.itemAmount = itemAmount;
		this.rarity = percentRarity;
	}
	
	public NPCDrop(int npcId, int itemId, int itemAmount, int itemAmount2, String percentRarity) {
		this.npcId = npcId;
		this.itemId = itemId;
		this.itemAmount = itemAmount;
		this.itemAmount2 = itemAmount2;
		this.rarity = percentRarity;
	}
}
