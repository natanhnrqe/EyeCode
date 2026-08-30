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
};

export type RunConfiguration = {
  id: string;
  name: string;
  mainClass: string;
  kind: string;
};

export type RunState = {
  running: boolean;
  rerunAvailable: boolean;
  configurations: RunConfiguration[];
  selectedConfigurationId: string;
};
