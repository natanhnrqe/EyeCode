import { useCallback, useEffect, useRef, useState } from 'react';
import {
  animate,
  motion,
  useMotionValue,
  useReducedMotion,
  useTransform,
  type MotionValue,
} from 'motion/react';
import { ChevronDown, ChevronUp, Folder } from 'lucide-react';

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
  onBackdropScale?(scale: MotionValue<number> | null): void;
};

const PANEL_SCROLL_DISTANCE = 200;
const SNAP_DELAY_MS = 140;
const MOMENTUM_WINDOW_MS = 280;
const SNAP_FORWARD_AT = 0.35;
const SNAP_BACKWARD_AT = 0.65;
const TRANSITION_DURATION = 0.46;
const TRANSITION_EASE: [number, number, number, number] = [0.22, 1, 0.36, 1];

function wheelDelta(event: WheelEvent): number {
  const raw =
    Math.abs(event.deltaY) >= Math.abs(event.deltaX)
      ? event.deltaY
      : event.deltaX;

  if (event.deltaMode === 1) return raw * 16;
  if (event.deltaMode === 2) return raw * 400;

  return raw;
}

export function WelcomeScreen({
  recentProjects,
  onNewProject,
  onOpenProject,
  onOpenRecentProject,
  onRemoveRecentProject,
  onLessons,
  onChallenges,
  onDocumentation,
  onBackdropScale,
}: Props) {
  const [page, setPage] = useState<0 | 1>(0);

  const screenRef = useRef<HTMLElement | null>(null);
  const listRef = useRef<HTMLDivElement | null>(null);
  const pageRef = useRef<0 | 1>(0);
  const progressRef = useRef(0);
  const animationRef = useRef<{ stop(): void } | null>(null);
  const snapRef = useRef<number | null>(null);
  const wheelAtRef = useRef(0);
  const directionRef = useRef(0);

  const progress = useMotionValue(0);
  const reducedMotion = useReducedMotion();
  const reducedMotionRef = useRef(reducedMotion);

  const trackX = useTransform(progress, [0, 1], ['0%', '-100%']);
  const backdropScale = useTransform(
    progress,
    [0, 1],
    reducedMotion ? [1, 1] : [1, 1.02],
  );

  useEffect(() => {
    reducedMotionRef.current = reducedMotion;
  }, [reducedMotion]);

  useEffect(() => {
    const sync = (value: number) => {
      const next: 0 | 1 = value >= 0.5 ? 1 : 0;

      if (pageRef.current === next) return;

      pageRef.current = next;
      setPage(next);
    };

    sync(progress.get());

    return progress.on('change', sync);
  }, [progress]);

  useEffect(() => {
    onBackdropScale?.(backdropScale);

    return () => {
      onBackdropScale?.(null);
    };
  }, [backdropScale, onBackdropScale]);

  const cancelSnap = useCallback(() => {
    if (snapRef.current === null) return;

    window.clearTimeout(snapRef.current);
    snapRef.current = null;
  }, []);

  const stopAnimation = useCallback(() => {
    animationRef.current?.stop();
    animationRef.current = null;
  }, []);

  const animateTo = useCallback(
    (target: 0 | 1) => {
      cancelSnap();
      stopAnimation();

      progressRef.current = target;

      if (reducedMotionRef.current) {
        progress.jump(target);
        return;
      }

      animationRef.current = animate(progress, target, {
        duration: TRANSITION_DURATION,
        ease: TRANSITION_EASE,
      });
    },
    [cancelSnap, progress, stopAnimation],
  );

  const scheduleSnap = useCallback(() => {
    cancelSnap();

    snapRef.current = window.setTimeout(() => {
      snapRef.current = null;

      const current = progressRef.current;

      if (current <= 0 || current >= 1) return;

      const forward = directionRef.current >= 0;
      const target: 0 | 1 = forward
        ? current >= SNAP_FORWARD_AT
          ? 1
          : 0
        : current <= SNAP_BACKWARD_AT
          ? 0
          : 1;

      animateTo(target);
    }, SNAP_DELAY_MS);
  }, [animateTo, cancelSnap]);

  useEffect(
    () => () => {
      if (snapRef.current !== null) window.clearTimeout(snapRef.current);

      animationRef.current?.stop();
    },
    [],
  );

  useEffect(() => {
    const screen = screenRef.current;
    if (!screen) return;

    const onWheel = (event: WheelEvent) => {
      const delta = wheelDelta(event);

      if (Math.abs(delta) < 1) return;

      const now = performance.now();
      const current = progressRef.current;
      const momentum = now - wheelAtRef.current < MOMENTUM_WINDOW_MS;

      if (delta > 0 && current >= 1) {
        if (momentum) event.preventDefault();
        return;
      }

      if (delta < 0 && current <= 0) {
        if (momentum) event.preventDefault();
        return;
      }

      if (delta < 0) {
        const list = listRef.current;

        if (list && list.scrollTop > 2) return;
      }

      event.preventDefault();

      if (animationRef.current) {
        stopAnimation();
        progressRef.current = progress.get();
      }

      wheelAtRef.current = now;
      directionRef.current = delta > 0 ? 1 : -1;

      const next = Math.min(
        1,
        Math.max(0, current + delta / PANEL_SCROLL_DISTANCE),
      );

      progressRef.current = next;
      progress.jump(next);
      scheduleSnap();
    };

    screen.addEventListener('wheel', onWheel, { passive: false });

    return () => {
      screen.removeEventListener('wheel', onWheel);
    };
  }, [progress, scheduleSnap, stopAnimation]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (
        event.defaultPrevented ||
        event.metaKey ||
        event.ctrlKey ||
        event.altKey ||
        event.shiftKey
      ) {
        return;
      }

      const target = event.target instanceof Element ? event.target : null;

      if (
        target?.closest(
          [
            'input',
            'textarea',
            'select',
            '[contenteditable="true"]',
            'button',
            'a',
            '[role="button"]',
          ].join(','),
        )
      ) {
        return;
      }

      if (event.key === 'ArrowRight' || event.key === 'PageDown') {
        if (pageRef.current === 0) {
          event.preventDefault();
          animateTo(1);
        }
      } else if (event.key === 'ArrowLeft' || event.key === 'PageUp') {
        if (pageRef.current === 1) {
          event.preventDefault();
          animateTo(0);
        }
      }
    };

    window.addEventListener('keydown', onKeyDown);

    return () => {
      window.removeEventListener('keydown', onKeyDown);
    };
  }, [animateTo]);

  function togglePage() {
    animateTo(progressRef.current >= 0.5 ? 0 : 1);
  }

  return (
    <section className="welcome-screen" ref={screenRef}>
      <motion.div className="welcome-track" style={{ x: trackX }}>
        <div
          className="welcome-stage"
          style={{ pointerEvents: page === 0 ? 'auto' : 'none' }}
          aria-hidden={page === 1}
          inert={page === 1}
        >
          <div className="welcome-hero shell-page-content">
            <div className="welcome-brand">
              <div className="welcome-mark">EC</div>

              <div className="welcome-product">
                <span>EyeCode</span>
                <small>Java IDE</small>
              </div>
            </div>

            <div className="welcome-message">
              <h1>The Learn Code Editor</h1>

              <p>
                Crie um projeto Maven, abra um projeto existente ou continue de
                onde parou.
              </p>
            </div>

            <div className="welcome-actions">
              <button
                type="button"
                className="primary-action"
                onClick={onNewProject}
              >
                <EyeCodeIcon name="plus-on-accent" />
                Novo Projeto
              </button>

              <button
                type="button"
                className="quiet-action"
                onClick={onOpenProject}
              >
                <EyeCodeIcon name="folder-open" />
                Abrir Projeto
              </button>
            </div>

            <div className="welcome-links">
              <button
                type="button"
                className="welcome-link"
                onClick={onLessons}
              >
                <EyeCodeIcon name="graduation-cap" />
                Aulas
              </button>

              {onChallenges && (
                <button
                  type="button"
                  className="welcome-link"
                  onClick={onChallenges}
                >
                  <EyeCodeIcon name="trophy" />
                  Desafios
                </button>
              )}
            </div>

            <button
              type="button"
              className="welcome-docs"
              onClick={onDocumentation}
            >
              <EyeCodeIcon name="book-open" />
              Explorar Documentação Java (Tela Cheia)
              <EyeCodeIcon name="arrow-up-right" />
            </button>
          </div>
        </div>

        <div
          id="welcome-recents-panel"
          className="welcome-recents"
          style={{ pointerEvents: page === 1 ? 'auto' : 'none' }}
          aria-hidden={page === 0}
          inert={page === 0}
        >
          <div className="welcome-recents-panel">
            <header className="projects-heading">
              <h2>Projetos recentes</h2>

              <p>
                {recentProjects.length
                  ? 'Continue de onde parou'
                  : 'Nenhum projeto recente ainda'}
              </p>
            </header>

            <div
              className="projects-list"
              ref={listRef}
              tabIndex={page === 1 ? 0 : -1}
              aria-label="Lista de projetos recentes"
            >
              {recentProjects.map((project) => (
                <div className="projects-row" key={project.path}>
                  <button
                    type="button"
                    className="projects-open"
                    onClick={() => onOpenRecentProject(project.path)}
                    tabIndex={page === 1 ? 0 : -1}
                  >
                    <Folder
                      className="projects-folder"
                      size={20}
                      strokeWidth={1.75}
                      aria-hidden="true"
                    />

                    <span className="projects-details">
                      <strong>{project.name}</strong>
                      <code>{project.path}</code>
                    </span>
                  </button>

                  <button
                    type="button"
                    className="projects-remove"
                    aria-label={`Remover ${project.name} dos projetos recentes`}
                    onClick={() => onRemoveRecentProject(project.path)}
                    tabIndex={page === 1 ? 0 : -1}
                  >
                    Remover
                  </button>
                </div>
              ))}

              {!recentProjects.length && (
                <p className="projects-empty">
                  Crie um projeto Maven ou abra uma pasta existente. Seus
                  projetos aparecerão aqui.
                </p>
              )}
            </div>
          </div>
        </div>
      </motion.div>

      <button
        type="button"
        className="welcome-scroll-hint"
        onClick={togglePage}
        aria-label={
          page === 0
            ? 'Ver projetos recentes'
            : 'Voltar para a tela inicial'
        }
        aria-controls="welcome-recents-panel"
        aria-expanded={page === 1}
      >
        {page === 0 ? (
          <ChevronDown size={15} aria-hidden="true" />
        ) : (
          <ChevronUp size={15} aria-hidden="true" />
        )}
      </button>
    </section>
  );
}
