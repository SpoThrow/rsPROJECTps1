/**
 * The behaviour graph — `BOT_TOOLING.md` §3b, the advanced view over the same document as the timeline
 * (`BOT_WORKSHOP_UX.md` §7).
 *
 * The timeline is a list, so it can hold one shape: `[Repeat(]Sequence(steps)[)]`. This view holds any
 * shape the loader accepts — `Selector`, `Parallel`, retries, decorators, nesting — because a document is
 * already a tree of objects and drawing it as one loses nothing. That is why an opened script the
 * timeline cannot represent is no longer read-only: it is read-only *there*, and editable here.
 *
 * <p><b>It is an outline, not a wire-and-node canvas, and that is a deliberate choice.</b> A bot tree is
 * tens of nodes deep and read top-to-bottom; indentation shows the whole thing at once, where a canvas
 * needs panning and hides the structure off-screen. Edges are implied by nesting, so there is no layout
 * state to persist and no way for the picture to disagree with the document. A canvas can be layered over
 * this later without changing what it edits.
 *
 * Nothing here writes a file or knows what a file is: it mutates the editor's document and calls back, and
 * the editor compiles, previews and sends it. Save still goes through the server's loader, so this view
 * cannot emit anything the runtime would refuse.
 */

import {
  addChild, childParams, convertNode, emptyNode, moveAt, removeAt, scalarParams,
} from './script-doc.js';
import { el, iconButton, paramField, statedOr } from './fields.js';

const CATEGORY_ORDER = ['composite', 'decorator', 'state', 'condition'];

export class GraphView {

  constructor(host, editor) {
    this.host = host;
    this.editor = editor;
  }

  render() {
    this.host.replaceChildren();
    const editor = this.editor;

    if (editor.broken) {
      this.host.append(el('div', 'step-empty bad', editor.brokenMessage));
      return;
    }

    const bar = el('div', 'graph-bar');
    bar.append(el('span', 'graph-root-label', 'root'));
    bar.append(this.nodeSelect(editor.root.node, (id) => {
      editor.setRoot(convertNode(editor.root, id, editor.byId));
    }));
    bar.append(el('span', 'hint',
      'A composite holds children; a leaf holds parameters. The timeline reads this same document.'));
    this.host.append(bar);

    this.host.append(this.nodeBox(editor.root, [], true));
  }

  /**
   * One node: its id, its scalar parameters, and one group per child parameter.
   *
   * @param {object} node the document node
   * @param {Array} path where it sits, as keys and indices from the root — see {@code script-doc.js}
   * @param {boolean} isRoot the root cannot be deleted, because it is the document
   */
  nodeBox(node, path, isRoot) {
    const editor = this.editor;
    const schema = editor.byId.get(node.node);
    const box = el('div', 'graph-node');
    const head = el('div', 'graph-head');
    head.append(el('span', 'graph-id', node.node));
    if (schema && schema.summary) {
      head.append(el('span', 'step-summary', schema.summary));
    }

    const actions = el('div', 'step-actions');
    if (!isRoot) {
      actions.append(
        iconButton('⧉', 'Duplicate', () => this.duplicate(node, path)),
        iconButton('⌫', 'Delete', () => {
          if (removeAt(editor.root, path)) {
            editor.graphChanged();
          }
        }),
      );
    }
    head.append(actions);
    box.append(head);

    if (schema) {
      const fields = el('div', 'step-fields');
      for (const param of scalarParams(schema)) {
        fields.append(paramField(
          param,
          () => statedOr(param, node[param.name]),
          (value) => {
            if (value === undefined) {
              delete node[param.name];
            } else {
              node[param.name] = value;
            }
            editor.graphChanged();
          },
        ));
      }
      if (fields.childElementCount) {
        box.append(fields);
      }
      for (const param of childParams(schema)) {
        box.append(this.childGroup(node, param, path));
      }
    } else {
      box.append(el('div', 'graph-empty bad', 'unknown node id — the palette has no ' + node.node));
    }
    return box;
  }

