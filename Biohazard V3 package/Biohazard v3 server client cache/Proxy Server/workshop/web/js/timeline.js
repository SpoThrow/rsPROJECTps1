/**
 * The script editor — `BOT_WORKSHOP_UX.md` §5 (the step timeline) and §7 (the graph), two views of one
 * document (`BOT_TOOLING.md` §7.1).
 *
 * <p><b>One document, and this class owns it.</b> Not the timeline's step list, which is a projection that
 * only exists while the document is a shape a list can hold. Everything the timeline shows is derived from
 * `this.document` and everything it edits rebuilds it, so the two views cannot drift: the graph edits the
 * document directly, and the timeline re-derives from it afterwards. A document the timeline cannot
 * represent is therefore not read-only any more — it is read-only *there*, and the graph is where it is
 * edited. That is `§7`'s promise, and `script-doc.js` is the single place the answer "can the timeline hold
 * this?" is decided.
 *
 * <p><b>Everything about what a step can be comes from `bot-nodes.json`</b> (the exporter's reflection of
 * the server's own annotated classes), so this file hardcodes no node id, no parameter name and no
 * parameter type. A new `BotState` and annotation appear in both views with zero changes here — which is
 * the property `BOT_TOOLING.md` §5 is built around.
 *
 * <p><b>The editor never writes the file.</b> `compile()` produces the document, and it is sent to the
 * workshop server, which validates it with the server's own loader and writes the canonical bytes
 * (`botworkshop.export.ScriptDocs`). The browser says what it means; the server decides what the file says.
 */

import {
  buildTimeline, isStepSchema, stepFromNode, structParam, timelineShape,
} from './script-doc.js';
import { el, iconButton, paramField, statedOr } from './fields.js';
import { GraphView } from './graph.js';

export class TimelineEditor {

  /**
   * @param {HTMLElement} panel the editor section's element
   * @param {{onStatus?: (message: string, bad?: boolean) => void}} hooks
   */
  constructor(panel, { onStatus } = {}) {
    this.panel = panel;
    this.onStatus = onStatus || (() => {});

    this.el = {
      name: panel.querySelector('#timeline-name'),
      repeat: panel.querySelector('#timeline-repeat'),
      times: panel.querySelector('#timeline-times'),
      newButton: panel.querySelector('#timeline-new'),
      open: panel.querySelector('#timeline-open'),
      save: panel.querySelector('#timeline-save'),
      validate: panel.querySelector('#timeline-validate'),
      status: panel.querySelector('#timeline-status'),
      palette: panel.querySelector('#timeline-palette'),
      steps: panel.querySelector('#timeline-steps'),
      file: panel.querySelector('#timeline-file'),
      json: panel.querySelector('#timeline-json'),
      viewTimeline: panel.querySelector('#view-timeline'),
      viewGraph: panel.querySelector('#view-graph'),
      timelineView: panel.querySelector('#timeline-view'),
      graphView: panel.querySelector('#graph-view'),
    };

    /** The step palette: node schemas with no child parameters. */
    this.nodes = [];
    /** Every node schema by id, steps and structure alike — the graph offers them all. */
    this.byId = new Map();

    /** The script being edited: the whole document, not just its root, so extra keys survive a save. */
    this.document = null;
    this.name = '';

    /** Derived from {@link #document} by {@link #deriveTimeline}. Never edited directly. */
    this.steps = [];
    this.repeat = true;
    this.times = -1;
    /** Whether the timeline can hold the current document. When false the graph is the only editor. */
    this.timelineHolds = false;

    /** Set when the file itself cannot be read, which no view can edit. */
    this.broken = false;
    this.brokenMessage = '';
    this.brokenText = '';

    this.view = 'timeline';
    this.graph = new GraphView(this.el.graphView, this);

    /** The scripts on disk, from `/scripts.json`. */
    this.scripts = [];

    this.bind();
  }

  // ---- palette -------------------------------------------------------------------------------

