package server.game.players.packets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import server.game.players.Client;

/**
 * Pins the dispatch guard in PacketHandler.processPacket. The 256-entry table
 * used to be indexed before its bounds were checked, with a bound of 257, so a
 * packet id of 256 escaped as an ArrayIndexOutOfBoundsException that
 * PlayerHandler swallowed -- costing the player their whole tick.
 */
class PacketHandlerTest {

	private static Client clientExpecting(int packetType, int packetSize) {
		Client c = new Client(null, 1);
		c.playerName = "Dispatch";
		c.packetType = packetType;
		c.packetSize = packetSize;
		c.disconnected = false;
		return c;
	}

	@Test
	void registeredPacketIsDispatchedAndTheClientStaysConnected() {
		// 200 is mapped to SilentPacket, which does nothing for a non-zero id.
		Client c = clientExpecting(200, 0);
		PacketHandler.processPacket(c, 200, 0);
		assertFalse(c.disconnected);
	}

	@Test
	void packetIdBeyondTheTableIsRejectedInsteadOfThrowing() {
		for (int id : new int[] { 256, 257, 300, 65535 }) {
			Client c = clientExpecting(id, 0);
			assertDoesNotThrow(() -> PacketHandler.processPacket(c, id, 0),
					"id " + id + " must not reach the array read");
			assertTrue(c.disconnected, "id " + id + " should take the invalid-packet path");
		}
	}

	@Test
	void negativePacketIdIsRejected() {
		Client c = clientExpecting(-1, 0);
		assertDoesNotThrow(() -> PacketHandler.processPacket(c, -1, 0));
		assertTrue(c.disconnected);
	}

	@Test
	void unregisteredPacketIdDisconnects() {
		// 199 is in range but has no handler assigned in the static table.
		Client c = clientExpecting(199, 0);
		PacketHandler.processPacket(c, 199, 0);
		assertTrue(c.disconnected);
	}

	@Test
	void sizeMismatchDisconnects() {
		Client c = clientExpecting(200, 0);
		PacketHandler.processPacket(c, 200, 5);
		assertTrue(c.disconnected);
	}
}
