/**
 * The step timeline — `BOT_WORKSHOP_UX.md` §5, the primary authoring surface.
 *
 * A bot is a list of steps, and this shows that list literally: an ordered set of nodes, wrapped in a
 * `Sequence` and optionally a `Repeat`. Everything about *what a step can be* comes from
 * `bot-nodes.json` (the exporter's reflection of the server's own annotated classes), so the editor
 * hardcodes no node id, no parameter name and no parameter type. A new `BotState` and annotation appear
 * here with zero changes to this file — which is the property `BOT_TOOLING.md` §5 is built around.
 *
 * Three things are deliberate:
 *
 *   - **The step palette offers leaves only.** A node that declares a `NODE` or `NODE_LIST` parameter
 *     (`repeat`, `sequence`, the decorators) is structure, not a step: it is what the timeline itself
 *     compiles to, or what a graph view (§7, a later stage) exists for. Offering them here would mean a
 *     nesting UI this pass does not have, and a half-built one.
 *   - **The editor never writes the file.** `compile()` produces the document, and it is sent to the
 *     workshop server, which validates it with the server's own loader and writes the canonical bytes
 *     (`botworkshop.export.ScriptDocs`). Same rule as the location authoring: the browser says what it
 *     means, the server decides what the file says.
 *   - **A script this editor did not write is opened read-only.** Reconstructing a timeline from an
 *     arbitrary graph would silently drop the parts a linear list cannot hold; showing the JSON and
 *     saying so loses nothing.
 */
export class TimelineEditor {

  /**
   * @param {HTMLElement} root the timeline section's element
   * @param {{onStatus?: (message: string, bad?: boolean) => void}} hooks
   */
  constructor(root, { onStatus } = {}) {
    this.root = root;
    this.onStatus = onStatus || (() => {});

    this.el = {
      name: root.querySelector('#timeline-name'),
      repeat: root.querySelector('#timeline-repeat'),
      times: root.querySelector('#timeline-times'),
      newButton: root.querySelector('#timeline-new'),
      open: root.querySelector('#timeline-open'),
      save: root.querySelector('#timeline-save'),
      validate: root.querySelector('#timeline-validate'),
      status: root.querySelector('#timeline-status'),
      palette: root.querySelector('#timeline-palette'),
      steps: root.querySelector('#timeline-steps'),
      file: root.querySelector('#timeline-file'),
      json: root.querySelector('#timeline-json'),
    };

    /** The step palette: node schemas with no child parameters. */
    this.nodes = [];
    /** Every node schema by id, steps and structure alike — the compile helpers need the latter. */
    this.byId = new Map();

    /** The script being edited. `params` holds only what the author changed, so a default is omitted. */
    this.name = '';
    this.steps = [];
    this.repeat = true;
    this.times = -1;

    /** Set when an opened script is not a shape this editor can hold. Then nothing is editable. */
    this.readOnly = false;
    this.readOnlyMessage = '';

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
    this.nodes = list.filter((node) => isStepNode(node));
    this.renderPalette();
    this.render();
  }

  /** A step is any node that takes no child: the leaves, however they are categorised. */
  isStepNode(id) {
    const node = this.byId.get(id);
    return Boolean(node) && isStepNode(node);
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
    this.steps = [];
    this.repeat = true;
    this.times = -1;
    this.readOnly = false;
    this.readOnlyMessage = '';
    this.el.name.value = '';
    this.el.repeat.checked = true;
    this.el.times.value = '-1';
    this.render();
    this.say('New script. Add steps from the palette, name it, and Save.');
  }

