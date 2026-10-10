package botworkshop.serve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import server.game.bots.LiveBotsServer;

/**
 * The T7 proxy: that a real live endpoint is wrapped whole, and that its absence is an answer.
 *
 * <p><b>Both halves are exercised against real code.</b> The up case starts the server's own
 * {@link LiveBotsServer} on an ephemeral port and fetches it through the proxy, so what is asserted is
 * the two halves agreeing rather than this file's idea of the envelope. The down case provokes a real
 * refusal by asking for a port nothing is listening on — a stopped game server is the ordinary state of
 * this feature, so it is the case that most needs to be known good.
 */
class LiveProxyTest {

	private HttpServer endpoint;
	private HttpServer stub;

	@AfterEach
	void stop() {
		if (endpoint != null) {
			endpoint.stop(0);
			endpoint = null;
		}
		if (stub != null) {
			stub.stop(0);
			stub = null;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> parse(String json) {
		return (Map<String, Object>) server.game.bots.script.Json.parse(json);
	}

	@Test
	void theLiveReportIsWrappedWholeWhenTheGameServerAnswers() throws Exception {
		endpoint = LiveBotsServer.start(0);
		int port = endpoint.getAddress().getPort();

		Map<String, Object> envelope = parse(LiveProxy.fetch(LiveBotsServer.HOST, port));

		assertEquals(Boolean.TRUE, envelope.get("live"));
		assertNull(envelope.get("error"));
		// Embedded verbatim: the proxy is not allowed to be a second opinion about the report's shape,
		// so the fields it never parses are still exactly the ones the server wrote.
		Map<String, Object> status = (Map<String, Object>) envelope.get("status");
		assertNotNull(status);
		assertEquals("/live/bots", status.get("path"));
		assertEquals(10.0, status.get("cap"));
		assertEquals(0.0, status.get("count"));
		assertNotNull(status.get("bots"));
	}

	@Test
	void noGameServerIsAnAnswerRatherThanAFailure() throws Exception {
		int dead = closedPort();

		Map<String, Object> envelope = parse(LiveProxy.fetch(LiveBotsServer.HOST, dead));

		assertEquals(Boolean.FALSE, envelope.get("live"));
		assertNull(envelope.get("status"));
		String error = String.valueOf(envelope.get("error"));
		// The address has to be in the message: "no game server" without it leaves the reader to guess
		// whether the server is down or the port is wrong, which are different fixes.
		assertTrue(error.contains(":" + dead), error);
	}

	@Test
	void anAnswerThatIsNotAReportIsRefusedRatherThanEmbedded() throws Exception {
		// A wrong port can be something else entirely, and embedding a non-object would produce an
		// envelope the viewer cannot parse — reported as a broken workshop rather than as a wrong port.
		stub = HttpServer.create(new java.net.InetSocketAddress(LiveBotsServer.HOST, 0), 0);
		stub.createContext("/live/bots", exchange -> {
			byte[] body = "hello, not JSON".getBytes(java.nio.charset.StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (java.io.OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		stub.start();

		Map<String, Object> envelope = parse(LiveProxy.fetch(LiveBotsServer.HOST,
				stub.getAddress().getPort()));

		assertEquals(Boolean.FALSE, envelope.get("live"));
		assertNull(envelope.get("status"));
		assertTrue(String.valueOf(envelope.get("error")).contains("did not answer with a report"));
	}

	@Test
	void theEnvelopeIsTheSameShapeEitherWay() {
		// The viewer reads one shape, so both answers carry all three keys: an absent one would be an
		// `undefined` the viewer has to remember not to touch.
		Map<String, Object> up = parse(LiveProxy.up("{\"count\": 1}"));
		assertEquals(Boolean.TRUE, up.get("live"));
		assertNotNull(up.get("status"));
		assertTrue(up.containsKey("error"));

		Map<String, Object> down = parse(LiveProxy.down("nope"));
		assertEquals(Boolean.FALSE, down.get("live"));
		assertTrue(down.containsKey("status"));
		assertEquals("nope", down.get("error"));
	}

	/** A port nothing is listening on, taken by opening and immediately closing a socket. */
	private static int closedPort() throws Exception {
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(false);
			return socket.getLocalPort();
		}
	}
}
