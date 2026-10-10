/**
 * The live bot view — `BOT_TOOLING.md` Stage T7's viewer half.
 *
 * The workshop server proxies `GET /live/bots` from the game server and answers with an envelope:
 * `{ live, status, error }`. This module turns that envelope into the panel's state and polls it while
 * "watch" is on. Nothing here knows what a bot *is* — the report's shape is the game server's to
 * define, and `LiveBots` on that side is where it is decided — so this file reads four fields and
 * shows them.
 *
 * **The two decisions worth testing are pure and are exported.** `readEnvelope` decides what an answer
 * means, and `placeOf` decides which plane a bot belongs to. Both fail in ways that look like
 * something else — a missing envelope reads as "no bots", a wrong plane reads as "the marker is
 * missing" — which is exactly why they are separated from the fetch and the DOM.
 */

/** How often to ask while watching. Two thirds of a game tick, so a step is visible when it happens. */
export const POLL_MS = 1000;

const LIVE_ROUTE = '/live/bots';

/**
 * What the proxy's envelope means.
 *
 * @param {object|null} body the parsed envelope, or null when the body was not JSON at all
 * @returns {{live: boolean, report: object|null, error: string|null}} `live` is true only when a report
 *     was actually delivered, so the panel never shows a stale list beside a failure message.
 */
export function readEnvelope(body) {
  if (!body || typeof body !== 'object') {
    return { live: false, report: null, error: 'the workshop server answered with something that was not JSON' };
  }
  if (body.live !== true) {
    return {
      live: false,
      report: null,
      error: typeof body.error === 'string' && body.error
        ? body.error
        : 'the workshop server did not say why the game server could not be reached',
    };
  }
  if (!body.status || typeof body.status !== 'object') {
    return { live: false, report: null, error: 'the game server sent no report' };
  }
  return { live: true, report: body.status, error: null };
}

/** The bots in a report, defensively: a report with no `bots` array is a report with no bots. */
export function botsOf(report) {
  return report && Array.isArray(report.bots) ? report.bots.filter((bot) => bot && bot.name) : [];
}

/**
 * The bots on one plane, in a stable order — what the map draws.
 *
 * Sorted by name so a bot does not jump position in a draw order that changes every poll, and filtered
 * because a bot on plane 2 drawn on plane 0 would be a marker at a tile it is not standing on.
 */
export function botsOnPlane(report, plane) {
  return botsOf(report)
    .filter((bot) => bot.plane === plane)
    .sort((a, b) => String(a.name).localeCompare(String(b.name)));
}

/** The state a bot is in right now, as one short line. `(idle)` is the server's word for nothing. */
export function currentState(bot) {
  const path = bot && typeof bot.path === 'string' ? bot.path.trim() : '';
  if (!path) {
    return '(no state running)';
  }
  // The path is root > … > leaf; the leaf is what the bot is doing. The full path is shown as the row's
  // title, so the one-line summary does not have to carry the whole tree.
  const parts = path.split('>');
  return parts[parts.length - 1].trim();
}

/** `3200,3200 p0`, the form the jump box accepts, so a row's position can be copied into it. */
export function placeOf(bot) {
  return `${bot.x},${bot.y} p${bot.plane}`;
}

/** One line for the row: what the bot is, and where. */
export function describeBot(bot) {
  return `${bot.script || 'no script'}  ·  ${currentState(bot)}  ·  ${placeOf(bot)}`;
}

/**
 * Polls the live report while watching.
 *
 * **Never overlapping requests.** A refresh in flight is not started again, so a slow or hanging answer
 * cannot stack polls up behind it — the failure mode being a browser slowly firing more and more
 * requests at a socket that is not answering. `inFlight` is that guard, and it is also why the interval
 * starts its first fetch immediately rather than after one tick.
 */
export class LiveMonitor {
  /**
   * @param {{intervalMs?: number, fetchImpl?: typeof fetch, onUpdate?: Function, onError?: Function}} options
   *     `fetchImpl` exists so the polling behaviour can be driven from a test without a server.
   */
  constructor({ intervalMs = POLL_MS, fetchImpl = null, onUpdate = () => {}, onError = () => {} } = {}) {
    this.intervalMs = intervalMs;
    this.fetch = fetchImpl || ((...args) => globalThis.fetch(...args));
    this.onUpdate = onUpdate;
    this.onError = onError;
    this.timer = null;
    this.inFlight = false;
    this.state = { live: false, report: null, error: null };
  }

  get watching() {
    return this.timer !== null;
  }

  get report() {
    return this.state.report;
  }

  get bots() {
    return botsOf(this.state.report);
  }

  /** Starts polling. Idempotent: a second call is not a second interval. */
  start() {
    if (this.watching) {
      return;
    }
    this.refresh();
    this.timer = setInterval(() => this.refresh(), this.intervalMs);
  }

  stop() {
    if (this.timer !== null) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  setWatching(watching) {
    if (watching) {
      this.start();
    } else {
      this.stop();
      // Stopping does not clear the report: the last known positions are still the best answer, and the
      // panel says which tick they came from.
    }
  }

  /** One request. Never throws: an unreachable workshop server is a state, not an exception. */
  async refresh() {
    if (this.inFlight) {
      return this.state;
    }
    this.inFlight = true;
    try {
      const response = await this.fetch(LIVE_ROUTE, { cache: 'no-store' });
      const body = await response.json();
      this.state = readEnvelope(body);
    } catch (e) {
      this.state = {
        live: false,
        report: null,
        error: `the workshop server could not be asked: ${e.message}`,
      };
      this.onError(this.state.error);
    } finally {
      this.inFlight = false;
    }
    this.onUpdate(this.state);
    return this.state;
  }
}