  /**
   * The node schemas from `bot-nodes.json`. Called once at boot and again if the palette is reloaded.
   */
  setPalette(doc) {
    const list = (doc && doc.nodes) || [];
    this.byId = new Map(list.map((node) => [node.id, node]));
    this.nodes = list.filter((node) => isStepSchema(node));
    this.renderPalette();
    // The palette decides what "sequence"/"repeat" are called, so a document loaded before it must be
    // re-read now that those names are known.
    if (this.document) {
      this.deriveTimeline();
      this.el.repeat.checked = this.repeat;
      this.el.times.value = String(this.times);
    }
    this.render();
  }

  /** A step is any node that takes no child: the leaves, however they are categorised. */
  isStepNode(id) {
    return isStepSchema(this.byId.get(id));
  }

  renderPalette() {
    this.el.palette.replaceChildren();
    if (this.nodes.length === 0) {
      this.el.palette.append(el('p', 'hint',
        'No node palette: run "gradlew workshopExportNodes" and reload.'));
      return;
    }
    // Grouped by the category the exporter read from @BotNode, so the grouping is the server's too.
    const groups = new Map();
    for (const node of this.nodes) {
      const key = node.category || 'node';
      if (!groups.has(key)) {
        groups.set(key, []);
      }
      groups.get(key).push(node);
    }
    for (const [category, nodes] of groups) {
      this.el.palette.append(el('div', 'palette-head', category));
      for (const node of nodes) {
        const button = el('button', 'palette-add', `+ ${node.id}`);
        button.type = 'button';
        button.title = node.summary || node.className;
        button.addEventListener('click', () => this.addStep(node.id));
        this.el.palette.append(button);
      }
    }
  }

  // ---- the document --------------------------------------------------------------------------

  newScript() {
    this.name = '';
    this.document = { root: buildTimeline([], true, -1, this.byId) };
    this.broken = false;
    this.brokenMessage = '';
    this.brokenText = '';
    this.el.name.value = '';
    this.deriveTimeline();
    this.el.repeat.checked = this.repeat;
    this.el.times.value = String(this.times);
    this.render();
    this.say('New script. Add steps from the palette, name it, and Save.');
  }

  /**
   * Opens a document. A file this editor cannot read at all is shown read-only; a document that is merely
   * beyond the timeline — a nested composite, a decorator around a step — opens in the graph, which holds
   * it exactly.
   */
  loadScript(name, text) {
    this.name = name;
    this.el.name.value = name;
    let document;
    try {
      document = JSON.parse(text);
    } catch (e) {
      this.setBroken(`the file is not valid JSON: ${e.message}`, text);
      return;
    }
    const root = document ? document.root : null;
    if (!root || typeof root !== 'object') {
      this.setBroken('the document has no "root" node', text);
      return;
    }

    this.document = document;
    this.broken = false;
    this.brokenMessage = '';
    this.brokenText = '';
    this.deriveTimeline();
    this.el.repeat.checked = this.repeat;
    this.el.times.value = String(this.times);
    this.render();

    const what = this.timelineHolds
      ? `${this.steps.length} step(s)`
      : 'a graph the timeline cannot flatten — opened in the Graph view';
    this.say(`Opened "${name}" — ${what}.`);
    if (!this.timelineHolds) {
      this.setView('graph');
    }
  }

  setBroken(message, text) {
    this.document = null;
    this.broken = true;
    this.brokenMessage = message;
    this.brokenText = text || '';
    this.deriveTimeline();
    this.render();
    this.say(`Opened "${this.name}" read-only: ${message}`, true);
  }

  /**
   * Re-reads the derived step list from the document — the one-way flow that keeps the views honest.
   *
   * The timeline owns no state of its own beyond this projection, so a graph edit needs nothing more than
   * this call to be reflected there, and the timeline cannot hold a document the graph has made
   * un-linear without knowing. A `null` result is an answer, not a failure: it means "not my shape".
   */
  deriveTimeline() {
    const root = this.document ? this.document.root : null;
    const shape = this.broken ? null : timelineShape(root, this.byId);
    if (shape) {
      this.steps = shape.steps;
      this.repeat = shape.repeat;
      this.times = shape.times;
      this.timelineHolds = true;
    } else {
      this.steps = [];
      this.timelineHolds = false;
    }
  }

