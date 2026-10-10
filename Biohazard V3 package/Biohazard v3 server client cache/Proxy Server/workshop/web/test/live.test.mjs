/**
 * The live view's decisions, checked without a browser — `gradlew workshopJsTest`.
 *
 * `live.js` has two halves. The polling half needs a socket, and the panel half needs a DOM; neither can
 * be checked here. What can be, and is what actually goes wrong, is the part in between: what an answer
 * from the workshop server *means*. A wrong reading there is invisible — an unreachable game server shows
 * an empty panel, which is also what a game server with no bots shows — so it is the part worth pinning.
 *
 * The monitor's own behaviour is checked against a fake `fetch`, which is why `LiveMonitor` takes one:
 * "an unreachable server is a state and not a thrown exception" and "a slow answer does not stack
 * requests up behind it" are both claims about code that only exist while it runs.
 */

import { LiveMonitor, botsOf, botsOnPlane, currentState, describeBot, placeOf, readEnvelope } from '../js/live.js';

let failures = 0;

function check(name, got, want) {
  const a = JSON.stringify(got);
  const b = JSON.stringify(want);
  if (a === b) {
    console.log(`ok   ${name}`);
    return;
  }
  failures++;
  console.log(`FAIL ${name}\n  got  ${a}\n  want ${b}`);
}

const willow = { name: 'willow', script: 'chop_and_bank', x: 3103, y: 3218, plane: 0,
  tree: 'Repeat(-1)', tick: 42, path: 'Repeat(-1) > Sequence(4) > Gather(tree)',
  lastFailure: 'WalkToNearest -> FAILURE (after 40t)', history: ['t=41 Gather(tree) -> ENTER'] };
const oak = { name: 'oak', script: 'chop_and_bank', x: 3200, y: 3200, plane: 1,
  tree: 'Repeat(-1)', tick: 42, path: '', lastFailure: null, history: [] };

// ---- what an answer means ---------------------------------------------------------------------

check('a report is taken as live', readEnvelope({ live: true, status: { bots: [] }, error: null }),
  { live: true, report: { bots: [] }, error: null });

check('an unreachable server keeps the reason', readEnvelope({ live: false, status: null, error: 'no game server at 127.0.0.1:8081' }),
  { live: false, report: null, error: 'no game server at 127.0.0.1:8081' });

// A refusal with no message still has to say something: an empty hint is a panel that looks broken.
check('a refusal with no reason still has one', readEnvelope({ live: false }).error.length > 0, true);

// live:true with no report is a contradiction, and carrying the report anyway would show an empty list
// as if it were the truth.
check('live true without a report is not live', readEnvelope({ live: true, status: null }),
  { live: false, report: null, error: 'the game server sent no report' });

check('a body that is not an envelope is reported as such',
  readEnvelope('not json').live, false);
check('no body at all is reported as such', readEnvelope(null).live, false);

// ---- what a report contains -------------------------------------------------------------------

check('a report with no bots array has no bots', botsOf({ count: 0 }), []);
check('a bot with no name is not a bot', botsOf({ bots: [{ x: 1 }, willow] }).length, 1);
check('bots on another plane are not drawn', botsOnPlane({ bots: [willow, oak] }, 0).map((b) => b.name), ['willow']);
check('and are drawn on their own', botsOnPlane({ bots: [willow, oak] }, 1).map((b) => b.name), ['oak']);
check('drawn in a stable order', botsOnPlane({ bots: [oak, willow] }, 0).map((b) => b.name), ['willow']);
check('a null report draws nothing', botsOnPlane(null, 0), []);

// ---- how a bot reads --------------------------------------------------------------------------

// The path is the whole tree; what the bot is *doing* is its leaf.
check('the current state is the leaf of the path', currentState(willow), 'Gather(tree)');
check('a bot with nothing running says so', currentState(oak), '(no state running)');
check('a missing path says so too', currentState({}), '(no state running)');
check('a position is in the jump box\'s form', placeOf(willow), '3103,3218 p0');
check('a row is script, state and place',
  describeBot(willow), 'chop_and_bank  ·  Gather(tree)  ·  3103,3218 p0');

// ---- the monitor -----------------------------------------------------------------------------

/** A fetch that answers what it is told to, in order, and counts what it was asked. */
function fakeFetch(answers) {
  const calls = [];
  const impl = async (url) => {
    calls.push(url);
    const next = answers.length > 1 ? answers.shift() : answers[0];
    if (next instanceof Error) {
      throw next;
    }
    return { json: async () => next };
  };
  impl.calls = calls;
  return impl;
}

const liveAnswer = { live: true, status: { path: '/live/bots', count: 2, cap: 10, stats: '2 ticked', bots: [willow, oak] }, error: null };

{
  const impl = fakeFetch([liveAnswer]);
  const monitor = new LiveMonitor({ intervalMs: 1000000, fetchImpl: impl });
  await monitor.refresh();
  check('a report reached the panel', monitor.state.live, true);
  check('and its bots are listed', monitor.bots.map((b) => b.name), ['willow', 'oak']);
  // The route is the proxy's, not the game server's: the browser only ever talks to the workshop.
  check('asked the route the workshop serves', impl.calls, ['/live/bots']);
  // Not watching means no interval: a panel that polled while switched off would be a panel that keeps
  // asking a stopped server for as long as the tab is open.
  check('a refresh on its own does not start polling', monitor.watching, false);
}

{
  // The whole point: a stopped game server is the ordinary case, so it must not be an exception the
  // caller has to catch.
  const monitor = new LiveMonitor({ intervalMs: 1000000, fetchImpl: fakeFetch([new Error('Failed to fetch')]) });
  const state = await monitor.refresh();
  check('an unreachable workshop server is a state', state.live, false);
  check('with the reason', state.error.includes('Failed to fetch'), true);
  check('and no bots', monitor.bots, []);
}

{
  // A refusal from the proxy is a normal answer, not a throw.
  const monitor = new LiveMonitor({ intervalMs: 1000000,
    fetchImpl: fakeFetch([{ live: false, status: null, error: 'no game server at 127.0.0.1:8081' }]) });
  const state = await monitor.refresh();
  check('a refusal is believed', state.live, false);
  check('with the address', state.error, 'no game server at 127.0.0.1:8081');
}

{
  // Two overlapping refreshes must be one request: a hanging answer would otherwise be joined by one
  // more every tick, which is how a polling tool becomes a denial of service on itself.
  let release;
  const gate = new Promise((resolve) => { release = resolve; });
  let calls = 0;
  const slow = async () => {
    calls++;
    await gate;
    return { json: async () => liveAnswer };
  };
  const monitor = new LiveMonitor({ intervalMs: 1000000, fetchImpl: slow });
  const first = monitor.refresh();
  const second = monitor.refresh();
  release();
  await Promise.all([first, second]);
  check('a refresh in flight is not started twice', calls, 1);
}

{
  const monitor = new LiveMonitor({ intervalMs: 5, fetchImpl: fakeFetch([liveAnswer]) });
  monitor.start();
  check('start polls', monitor.watching, true);
  for (let i = 0; i < 200 && monitor.bots.length === 0; i++) {
    await new Promise((resolve) => setTimeout(resolve, 5));
  }
  check('and keeps polling', monitor.bots.length, 2);
  monitor.start();
  check('start is idempotent', monitor.watching, true);
  monitor.stop();
  check('stop stops', monitor.watching, false);
}

if (failures > 0) {
  console.log(`\n${failures} failure(s)`);
  process.exit(1);
}
console.log('\nall live-view checks passed');
