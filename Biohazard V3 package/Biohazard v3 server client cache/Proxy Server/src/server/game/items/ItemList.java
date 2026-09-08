package server.game.items;

public class ItemList {
	public int itemId;
	public String itemName;
	public String itemDescription;
	public double ShopValue;
	public int[] req = new int[24];
	public double LowAlch;
	public double HighAlch;
	public int[] Bonuses = new int[100];

	public ItemList(int _itemId) {
		itemId = _itemId;
	}
}
