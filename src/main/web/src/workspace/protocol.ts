export type ProjectNodeKind = 'project' | 'directory' | 'file';

export type ProjectNode = {
  name: string;
  path: string;
  kind: ProjectNodeKind;
  hasChildren: boolean;
};

export type ProjectSnapshot = {
  name: string;
  path: string;
  type: string;
  root: ProjectNode;
};

export type RecentProject = {
  name: string;
  path: string;
};

export type WorkspaceSnapshot = {
  project?: ProjectSnapshot;
  recentProjects: RecentProject[];
  reveal?: TreeReveal;
  validPaths?: string[];
};

export type TreeReveal = {
  targetPath: string;
  ancestors: string[];
};

export type RunConfiguration = {
  id: string;
  name: string;
  mainClass: string;
  kind: string;
};

export type RunPhase = 'IDLE' | 'PREPARING' | 'COMPILING' | 'RUNNING';

export type RunState = {
  running: boolean;
  phase: RunPhase;
  finished: boolean;
  exitCode: number | null;
  stopped: boolean;
  rerunAvailable: boolean;
  configurations: RunConfiguration[];
  selectedConfigurationId: string;
};

export type RunOutputChunk = {
  text: string;
  error: boolean;
};

export type TerminalState = {
  requested: boolean;
  running: boolean;
  workingDirectory: string;
  endpoint?: string;
};
