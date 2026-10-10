/**
 * The script document, and the schema rules that read it — `BOT_TOOLING.md` §7.1, shared by the timeline
 * and the graph (`BOT_WORKSHOP_UX.md` §7).
 *
 * One document, two views. The timeline is a *projection*: it can hold the `[Repeat(]Sequence(steps)[)]`
 * shape it compiles to, and nothing else. The graph holds any shape the loader accepts, which is why a
 * document the timeline cannot represent is not read-only any more — it is read-only *in the timeline*.
 * Everything here is pure: no DOM, no fetch, no editor state. That is what makes "never loses data"
 * checkable rather than merely asserted — both views call the same two functions, so they cannot disagree
 * about what a document means.
 *
 * The schema is still the only source of truth. Nothing here names a parameter: `children`, `child` and
 * `count` are read off the node that declares them, which is what `BOT_TOOLING.md` §7.2 requires.
 */

/** A node schema is structure, not a step, exactly when it takes a child. The one filtering rule. */
export function isStepSchema(node) {
  return Boolean(node) && !node.params.some((param) => isChildType(param.type));
}

export function isChildType(type) {
  return type === 'NODE' || type === 'NODE_LIST';
}

/** A schema default as a real value. The annotation's default is a string, so INT/BOOLEAN convert. */
export function defaultFor(param) {
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
 * A value from an input, as the schema's type.
 *
 * INT is the one that matters: an `<input>` hands back text, and a text value where the loader wants a
 * whole number is a document the server refuses — so an author who typed "2" would be told their script is
 * invalid for a reason they cannot see. A blank or unusable number is `undefined`, which omits the
 * parameter so the node's default (or the loader's "missing required field") applies.
 */
export function coerce(param, value) {
  if (value === '' || value === null || value === undefined) {
    return undefined;
  }
  if (param.type === 'INT') {
    const number = Number(value);
    return Number.isFinite(number) ? Math.trunc(number) : undefined;
  }
  return value;
}

/** The name of a node's parameter of a given schema type — read off the palette, never assumed. */
export function structParam(byId, nodeId, type) {
  const node = byId.get(nodeId);
  if (!node) {
    return null;
  }
  const param = node.params.find((candidate) => candidate.type === type);
  return param ? param.name : null;
}

/** The child parameters of a node, in declared order. The graph nests one group per entry. */
export function childParams(node) {
  return node ? node.params.filter((param) => isChildType(param.type)) : [];
}

/** The scalar parameters of a node, in declared order. Both views generate a field for each. */
export function scalarParams(node) {
  return node ? node.params.filter((param) => !isChildType(param.type)) : [];
}

/** One step as a document node. A parameter the author never touched is left out, so it defaults. */
export function nodeObject(step) {
  const node = { node: step.id };
  for (const [key, value] of Object.entries(step.params)) {
    node[key] = value;
  }
  return node;
}

/** One step from a document node: the keys it states become the keys the editor keeps. */
export function stepFromNode(node, byId) {
  const schema = byId.get(node.node);
  const params = {};
  for (const param of schema ? schema.params : []) {
    if (Object.prototype.hasOwnProperty.call(node, param.name)) {
      params[param.name] = node[param.name];
    }
  }
  return { id: node.node, params };
}

/**
 * The document as the timeline holds it, or `null` when it is a shape a linear list cannot represent.
 *
 * Owning this rule in one place is what lets the graph be the general view: the timeline asks "can I hold
 * this?" and gets a yes or a no, rather than trying to hold it and quietly dropping the parts that do not
 * fit.
 *
 * @returns {{steps: Array, repeat: boolean, times: number}|null}
 */
export function timelineShape(root, byId) {
  if (!root || typeof root !== 'object') {
    return null;
  }
  const childName = structParam(byId, 'repeat', 'NODE');
  const countName = structParam(byId, 'repeat', 'INT');
  const childrenName = structParam(byId, 'sequence', 'NODE_LIST');
  if (!childName || !childrenName) {
    return null;
  }

  let body = root;
  let repeat = false;
  let times = -1;
  if (root.node === 'repeat' && root[childName] && root[childName].node === 'sequence') {
    repeat = true;
    times = typeof root[countName] === 'number' ? root[countName] : -1;
    body = root[childName];
  }

  let rawSteps;
  if (body.node === 'sequence' && Array.isArray(body[childrenName])) {
    rawSteps = body[childrenName];
  } else if (isStepSchema(byId.get(body.node))) {
    rawSteps = [body];
  } else {
    return null;
  }

  for (const step of rawSteps) {
    // A composite or a decorator among the steps is exactly what a linear list cannot hold.
    if (!step || typeof step !== 'object' || !isStepSchema(byId.get(step.node))) {
      return null;
    }
  }
  return { steps: rawSteps.map((step) => stepFromNode(step, byId)), repeat, times };
}

