/**
 * One parameter's form control, generated from its schema — shared by the timeline and the graph so the
 * two views cannot offer different ways to say the same thing.
 *
 * Everything about the control comes from `bot-nodes.json` (`BOT_TOOLING.md` §5): the type, the allowed
 * values of a `KIND`, whether it is required. Nothing here names a parameter, which is why a new
 * `@Param` appears in both views with no change to either.
 *
 * It is a plain function of `(param, get, set)` rather than a method on a view, because the two views
 * disagree about almost everything else — list position, nesting, rebuilding — and agree about exactly
 * this. `set` is handed a value already coerced to the schema's type, so a view stores what the file
 * needs rather than what an `<input>` handed back.
 */

import { coerce, defaultFor } from './script-doc.js';

/** A parameter's form, as a `<label>` with its name and, when required, a marker. */
export function paramField(param, get, set) {
  const field = el('label', 'step-field');
  field.title = param.description || param.name;
  field.append(el('span', 'step-param', param.name + (param.required ? ' *' : '')));

  const current = get();

  switch (param.type) {
    case 'BOOLEAN': {
      const input = document.createElement('input');
      input.type = 'checkbox';
      input.checked = current === true;
      input.addEventListener('change', () => set(coerce(param, input.checked)));
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
        select.addEventListener('change', () => set(coerce(param, select.value)));
        field.append(select);
      } else {
        field.append(textInput(param, current, set));
      }
      break;
    }
    case 'TILE':
      field.append(tileInputs(current, set));
      break;
    case 'LOCATION':
      // No node declares one yet, and ScriptDocument refuses to encode one, so the editor says so
      // rather than inventing a field the loader would reject.
      field.append(el('span', 'step-unsupported', 'not supported yet'));
      break;
    default: {
      // INT and STRING both read as text; INT is coerced on the way out.
      const input = textInput(param, current, set);
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

/** The value a node states for a parameter, or the schema's default. What {@code get} normally returns. */
export function statedOr(param, value) {
  return value === undefined ? defaultFor(param) : value;
}

function textInput(param, current, set) {
  const input = document.createElement('input');
  input.type = 'text';
  input.value = current === undefined || current === null ? '' : String(current);
  if (param.name === 'name') {
    input.spellcheck = false;
  }
  input.addEventListener('change', () => set(coerce(param, input.value)));
  return input;
}

/** A TILE parameter is {x, y, plane}; plane is optional, so a blank axis is omitted rather than zero. */
function tileInputs(current, set) {
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
      set(Object.keys(next).length ? next : undefined);
    });
    inputs[axis] = input;
    wrap.append(input);
  }
  return wrap;
}

export function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  if (text !== undefined) {
    node.textContent = text;
  }
  return node;
}

/** A small icon button, the shape both views use for reorder/duplicate/delete. */
export function iconButton(label, title, action) {
  const button = el('button', 'step-button', label);
  button.type = 'button';
  button.title = title;
  button.addEventListener('click', action);
  return button;
}