  /**
   * Loads a document written by this editor, or opens it read-only when it is not one.
   *
   * The shape this owns is `[Repeat(]Sequence(steps...)[)]` made of leaves. Anything else — a nested
   * composite, a decorator wrapping a child, a lone tree — is shown as JSON with the reason, because a
   * linear list cannot hold it and pretending otherwise would drop it on the next save.
   */
  loadScript(name, text) {
    this.name = name;
    this.el.name.value = name;
    let document;
    try {
      document = JSON.parse(text);
    } catch (e) {
      this.setReadOnly(`the file is not valid JSON: ${e.message}`, text);
      return;
    }
    const root = document ? document.root : null;
    if (!root || typeof root !== 'object') {
      this.setReadOnly('the document has no "root" node', text);
      return;
    }

    const childName = this.structParam('repeat', 'NODE');
    const countName = this.structParam('repeat', 'INT');
    const childrenName = this.structParam('sequence', 'NODE_LIST');
    if (!childName || !childrenName) {
      this.setReadOnly('the node palette has no sequence/repeat to build a timeline from', text);
      return;
    }

    let body = root;
    let repeat = false;
    let times = -1;
    if (root.node === 'repeat' && root[childName] && root[childName].node === 'sequence') {
      repeat = true;
      times = typeof root[countName] === 'number' ? root[countName] : -1;
      body = root[childName];
    }

    let rawSteps = null;
    if (body.node === 'sequence' && Array.isArray(body[childrenName])) {
      rawSteps = body[childrenName];
    } else if (this.isStepNode(body.node)) {
      rawSteps = [body];
    }

    if (!rawSteps || rawSteps.some((step) => !this.isStepNode(step.node))) {
      this.setReadOnly('this script uses nodes the linear timeline cannot hold '
        + '(a composite or a decorator around a step); edit it as a graph, or rebuild it here', text);
      return;
    }

    this.steps = rawSteps.map((step) => this.stepFromNode(step));
    this.repeat = repeat;
    this.times = times;
    this.readOnly = false;
    this.readOnlyMessage = '';
    this.el.repeat.checked = repeat;
    this.el.times.value = String(times);
    this.render();
    this.say(`Opened "${name}" — ${this.steps.length} step(s).`);
  }

  setReadOnly(message, text) {
    this.steps = [];
    this.readOnly = true;
    this.readOnlyMessage = message;
    this.render(text);
    this.say(`Opened "${this.name}" read-only: ${message}`, true);
  }

  /** One step from a document node: the keys it states become the keys the editor keeps. */
  stepFromNode(node) {
    const schema = this.byId.get(node.node);
    const params = {};
    for (const param of schema ? schema.params : []) {
      if (Object.prototype.hasOwnProperty.call(node, param.name)) {
        params[param.name] = node[param.name];
      }
    }
    return { id: node.node, params };
  }

  /** The name of a structural node's parameter of a given type, from the palette rather than assumed. */
  structParam(nodeId, type) {
    const node = this.byId.get(nodeId);
    if (!node) {
      return null;
    }
    const param = node.params.find((candidate) => candidate.type === type);
    return param ? param.name : null;
  }

  addStep(id) {
    if (this.readOnly) {
      return;
    }
    this.steps.push({ id, params: {} });
    this.render();
  }

  removeStep(index) {
    if (this.readOnly) {
      return;
    }
    this.steps.splice(index, 1);
    this.render();
  }

  duplicateStep(index) {
    if (this.readOnly) {
      return;
    }
    // A shallow copy is enough: params holds only primitives, so the copy shares nothing mutable.
    const copy = { id: this.steps[index].id, params: { ...this.steps[index].params } };
    this.steps.splice(index + 1, 0, copy);
    this.render();
  }

  moveStep(index, delta) {
    if (this.readOnly) {
      return;
    }
    const target = index + delta;
    if (target < 0 || target >= this.steps.length) {
      return;
    }
    const [step] = this.steps.splice(index, 1);
    this.steps.splice(target, 0, step);
    this.render();
  }

  /**
   * The behaviour-graph document this timeline describes — `BOT_TOOLING.md` §7.1's format, which is
   * also the shape `ScriptDocument` loads.
   */
  compile() {
    const childrenName = this.structParam('sequence', 'NODE_LIST') || 'children';
    const childName = this.structParam('repeat', 'NODE') || 'child';
    const countName = this.structParam('repeat', 'INT') || 'count';

    const children = this.steps.map((step) => this.nodeObject(step));
    const sequence = { node: 'sequence' };
    sequence[childrenName] = children;
    if (!this.repeat) {
      return { root: sequence };
    }
    const repeat = { node: 'repeat' };
    repeat[childName] = sequence;
    if (this.times !== -1) {
      repeat[countName] = this.times;
    }
    return { root: repeat };
  }

