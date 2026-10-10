package server.game.bots;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import core.util.Misc;
import server.Config;

/**
 * Serves {@link LiveBots} over loopback — {@code BOT_TOOLING.md} Stage T7's read-only half.
 *
 * <p><b>Carried by the JDK, not by a dependency.</b> {@code com.sun.net.httpserver} is in the platform,
 * so this adds no jar to a server that deliberately ships only the five it loads. The alternative — the
 * workshop holding the socket and the server pushing to it — would invert the dependency and make the
 * tool something the server needs, which is exactly what {@code BOT_TOOLING.md} §11 forbids.
 *
 * <p><b>One guarded call, so the endpoint can be removed with the feature.</b> {@link
 * #startIfEnabled()} is a no-op when {@link Config#BOT_STATUS_PORT} is non-positive, so the boot path is
 * unchanged and the whole endpoint can be deleted without a trace. It is enabled at {@code 8081} in the
 * shipped config because the workshop's live panel is the one route that cannot be answered from a file;
 * setting the port to {@code 0} turns it off without a code change.
 *
 * <p><b>One route, one method, one address.</b> {@code GET /live/bots} and nothing else; anything else is
 * a 404 and a POST is a 405. There is no write and no parameter, so the reachable surface is a document
 * that lists where bots are standing — which is why this is bound to {@link #HOST} rather than left open:
 * it cannot be made to do anything, but it can be read, and a player list is not something to publish.
 *
 * <p><b>Not threaded.</b> {@code setExecutor(null)} uses the JDK's default single-thread executor, so two
 * browsers polling cannot race each other through {@link LiveBots#toJson()}. The bot list itself is read
 * without a lock by design — see that class for what that does and does not guarantee.
 */
public final class LiveBotsServer {

	/** Loopback only. A debugging endpoint that answers to the network is a different thing. */
	public static final String HOST = "127.0.0.1";

	private LiveBotsServer() {
	}

	/**
	 * Starts the endpoint when {@link Config#BOT_STATUS_PORT} asks for one, and does nothing otherwise.
	 *
	 * @return the running server, or null when it is disabled or could not start
	 */
	public static HttpServer startIfEnabled() {
		return startIfEnabled(Config.BOT_STATUS_PORT);
	}

	/**
	 * The same, over a port a caller supplies.
	 *
	 * <p>Package-private and separate so the <em>disabled</em> path is testable: {@link Config} is a
	 * compile-time constant, so a test cannot ask the public form to be off, and "a non-positive port
	 * opens nothing" is the whole of the feature's off switch.
	 *
	 * <p>A port already in use is reported and ignored: this is a debug view, and refusing to boot the
	 * game because something else holds the port would be the wrong way round.
	 *
	 * @return the running server, or null when the port is non-positive or the socket cannot be bound
	 */
	static HttpServer startIfEnabled(int port) {
		if (port <= 0) {
			return null;
		}
		try {
			HttpServer server = start(port);
			Misc.println("[bots] live view on http://" + HOST + ":" + port + LiveBots.PATH);
			return server;
		} catch (IOException e) {
			Misc.println("[bots] could not start the live view on " + HOST + ":" + port + ": "
					+ e.getMessage());
			return null;
		}
	}

	/**
	 * Starts the endpoint on {@code port} and returns it. {@code 0} picks an ephemeral port, which is how
	 * a test gets one without reserving a number the machine may not have.
	 *
	 * <p>Public so it can be driven directly: a caller that wants the view without the config flag — a
	 * test, or a future {@code ::bot live} command — should not have to edit a constant to get it.
	 */
	public static HttpServer start(int port) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(HOST, port), 0);
		server.createContext(LiveBots.PATH, LiveBotsServer::handle);
		// The default executor: a single thread, so the report is built once at a time.
		server.setExecutor(null);
		server.start();
		return server;
	}

	private static void handle(HttpExchange exchange) throws IOException {
		// HttpServer matches a context by prefix, so the context alone would also answer
		// /live/bots/anything. The path is checked exactly, which is what makes "one route" true.
		if (!LiveBots.PATH.equals(exchange.getRequestURI().getPath())) {
			send(exchange, 404, "text/plain; charset=utf-8", "not found\n".getBytes(StandardCharsets.UTF_8));
			return;
		}
		if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
			exchange.getResponseHeaders().set("Allow", "GET");
			send(exchange, 405, "text/plain; charset=utf-8", "GET only\n".getBytes(StandardCharsets.UTF_8));
			return;
		}
		send(exchange, 200, "application/json; charset=utf-8",
				LiveBots.toJson().getBytes(StandardCharsets.UTF_8));
	}

	private static void send(HttpExchange exchange, int status, String contentType, byte[] body)
			throws IOException {
		exchange.getResponseHeaders().set("Content-Type", contentType);
		// Never cached: the whole value of this endpoint is that it says what is happening now.
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		boolean head = "HEAD".equals(exchange.getRequestMethod());
		exchange.sendResponseHeaders(status, head || body.length == 0 ? -1 : body.length);
		if (head || body.length == 0) {
			exchange.close();
			return;
		}
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(body);
		}
	}
}
