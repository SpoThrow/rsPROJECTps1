package server.game.bots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import server.game.bots.script.Json;

/**
 * The T7 read-only live view: what {@link LiveBots} writes, and that {@link LiveBotsServer} serves it.
 *
 * <p><b>Why the views are injected.</b> {@link LiveBots#views()} reads {@link BotManager}, which means a
 * live character, a save file and a possessed client — a fixture that would be most of the game to test
 * six fields. {@link LiveBots.View} exists so the part that was actually written by hand, the JSON and
 * its escaping, is checked directly; {@code views()} is then thin glue over accessors that
 * {@code BotManagerTest} and {@code BotTraceTest} already cover.
 *
 * <p><b>And the socket is real.</b> The routing is three lines, but "GET is answered, POST is refused,
 * and the body parses" is exactly the kind of claim that is true in the source and false in the running
 * server, so it is fetched over an actual loopback connection rather than asserted about.
 */
class LiveBotsTest {

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
			server = null;
		}
	}

	private static LiveBots.View bot(String name, String script, int x, int y, int plane, String tree,
			long tick, List<String> path, String lastFailure, List<String> history) {
		return new LiveBots.View(name, script, x, y, plane, tree, tick, path, lastFailure, history);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> parse(String json) {
		return (Map<String, Object>) Json.parse(json);
	}

	@SuppressWarnings("unchecked")
	private static List<Object> bots(Map<String, Object> report) {
		return (List<Object>) report.get("bots");
	}

	@Test
	void reportsEachBotWithItsPositionAndCurrentPath() {
		List<LiveBots.View> views = List.of(bot("willow", "chop_and_bank", 3103, 3218, 0,
				"Sequence(4)", 42, List.of("Repeat(-1)", "Sequence(4)", "Gather(tree)"),
				"WalkToNearest -> FAILURE (after 40t, no tree within 8 tiles)",
				List.of("t=40 Gather(tree) -> ENTER")));

		Map<String, Object> report = parse(LiveBots.toJson(views, "1 ticked, 3ms in trees", 1, 10));

		assertEquals(1.0, report.get("count"));
		assertEquals(10.0, report.get("cap"));
		assertEquals("1 ticked, 3ms in trees", report.get("stats"));
		assertEquals(1, bots(report).size());

		Map<String, Object> one = (Map<String, Object>) bots(report).get(0);
		assertEquals("willow", one.get("name"));
		assertEquals("chop_and_bank", one.get("script"));
		assertEquals(3103.0, one.get("x"));
		assertEquals(3218.0, one.get("y"));
		assertEquals(0.0, one.get("plane"));
		assertEquals("Sequence(4)", one.get("tree"));
		assertEquals(42.0, one.get("tick"));
		// The path is flattened to the line `::botinfo` prints, so the two read the same.
		assertEquals("Repeat(-1) > Sequence(4) > Gather(tree)", one.get("path"));
		assertEquals("WalkToNearest -> FAILURE (after 40t, no tree within 8 tiles)",
				one.get("lastFailure"));
		assertEquals(List.of("t=40 Gather(tree) -> ENTER"), one.get("history"));
	}

	@Test
	void anEmptyPathIsAnEmptyStringAndANullFailureStaysNull() {
		// A freshly possessed bot has a tree and a tick but nothing current and nothing failed. "null"
		// as a path would read as a state called null in the viewer.
		Map<String, Object> one = (Map<String, Object>) bots(parse(LiveBots.toJson(
				List.of(bot("oak", "chop_and_bank", 3200, 3200, 0, "Repeat(-1)", 0,
						List.of(), null, List.of())),
				"0 ticked", 1, 10))).get(0);

		assertEquals("", one.get("path"));
		assertNull(one.get("lastFailure"));
		assertEquals(List.of(), one.get("history"));
	}

	@Test
	void withNoBotsTheReportIsStillWellFormed() {
		Map<String, Object> report = parse(LiveBots.toJson(List.of(), "0 ticked", 0, 10));
		assertEquals(0.0, report.get("count"));
		assertEquals(List.of(), bots(report));
	}

	@Test
	void textTheServerDidNotComposeIsEscaped() {
		// A note is a state's own sentence and a name comes from a config file — either can hold a quote
		// or a backslash, and one unescaped would make the whole document unparseable, which the viewer
		// would report as a broken server rather than as the odd name it is.
		String hostile = "note with \"quotes\", a \\backslash\n and a\ttab";
		Map<String, Object> one = (Map<String, Object>) bots(parse(LiveBots.toJson(
				List.of(bot("we\"ird\\name", "s", 1, 2, 3, "t", 0, List.of(), hostile,
						List.of("line\none"))),
				"stats", 1, 10))).get(0);

		assertEquals("we\"ird\\name", one.get("name"));
		assertEquals(hostile, one.get("lastFailure"));
		assertEquals(List.of("line\none"), one.get("history"));
	}

	@Test
	void aNullEntryInTheListIsSkippedRatherThanWritten() {
		// BotManager.all() copies the live list, which the game thread may be adding to, so a null can
		// reach here. Writing it would produce a bot object of nulls; skipping it is the honest answer.
		List<LiveBots.View> views = new ArrayList<LiveBots.View>();
		views.add(null);
		views.add(bot("oak", "s", 1, 2, 0, "t", 0, List.of(), null, List.of()));
		assertEquals(1, bots(parse(LiveBots.toJson(views, "s", 1, 10))).size());
	}

	@Test
	void aNullOrAbsentListFieldBecomesAnEmptyListRatherThanNull() {
		// View normalises, so the writer never has to: a caller-built view with nulls must not be able to
		// emit a `history: null` the viewer would then have to special-case.
		LiveBots.View view = bot("oak", "s", 1, 2, 0, "t", 0, null, null, null);
		assertEquals(List.of(), view.path());
		assertEquals(List.of(), view.history());
		Map<String, Object> one = (Map<String, Object>) bots(parse(LiveBots.toJson(
				List.of(view), "s", 1, 10))).get(0);
		assertEquals(List.of(), one.get("history"));
	}

	@Test
	void aViewIsImmutableSoTheWriterCannotBeRacedByItsCaller() {
		List<String> history = new ArrayList<String>(List.of("a"));
		LiveBots.View view = bot("oak", "s", 1, 2, 0, "t", 0, List.of(), null, history);
		history.add("b");
		assertEquals(List.of("a"), view.history());
	}

	@Test
	void theEndpointServesTheReportOnLoopback() throws Exception {
		server = LiveBotsServer.start(0);
		int port = server.getAddress().getPort();

		HttpResponse<String> response = get(port, "/live/bots");
		assertEquals(200, response.statusCode());
		assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
		// No bots are possessed in a test JVM, so the honest report is an empty list — but it is a real
		// report built by real code, which is what makes this a test of the route rather than of a fixture.
		Map<String, Object> report = parse(response.body());
		assertEquals(0.0, report.get("count"));
		assertEquals(LiveBots.PATH, report.get("path"));
		assertEquals(10.0, report.get("cap"));
	}

	@Test
	void theEndpointRefusesAnythingButAGet() throws Exception {
		server = LiveBotsServer.start(0);
		int port = server.getAddress().getPort();

		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		HttpResponse<String> post = client.send(
				HttpRequest.newBuilder(URI.create("http://" + LiveBotsServer.HOST + ":" + port + LiveBots.PATH))
						.POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(405, post.statusCode());
		assertTrue(post.body().contains("GET only"));
	}

	@Test
	void anyOtherPathIs404SoTheEndpointIsOneRoute() throws Exception {
		server = LiveBotsServer.start(0);
		int port = server.getAddress().getPort();
		assertEquals(404, get(port, "/").statusCode());
		assertEquals(404, get(port, "/secrets").statusCode());
		// The context matches by prefix, so this is the case that would otherwise be answered by the
		// handler without the exact-path check.
		assertEquals(404, get(port, LiveBots.PATH + "/extra").statusCode());
	}

	@Test
	void thePathAndAddressTheViewerLooksForArePinned() {
		// The workshop's proxy and the viewer both hardcode these; pinning them means a rename fails a
		// test rather than silently breaking the live view.
		assertEquals("/live/bots", LiveBots.PATH);
		assertEquals("127.0.0.1", LiveBotsServer.HOST);
	}

	@Test
	void aNonPositivePortOpensNothing() {
		// The whole of the feature's off switch, and the reason Config.BOT_STATUS_PORT can default to 0:
		// a server with no live view has no live view, not a listener it does not know about.
		assertNull(LiveBotsServer.startIfEnabled(0));
		assertNull(LiveBotsServer.startIfEnabled(-1));
	}

	private static HttpResponse<String> get(int port, String path) throws Exception {
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		return client.send(
				HttpRequest.newBuilder(
						URI.create("http://" + LiveBotsServer.HOST + ":" + port + path)).GET().build(),
				HttpResponse.BodyHandlers.ofString());
	}
}