  /** One step as a document node. A parameter the author never touched is left out, so it defaults. */
  nodeObject(step) {
    const node = { node: step.id };
    for (const [key, value] of Object.entries(step.params)) {
      if (value === '' || value === null || value === undefined) {
        continue;
      }
      node[key] = value;
    }
    return node;
  }

  documentText() {
    if (this.readOnly) {
      return this.readOnlyJson || '';
    }
    return JSON.stringify(this.compile(), null, 2);
  }

  // ---- rendering -----------------------------------------------------------------------------

  render(readOnlyJson) {
    if (readOnlyJson !== undefined) {
      this.readOnlyJson = readOnlyJson;
    }
    this.el.name.disabled = this.readOnly;
    this.el.repeat.disabled = this.readOnly;
    this.el.times.disabled = this.readOnly;
    this.el.save.disabled = this.readOnly;
    this.renderSteps();
    this.renderJson();
    for (const button of this.el.palette.querySelectorAll('button')) {
      button.disabled = this.readOnly;
    }
  }

  renderSteps() {
    this.el.steps.replaceChildren();
    if (this.readOnly) {
      this.el.steps.append(el('div', 'step-empty bad', this.readOnlyMessage));
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
      fields.append(this.paramField(step, param));
    }
    body.append(fields);
    row.append(body);

    const actions = el('div', 'step-actions');
    actions.append(
      this.stepButton('↑', 'Move up', () => this.moveStep(index, -1)),
      this.stepButton('↓', 'Move down', () => this.moveStep(index, 1)),
      this.stepButton('⧉', 'Duplicate', () => this.duplicateStep(index)),
      this.stepButton('⌫', 'Delete', () => this.removeStep(index)),
    );
    row.append(actions);
    return row;
  }

  stepButton(label, title, action) {
    const button = el('button', 'step-button', label);
    button.type = 'button';
    button.title = title;
    button.addEventListener('click', action);
    return button;
  }

  /** One parameter's input, generated from the schema type. */
  paramField(step, param) {
    const field = el('label', 'step-field');
    field.title = param.description || param.name;
    field.append(el('span', 'step-param', param.name + (param.required ? ' *' : '')));

    const current = Object.prototype.hasOwnProperty.call(step.params, param.name)
      ? step.params[param.name]
      : defaultFor(param);

    switch (param.type) {
      case 'BOOLEAN': {
        const input = document.createElement('input');
        input.type = 'checkbox';
        input.checked = current === true;
        input.addEventListener('change', () => this.setParam(step, param, input.checked));
        field.append(input);
        break;
      }
      case 'KIND': {
        // The allowed kinds ship in bot-nodes.json (from LocationKind), so this cannot offer one the
        // server would reject. A palette without them falls back to free text rather than an empty box.
        if (Array.isArray(param.values)) {
          const select = document.createElement('select');
          for (const value of param.values) {
            const option = document.createElement('option');
            option.value = value;
            option.textContent = value;
            select.append(option);
          }
          select.value = typeof current === 'string' ? current : param.values[0];
          select.addEventListener('change', () => this.setParam(step, param, select.value));
          field.append(select);
        } else {
          field.append(this.textInput(step, param, current));
        }
        break;
      }
      case 'TILE': {
        field.append(this.tileInputs(step, param, current));
        break;
      }
      case 'LOCATION':
        // No node declares one yet, and ScriptDocument refuses to encode one, so the editor says so
        // rather than inventing a field the loader would reject.
        field.append(el('span', 'step-unsupported', 'not supported yet'));
        break;
      default: {
        // INT and STRING both read as text; INT is coerced on the way out.
        const input = this.textInput(step, param, current);
        if (param.type === 'INT') {
          input.type = 'number';
          input.step = '1';
        }
        field.append(input);
        break;
      }
    }
    return field;
  }