  /** Rebuilds the document's root from the timeline projection. The only thing the timeline writes. */
  syncFromSteps() {
    if (this.document) {
      this.document.root = buildTimeline(this.steps, this.repeat, this.times, this.byId);
    }
  }

  /**
   * The document's root node — what the graph edits, and the only part either view addresses by path.
   *
   * A getter rather than a field so there is one place the document lives: the graph mutates the object it
   * gets here, and `syncFromSteps`/`setRoot` replace it, with nothing to keep in step by hand.
   */
  get root() {
    return this.document ? this.document.root : null;
  }

  // ---- the graph's half of the contract -------------------------------------------------------

  /** The graph replaced the document's root (a node changed shape, was added or removed). */
  setRoot(root) {
    if (this.document) {
      this.document.root = root;
    }
    this.graphChanged();
  }

  /** A structural graph edit: the timeline must re-derive, and the outline must be redrawn. */
  graphChanged() {
    this.deriveTimeline();
    this.render();
  }

  /** A scalar field edit: only the compiled preview and the tabs can have changed, so keep focus. */
  graphFieldChanged() {
    this.renderJson();
  }

  // ---- steps ---------------------------------------------------------------------------------

  addStep(id) {
    if (!this.editable()) {
      return;
    }
    this.steps.push({ id, params: {} });
    this.syncFromSteps();
    this.render();
  }

  removeStep(index) {
    if (!this.editable()) {
      return;
    }
    this.steps.splice(index, 1);
    this.syncFromSteps();
    this.render();
  }

  duplicateStep(index) {
    if (!this.editable()) {
      return;
    }
    // A shallow copy is enough: params holds only primitives, so the copy shares nothing mutable.
    const copy = { id: this.steps[index].id, params: { ...this.steps[index].params } };
    this.steps.splice(index + 1, 0, copy);
    this.syncFromSteps();
    this.render();
  }

  moveStep(index, delta) {
    if (!this.editable()) {
      return;
    }
    const target = index + delta;
    if (target < 0 || target >= this.steps.length) {
      return;
    }
    const [step] = this.steps.splice(index, 1);
    this.steps.splice(target, 0, step);
    this.syncFromSteps();
    this.render();
  }

  /** Whether the timeline may be edited: the file is readable and the shape is one a list can hold. */
  editable() {
    return !this.broken && this.timelineHolds;
  }

  // ---- the document as data ------------------------------------------------------------------

  /** The document this editor describes — `BOT_TOOLING.md` §7.1's format, which `ScriptDocument` loads. */
  compile() {
    if (this.broken || !this.document) {
      return { root: null };
    }
    return this.document;
  }

  documentText() {
    if (this.broken) {
      return this.brokenText || '';
    }
    return JSON.stringify(this.compile(), null, 2);
  }

  // ---- rendering -----------------------------------------------------------------------------

  render() {
    this.renderControls();
    this.renderSteps();
    this.renderJson();
    this.renderGraph();
    this.renderTabs();
  }

  renderControls() {
    const timelineEditable = this.editable();
    this.el.name.disabled = this.broken;
    this.el.repeat.disabled = !timelineEditable;
    this.el.times.disabled = !timelineEditable;
    this.el.save.disabled = this.broken;
    this.el.validate.disabled = this.broken;
    for (const button of this.el.palette.querySelectorAll('button')) {
      button.disabled = !timelineEditable;
    }
  }

