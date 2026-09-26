export type SignatureCall = {
  openOffset: number;
  calleeStart: number;
  calleeEnd: number;
  activeParameter: number;
  argValues: string[];
};

const CALL_KEYWORDS = new Set([
  'if', 'while', 'for', 'switch', 'catch', 'synchronized', 'assert',
  'do', 'else', 'try', 'finally', 'return', 'throw', 'case', 'default',
  'break', 'continue', 'yield', 'instanceof', 'record'
]);

type ScanState = 'code' | 'line' | 'block' | 'string' | 'char' | 'textblock';

function isSpace(character: string): boolean {
  return character === ' ' || character === '\t' || character === '\n' || character === '\r' || character === '\f';
}

function isIdentifierCharacter(character: string): boolean {
  return /[\p{L}\p{N}_$]/u.test(character);
}

export function callContextAt(text: string, offset: number): SignatureCall | null {
  const limit = Math.max(0, Math.min(offset, text.length));
  const stack: Array<{ open: number; commas: number }> = [];
  let state: ScanState = 'code';

  for (let index = 0; index < limit; index += 1) {
    const character = text[index];

    if (state === 'line') {
      if (character === '\n') state = 'code';
      continue;
    }

    if (state === 'block') {
      if (character === '*' && text[index + 1] === '/') {
        state = 'code';
        index += 1;
      }
      continue;
    }

    if (state === 'string') {
      if (character === '\\') {
        index += 1;
        continue;
      }
      if (character === '"') state = 'code';
      continue;
    }

    if (state === 'char') {
      if (character === '\\') {
        index += 1;
        continue;
      }
      if (character === '\'') state = 'code';
      continue;
    }

    if (state === 'textblock') {
      if (character === '"' && text[index + 1] === '"' && text[index + 2] === '"') {
        state = 'code';
        index += 2;
      }
      continue;
    }

    if (character === '/' && text[index + 1] === '/') {
      state = 'line';
      index += 1;
      continue;
    }
    if (character === '/' && text[index + 1] === '*') {
      state = 'block';
      index += 1;
      continue;
    }
    if (character === '"') {
      if (text[index + 1] === '"' && text[index + 2] === '"') {
        state = 'textblock';
        index += 2;
        continue;
      }
      state = 'string';
      continue;
    }
    if (character === '\'') {
      state = 'char';
      continue;
    }
    if (character === '(') {
      stack.push({ open: index, commas: 0 });
      continue;
    }
    if (character === ')') {
      stack.pop();
      continue;
    }
    if (character === ',' && stack.length > 0) {
      stack[stack.length - 1].commas += 1;
    }
  }

  if (stack.length === 0) return null;

  for (let level = stack.length - 1; level >= 0; level -= 1) {
    const open = stack[level].open;
    let end = open;
    while (end > 0 && isSpace(text[end - 1])) end -= 1;
    let start = end;
    while (start > 0 && isIdentifierCharacter(text[start - 1])) start -= 1;
    if (start === end) continue;
    const callee = text.slice(start, end);
    if (CALL_KEYWORDS.has(callee)) continue;
    return {
      openOffset: open,
      calleeStart: start,
      calleeEnd: end,
      activeParameter: stack[level].commas,
      argValues: argumentValues(text, open, limit)
    };
  }

  return null;
}

export function signatureHoverRegionAt(text: string, offset: number): SignatureCall | null {
  const clamped = Math.max(0, Math.min(offset, text.length));
  let call = callContextAt(text, Math.min(clamped + 1, text.length));
  if (!call && clamped < text.length && text[clamped] === ')') {
    call = callContextAt(text, clamped);
  }
  if (!call) return null;
  if (clamped < call.openOffset) return null;
  const close = findMatchingClose(text, call.openOffset);
  if (close >= 0) {
    if (clamped > close) return null;
    return call;
  }
  const lineEnd = text.indexOf('\n', call.openOffset);
  const bound = lineEnd < 0 ? text.length : lineEnd;
  if (clamped > bound) return null;
  return call;
}