  textInput(step, param, current) {
    const input = document.createElement('input');
    input.type = 'text';
    input.value = current === undefined || current === null ? '' : String(current);
    if (param.name === 'name') {
      input.spellcheck = false;
    }
    input.addEventListener('change', () => this.setParam(step, param, input.value));
    return input;
  }

  /** A TILE parameter is {x, y, plane}; plane is optional, so it is omitted when left blank. */
  tileInputs(step, param, current) {
    const wrap = el('span', 'tile-inputs');
    const tile = current && typeof current === 'object' ? current : {};
    const inputs = {};
    for (const axis of ['x', 'y', 'plane']) {
      const input = document.createElement('input');
      input.type = 'number';
      input.step = '1';
      input.placeholder = axis;
      input.title = axis;
      input.size = 4;
      input.value = typeof tile[axis] === 'number' ? String(tile[axis]) : '';
      input.addEventListener('change', () => {
        inputs[axis].value = input.value;
        const next = {};
        for (const key of ['x', 'y', 'plane']) {
          if (inputs[key].value !== '') {
            next[key] = Number(inputs[key].value);
          }
        }
        this.setParam(step, param, Object.keys(next).length ? next : undefined);
      });
      inputs[axis] = input;
      wrap.append(input);
    }
    return wrap;
  }

  /** Records an edited value and refreshes the compiled preview. */
  setParam(step, param, value) {
    const coerced = coerce(param, value);
    if (coerced === undefined) {
      delete step.params[param.name];
    } else {
      step.params[param.name] = coerced;
    }
    this.renderJson();
  }

  renderJson() {
    this.el.file.textContent = (this.name || 'untitled') + '.json';
    this.el.json.textContent = this.documentText();
  }

  // ---- save and validate ---------------------------------------------------------------------

  bind() {
    this.el.newButton.addEventListener('click', () => this.newScript());
    this.el.repeat.addEventListener('change', () => {
      this.repeat = this.el.repeat.checked;
      this.renderJson();
    });
    this.el.times.addEventListener('change', () => {
      const value = Number(this.el.times.value);
      this.times = Number.isFinite(value) && value >= 0 ? Math.floor(value) : -1;
      this.el.times.value = String(this.times);
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
    if (this.readOnly) {
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
    if (this.readOnly) {
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
    this.el.save.disabled = busy || this.readOnly;
    this.el.validate.disabled = busy || this.readOnly;
  }

  /**
   * The timeline's own status line, mirrored to the viewer's toolbar.
   *
   * Both, because the timeline's line is where an author is looking while they edit, and the toolbar's is
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

// ---- helpers ---------------------------------------------------------------------------------

/** A node is a timeline step exactly when it takes no child. The one rule the palette is filtered by. */
function isStepNode(node) {
  return !node.params.some((param) => param.type === 'NODE' || param.type === 'NODE_LIST');
}

/** A schema default as a real value. The default is a string on the annotation, so INT/BOOLEAN convert. */
function defaultFor(param) {
  if (param.default === null || param.default === undefined) {
    return undefined;
  }
  if (param.type === 'INT') {
    return Number(param.default);
  }
  if (param.type === 'BOOLEAN') {
    return param.default === 'true';
  }
  return param.default;
}

/**
 * A value from an input as the schema's type.
 *
 * <p>INT is the one that matters: an `<input>` hands back text, and a text value where the loader wants a
 * whole number is a document the server refuses — so an author who typed "2" would be told their script is
 * invalid for a reason they cannot see. Coercing here keeps what is stored the same shape the file needs.
 * A blank or unusable number is `undefined`, which omits the parameter so the node's default (or the
 * loader's "missing required field") applies.
 */
function coerce(param, value) {
  if (value === '' || value === null || value === undefined) {
    return undefined;
  }
  if (param.type === 'INT') {
    const number = Number(value);
    return Number.isFinite(number) ? Math.trunc(number) : undefined;
  }
  return value;
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

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  if (text !== undefined) {
    node.textContent = text;
  }
  return node;
}
