export type DocumentationEntry = {
  id: string;
  title: string;
  type?: string;
  branch: string;
  subgroup?: string;
  summary?: string;
  level?: string;
  duration?: number;
};

export type DocumentationLink = {
  id: string;
  title: string;
};

export type DocumentationPage = {
  id: string;
  title: string;
  type?: string;
  html: string;
  summary?: string;
  level?: string;
  duration?: number;
  breadcrumb: string[];
  officialDocs?: { label: string; url: string };
  related: DocumentationLink[];
};

export type DocumentationCatalogResponse = {
  entries: DocumentationEntry[];
};
