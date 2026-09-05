export type LearningItem = {
  id: string;
  title: string;
};

export type LearningCardPayload = {
  identifier: string;
  title: string;
  subtitle: string;
  sizeClass: 'quick' | 'medium' | 'full';
  iconKind: string;
  iconUrl: string;
  breadcrumb: LearningItem[];
  renderedBodyHtml: string;
  commonMethods: LearningItem[];
  relatedItems: LearningItem[];
  sourceAvailable: boolean;
  docsAvailable: boolean;
};

export type LearningResponse = {
  requestId: string;
  uri: string;
  version: number;
  found: boolean;
  card: LearningCardPayload;
};

export type LearningPopupState = {
  requestId: string;
  uri: string;
  version: number;
  card: LearningCardPayload;
  anchor: { left: number; top: number };
};
