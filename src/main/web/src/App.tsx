import './styles.css';
import { useEffect, useState } from 'react';
import { ChallengeArena } from './challenge/ChallengeArena';
import { ChallengeLobby } from './challenge/ChallengeLobby';
import { Workspace } from './workspace/Workspace';

type AppRoute =
  | { name: 'workspace' }
  | { name: 'challenge-lobby' }
  | { name: 'challenge-arena'; challengeId: string };

const ROUTE_STORAGE_KEY = 'eyecode.challenge.route';

function readStoredRoute(): AppRoute {
  try {
    const raw = sessionStorage.getItem(ROUTE_STORAGE_KEY);
    if (!raw) return { name: 'workspace' };
    const parsed = JSON.parse(raw) as AppRoute;
    if (parsed.name === 'challenge-lobby') return parsed;
    if (parsed.name === 'challenge-arena' && typeof parsed.challengeId === 'string') return parsed;
  } catch {
  }
  return { name: 'workspace' };
}

export function App() {
  const [route, setRoute] = useState<AppRoute>(readStoredRoute);

  useEffect(() => {
    if (route.name === 'workspace') sessionStorage.removeItem(ROUTE_STORAGE_KEY);
    else sessionStorage.setItem(ROUTE_STORAGE_KEY, JSON.stringify(route));
  }, [route]);

  if (route.name === 'challenge-lobby') {
    return <ChallengeLobby
      onBack={() => setRoute({ name: 'workspace' })}
      onOpenChallenge={challengeId => setRoute({ name: 'challenge-arena', challengeId })} />;
  }
  if (route.name === 'challenge-arena') {
    return <ChallengeArena challengeId={route.challengeId} onBack={() => setRoute({ name: 'challenge-lobby' })} />;
  }
  return <Workspace onOpenChallenges={() => setRoute({ name: 'challenge-lobby' })} />;
}
