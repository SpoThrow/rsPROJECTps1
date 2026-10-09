package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import core.util.ISAACRandomGen;
import server.Config;
import server.game.players.Client;

/**
 * Slice-1 step 1: a sessionless client must never throw on flush and must never let its
 * out-buffer grow.
 *
 * <p>The bug this pins: {@code flushOutStream()} wrote to {@code session}. A bot has no
 * session, so either it NPEs, or — if the guard merely returned — every buffered
 * {@code sendMessage}/{@code addItem} frame accumulates in {@code outStream.buffer} until
 * it overruns {@link Config#BUFFER_SIZE} (10000). The fix drops the bytes instead.
 */
class SessionlessFlushTest {

	private static final int SLOT = 1;

	private static Client sessionless() {
		Client c = new Client(null, SLOT);
		// A real login installs the ISAAC encoder on the out-stream; without it the frame
		// writers NPE before the flush is even reached. Installing it keeps this test
		// about flushOutStream rather than about login state.
		c.getOutStream().packetEncryption = new ISAACRandomGen(new int[] { 1, 2, 3, 4 });
		return c;
	}

	@Test
	void aSessionlessClientDiscardsItsBufferedBytes() {
		Client c = sessionless();

		c.sendMessage("hello");
		assertTrue(c.getOutStream().currentOffset > 0, "the frame is buffered before flushing");

		c.flushOutStream();
		assertEquals(0, c.getOutStream().currentOffset, "a bot drops what it cannot send");
	}

	@Test
	void aThousandFlushesNeverGrowTheBuffer() {
		Client c = sessionless();

		for (int i = 0; i < 1000; i++) {
			c.sendMessage("message number " + i);
			c.flushOutStream();
			assertEquals(0, c.getOutStream().currentOffset,
					"the buffer must be empty after every flush, not merely capped");
		}
	}

	@Test
	void aDisconnectedClientReturnsBeforeTouchingTheBuffer() {
		Client c = sessionless();
		c.sendMessage("hi");
		int buffered = c.getOutStream().currentOffset;

		c.disconnected = true;
		c.flushOutStream();

		assertEquals(buffered, c.getOutStream().currentOffset,
				"the disconnected guard still wins over the sessionless guard");
	}

	@Test
	void aBufferSizeByteCountStillFitsTheRealFrame() {
		// Guard against a future BUFFER_SIZE change silently invalidating the premise above.
		assertTrue(Config.BUFFER_SIZE > "hello".length());
	}
}
