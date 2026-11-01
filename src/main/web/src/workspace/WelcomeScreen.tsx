import { useEffect, useRef, useState } from 'react';
import { motion, useMotionValue, useReducedMotion, useSpring, useTransform } from 'motion/react';
import { ChevronDown, ChevronUp, Folder } from 'lucide-react';
import { EyeCodeIcon } from './EyeCodeIcon';
import type { RecentProject } from './protocol';
import glowBottom from './assets/welcome-glow-a.svg';
import glowTop from './assets/welcome-glow-b.svg';
import noiseTile from './assets/welcome-noise-300.png';

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

const GESTURE_LOCK_MS = 760;

export function WelcomeScreen({ recentProjects, onNewProject, onOpenProject, onOpenRecentProject, onRemoveRecentProject, onLessons, onChallenges, onDocumentation }: Props) {
  const [page, setPage] = useState<0 | 1>(0);
  const screenRef = useRef<HTMLElement | null>(null);
  const listRef = useRef<HTMLDivElement | null>(null);
  const pageRef = useRef<0 | 1>(0);
  const lockRef = useRef(0);

  const progress = useMotionValue(0);
  const spring = useSpring(progress, { stiffness: 170, damping: 26, mass: .7 });
  const reducedMotion = useReducedMotion();
  const animated = reducedMotion ? progress : spring;

  const heroX = useTransform(animated, [0, 1], ['0%', '-34%']);
  const heroScale = useTransform(animated, [0, 1], [1, .92]);
  const heroOpacity = useTransform(animated, [0, .55, 1], [1, .45, 0]);
  const heroBlur = useTransform(animated, value => `blur(${(value * 7).toFixed(2)}px)`);
  const recentsX = useTransform(animated, [0, 1], ['22%', '0%']);
  const recentsOpacity = useTransform(animated, [0, .35, 1], [0, .6, 1]);
  const backdropScale = useTransform(animated, [0, 1], [1, 1.06]);

  useEffect(() => {
    pageRef.current = page;
    progress.set(page);
  }, [page, progress]);

  useEffect(() => {
    const screen = screenRef.current;
    if (!screen) return;
    const onWheel = (event: WheelEvent) => {
      const delta = Math.abs(event.deltaY) >= Math.abs(event.deltaX) ? event.deltaY : event.deltaX;
      if (Math.abs(delta) < 10) return;
      const now = performance.now();
      if (now < lockRef.current) {
        event.preventDefault();
        return;
      }
      if (delta > 0 && pageRef.current === 0) {
        event.preventDefault();
        lockRef.current = now + GESTURE_LOCK_MS;
        setPage(1);
        return;
      }
      if (delta < 0 && pageRef.current === 1) {
        const list = listRef.current;
        if (list && list.scrollTop > 2) return;
        event.preventDefault();
        lockRef.current = now + GESTURE_LOCK_MS;
        setPage(0);
      }
    };
    screen.addEventListener('wheel', onWheel, { passive: false });
    return () => screen.removeEventListener('wheel', onWheel);
  }, []);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.defaultPrevented || event.metaKey || event.ctrlKey || event.altKey) return;
      const target = event.target as HTMLElement | null;
      if (target && target.closest('input,textarea,select,[contenteditable="true"]')) return;
      if (event.key === 'ArrowRight' || event.key === 'PageDown') {
        event.preventDefault();
        setPage(1);
      } else if (event.key === 'ArrowLeft' || event.key === 'PageUp') {
        event.preventDefault();
        setPage(0);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);

  return <section className="welcome-screen" ref={screenRef}>
    <div className="welcome-backdrop" aria-hidden="true">
      <motion.div className="welcome-backdrop-glow is-top" style={{ scale: backdropScale }}>
        <img src={glowTop} alt="" />
      </motion.div>
      <div className="welcome-backdrop-glow is-bottom">
        <img src={glowBottom} alt="" />
      </div>
      <div className="welcome-backdrop-noise" style={{ backgroundImage: `url(${noiseTile})` }} />
    </div>

    <motion.div className="welcome-stage" style={{ x: heroX, scale: heroScale, opacity: heroOpacity, filter: heroBlur }} aria-hidden={page === 1} inert={page === 1}>
      <div className="welcome-hero shell-page-content">
        <div className="welcome-brand">
          <div className="welcome-mark">EC</div>
          <div className="welcome-product"><span>EyeCode</span><small>Java IDE</small></div>
        </div>
        <div className="welcome-message">
          <h1>The Learn Code Editor</h1>
          <p>Crie um projeto Maven, abra um projeto existente ou continue de onde parou.</p>
        </div>
        <div className="welcome-actions">
          <button type="button" className="primary-action" onClick={onNewProject}><EyeCodeIcon name="plus-on-accent" />Novo Projeto</button>
          <button type="button" className="quiet-action" onClick={onOpenProject}><EyeCodeIcon name="folder-open" />Abrir Projeto</button>
        </div>
        <div className="welcome-links">
          <button type="button" className="welcome-link" onClick={onLessons}><EyeCodeIcon name="graduation-cap" />Aulas</button>
          {onChallenges && <button type="button" className="welcome-link" onClick={onChallenges}><EyeCodeIcon name="trophy" />Desafios</button>}
        </div>
        <button type="button" className="welcome-docs" onClick={onDocumentation}>
          <EyeCodeIcon name="book-open" />Explorar Documentação Java (Tela Cheia)<EyeCodeIcon name="arrow-up-right" />
        </button>
      </div>
    </motion.div>

    <button type="button" className="welcome-scroll-hint" onClick={() => setPage(page === 0 ? 1 : 0)} aria-label={page === 0 ? 'Ver projetos recentes' : 'Voltar para a tela inicial'}>
      {page === 0 ? <ChevronDown size={15} aria-hidden /> : <ChevronUp size={15} aria-hidden />}
    </button>

    <motion.div className="welcome-recents" style={{ x: recentsX, opacity: recentsOpacity }} aria-hidden={page === 0} inert={page === 0}>
      <div className="welcome-recents-panel">
        <header className="projects-heading">
          <h2>Projetos recentes</h2>
          <p>{recentProjects.length ? 'Continue de onde parou' : 'Nenhum projeto recente ainda'}</p>
        </header>
        <div className="projects-list" ref={listRef}>
          {recentProjects.map(project => <div className="projects-row" key={project.path}>
            <button type="button" className="projects-open" onClick={() => onOpenRecentProject(project.path)}>
              <Folder className="projects-folder" size={20} strokeWidth={1.75} aria-hidden />
              <span className="projects-details"><strong>{project.name}</strong><code>{project.path}</code></span>
            </button>
            <button type="button" className="projects-remove" aria-label={`Remover ${project.name} dos projetos recentes`} onClick={() => onRemoveRecentProject(project.path)}>Remover</button>
          </div>)}
          {!recentProjects.length && <p className="projects-empty">Crie um projeto Maven ou abra uma pasta existente. Seus projetos aparecerão aqui.</p>}
        </div>
      </div>
    </motion.div>
  </section>;
}
