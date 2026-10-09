import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import type React from 'react';
import { bridge } from '../bridge/EyeCodeBridge';
import { challengeFor } from './challengeData';
import type { Challenge, ChallengeController, ChallengeSummary, ChallengeTab, TestResult } from './types';

const ChallengeContext = createContext<ChallengeController | null>(null);

function createPendingTests(): TestResult[] {
  return [];
}

export function ChallengeProvider({ children, challengeId = 'cnpj-validator', beforeRun }: {
  children: React.ReactNode;
  challengeId?: string | null;
  beforeRun?: () => Promise<boolean>;
}): React.ReactElement {
  const challenge: Challenge = challengeFor(challengeId);
  const [tests, setTests] = useState<TestResult[]>(createPendingTests);
  const [activeTab, setActiveTab] = useState<ChallengeTab>('statement');
  const [running, setRunning] = useState(false);
  const [restoring, setRestoring] = useState(false);
  const [hasSavedProgress, setHasSavedProgress] = useState(false);
  const runningRef = useRef(false);
  const restoringRef = useRef(false);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (!challengeId) return;
    let cancelled = false;
    bridge.request<{ exists: boolean }>('challenges', 'state', { id: challengeId }, { timeoutMs: 5000 })
      .then(result => {
        if (!cancelled) setHasSavedProgress(!!result?.exists);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [challengeId]);

  const selectTab = useCallback((tab: ChallengeTab): void => {
    setActiveTab(tab);
  }, []);

  const submitChallenge = useCallback(async (): Promise<void> => {
    if (runningRef.current) {
      return;
    }
    runningRef.current = true;
    setRunning(true);
    setTests(createPendingTests());

    try {
      if (beforeRun && !await beforeRun()) throw new Error('Salve os arquivos do desafio antes de executar os testes.');
      const result = await bridge.request<{ tests: TestResult[] }>(
        'challenges', 'run', { id: challenge.id }, { timeoutMs: 190000 }
      );
      if (mountedRef.current) setTests(result.tests ?? []);
    } catch (error) {
      if (mountedRef.current) setTests([{
        id: 'test-runner', name: 'Falha ao executar os testes', status: 'failure',
        errorMessage: error instanceof Error ? error.message : 'Não foi possível executar os testes.'
      }]);
    } finally {
      if (mountedRef.current) setRunning(false);
      runningRef.current = false;
    }
  }, [beforeRun, challenge.id]);

  const restoreStarter = useCallback(async (): Promise<void> => {
    if (!challengeId || restoringRef.current) {
      return;
    }
    restoringRef.current = true;
    setRestoring(true);
    try {
      await bridge.request('challenges', 'reset', { id: challengeId }, { timeoutMs: 30000 });
      window.location.reload();
    } catch {
      restoringRef.current = false;
      setRestoring(false);
    }
  }, [challengeId]);

  const resetTests = useCallback((): void => {
    runningRef.current = false;
    setTests(createPendingTests());
    setRunning(false);
  }, []);

  const summary: ChallengeSummary = useMemo(() => {
    let passed = 0;
    let failed = 0;
    for (const test of tests) {
      if (test.status === 'success') passed += 1;
      else if (test.status === 'failure') failed += 1;
    }
    return { passed, failed, total: tests.length };
  }, [tests]);

  const value = useMemo<ChallengeController>(
    () => ({
      challenge,
      tests,
      activeTab,
      running,
      restoring,
      hasSavedProgress,
      summary,
      selectTab,
      submitChallenge,
      restoreStarter,
      resetTests
    }),
    [challenge, tests, activeTab, running, restoring, hasSavedProgress, summary, selectTab, submitChallenge, restoreStarter, resetTests]
  );

  return <ChallengeContext.Provider value={value}>{children}</ChallengeContext.Provider>;
}

export function useChallenge(): ChallengeController {
  const controller = useContext(ChallengeContext);
  if (!controller) {
    throw new Error('useChallenge deve ser usado dentro de ChallengeProvider');
  }
  return controller;
}
