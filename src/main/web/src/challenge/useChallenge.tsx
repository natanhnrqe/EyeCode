import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import type React from 'react';
import { bridge } from '../bridge/EyeCodeBridge';
import { CNPJ_VALIDATOR_CHALLENGE, HIDDEN_TESTS } from './challengeData';
import { runMockSubmission, summarize } from './mockRunner';
import type { Challenge, ChallengeController, ChallengeSummary, ChallengeTab, TestResult } from './types';

const ChallengeContext = createContext<ChallengeController | null>(null);

function createPendingTests(): TestResult[] {
  return HIDDEN_TESTS.map(test => ({ ...test, status: 'pending' }));
}

export function ChallengeProvider({ children, challengeId = 'cnpj-validator' }: {
  children: React.ReactNode;
  challengeId?: string | null;
}): React.ReactElement {
  const challenge: Challenge = CNPJ_VALIDATOR_CHALLENGE;
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

    const results = await runMockSubmission(createPendingTests(), 2000, progress => {
      if (mountedRef.current) {
        setTests(progress);
      }
    });

    if (mountedRef.current) {
      setTests(results);
      setRunning(false);
    }
    runningRef.current = false;
  }, []);

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

  const summary: ChallengeSummary = useMemo(() => summarize(tests), [tests]);

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
