import { useEffect, useMemo, useState } from 'react';
import type React from 'react';
import { bridge } from '../bridge/EyeCodeBridge';
import { Workspace, type WorkspaceChallengeContext } from '../workspace/Workspace';

type EnsureResponse = { path: string; mainFilePath?: string; fresh: boolean };

type Props = {
  challengeId: string;
  onBack(): void;
};

export function ChallengeArena({ challengeId, onBack }: Props): React.ReactElement {
  const [environment, setEnvironment] = useState<EnsureResponse | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    setEnvironment(null);
    setError('');
    bridge.request<EnsureResponse>('challenges', 'ensure', { id: challengeId }, { timeoutMs: 30000 })
      .then(result => {
        if (!cancelled && result?.path) setEnvironment(result);
      })
      .catch(reason => {
        if (!cancelled) setError(reason instanceof Error ? reason.message : 'Não foi possível preparar o ambiente do desafio.');
      });
    return () => {
      cancelled = true;
    };
  }, [challengeId]);

  const context = useMemo<WorkspaceChallengeContext | null>(
    () => (environment ? {
      id: challengeId,
      path: environment.path,
      mainFilePath: environment.mainFilePath,
      onExit: onBack
    } : null),
    [challengeId, environment, onBack]);

  if (error) return <main className="challenge-arena-gate" role="alert"><p>{error}</p></main>;
  if (!context) return <main className="challenge-arena-gate" aria-busy="true">
    <span className="challenge-spinner" aria-hidden="true" />
    <p>Preparando o ambiente do desafio...</p>
  </main>;
  return <Workspace key={challengeId} challenge={context} />;
}
