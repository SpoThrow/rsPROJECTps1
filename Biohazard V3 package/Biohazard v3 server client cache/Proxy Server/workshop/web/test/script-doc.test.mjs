/**
 * The document model, checked without a browser — `gradlew workshopJsTest`.
 *
 * `script-doc.js` is the one place that decides what a document *means*: whether the timeline can hold it,
 * what the timeline compiles to, and how a node is addressed and replaced. Both views depend on it and
 * neither can see it break, because a wrong answer there shows up as an editor that quietly refuses a
 * script or, worse, drops a step on save. That is why it is tested directly and against the real
 * `bot-nodes.json` rather than a fixture: the parameter names `child`, `children` and `count` are read off
 * the schema, so a fixture could agree with a bug.
 *
 * Runs under Node (the only Node the workshop needs; the viewer itself is dependency-free). No framework:
 * the assertions are `deepEqual` and the exit code is the result. `bot-nodes.json` must exist — run
 * `gradlew workshopExportNodes` if this reports it missing.
 */

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

import {
  addChild, buildTimeline, convertNode, moveAt, nodeAt, removeAt, timelineShape,
} from '../js/script-doc.js';

const here = dirname(fileURLToPath(import.meta.url));
const schemaPath = process.argv[2]
  || join(here, '..', '..', '..', 'Data', 'workshop', 'bot-nodes.json');

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

let nodes;
try {
  nodes = JSON.parse(readFileSync(schemaPath, 'utf8')).nodes;
} catch (e) {
  console.error(`could not read ${schemaPath}: ${e.message}`);
  console.error('run "gradlew workshopExportNodes" first');
  process.exit(2);
}
const byId = new Map(nodes.map((node) => [node.id, node]));

// ---- compile and read back --------------------------------------------------------------------

const loop = [
  { id: 'walk_to_nearest', params: { kind: 'tree', range: 3 } },
  { id: 'gather', params: { kind: 'tree', itemId: 1511 } },
  { id: 'bank_logs', params: { logItemId: 1511 } },
];

const repeatDoc = buildTimeline(loop, true, -1, byId);
check('buildTimeline wraps the steps in a forever repeat', repeatDoc, {
  node: 'repeat',
  child: {
    node: 'sequence',
    children: [
      { node: 'walk_to_nearest', kind: 'tree', range: 3 },
      { node: 'gather', kind: 'tree', itemId: 1511 },
      { node: 'bank_logs', logItemId: 1511 },
    ],
  },
});

const shape = timelineShape(repeatDoc, byId);
check('timelineShape reads the steps back', shape.steps, loop);
check('timelineShape reads the repeat', [shape.repeat, shape.times], [true, -1]);

check('buildTimeline without the repeat is a bare sequence',
  buildTimeline([{ id: 'delay', params: {} }], false, -1, byId),
  { node: 'sequence', children: [{ node: 'delay' }] });
check('a counted repeat states its count',
  buildTimeline([{ id: 'delay', params: {} }], true, 5, byId),
  { node: 'repeat', child: { node: 'sequence', children: [{ node: 'delay' }] }, count: 5 });

// ---- what the timeline must refuse -----------------------------------------------------------

check('timelineShape refuses a nested composite',
  timelineShape({ node: 'sequence', children: [{ node: 'selector', children: [{ node: 'bank_open' }] }] }, byId),
  null);
check('timelineShape refuses a decorator around a step',
  timelineShape({ node: 'sequence', children: [{ node: 'retry', child: { node: 'delay' }, attempts: 3 }] }, byId),
  null);
check('timelineShape accepts a lone leaf as a one-step timeline',
  timelineShape({ node: 'bank_open' }, byId).steps,
  [{ id: 'bank_open', params: {} }]);

// ---- changing a node's id, which must not lose a branch or invent a field ---------------------

check('convertNode sequence -> selector keeps its children',
  convertNode({ node: 'sequence', children: [{ node: 'delay', ticks: 1 }, { node: 'bank_open' }] }, 'selector', byId),
  { node: 'selector', children: [{ node: 'delay', ticks: 1 }, { node: 'bank_open' }] });
check('convertNode sequence -> repeat takes the first child only',
  convertNode({ node: 'sequence', children: [{ node: 'delay', ticks: 1 }, { node: 'bank_open' }] }, 'repeat', byId),
  { node: 'repeat', child: { node: 'delay', ticks: 1 } });
check('convertNode repeat -> sequence wraps the single child in a list',
  convertNode({ node: 'repeat', count: 4, child: { node: 'delay', ticks: 1 } }, 'sequence', byId),
  { node: 'sequence', children: [{ node: 'delay', ticks: 1 }] });
// The regression this test exists for: `count` belongs to `repeat`, and a `sequence` that carried it
// would be a document the loader refuses for an unknown field.
check('convertNode drops a parameter the target does not declare',
  Object.prototype.hasOwnProperty.call(
    convertNode({ node: 'repeat', count: 4, child: { node: 'delay' } }, 'sequence', byId), 'count'),
  false);

// ---- addressing a node by path ---------------------------------------------------------------

const tree = {
  node: 'sequence',
  children: [
    { node: 'selector', children: [{ node: 'delay', ticks: 1 }, { node: 'bank_open' }] },
    { node: 'bank_logs', logItemId: 1 },
  ],
};
const deep = ['children', 0, 'children', 1];
check('nodeAt finds a node by path', nodeAt(tree, deep), { node: 'bank_open' });
check('moveAt reorders within the parent list',
  [moveAt(tree, deep, -1), tree.children[0].children.map((n) => n.node)],
  [true, ['bank_open', 'delay']]);
check('removeAt removes a node by path',
  [removeAt(tree, ['children', 0, 'children', 0]), tree.children[0].children.map((n) => n.node)],
  [true, ['delay']]);
check('removeAt refuses the root, because the root is the document', removeAt(tree, []), false);

const holder = { node: 'selector' };
addChild(holder, byId.get('selector').params.find((p) => p.name === 'children'), { node: 'bank_open' });
check('addChild creates the list a NODE_LIST parameter needs', holder,
  { node: 'selector', children: [{ node: 'bank_open' }] });
const single = { node: 'repeat' };
addChild(single, byId.get('repeat').params.find((p) => p.name === 'child'), { node: 'delay' });
check('addChild sets a single child', single, { node: 'repeat', child: { node: 'delay' } });

console.log(failures === 0 ? '\nAll checks passed.' : `\n${failures} check(s) FAILED.`);
process.exit(failures === 0 ? 0 : 1);
