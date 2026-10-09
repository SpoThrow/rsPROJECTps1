package botworkshop.serve;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Serves the Bot Workshop map viewer — {@code BOT_TOOLING.md} stage T2.
 *
 * <p><b>Why a server and not {@code file://}.</b> The viewer fetches JSON, and a browser refuses
 * {@code fetch} against {@code file://} as cross-origin. A window that only works after the user
 * disables a security feature is not a window that works. This is the smallest thing that fixes it:
 * the JDK's own HTTP server, so the tool adds no dependency, no node_modules and no build step to a
 * Java-only repository.
 *
 * <p><b>What it serves.</b> Three routes, and it will not serve anything outside them:
 *
 * <ul>
 * <li>{@code /} — the viewer itself, from {@code workshop/web}.
 * <li>{@code /map/*} — what {@code workshopExport} wrote to {@code Data/workshop/map}.
 * <li>{@code /palette.json} — {@link FloorPalette}, generated per request so an edit to the override
 *     file shows up on reload.
 * </ul>
 *
 * <p>Requests are logged with their status and size: the log is the only way to tell "the viewer is
 * broken" from "the viewer never asked for the file", which are very different bugs.
 *
 * <pre>gradlew workshopServe                # http://127.0.0.1:8080
 * gradlew workshopServe -PworkshopPort=9000</pre>
 */
public final class WorkshopServer {

	private static final int DEFAULT_PORT = 8080;

	/** The override file the palette reads, relative to the project directory. */
	private static final String PALETTE_OVERRIDE = "Data/cfg/floor-palette.cfg";

	private static final String WEB_ROOT = "workshop/web";
	private static final String MAP_ROOT = "Data/workshop/map";

	/** Bound to loopback: this exposes the whole of the map export, so it stays off the network. */
	private static final String HOST = "127.0.0.1";

	private WorkshopServer() {
	}

	public static void main(String[] args) throws Exception {
		int port = port(args);
		Path root = Paths.get(".").toAbsolutePath().normalize();
		Path webRoot = root.resolve(WEB_ROOT);
		Path mapRoot = root.resolve(MAP_ROOT);
		Path mapIndex = mapRoot.resolve("index.json");
		Path paletteOverride = root.resolve(PALETTE_OVERRIDE);

		if (!Files.isDirectory(webRoot)) {
			throw new IllegalStateException("the viewer is missing: " + webRoot
					+ "\n  this task must run from the server directory (gradlew sets it)");
		}
		if (!Files.isRegularFile(mapIndex)) {
			throw new IllegalStateException("no exported map at " + mapRoot
					+ "\n  run 'gradlew workshopExport' first — the viewer has nothing to draw until then");
		}

		Map<String, String> overrides = FloorPalette.loadOverrides(paletteOverride);
		String palette = FloorPalette.toJson(overrides);

		AtomicInteger served = new AtomicInteger();
		HttpServer server = HttpServer.create(new InetSocketAddress(HOST, port), 0);
		server.createContext("/", exchange -> {
			try {
				route(exchange, webRoot, mapRoot, palette);
			} catch (Exception e) {
				// A handler that throws leaves the browser hanging with no clue why. Say what broke.
				System.out.println("[workshop] " + exchange.getRequestURI() + " failed: " + e);
				send(exchange, 500, "text/plain; charset=utf-8",
						("viewer error: " + e + "\n").getBytes(StandardCharsets.UTF_8));
			}
		});
		server.setExecutor(null);
		server.start();

		System.out.println();
		System.out.println("[workshop] Bot Workshop map viewer");
		System.out.println("[workshop]   " + "http://" + HOST + ":" + port + "/");
		System.out.println("[workshop]   viewer  " + webRoot);
		System.out.println("[workshop]   map     " + mapRoot);
		System.out.println("[workshop]   palette " + (overrides.isEmpty()
				? "generated (no " + PALETTE_OVERRIDE + ")"
				: overrides.size() + " override(s) from " + PALETTE_OVERRIDE));
		System.out.println("[workshop]   regions " + mapIndex + " (the navigator reads this)");
		System.out.println("[workshop] Ctrl+C to stop");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println();
			System.out.println("[workshop] served " + served.get() + " request(s)");
			server.stop(0);
		}));
	}

	private static void route(HttpExchange exchange, Path webRoot, Path mapRoot, String palette)
			throws IOException {
		String path = exchange.getRequestURI().getPath();
		if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
			send(exchange, 405, "text/plain; charset=utf-8", "GET only\n".getBytes(StandardCharsets.UTF_8));
			return;
		}

		if (path.equals("/palette.json")) {
			send(exchange, 200, "application/json; charset=utf-8", palette.getBytes(StandardCharsets.UTF_8));
			return;
		}
		if (path.equals("/favicon.ico")) {
			// Browsers ask for this unprompted; a 404 here just fills the log with noise.
			send(exchange, 204, "text/plain; charset=utf-8", new byte[0]);
			return;
		}
		if (path.startsWith("/map/")) {
			String name = path.substring("/map/".length());
			sendFile(exchange, mapRoot, name);
			return;
		}
		String name = path.equals("/") ? "index.html" : path.substring(1);
		sendFile(exchange, webRoot, name);
	}

	/**
	 * Resolves {@code name} under {@code root} and serves it, or 404s.
	 *
	 * <p>The resolved path is checked against the root so that {@code /map/../../Data/characters/...}
	 * cannot walk out of the served directory. Cheap to do, and the alternative is a tool that will
	 * hand out any file on the machine to anything that can reach the port.
	 */
	private static void sendFile(HttpExchange exchange, Path root, String name) throws IOException {
		Path resolved = root.resolve(name).normalize();
		if (!resolved.startsWith(root)) {
			send(exchange, 403, "text/plain; charset=utf-8",
					"outside the served directory\n".getBytes(StandardCharsets.UTF_8));
			return;
		}
		if (!Files.isRegularFile(resolved)) {
			String hint = name.endsWith(".json") && !name.startsWith("map/")
					? "\n  map exports live under /map/, e.g. /map/index.json"
					: "";
			send(exchange, 404, "text/plain; charset=utf-8",
					("not found: " + name + hint + "\n").getBytes(StandardCharsets.UTF_8));
			return;
		}
		byte[] body = Files.readAllBytes(resolved);
		send(exchange, 200, contentType(name), body);
	}

	private static void send(HttpExchange exchange, int status, String contentType, byte[] body)
			throws IOException {
		exchange.getResponseHeaders().set("Content-Type", contentType);
		// The whole point of this server is iterating on the viewer; a cached file would mean
		// editing the JS and seeing the old one.
		exchange.getResponseHeaders().set("Cache-Control", "no-store");
		boolean head = "HEAD".equals(exchange.getRequestMethod());
		exchange.sendResponseHeaders(status, head || body.length == 0 ? -1 : body.length);
		if (head || body.length == 0) {
			exchange.close();
		} else {
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		}
		System.out.println("[workshop] " + exchange.getRequestMethod() + " "
				+ exchange.getRequestURI().getPath() + " " + status + " (" + body.length + " bytes)");
	}

	private static String contentType(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".html")) {
			return "text/html; charset=utf-8";
		}
		if (lower.endsWith(".css")) {
			return "text/css; charset=utf-8";
		}
		if (lower.endsWith(".js") || lower.endsWith(".mjs")) {
			return "text/javascript; charset=utf-8";
		}
		if (lower.endsWith(".json")) {
			return "application/json; charset=utf-8";
		}
		if (lower.endsWith(".svg")) {
			return "image/svg+xml";
		}
		if (lower.endsWith(".png")) {
			return "image/png";
		}
		// Nothing else is expected; octet-stream at least makes a surprise downloadable, not blank.
		return "application/octet-stream";
	}

	private static int port(String[] args) {
		for (int i = 0; i < args.length - 1; i++) {
			if ("--port".equals(args[i])) {
				return Integer.parseInt(args[i + 1]);
			}
		}
		return DEFAULT_PORT;
	}
}
