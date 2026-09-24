import type { RecentProject } from './protocol';

type Props = {
  recentProjects: RecentProject[];
  onNewProject(): void;
  onOpenProject(): void;
  onOpenRecentProject(path: string): void;
  onRemoveRecentProject(path: string): void;
  onLessons(): void;
};

export function WelcomeScreen({ recentProjects, onNewProject, onOpenProject, onOpenRecentProject, onRemoveRecentProject, onLessons }: Props) {
  return <section className="welcome-screen">
    <div className="shell-page-content">
      <div className="welcome-intro"><div className="welcome-mark">EC</div><div className="welcome-product"><span>EyeCode</span><small>Java IDE</small></div><strong>Your Java workspace, ready when you are.</strong>
        <p>Create a Maven project, open existing code, or continue from a recent workspace.</p>
        <div className="welcome-actions"><button type="button" className="primary-action" onClick={onNewProject}>New Project</button><button type="button" className="quiet-action" onClick={onOpenProject}>Open Project</button><button type="button" className="text-action" onClick={onLessons}>Aulas</button></div></div>
      <div className="welcome-recents"><header><strong>Recent Projects</strong><span>{recentProjects.length ? 'Open where you left off' : 'Your projects will appear here'}</span></header>
        {recentProjects.length ? <div>{recentProjects.map(project => <div className="welcome-recent-row" key={project.path}><button type="button" onClick={() => onOpenRecentProject(project.path)}><strong>{project.name}</strong><span>{project.path}</span></button><button type="button" aria-label={`Remove ${project.name} from recent projects`} onClick={() => onRemoveRecentProject(project.path)}>Remove</button></div>)}</div> : <p>No recent projects yet.</p>}
      </div>
    </div>
  </section>;
}
