package botworkshop.serve;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import botworkshop.export.Json;
import botworkshop.export.ScriptDocs;
import server.game.bots.world.Location;
import server.game.bots.world.LocationsConfig;

/**
 * Serves the Bot Workshop map viewer — {@code BOT_TOOLING.md} stage T2.
 *
 * <p><b>Why a server and not {@code file://}.</b> The viewer fetches JSON, and a browser refuses
 * {@code fetch} against {@code file://} as cross-origin. A window that only works after the user
 * disables a security feature is not a window that works. This is the smallest thing that fixes it:
 * the JDK's own HTTP server, so the tool adds no dependency, no node_modules and no build step to a
 * Java-only repository.
 *
 * <p><b>What it serves.</b> Reads are three routes, and it will not serve anything outside them:
 *
 * <ul>
 * <li>{@code /} — the viewer itself, from {@code workshop/web}.
 * <li>{@code /map/*} — what {@code workshopExport} wrote to {@code Data/workshop/map}.
 * <li>{@code /nodes.json} — what {@code workshopExportNodes} wrote, the {@code @BotNode} palette.
 * <li>{@code /palette.json} — {@link FloorPalette}, generated per request so an edit to the override
 *     file shows up on reload.
 * </ul>
 *
 * <p><b>And two writes, which are the only writes a browser can make to this repository.</b>
 * {@code POST /locations/check} and {@code POST /locations/append} take drafted {@code locations.cfg}
 * rows as {@code text/plain} and run them through {@link LocationsConfig} ({@code BOT_ROADMAP.md}
 * §5.2). {@code check} only answers with the canonical rows the parser accepts and the reason for
 * each one it rejects; {@code append} is the one that adds them to the file, and it refuses the whole
 * batch if a single row is bad rather than writing half of an author's box. The browser never gets to
 * write text of its own — it sends what it meant, and the server writes what it parsed.
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

	/** The node palette {@code workshopExportNodes} writes; served at {@code /nodes.json}. */
	private static final String NODES_FILE = "Data/workshop/bot-nodes.json";

	/** One authoring request is a handful of rows; this bounds a malformed or hostile one. */
	private static final int MAX_BODY_BYTES = 64 * 1024;

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
		Path nodesFile = root.resolve(NODES_FILE);
		Path paletteOverride = root.resolve(PALETTE_OVERRIDE);
		Path locationsFile = root.resolve(LocationsConfig.DEFAULT_PATH);
		Path scriptsDir = root.resolve(ScriptDocs.DIR);

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
				route(exchange, webRoot, mapRoot, palette, nodesFile, locationsFile, scriptsDir);
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
		System.out.println("[workshop]   scripts " + scriptsDir);
		System.out.println("[workshop] Ctrl+C to stop");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println();
			System.out.println("[workshop] served " + served.get() + " request(s)");
			server.stop(0);
		}));
	}

	private static void route(HttpExchange exchange, Path webRoot, Path mapRoot, String palette,
			Path nodesFile, Path locationsFile, Path scriptsDir) throws IOException {
		String path = exchange.getRequestURI().getPath();
		String method = exchange.getRequestMethod();
		boolean read = "GET".equals(method) || "HEAD".equals(method);

		// The only routes that accept a write. Everything else is read-only, which is worth
		// enforcing here rather than trusting each branch to remember.
		if (path.equals("/locations/check") || path.equals("/locations/append")) {
			if (!"POST".equals(method)) {
				send(exchange, 405, "text/plain; charset=utf-8",
						"POST only\n".getBytes(StandardCharsets.UTF_8));
				return;
			}
			authoring(exchange, path, locationsFile);
			return;
		}
		// The script editor's two writes (BOT_TOOLING.md T5). Same rule as above: the browser never
		// writes the file, it sends a document and the server decides what the file says.
		if (path.equals("/scripts/check") || path.equals("/scripts/save")) {
			if (!"POST".equals(method)) {
				send(exchange, 405, "text/plain; charset=utf-8",
						"POST only\n".getBytes(StandardCharsets.UTF_8));
				return;
			}
			scripts(exchange, path, scriptsDir);
			return;
		}
		if (!read) {
			send(exchange, 405, "text/plain; charset=utf-8", "GET only\n".getBytes(StandardCharsets.UTF_8));
			return;
		}

		if (path.equals("/palette.json")) {
			send(exchange, 200, "application/json; charset=utf-8", palette.getBytes(StandardCharsets.UTF_8));
			return;
		}
		if (path.equals("/scripts.json")) {
			send(exchange, 200, "application/json; charset=utf-8",
					ScriptDocs.list(scriptsDir).getBytes(StandardCharsets.UTF_8));
			return;
		}
		if (path.equals("/scripts/doc")) {
			scriptDocument(exchange, scriptsDir);
			return;
		}
		if (path.equals("/nodes.json")) {
			if (!Files.isRegularFile(nodesFile)) {
				send(exchange, 404, "text/plain; charset=utf-8",
						("no node schema yet — run 'gradlew workshopExportNodes' first\n")
								.getBytes(StandardCharsets.UTF_8));
				return;
			}
			send(exchange, 200, "application/json; charset=utf-8", Files.readAllBytes(nodesFile));
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
	 * Drafted {@code locations.cfg} rows in, canonical rows (or the reason they were rejected) out.
	 *
	 * <p>{@code /locations/check} answers and writes nothing. {@code /locations/append} writes only
	 * when every row parsed, so a bad row in a batch of five cannot leave four of them in the file —
	 * a half-applied edit is the one outcome an authoring tool must never produce silently.
	 */
	private static void authoring(HttpExchange exchange, String path, Path locationsFile)
			throws IOException {
		byte[] body;
		try (InputStream in = exchange.getRequestBody()) {
			body = in.readNBytes(MAX_BODY_BYTES + 1);
		}
		if (body.length > MAX_BODY_BYTES) {
			send(exchange, 413, "text/plain; charset=utf-8",
					("request body is larger than " + MAX_BODY_BYTES + " bytes\n")
							.getBytes(StandardCharsets.UTF_8));
			return;
		}

		List<String> lines = new ArrayList<String>();
		for (String line : new String(body, StandardCharsets.UTF_8).split("\n")) {
			if (!line.trim().isEmpty()) {
				lines.add(line.trim());
			}
		}
		if (lines.isEmpty()) {
			send(exchange, 400, "application/json; charset=utf-8",
					authoringReport(List.of(), List.of("no rows in the request body"), -1)
							.getBytes(StandardCharsets.UTF_8));
			return;
		}

		// Parsed through the same entry point the file loader uses, so a row this accepts is a row
		// the server will read back at boot.
		List<String> canonical = new ArrayList<String>();
		List<String> problems = new ArrayList<String>();
		List<Location> parsed = new ArrayList<Location>();
		for (String line : lines) {
			LocationsConfig.Result result = LocationsConfig.parseLines(List.of(line));
			if (!result.problems().isEmpty() || result.locations().isEmpty()) {
				canonical.add(null);
				problems.add(result.problems().isEmpty()
						? "the row produced no location"
						: result.problems().get(0));
				continue;
			}
			Location location = result.locations().get(0);
			parsed.add(location);
			canonical.add(LocationsConfig.toRow(location));
			problems.add(null);
		}

		boolean append = path.equals("/locations/append");
		if (append && parsed.size() != lines.size()) {
			// Refuse the batch rather than writing part of it.
			send(exchange, 400, "application/json; charset=utf-8",
					authoringReport(canonical, problems, -1).getBytes(StandardCharsets.UTF_8));
			return;
		}

		int appended = -1;
		if (append) {
			String note = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
			appended = LocationsConfig.appendRows(locationsFile, parsed, note);
			System.out.println("[workshop] appended " + appended + " row(s) to " + locationsFile);
			System.out.println("[workshop]   re-export with 'gradlew workshopExport' to see them on the map");
		}

		send(exchange, 200, "application/json; charset=utf-8",
				authoringReport(canonical, problems, appended).getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * A script document in, an answer out — {@code BOT_TOOLING.md} T5's Save/Validate half.
	 *
	 * <p>{@code /scripts/check} only answers; {@code /scripts/save} writes {@code Data/cfg/bots/<name>.json}.
	 * Both run the document through {@link ScriptDocs}, which validates it with the server's own loader and
	 * then reformats it, so the browser never writes the file — it sends the graph it built and the server
	 * decides what the file says. Same arrangement as {@link #authoring}.
	 *
	 * <p>A rejected document is a 200 with {@code ok:false} and the reason, not a 4xx: it is an answer to
	 * the question that was asked, and the editor shows it on the step rather than as a transport failure.
	 * The 4xx/5xx codes here stay reserved for a request the server could not act on at all.
	 */
	private static void scripts(HttpExchange exchange, String path, Path scriptsDir) throws IOException {
		byte[] body;
		try (InputStream in = exchange.getRequestBody()) {
			body = in.readNBytes(MAX_BODY_BYTES + 1);
		}
		if (body.length > MAX_BODY_BYTES) {
			send(exchange, 413, "text/plain; charset=utf-8",
					("request body is larger than " + MAX_BODY_BYTES + " bytes\n")
							.getBytes(StandardCharsets.UTF_8));
			return;
		}

		String json = new String(body, StandardCharsets.UTF_8);
		String name = queryParam(exchange, "name");
		boolean save = path.equals("/scripts/save");
		try {
			if (save) {
				if (name == null || name.isBlank()) {
					answer(exchange, scriptReport(false, null, "a save needs a name"));
					return;
				}
				String canonical = ScriptDocs.save(scriptsDir, name, json);
				System.out.println("[workshop] wrote " + scriptsDir.resolve(name + ScriptDocs.EXTENSION));
				System.out.println("[workshop]   reload the server with ::bot reload to run it");
				answer(exchange, scriptReport(true, canonical, null));
			} else {
				// A check has no file name of its own yet, so the document is validated under a placeholder.
				// The canonical output never carries a name field, so nothing depends on this one.
				String checkName = name == null || name.isBlank() ? "untitled" : name;
				answer(exchange, scriptReport(true, ScriptDocs.canonicalize(checkName, json), null));
			}
		} catch (RuntimeException e) {
			// The loader's message, or the writer's: either way it names the node and field.
			answer(exchange, scriptReport(false, null, e.getMessage()));
		}
	}

	/** One script document as JSON, for the editor to reopen. */
	private static void scriptDocument(HttpExchange exchange, Path scriptsDir) throws IOException {
		String name = queryParam(exchange, "name");
		if (name == null || name.isBlank()) {
			send(exchange, 400, "text/plain; charset=utf-8", "no name given\n".getBytes(StandardCharsets.UTF_8));
			return;
		}
		try {
			String text = ScriptDocs.read(scriptsDir, name);
			if (text == null) {
				send(exchange, 404, "text/plain; charset=utf-8",
						("no script \"" + name + "\"\n").getBytes(StandardCharsets.UTF_8));
				return;
			}
			send(exchange, 200, "application/json; charset=utf-8", text.getBytes(StandardCharsets.UTF_8));
		} catch (ScriptDocs.DocException e) {
			send(exchange, 400, "text/plain; charset=utf-8",
					(e.getMessage() + "\n").getBytes(StandardCharsets.UTF_8));
		}
	}

	/** The check/save answer: whether it was accepted, the canonical text, or why it was not. */
	private static String scriptReport(boolean ok, String canonical, String error) {
		Json json = new Json();
		json.openObject();
		json.field("ok", ok);
		json.name("canonical").value(ok ? canonical : null);
		json.name("error").value(ok ? null : error);
		json.closeObject();
		return json.toString();
	}

	private static void answer(HttpExchange exchange, String json) throws IOException {
		send(exchange, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
	}

	/** One value from the request's query string, decoded, or null. Repeated keys are not expected here. */
	private static String queryParam(HttpExchange exchange, String key) {
		String query = exchange.getRequestURI().getRawQuery();
		if (query == null) {
			return null;
		}
		for (String part : query.split("&")) {
			int equals = part.indexOf('=');
			if (equals > 0 && part.substring(0, equals).equals(key)) {
				return java.net.URLDecoder.decode(part.substring(equals + 1), StandardCharsets.UTF_8);
			}
		}
		return null;
	}

	/**
	 * The authoring answer: one entry per submitted row, plus the whole batch as cfg text.
	 *
	 * <p>{@code appended} is -1 when nothing was written, so "checked and fine" and "written" are
	 * distinguishable by the viewer without it having to remember which button it pressed.
	 */
	private static String authoringReport(List<String> canonical, List<String> problems, int appended) {
		int accepted = 0;
		int rejected = 0;
		StringBuilder cfg = new StringBuilder();
		for (int i = 0; i < canonical.size(); i++) {
			if (canonical.get(i) == null) {
				rejected++;
			} else {
				accepted++;
				cfg.append(canonical.get(i)).append('\n');
			}
		}

		Json json = new Json();
		json.openObject();
		json.field("accepted", accepted);
		json.field("rejected", rejected);
		json.field("appended", appended);
		json.name("cfg").value(cfg.toString());
		json.name("rows").openArray();
		for (int i = 0; i < canonical.size(); i++) {
			json.openObject();
			if (canonical.get(i) == null) {
				json.field("ok", false);
				json.field("error", problems.get(i));
			} else {
				json.field("ok", true);
				json.field("canonical", canonical.get(i));
			}
			json.closeObject();
		}
		json.closeArray();
		json.closeObject();
		return json.toString();
	}

	/**
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
