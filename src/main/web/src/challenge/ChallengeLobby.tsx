import { EyeCodeIcon } from '../workspace/EyeCodeIcon';
import { CHALLENGE_CATALOG, CHALLENGE_DIFFICULTY_LABEL } from './catalog';

type Props = {
  onBack(): void;
  onOpenChallenge(challengeId: string): void;
};

export function ChallengeLobby({ onBack, onOpenChallenge }: Props) {
  return <main className="challenge-lobby">
    <header className="challenge-lobby-header">
      <button type="button" className="challenge-lobby-back" onClick={onBack}>
        <span aria-hidden="true">←</span> Voltar ao editor
      </button>
      <div className="challenge-lobby-title">
        <h1>Desafios</h1>
        <p>Pratique lógica, algoritmos e APIs Spring Boot com correção automática.</p>
      </div>
      <span className="challenge-lobby-count">{CHALLENGE_CATALOG.length} desafios</span>
    </header>
    <section className="challenge-grid" aria-label="Catálogo de desafios">
      {CHALLENGE_CATALOG.map(challenge => <button key={challenge.id} type="button"
        className={`challenge-card difficulty-${challenge.difficulty}`}
        onClick={() => onOpenChallenge(challenge.id)} aria-label={challenge.title}>
        <header className="challenge-card-head">
          <h2>{challenge.title}</h2>
          {challenge.solved && <span className="challenge-card-solved" aria-label="Resolvido"><EyeCodeIcon name="successDialog" /></span>}
        </header>
        <p className="challenge-card-summary">{challenge.summary}</p>
        <div className="challenge-card-tags">
          {challenge.tags.map(tag => <span key={tag} className="challenge-tag">{tag}</span>)}
        </div>
        <footer className="challenge-card-foot">
          <span className={`challenge-difficulty difficulty-${challenge.difficulty}`}>{CHALLENGE_DIFFICULTY_LABEL[challenge.difficulty]}</span>
          <span className="challenge-card-open" aria-hidden="true">Abrir →</span>
        </footer>
      </button>)}
    </section>
  </main>;
}
