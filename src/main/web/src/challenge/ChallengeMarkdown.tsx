import type React from 'react';

const INLINE_PATTERN = /`([^`]+)`|\*\*([^*]+)\*\*/g;

function parseInline(text: string): React.ReactNode[] {
  const nodes: React.ReactNode[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  INLINE_PATTERN.lastIndex = 0;
  while ((match = INLINE_PATTERN.exec(text)) !== null) {
    if (match.index > lastIndex) {
      nodes.push(text.slice(lastIndex, match.index));
    }
    if (match[1] !== undefined) {
      nodes.push(<code key={`code-${match.index}`}>{match[1]}</code>);
    } else if (match[2] !== undefined) {
      nodes.push(<strong key={`bold-${match.index}`}>{match[2]}</strong>);
    }
    lastIndex = INLINE_PATTERN.lastIndex;
  }
  if (lastIndex < text.length) {
    nodes.push(text.slice(lastIndex));
  }
  return nodes;
}

function renderInlineList(items: string[], key: string): React.ReactElement {
  return (
    <ul key={key}>
      {items.map((item, index) => (
        <li key={index}>{parseInline(item)}</li>
      ))}
    </ul>
  );
}

export function ChallengeMarkdown({ markdown }: { markdown: string }): React.ReactElement {
  const source = typeof markdown === 'string' ? markdown : '';
  if (!source.trim()) {
    return <p>Nenhum conteúdo disponível.</p>;
  }

  const lines = source.replace(/\r\n?/g, '\n').split('\n');
  const nodes: React.ReactNode[] = [];
  let paragraph: string[] = [];
  let listItems: string[] = [];
  let index = 0;
  let key = 0;

  const flushParagraph = () => {
    if (!paragraph.length) return;
    const text = paragraph.join(' ').trim();
    if (text) {
      nodes.push(<p key={`p-${key++}`}>{parseInline(text)}</p>);
    }
    paragraph = [];
  };

  const flushList = () => {
    if (!listItems.length) return;
    nodes.push(renderInlineList(listItems, `ul-${key++}`));
    listItems = [];
  };

  while (index < lines.length) {
    const line = lines[index];
    const fence = /^```(\w+)?\s*$/.exec(line.trim());
    if (fence) {
      flushParagraph();
      flushList();
      const language = fence[1];
      const code: string[] = [];
      index += 1;
      while (index < lines.length && !/^```\s*$/.test(lines[index].trim())) {
        code.push(lines[index]);
        index += 1;
      }
      index += 1;
      nodes.push(
        <pre key={`pre-${key++}`} className="challenge-code">
          <code className={language ? `language-${language}` : undefined}>{code.join('\n')}</code>
        </pre>
      );
      continue;
    }

    const heading = /^(#{1,3})\s+(.*)$/.exec(line);
    if (heading) {
      flushParagraph();
      flushList();
      const level = heading[1].length;
      const content = parseInline(heading[2].trim());
      if (level === 1) {
        nodes.push(<h1 key={`h-${key++}`}>{content}</h1>);
      } else if (level === 2) {
        nodes.push(<h2 key={`h-${key++}`}>{content}</h2>);
      } else {
        nodes.push(<h3 key={`h-${key++}`}>{content}</h3>);
      }
      index += 1;
      continue;
    }

    const listItem = /^-\s+(.*)$/.exec(line);
    if (listItem) {
      flushParagraph();
      listItems.push(listItem[1]);
      index += 1;
      continue;
    }

    if (!line.trim()) {
      flushParagraph();
      flushList();
      index += 1;
      continue;
    }

    flushList();
    paragraph.push(line.trim());
    index += 1;
  }

  flushParagraph();
  flushList();

  return <>{nodes}</>;
}
