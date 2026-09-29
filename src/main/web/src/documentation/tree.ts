import type { DocumentationEntry } from './protocol';

export type DocumentationSubgroup = {
  key: string;
  label: string;
  entries: DocumentationEntry[];
};

export type DocumentationBranch = {
  branch: string;
  label: string;
  entries: DocumentationEntry[];
  subgroups: DocumentationSubgroup[];
  total: number;
};

export type TocItem = {
  id: string;
  text: string;
  level: number;
};

const BRANCH_LABELS: Record<string, string> = {
  jdk: 'JDK',
  spring: 'Spring Boot',
  javafx: 'JavaFX',
  junit: 'JUnit',
};

export function branchLabel(branch: string): string {
  const known = BRANCH_LABELS[branch];
  if (known) return known;
  return titleCaseWords(branch.split('-'));
}

export function subgroupLabel(subgroup: string): string {
  if (subgroup.includes('.')) return subgroup;
  return titleCaseWords(subgroup.split('-'));
}

function titleCaseWords(words: string[]): string {
  return words
    .filter(Boolean)
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export function groupBranches(entries: DocumentationEntry[]): DocumentationBranch[] {
  const groups = new Map<string, DocumentationBranch>();
  for (const entry of entries) {
    let branch = groups.get(entry.branch);
    if (!branch) {
      branch = { branch: entry.branch, label: branchLabel(entry.branch), entries: [], subgroups: [], total: 0 };
      groups.set(entry.branch, branch);
    }
    branch.total += 1;
    if (!entry.subgroup) {
      branch.entries.push(entry);
      continue;
    }
    let subgroup = branch.subgroups.find(item => item.key === entry.subgroup);
    if (!subgroup) {
      subgroup = { key: entry.subgroup, label: subgroupLabel(entry.subgroup), entries: [] };
      branch.subgroups.push(subgroup);
    }
    subgroup.entries.push(entry);
  }
  return [...groups.values()];
}

export function filterEntries(entries: DocumentationEntry[], query: string): DocumentationEntry[] {
  const normalized = query.trim().toLowerCase();
  if (!normalized) return entries;
  return entries.filter(entry =>
    entry.title.toLowerCase().includes(normalized)
    || entry.id.toLowerCase().includes(normalized)
    || (entry.summary ?? '').toLowerCase().includes(normalized));
}

export function extractToc(html: string): TocItem[] {
  const pattern = /<h([23]) id="([^"]+)">([\s\S]*?)<\/h\1>/g;
  const items: TocItem[] = [];
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(html)) !== null) {
    items.push({
      level: Number(match[1]),
      id: match[2],
      text: decodeEntities(match[3].replace(/<[^>]+>/g, '').trim()),
    });
  }
  return items;
}

function decodeEntities(text: string): string {
  return text
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&amp;/g, '&');
}
