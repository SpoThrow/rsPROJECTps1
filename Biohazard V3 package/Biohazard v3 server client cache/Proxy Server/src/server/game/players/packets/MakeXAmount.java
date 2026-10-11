package server.game.players.packets;

import server.content.skills.CreationMenu;
import server.game.players.Client;

/**
 * Make X from the creation chatbox: a row and a count, in four bytes.
 *
 * <p><b>Why this packet exists.</b> Every other click on the creation chatbox travels as a button
 * id, and a button id is enough because each product owns four of them and each of the four means a
 * fixed amount - 1, 5, 10, or the pack. That is the whole coupling, and it is why the client could
 * be redesigned around it without the server hearing about it. It cannot carry a custom number,
 * though: there are four ids and no fifth, and the amount is not in the id anywhere.
 *
 * <p>Rather than invent a fifth button and a stored amount on the player, the client sends what it
 * actually knows - which row, and how many - and this class hands both to the same
 * {@link CreationMenu} entry point the button clicks use. So make-X and make-10 differ in how they
 * arrive and not in what they do once they have.
 *
 * <p><b>The payload is the client's word and nothing more.</b> The row is checked against the menu
 * the player actually has open and the count against {@link CreationMenu#MAX_X}; the item being
 * made comes from {@code creationRows}, which only the server has ever written. A forged packet can
 * therefore ask to make a great many of something already on the player's own menu, and cannot ask
 * for anything else - which is the same ceiling the four buttons have.
 */
public class MakeXAmount implements PacketType {

	/**
	 * The opcode, and the one number that has to agree with two other files.
	 *
	 * <p>The client sends it ({@code MakeX.MAKE_X_OPCODE}) and {@code Client.PACKET_SIZES} gives it a
	 * length of four. An opcode the size table does not know is a length of zero, which would make
	 * every make-X disconnect the player, so all three are named here rather than spelled out at
	 * each site.
	 */
	public static final int OPCODE = 5;

	@Override
	public void processPacket(Client c, int packetType, int packetSize) {
		int payload = c.getInStream().readDWord();
		int row = (payload >>> 16) & 0xFFFF;
		int amount = payload & 0xFFFF;
		CreationMenu.submit(c, row, amount);
	}
}
