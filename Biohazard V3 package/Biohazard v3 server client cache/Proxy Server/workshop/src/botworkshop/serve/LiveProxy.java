package botworkshop.serve;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import botworkshop.export.Json;

/**
 * Fetches the game server's live bot report and wraps it for the viewer — {@code BOT_TOOLING.md} Stage T7.
 *
 * <p><b>Why the tool proxies instead of the browser fetching directly.</b> The viewer is served from this
 * process on one port and the game server would answer on another, so a direct {@code fetch} is
 * cross-origin — and the alternatives are loosening the game server's headers for a debug endpoint or
 * making the browser refuse to talk to it at all. Proxying keeps one origin, keeps the game server's own
 * configuration unchanged, and has the pleasant side effect that "the server is not running" is an answer
 * the viewer can be told rather than a network error it has to guess at.
 *
 * <p><b>The envelope, not the report.</b> The answer is always
 * {@code {"live": bool, "status": {...}|null, "error": "…"|null}}. When the game server is up the status
 * is embedded verbatim through {@link Json#raw(String)}, because parsing a document here only to write it
 * back would create a second opinion about its shape — and the shape is the server's to define. When it
 * is not up, {@code live:false} is an answer the viewer can render, not a failure it has to interpret.
 *
 * <p><b>Short timeouts, deliberately.</b> A stopped game server is the ordinary case here — you do not run
 * a game server to browse a map — so the wait for one to fail must not be noticeable. Half a second to
 * connect and a second and a half to answer is generous for a loopback request and still fast enough that
 * a poll cycle does not stack up.
 */
public final class LiveProxy {

	/** Where the game server's endpoint listens when {@code Config.BOT_STATUS_PORT} is set to it. */
	public static final int DEFAULT_PORT = 8081;

	private static final int CONNECT_TIMEOUT_MS = 500;
	private static final int READ_TIMEOUT_MS = 1500;

	private LiveProxy() {
	}

	/**
	 * The live report as the viewer's envelope, from the game server at {@code host:port}.
	 *
	 * <p>Never throws: every failure — refused, timed out, not JSON, wrong status — is an error string in
	 * an envelope the viewer shows. A tool for looking at a server must not itself break when the server
	 * is absent, which is the state it is in most of the time.
	 */
	public static String fetch(String host, int port) {
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
				.build();
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + host + ":" + port + "/live/bots"))
				.timeout(Duration.ofMillis(READ_TIMEOUT_MS))
				.GET()
				.build();
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				return down("the game server answered " + response.statusCode() + " at " + host + ":" + port);
			}
			String body = response.body();
			// The status is embedded rather than parsed, so it has to at least look like the object it is
			// claimed to be: anything else would produce an envelope the viewer cannot parse and would
			// report as a broken workshop rather than as a wrong port.
			if (body == null || body.trim().isEmpty() || body.trim().charAt(0) != '{') {
				return down("the game server at " + host + ":" + port + " did not answer with a report");
			}
			return up(body.trim());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return down("interrupted while asking " + host + ":" + port);
		} catch (Exception e) {
			// Connection refused is the expected case (no server running); say which address so a wrong
			// port is a two-second diagnosis rather than a hunt.
			return down("no game server at " + host + ":" + port + " (" + e.getClass().getSimpleName()
					+ ": " + e.getMessage() + ")");
		}
	}

	/** The report reached us: pass it through unread. */
	static String up(String reportJson) {
		Json json = new Json();
		json.openObject();
		json.field("live", true);
		json.name("status").raw(reportJson);
		json.name("error").value(null);
		json.closeObject();
		return json.toString();
	}

	/** The report did not: name the reason and the address. */
	static String down(String error) {
		Json json = new Json();
		json.openObject();
		json.field("live", false);
		json.name("status").value(null);
		json.field("error", error);
		json.closeObject();
		return json.toString();
	}
}