  renderTabs() {
    this.el.viewTimeline.setAttribute('aria-selected', String(this.view === 'timeline'));
    this.el.viewGraph.setAttribute('aria-selected', String(this.view === 'graph'));
    // Only the graph can hold what the timeline cannot, so say so on the tab rather than leaving the
    // author to wonder why their steps vanished when they switch.
    this.el.viewTimeline.classList.toggle('warn', !this.broken && !this.timelineHolds);
    this.el.viewTimeline.title = this.broken
      ? this.brokenMessage
      : (this.timelineHolds ? 'The step list' : 'This document is not a step list — open the Graph view');
    this.el.timelineView.hidden = this.view !== 'timeline';
    this.el.graphView.hidden = this.view === 'timeline';
  }

  setView(view) {
    this.view = view === 'graph' ? 'graph' : 'timeline';
    this.renderTabs();
  }

  renderSteps() {
    this.el.steps.replaceChildren();
    if (this.broken) {
      this.el.steps.append(el('div', 'step-empty bad', this.brokenMessage));
      return;
    }
    if (!this.timelineHolds) {
      this.el.steps.append(el('div', 'step-empty bad',
        'this script uses nodes the linear timeline cannot hold (a composite or a decorator around a '
        + 'step) — edit it in the Graph view, or rebuild it as a flat list here'));
      return;
    }
    if (this.steps.length === 0) {
      this.el.steps.append(el('div', 'step-empty',
        'No steps yet. Click a node in the palette to add the first one.'));
      return;
    }
    this.steps.forEach((step, index) => {
      this.el.steps.append(this.stepRow(step, index));
    });
  }

  stepRow(step, index) {
    const schema = this.byId.get(step.id);
    const row = el('div', 'step');
    row.append(el('span', 'step-n', String(index + 1)));
    const body = el('div', 'step-body');
    const head = el('div', 'step-head');
    head.append(el('span', 'step-name', step.id));
    if (schema && schema.summary) {
      head.append(el('span', 'step-summary', schema.summary));
    }
    body.append(head);

    const fields = el('div', 'step-fields');
    for (const param of schema ? schema.params : []) {
      if (param.type === 'NODE' || param.type === 'NODE_LIST') {
        continue; // a step takes no child, so a schema that has one is not offered here at all
      }
      fields.append(paramField(
        param,
        () => statedOr(param, step.params[param.name]),
        (value) => this.setParam(step, param, value),
      ));
    }
    body.append(fields);
    row.append(body);

    const actions = el('div', 'step-actions');
    actions.append(
      iconButton('↑', 'Move up', () => this.moveStep(index, -1)),
      iconButton('↓', 'Move down', () => this.moveStep(index, 1)),
      iconButton('⧉', 'Duplicate', () => this.duplicateStep(index)),
      iconButton('⌫', 'Delete', () => this.removeStep(index)),
    );
    row.append(actions);
    return row;
  }

  renderGraph() {
    this.graph.render();
  }

  /** Records an edited value and refreshes the compiled preview. The value is already coerced. */
  setParam(step, param, value) {
    if (value === undefined) {
      delete step.params[param.name];
    } else {
      step.params[param.name] = value;
    }
    this.syncFromSteps();
    this.renderJson();
  }

  renderJson() {
    this.el.file.textContent = (this.name || 'untitled') + '.json';
    this.el.json.textContent = this.documentText();
  }

  // ---- save and validate ---------------------------------------------------------------------

  bind() {
    this.el.newButton.addEventListener('click', () => this.newScript());
    this.el.viewTimeline.addEventListener('click', () => this.setView('timeline'));
    this.el.viewGraph.addEventListener('click', () => this.setView('graph'));
    this.el.repeat.addEventListener('change', () => {
      this.repeat = this.el.repeat.checked;
      this.syncFromSteps();
      this.renderJson();
    });
    this.el.times.addEventListener('change', () => {
      const value = Number(this.el.times.value);
      this.times = Number.isFinite(value) && value >= 0 ? Math.floor(value) : -1;
      this.el.times.value = String(this.times);
      this.syncFromSteps();
      this.renderJson();
    });
    this.el.name.addEventListener('change', () => {
      this.name = this.el.name.value.trim();
      this.renderJson();
    });
    this.el.save.addEventListener('click', () => this.save());
    this.el.validate.addEventListener('click', () => this.validate());
    this.el.open.addEventListener('change', () => {
      if (this.el.open.value) {
        this.open(this.el.open.value);
      }
    });
  }

