export type TestStatus = 'pending' | 'running' | 'success' | 'failure';

export type TestResult = {
  id: string;
  name: string;
  status: TestStatus;
  errorMessage?: string;
  stackTrace?: string;
};

export type Challenge = {
  id: string;
  title: string;
  descriptionMarkdown: string;
  starterCode: string;
  entryClassName: string;
};

export type ChallengeTab = 'statement' | 'tests' | 'assistant';

export type ChallengeSummary = { passed: number; failed: number; total: number };

export type ChallengeController = {
  challenge: Challenge;
  tests: TestResult[];
  activeTab: ChallengeTab;
  running: boolean;
  restoring: boolean;
  hasSavedProgress: boolean;
  summary: ChallengeSummary;
  selectTab(tab: ChallengeTab): void;
  submitChallenge(): Promise<void>;
  restoreStarter(): Promise<void>;
  resetTests(): void;
};
