export function renderHoverContent(kind: string | undefined, value: string): string {
  const effective = kind ?? 'plaintext';
  if (effective === 'html') return value;
  if (effective === 'markdown') return renderMarkdown(value);
  if (effective === 'java') return `<pre><code class="language-java">${escapeHtml(value)}</code></pre>`;
  return escapeHtml(value).replace(/\n/g, '<br>');
}

function renderMarkdown(source: string): string {
  const blocks: string[] = [];
  let paragraph: string[] = [];
  let items: string[] = [];
  let ordered = false;
  let fence: string[] | null = null;

  const flushParagraph = (): void => {
    if (paragraph.length > 0) {
      blocks.push(`<p>${paragraph.join('<br>')}</p>`);
      paragraph = [];
    }
  };
  const flushItems = (): void => {
    if (items.length > 0) {
      blocks.push(ordered ? `<ol>${items.join('')}</ol>` : `<ul>${items.join('')}</ul>`);
      items = [];
      ordered = false;
    }
  };
  const flushAll = (): void => {
    flushParagraph();
    flushItems();
  };

  for (const line of source.split(/\r?\n/)) {
    if (/^\s*```/.test(line)) {
      if (fence) {
        blocks.push(`<pre><code>${escapeHtml(fence.join('\n'))}</code></pre>`);
        fence = null;
      } else {
        flushAll();
        fence = [];
      }
      continue;
    }
    if (fence) {
      fence.push(line);
      continue;
    }
    const heading = /^(#{1,6})\s+(.*)$/.exec(line);
    if (heading) {
      flushAll();
      const level = heading[1].length;
      blocks.push(`<h${level}>${renderInline(heading[2])}</h${level}>`);
      continue;
    }
    const bullet = /^\s*[*-]\s+(.*)$/.exec(line);
    const numbered = /^\s*\d+\.\s+(.*)$/.exec(line);
    if (bullet || numbered) {
      flushParagraph();
      const isOrdered = numbered !== null && bullet === null;
      if (items.length > 0 && ordered !== isOrdered) flushItems();
      ordered = isOrdered;
      items.push(`<li>${renderInline((bullet ?? numbered)![1])}</li>`);
      continue;
    }
    if (!line.trim()) {
      flushAll();
      continue;
    }
    flushItems();
    paragraph.push(renderInline(line));
  }
  if (fence) blocks.push(`<pre><code>${escapeHtml(fence.join('\n'))}</code></pre>`);
  flushAll();
  return blocks.join('\n');
}

function renderInline(value: string): string {
  let out = escapeHtml(value);
  out = out.replace(/`([^`]+)`/g, '<code>$1</code>');
  out = out.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  out = out.replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>');
  out = out.replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, '<a href="$2" rel="noreferrer">$1</a>');
  return out;
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
