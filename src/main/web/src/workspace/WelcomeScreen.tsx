import { EyeCodeIcon } from './EyeCodeIcon';
import type { RecentProject } from './protocol';

type Props = {
  recentProjects: RecentProject[];
  onNewProject(): void;
  onOpenProject(): void;
  onOpenRecentProject(path: string): void;
  onRemoveRecentProject(path: string): void;
  onLessons(): void;
  onChallenges?(): void;
  onDocumentation(): void;
};

export function WelcomeScreen({ recentProjects, onNewProject, onOpenProject, onOpenRecentProject, onRemoveRecentProject, onLessons, onChallenges, onDocumentation }: Props) {
  return <section className="welcome-screen">
    <div className="shell-page-content">
      <div className="welcome-intro"><div className="welcome-mark">EC</div><div className="welcome-product"><span>EyeCode</span><small>Java IDE</small></div><strong>Seu ambiente Java, pronto quando você estiver.</strong>
        <p>Crie um projeto Maven, abra um código existente ou continue de onde parou.</p>
        <div className="welcome-actions"><button type="button" className="primary-action" onClick={onNewProject}>Novo Projeto</button><button type="button" className="quiet-action" onClick={onOpenProject}>Abrir Projeto</button><button type="button" className="text-action" onClick={onLessons}>Aulas</button>{onChallenges && <button type="button" className="text-action" onClick={onChallenges}>Desafios</button>}<button type="button" className="quiet-action" onClick={onDocumentation}><EyeCodeIcon name="markdown" className="welcome-action-icon" />Explorar Documentação Java (Tela Cheia)</button></div></div>
      <div className="welcome-recents"><header><strong>Projetos recentes</strong>{recentProjects.length ? <span>Continue de onde parou</span> : null}</header>
        {recentProjects.length ? <div>{recentProjects.map(project => <div className="welcome-recent-row" key={project.path}><button type="button" onClick={() => onOpenRecentProject(project.path)}><strong>{project.name}</strong><span>{project.path}</span></button><button type="button" aria-label={`Remover ${project.name} dos projetos recentes`} onClick={() => onRemoveRecentProject(project.path)}>Remover</button></div>)}</div> : <p>Nenhum projeto recente ainda. Abra ou crie um projeto para vê-lo aqui.</p>}
      </div>
    </div>
  </section>;
}