/** The document a timeline describes: the steps as a `sequence`, wrapped in a `repeat` when ticked. */
export function buildTimeline(steps, repeat, times, byId) {
  const childrenName = structParam(byId, 'sequence', 'NODE_LIST') || 'children';
  const childName = structParam(byId, 'repeat', 'NODE') || 'child';
  const countName = structParam(byId, 'repeat', 'INT') || 'count';

  const sequence = { node: 'sequence' };
  sequence[childrenName] = steps.map(nodeObject);
  if (!repeat) {
    return sequence;
  }
  const wrapper = { node: 'repeat' };
  wrapper[childName] = sequence;
  if (times !== -1) {
    wrapper[countName] = times;
  }
  return wrapper;
}

// ---- walking a document ------------------------------------------------------------------------

/**
 * Where a node sits, as a path of object keys and array indices from the root. This is the whole
 * addressing scheme the graph view needs: a path is enough to find, replace or remove any node, so the
 * view holds no parent pointers and a re-render cannot leave one stale.
 */
export function nodeAt(root, path) {
  let node = root;
  for (const step of path) {
    if (node === null || node === undefined) {
      return undefined;
    }
    node = node[step];
  }
  return node;
}

/** The container holding a node and the key into it, so a node can be removed or replaced. */
export function locate(root, path) {
  if (path.length === 0) {
    return { container: null, key: null };
  }
  const key = path[path.length - 1];
  const container = nodeAt(root, path.slice(0, -1));
  return { container, key };
}

/** Removes a node. The root cannot be removed — that is the document. */
export function removeAt(root, path) {
  if (path.length === 0) {
    return false;
  }
  const { container, key } = locate(root, path);
  if (Array.isArray(container)) {
    container.splice(key, 1);
    return true;
  }
  return delete container[key];
}

/** Moves a node within its parent list. No-op for a single-child parameter, which has no order. */
export function moveAt(root, path, delta) {
  const { container, key } = locate(root, path);
  if (!Array.isArray(container)) {
    return false;
  }
  const target = key + delta;
  if (target < 0 || target >= container.length) {
    return false;
  }
  const [node] = container.splice(key, 1);
  container.splice(target, 0, node);
  return true;
}

/** Appends (or replaces, for a single-child parameter) a child of {@code parent}. */
export function addChild(parent, param, child) {
  if (param.type === 'NODE_LIST') {
    if (!Array.isArray(parent[param.name])) {
      parent[param.name] = [];
    }
    parent[param.name].push(child);
  } else {
    parent[param.name] = child;
  }
}

/** A fresh, empty node of a schema id: only the id, so every parameter takes its default. */
export function emptyNode(id) {
  return { node: id };
}

/**
 * Replaces a node's id in place, keeping what still applies.
 *
 * <p>Used when the author changes the root (or a step) from one node to another: children are carried
 * across when the new node takes a compatible child parameter, because losing a branch on a change of mind
 * is the one thing an editor must not do.
 */
export function convertNode(node, id, byId) {
  const next = emptyNode(id);
  // Only the *target's* parameters, and only those the source actually states: copying a source parameter
  // the target does not declare would put an unknown field in the document, which the loader refuses
  // (e.g. a `repeat`'s `count` onto a `sequence`, which has no such parameter).
  for (const param of scalarParams(byId.get(id))) {
    if (Object.prototype.hasOwnProperty.call(node, param.name)) {
      next[param.name] = node[param.name];
    }
  }
  const target = childParams(byId.get(id));
  const source = childParams(byId.get(node.node));
  for (let i = 0; i < target.length; i++) {
    const from = source[i];
    if (!from || !Object.prototype.hasOwnProperty.call(node, from.name)) {
      continue;
    }
    const value = node[from.name];
    if (target[i].type === 'NODE_LIST' && Array.isArray(value)) {
      next[target[i].name] = value;
    } else if (target[i].type === 'NODE' && value && !Array.isArray(value)) {
      next[target[i].name] = value;
    } else if (target[i].type === 'NODE_LIST' && value && !Array.isArray(value)) {
      next[target[i].name] = [value];
    } else if (target[i].type === 'NODE' && Array.isArray(value) && value.length) {
      next[target[i].name] = value[0];
    }
  }
  return next;
}
