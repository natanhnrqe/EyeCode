import type React from 'react';
import { EyeCodeIcon } from '../workspace/EyeCodeIcon';
import { ChallengeStatement } from './ChallengeStatement';
import type { TestResult } from './types';
import { useChallenge } from './useChallenge';

type Props = {
  onExit(): void;
  onCollapse(): void;
  onOpenDocumentation(id: string): void;
};

export function ChallengeRightPanel({
  onExit,
  onCollapse,
  onOpenDocumentation
}: Props): React.ReactElement {
  const controller = useChallenge();

  return (
    <aside className="challenge-panel" aria-label="Painel do desafio">
      <nav
        className="challenge-tabs"
        role="tablist"
        aria-label="Seções do desafio"
      >
        <button
          type="button"
          role="tab"
          aria-selected={controller.activeTab === 'statement'}
          className={`challenge-tab${
            controller.activeTab === 'statement' ? ' is-active' : ''
          }`}
          onClick={() => controller.selectTab('statement')}
        >
          Enunciado
        </button>

        <button
          type="button"
          role="tab"
          aria-selected={controller.activeTab === 'tests'}
          className={`challenge-tab${
            controller.activeTab === 'tests' ? ' is-active' : ''
          }`}
          onClick={() => controller.selectTab('tests')}
        >
          Testes{' '}
          <span className="challenge-lock" aria-hidden="true">
            🔒
          </span>
        </button>

        <button
          type="button"
          className="challenge-collapse-button"
          onClick={onCollapse}
          aria-label="Recolher painel do desafio"
          title="Recolher painel do desafio"
        >
          <span aria-hidden="true">❯</span>
        </button>
      </nav>

      {/* ENUNCIADO */}
      <div
        className="challenge-statement"
        hidden={controller.activeTab !== 'statement'}
      >
        {/* Somente este conteúdo rola */}
        <div className="challenge-statement-scroll">
          <div className="challenge-statement-meta">
            <h2>{controller.challenge.title}</h2>
          </div>

          <ChallengeStatement challenge={controller.challenge} onOpenDocumentation={onOpenDocumentation} />

          <div
            className="challenge-assistant is-disabled"
            aria-disabled="true"
          >
            <span className="challenge-lock" aria-hidden="true">
              🔒
            </span>
            <span>Assistente IA</span>
            <span className="challenge-badge">Em breve</span>
          </div>
        </div>

        {/* RODAPÉ FIXO DO ENUNCIADO */}
        <footer
          className="challenge-panel-actions"
          aria-label="Ações do enunciado"
        >
          <button
            type="button"
            className="challenge-panel-back"
            onClick={onExit}
          >
            <span aria-hidden="true">←</span>
            Voltar para o Catálogo
          </button>

          <button
            type="button"
            className="challenge-panel-restore"
            onClick={() => void controller.restoreStarter()}
            disabled={controller.restoring}
          >
            {controller.restoring
              ? 'Restaurando...'
              : 'Restaurar Código Inicial'}
          </button>
        </footer>
      </div>

      {/* TESTES — comportamento preservado */}
      <div
        className="challenge-tests"
        hidden={controller.activeTab !== 'tests'}
      >
        <div className="challenge-tests-toolbar">
          <button
            type="button"
            className="challenge-run-button"
            onClick={() => void controller.submitChallenge()}
            disabled={controller.running}
          >
            <EyeCodeIcon name="run" />
            <span>
              {controller.running ? 'Executando...' : 'Executar'}
            </span>
          </button>

          <p className="challenge-summary">
            Aprovados: {controller.summary.passed} · Falhas:{' '}
            {controller.summary.failed} · Total: {controller.summary.total}
          </p>
        </div>

        <div className="challenge-test-list">
          {controller.tests.map(test => (
            <ChallengeTestRow key={test.id} test={test} />
          ))}
        </div>
      </div>
    </aside>
  );
}

function ChallengeTestRow({ test }: { test: TestResult }): React.ReactElement {
  return <div className="challenge-test-row">
    <ChallengeTestStatus status={test.status} />
    <span className="challenge-test-name">{test.name}</span>
    {test.status === 'failure' && (test.errorMessage || test.stackTrace) && (
      <details className="challenge-stacktrace">
        <summary>{test.errorMessage ? `Detalhes do erro: ${test.errorMessage}` : 'Detalhes do erro'}</summary>
        <pre>{test.stackTrace || test.errorMessage}</pre>
      </details>
    )}
  </div>;
}

function ChallengeTestStatus({ status }: { status: TestResult['status'] }): React.ReactElement {
  if (status === 'success') {
    return <span className="challenge-test-status is-success" aria-label="Sucesso"><EyeCodeIcon name="successDialog" /></span>;
  }
  if (status === 'failure') {
    return <span className="challenge-test-status is-failure" aria-label="Falha"><EyeCodeIcon name="errorDialog" /></span>;
  }
  if (status === 'running') {
    return <span className="challenge-test-status is-running" aria-label="Executando"><EyeCodeIcon name="reload" className="is-spinning" /></span>;
  }
  return <span className="challenge-test-status is-pending" aria-label="Pendente" aria-hidden="true">•</span>;
}
