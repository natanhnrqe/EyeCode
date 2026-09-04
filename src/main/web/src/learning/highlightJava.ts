const JAVA_KEYWORDS = new Set([
  'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const', 'continue',
  'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float', 'for', 'goto', 'if',
  'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new', 'package', 'private',
  'protected', 'public', 'record', 'return', 'sealed', 'short', 'static', 'strictfp', 'super', 'switch',
  'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'var', 'void', 'volatile', 'while',
]);

const JAVA_TOKEN = /\/\*[\s\S]*?\*\/|\/\/[^\r\n]*|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\b\d+(?:\.\d+)?(?:[dDfFlL])?\b|\b(?:[A-Za-z_$][\w$]*)\b/g;

function tokenClass(token: string): string | null {
  if (token.startsWith('//') || token.startsWith('/*')) return 'learning-token-comment';
  if (token.startsWith('"') || token.startsWith("'")) return 'learning-token-string';
  if (/^\d/.test(token)) return 'learning-token-number';
  if (JAVA_KEYWORDS.has(token)) return 'learning-token-keyword';
  if (/^[A-Z]/.test(token)) return 'learning-token-type';
  return null;
}

export type LearningHighlightCounts = {
  codeBlocks: number;
  javaBlocks: number;
  highlighted: number;
};

export function highlightLearningJavaCode(root: HTMLElement | null): LearningHighlightCounts {
  if (!root) return { codeBlocks: 0, javaBlocks: 0, highlighted: 0 };
  const codeBlocks = root.querySelectorAll('pre code').length;
  const javaBlocks = root.querySelectorAll('pre > code.language-java').length;
  let highlighted = 0;
  for (const block of root.querySelectorAll<HTMLElement>('pre > code.language-java:not([data-learning-highlighted])')) {
    const source = block.textContent ?? '';
    const fragment = document.createDocumentFragment();
    let cursor = 0;
    for (const match of source.matchAll(JAVA_TOKEN)) {
      const index = match.index ?? cursor;
      if (index > cursor) fragment.append(document.createTextNode(source.slice(cursor, index)));
      const token = match[0];
      const className = tokenClass(token);
      if (className) {
        const span = document.createElement('span');
        span.className = className;
        span.textContent = token;
        fragment.append(span);
      } else {
        fragment.append(document.createTextNode(token));
      }
      cursor = index + token.length;
    }
    if (cursor < source.length) fragment.append(document.createTextNode(source.slice(cursor)));
    block.replaceChildren(fragment);
    block.dataset.learningHighlighted = 'true';
    highlighted++;
  }
  return { codeBlocks, javaBlocks, highlighted };
}