import type { Challenge, ChallengeDocumentationReference, ChallengeExample } from './types';

type LegacyStatement = {
  instructions: string;
  businessRules: string[];
  examples: ChallengeExample[];
  constraints: string[];
  hints: string[];
};

function sectionKey(title: string): string {
  return title.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR').trim();
}

export function parseLegacyStatement(markdown: string): LegacyStatement {
  const sections = new Map<string, string[]>();
  const introductory: string[] = [];
  let current = '';
  let inCode = false;
  let codeLanguage = '';
  let code: string[] = [];

  const add = (value: string) => {
    if (current) sections.get(current)?.push(value);
    else introductory.push(value);
  };

  for (const line of (markdown || '').replace(/\r\n?/g, '\n').split('\n')) {
    const heading = /^(#{1,3})\s+(.*)$/.exec(line.trim());
    if (!inCode && heading) {
      current = heading[1].length === 1 ? '' : sectionKey(heading[2]);
      if (current) sections.set(current, []);
      continue;
    }
    const fence = /^```([\w+-]*)\s*$/.exec(line.trim());
    if (fence) {
      if (!inCode) {
        inCode = true;
        codeLanguage = fence[1];
        code = [];
      } else {
        add(`\u0000${codeLanguage}\n${code.join('\n')}`);
        inCode = false;
      }
      continue;
    }
    if (inCode) code.push(line);
    else if (/^-\s+/.test(line.trim())) add(line.trim().replace(/^-\s+/, '').trim());
    else if (line.trim()) add(line.trim());
  }

  const read = (...names: string[]) => names.flatMap(name => sections.get(sectionKey(name)) || []);
  const codeValue = (value: string) => value.startsWith('\u0000') ? value.slice(value.indexOf('\n') + 1).trim() : value;
  const inputs = read('entrada', 'input').map(codeValue).filter(Boolean);
  const outputs = read('saída', 'saida', 'output', 'expected output').map(codeValue).filter(Boolean);
  const examples = inputs.reduce<ChallengeExample[]>((result, input, index) => {
    if (outputs[index]) result.push({ input, output: outputs[index], ...(input.startsWith('isValid(') ? { language: 'java' } : {}) });
    return result;
  }, []);

  return {
    instructions: introductory.join(' '),
    businessRules: read('regras de negócio', 'regras', 'business rules'),
    examples,
    constraints: read('restrições', 'restricoes', 'constraints', 'edge cases', 'casos-limite'),
    hints: read('dicas', 'hints')
  };
}

export function validDocumentationReference(reference: ChallengeDocumentationReference): { id: string; title: string; section?: string; relevance?: string } | undefined {
  const id = reference.id.trim();
  const title = reference.title.trim();
  if (!/^[a-z0-9][a-z0-9/._-]*$/.test(id) || id.includes('..') || !title) return undefined;
  return { id, title, section: reference.section?.trim() || undefined, relevance: reference.relevance?.trim() || undefined };
}

export function legacyStatementFor(challenge: Challenge): LegacyStatement {
  return parseLegacyStatement(challenge.descriptionMarkdown);
}