  async validate() {
    if (this.broken) {
      return;
    }
    this.setBusy(true);
    this.say('validating…');
    try {
      const report = await postScript('check', null, this.compile());
      if (report.ok) {
        this.el.json.textContent = report.canonical;
        this.say('Valid: the server\'s loader accepts this script.');
      } else {
        this.say(`Rejected: ${report.error}`, true);
      }
    } catch (e) {
      this.say(`could not reach the workshop server: ${e.message}`, true);
    } finally {
      this.setBusy(false);
    }
  }

  async save() {
    if (this.broken) {
      return;
    }
    const name = (this.el.name.value || '').trim();
    if (!/^[a-z0-9_]{1,48}$/.test(name)) {
      this.say('a script name must be 1-48 characters of a-z, 0-9 or underscore', true);
      return;
    }
    this.setBusy(true);
    this.say('saving…');
    try {
      const report = await postScript('save', name, this.compile());
      if (!report.ok) {
        this.say(`not saved: ${report.error}`, true);
        return;
      }
      this.name = name;
      this.el.json.textContent = report.canonical;
      this.el.file.textContent = name + '.json';
      this.say(`Saved Data/cfg/bots/${name}.json — reload the server with ::bot reload to run it.`);
      await this.refreshList(name);
    } catch (e) {
      this.say(`save failed: ${e.message}`, true);
    } finally {
      this.setBusy(false);
    }
  }

  setBusy(busy) {
    this.el.save.disabled = busy || this.broken;
    this.el.validate.disabled = busy || this.broken;
  }

  /**
   * The editor's own status line, mirrored to the viewer's toolbar.
   *
   * Both, because the editor's line is where an author is looking while they edit, and the toolbar's is
   * the only one visible when the panel is closed. The busy messages go through here too, so the line never
   * sticks on "saving…" after the answer has arrived.
   */
  say(message, bad) {
    this.el.status.textContent = message;
    this.el.status.classList.toggle('bad', Boolean(bad));
    this.onStatus(message, bad);
  }

  /** The scripts on disk, for the Open picker. */
  async refreshList(select) {
    try {
      const response = await fetch('/scripts.json');
      const doc = await response.json();
      this.scripts = doc.scripts || [];
    } catch (e) {
      this.scripts = [];
    }
    this.renderList(select);
  }

  renderList(select) {
    this.el.open.replaceChildren();
    const none = document.createElement('option');
    none.value = '';
    none.textContent = this.scripts.length ? '…' : '(none saved)';
    this.el.open.append(none);
    for (const script of this.scripts) {
      const option = document.createElement('option');
      option.value = script.name;
      option.textContent = script.ok ? script.name : `${script.name} (invalid)`;
      this.el.open.append(option);
    }
    if (select) {
      this.el.open.value = select;
    }
  }

  async open(name) {
    try {
      const response = await fetch(`/scripts/doc?name=${encodeURIComponent(name)}`);
      if (!response.ok) {
        this.say(`could not open "${name}": ${response.status}`, true);
        return;
      }
      this.loadScript(name, await response.text());
    } catch (e) {
      this.say(`could not open "${name}": ${e.message}`, true);
    }
  }
}

async function postScript(action, name, document) {
  const query = name ? `?name=${encodeURIComponent(name)}` : '';
  const response = await fetch(`/scripts/${action}${query}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify(document),
  });
  const report = await response.json();
  if (!response.ok && report.ok === undefined) {
    throw new Error(`POST /scripts/${action} -> ${response.status} ${response.statusText}`);
  }
  return report;
}