  /** A child parameter: the nodes under it, and a menu that adds one. */
  childGroup(owner, param, ownerPath) {
    const editor = this.editor;
    const group = el('div', 'graph-children');
    const head = el('div', 'graph-children-head');
    head.append(el('span', 'graph-param', param.name));
    head.append(this.addSelect(param, (id) => {
      addChild(owner, param, emptyNode(id));
      editor.graphChanged();
    }));
    group.append(head);

    const list = el('div', 'graph-child-list');
    const children = param.type === 'NODE_LIST'
      ? (Array.isArray(owner[param.name]) ? owner[param.name] : [])
      : (owner[param.name] ? [owner[param.name]] : []);
    if (children.length === 0) {
      list.append(el('div', 'graph-empty',
        param.type === 'NODE_LIST' ? 'no children' : 'no child'));
    }

    children.forEach((child, index) => {
      const path = param.type === 'NODE_LIST'
        ? [...ownerPath, param.name, index]
        : [...ownerPath, param.name];
      const row = el('div', 'graph-child');
      const bar = el('div', 'graph-child-bar');
      if (param.type === 'NODE_LIST') {
        bar.append(
          iconButton('↑', 'Move up', () => { if (moveAt(editor.root, path, -1)) { editor.graphChanged(); } }),
          iconButton('↓', 'Move down', () => { if (moveAt(editor.root, path, 1)) { editor.graphChanged(); } }),
        );
      } else {
        bar.append(iconButton('⌫', 'Clear', () => {
          delete owner[param.name];
          editor.graphChanged();
        }));
      }
      row.append(bar);
      row.append(this.nodeBox(child, path, false));
      list.append(row);
    });

    group.append(list);
    return group;
  }

  duplicate(node, path) {
    const editor = this.editor;
    // A deep copy, so editing the copy cannot reach back into the original. The document is plain JSON.
    const copy = JSON.parse(JSON.stringify(node));
    const { container, key } = locateForPath(editor.root, path);
    if (Array.isArray(container)) {
      container.splice(key + 1, 0, copy);
    } else if (container) {
      container[key] = copy;
    }
    editor.graphChanged();
  }

  /** Every node the palette offers, grouped by the category the exporter read from `@BotNode`. */
  byCategory() {
    const groups = new Map();
    const schemas = [...this.editor.byId.values()].sort((a, b) => {
      const byCategory = CATEGORY_ORDER.indexOf(a.category) - CATEGORY_ORDER.indexOf(b.category);
      return byCategory !== 0 ? byCategory : a.id.localeCompare(b.id);
    });
    for (const schema of schemas) {
      const key = CATEGORY_ORDER.includes(schema.category) ? schema.category : (schema.category || 'node');
      if (!groups.has(key)) {
        groups.set(key, []);
      }
      groups.get(key).push(schema);
    }
    return groups;
  }

  /** The menu that adds a child. Every node is offered: a step or a composite are both legitimate. */
  addSelect(param, onPick) {
    const select = el('select', 'graph-add');
    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = param.type === 'NODE_LIST' ? '+ add child' : '+ set child';
    select.append(placeholder);
    for (const [category, schemas] of this.byCategory()) {
      const group = document.createElement('optgroup');
      group.label = category;
      for (const schema of schemas) {
        const option = document.createElement('option');
        option.value = schema.id;
        option.textContent = schema.id;
        group.append(option);
      }
      select.append(group);
    }
    select.addEventListener('change', () => {
      if (select.value) {
        onPick(select.value);
        select.value = '';
      }
    });
    return select;
  }

  /** The root node's own picker. Changing it carries the children that still apply (`convertNode`). */
  nodeSelect(current, onPick) {
    const select = el('select', 'graph-root-select');
    for (const [category, schemas] of this.byCategory()) {
      const group = document.createElement('optgroup');
      group.label = category;
      for (const schema of schemas) {
        const option = document.createElement('option');
        option.value = schema.id;
        option.textContent = schema.id;
        group.append(option);
      }
      select.append(group);
    }
    select.value = current;
    select.addEventListener('change', () => onPick(select.value));
    return select;
  }
}

/** The container holding a node, so a sibling can be inserted next to it. */
function locateForPath(root, path) {
  if (path.length === 0) {
    return { container: null, key: null };
  }
  const key = path[path.length - 1];
  let container = root;
  for (const step of path.slice(0, -1)) {
    container = container[step];
  }
  return { container, key };
}