function findMatchingClose(text: string, open: number): number {
  let depth = 0;
  let state: ScanState = 'code';
  for (let index = open; index < text.length; index += 1) {
    const character = text[index];

    if (state === 'line') {
      if (character === '\n') state = 'code';
      continue;
    }
    if (state === 'block') {
      if (character === '*' && text[index + 1] === '/') {
        state = 'code';
        index += 1;
      }
      continue;
    }
    if (state === 'string') {
      if (character === '\\') {
        index += 1;
        continue;
      }
      if (character === '"') state = 'code';
      continue;
    }
    if (state === 'char') {
      if (character === '\\') {
        index += 1;
        continue;
      }
      if (character === '\'') state = 'code';
      continue;
    }
    if (state === 'textblock') {
      if (character === '"' && text[index + 1] === '"' && text[index + 2] === '"') {
        state = 'code';
        index += 2;
      }
      continue;
    }

    if (character === '/' && text[index + 1] === '/') {
      state = 'line';
      index += 1;
      continue;
    }
    if (character === '/' && text[index + 1] === '*') {
      state = 'block';
      index += 1;
      continue;
    }
    if (character === '"') {
      if (text[index + 1] === '"' && text[index + 2] === '"') {
        state = 'textblock';
        index += 2;
        continue;
      }
      state = 'string';
      continue;
    }
    if (character === '\'') {
      state = 'char';
      continue;
    }
    if (character === '(') {
      depth += 1;
      continue;
    }
    if (character === ')') {
      depth -= 1;
      if (depth === 0) return index;
    }
  }
  return -1;
}

function argumentValues(text: string, open: number, limit: number): string[] {
  let depth = 0;
  let state: ScanState = 'code';
  let close = -1;

  for (let index = open + 1; index < text.length; index += 1) {
    const character = text[index];

    if (state === 'line') {
      if (character === '\n') state = 'code';
      continue;
    }
    if (state === 'block') {
      if (character === '*' && text[index + 1] === '/') {
        state = 'code';
        index += 1;
      }
      continue;
    }
    if (state === 'string') {
      if (character === '\\') {
        index += 1;
        continue;
      }
      if (character === '"') state = 'code';
      continue;
    }
    if (state === 'char') {
      if (character === '\\') {
        index += 1;
        continue;
      }
      if (character === '\'') state = 'code';
      continue;
    }

    if (character === '/' && text[index + 1] === '/') {
      state = 'line';
      index += 1;
      continue;
    }
    if (character === '/' && text[index + 1] === '*') {
      state = 'block';
      index += 1;
      continue;
    }
    if (character === '"') {
      state = 'string';
      continue;
    }
    if (character === '\'') {
      state = 'char';
      continue;
    }
    if (character === '(' || character === '[' || character === '{' || character === '<') {
      depth += 1;
      continue;
    }
    if (character === ')' && depth === 0) {
      close = index;
      break;
    }
    if (character === ')' || character === ']' || character === '}' || character === '>') {
      depth = Math.max(0, depth - 1);
    }
  }

  const raw = text.slice(open + 1, close >= 0 ? close : Math.max(open + 1, limit));
  return splitArguments(raw).map(value => value.replace(/\s+/g, ' ').trim().slice(0, 60));
}

function splitArguments(source: string): string[] {
  const values: string[] = [];
  let depth = 0;
  let start = 0;
  let state: ScanState = 'code';

  for (let index = 0; index < source.length; index += 1) {
    const character = source[index];

    if (state === 'line') {
      if (character === '\n') state = 'code';
      continue;
    }
    if (state === 'block') {
      if (character === '*' && source[index + 1] === '/') {
        state = 'code';
        index += 1;
      }
      continue;
    }
    if (state === 'string') {
      if (character === '\\') {
        index += 1;
        continue;
      }
      if (character === '"') state = 'code';
      continue;
    }
    if (state === 'char') {
      if (character === '\\') {
        index += 1;
        continue;
      }
      if (character === '\'') state = 'code';
      continue;
    }

    if (character === '/' && source[index + 1] === '/') {
      state = 'line';
      index += 1;
      continue;
    }
    if (character === '/' && source[index + 1] === '*') {
      state = 'block';
      index += 1;
      continue;
    }
    if (character === '"') {
      state = 'string';
      continue;
    }
    if (character === '\'') {
      state = 'char';
      continue;
    }
    if (character === '(' || character === '[' || character === '{' || character === '<') {
      depth += 1;
      continue;
    }
    if (character === ')' || character === ']' || character === '}' || character === '>') {
      depth = Math.max(0, depth - 1);
      continue;
    }
    if (character === ',' && depth === 0) {
      values.push(source.slice(start, index));
      start = index + 1;
    }
  }

  const last = source.slice(start);
  if (last.trim() || values.length > 0) values.push(last);
  return values;
}
