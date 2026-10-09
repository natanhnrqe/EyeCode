
import { useState } from 'react';
import type React from 'react';
import { EyeCodeIcon } from '../workspace/EyeCodeIcon';
import {
  CHALLENGE_CATALOG,
  CHALLENGE_DIFFICULTY_LABEL,
  CHALLENGE_TRACKS
} from './catalog';
import type { ChallengeCatalogItem, ChallengeTrackId } from './catalog';

type Props = {
  onBack(): void;
  onOpenChallenge(challengeId: string): void;
};

export function ChallengeLobby({
  onBack,
  onOpenChallenge
}: Props): React.ReactElement {
  const [activeTrackId, setActiveTrackId] =
    useState<ChallengeTrackId>('java-fundamentals');

  const activeTrackIndex = CHALLENGE_TRACKS.findIndex(
    track => track.id === activeTrackId
  );

  const activeTrack =
    CHALLENGE_TRACKS[activeTrackIndex] ?? CHALLENGE_TRACKS[0];

  const trackChallenges = CHALLENGE_CATALOG.filter(
    challenge => challenge.trackId === activeTrack.id
  );

  const topics = activeTrack.topics.map(topic => ({
    topic,
    challenges: trackChallenges.filter(
      challenge => challenge.topicId === topic.id
    )
  }));

  const populatedTopics = topics.filter(
    group => group.challenges.length > 0
  );

  const upcomingTopics = topics.filter(
    group => group.challenges.length === 0
  );

  const solvedCount = CHALLENGE_CATALOG.filter(
    challenge => challenge.solved
  ).length;

  return (
    <main className="challenge-lobby">
      <header className="challenge-lobby-header">
        <button
          type="button"
          className="challenge-lobby-back"
          onClick={onBack}
        >
          <span aria-hidden="true">←</span>
          Voltar ao editor
        </button>

        <div className="challenge-lobby-title">
          <h1>Desafios</h1>
          <p>
            Aprenda por trilhas, pratique conceitos e evolua resolvendo
            problemas.
          </p>
        </div>

        <div className="challenge-lobby-counts">
          <span className="challenge-lobby-count">
            {CHALLENGE_CATALOG.length} desafios
          </span>
          <span className="challenge-lobby-solved-count">
            {solvedCount} resolvido{solvedCount === 1 ? '' : 's'}
          </span>
        </div>
      </header>

      <div className="challenge-lobby-layout">
        <aside className="challenge-track-sidebar">
          <div className="challenge-track-sidebar-heading">
            <span>APRENDIZADO</span>
            <h2>Trilhas</h2>
          </div>

          <nav
            className="challenge-track-nav"
            aria-label="Trilhas de desafios"
          >
            {CHALLENGE_TRACKS.map(track => {
              const count = CHALLENGE_CATALOG.filter(
                challenge => challenge.trackId === track.id
              ).length;

              const solved = CHALLENGE_CATALOG.filter(
                challenge =>
                  challenge.trackId === track.id && challenge.solved
              ).length;

              const isActive = activeTrack.id === track.id;

              return (
                <button
                  key={track.id}
                  type="button"
                  className={`challenge-track-nav-item${
                    isActive ? ' is-active' : ''
                  }`}
                  aria-pressed={isActive}
                  onClick={() => setActiveTrackId(track.id)}
                >
                  <span className="challenge-track-icon" aria-hidden="true">
                    <EyeCodeIcon name={track.icon} />
                  </span>

                  <span className="challenge-track-nav-copy">
                    <span className="challenge-track-nav-title">
                      {track.title}
                    </span>
                    <span className="challenge-track-nav-description">
                      {count > 0
                        ? `${count} desafio${count === 1 ? '' : 's'}`
                        : 'Em breve'}
                    </span>
                  </span>

                  <span className="challenge-track-nav-count">
                    {count > 0 ? `${solved}/${count}` : '—'}
                  </span>
                </button>
              );
            })}
          </nav>
        </aside>

        <section
          className="challenge-track-content"
          aria-label={`Trilha ${activeTrack.title}`}
          aria-live="polite"
        >
          <header className="challenge-track-heading">
            <div className="challenge-track-heading-copy">
              <span className="challenge-track-overline">
                TRILHA {String(activeTrackIndex + 1).padStart(2, '0')} /{' '}
                {String(CHALLENGE_TRACKS.length).padStart(2, '0')}
              </span>
              <h2>{activeTrack.title}</h2>
              <p>{activeTrack.description}</p>
            </div>

            <div className="challenge-track-total">
              <strong>{trackChallenges.length}</strong>
              <span>
                {trackChallenges.length === 1 ? 'desafio' : 'desafios'}
              </span>
            </div>
          </header>

          {populatedTopics.length > 0 ? (
            <>
              {populatedTopics.map(({ topic, challenges }) => (
                <section
                  className="challenge-topic-section"
                  key={topic.id}
                >
                  <header className="challenge-topic-heading">
                    <div>
                      <h3>{topic.title}</h3>
                      <p>{topic.description}</p>
                    </div>

                    <span className="challenge-topic-count">
                      {challenges.length}
                    </span>
                  </header>

                  <div className="challenge-track-card-grid">
                    {challenges.map(challenge => (
                      <ChallengeCard
                        key={challenge.id}
                        challenge={challenge}
                        onOpen={() => onOpenChallenge(challenge.id)}
                      />
                    ))}
                  </div>
                </section>
              ))}

              {upcomingTopics.length > 0 && (
                <section className="challenge-upcoming-topics">
                  <h3>Outros tópicos desta trilha</h3>
                  <div className="challenge-upcoming-topic-list">
                    {upcomingTopics.map(({ topic }) => (
                      <div
                        className="challenge-upcoming-topic"
                        key={topic.id}
                      >
                        <span>{topic.title}</span>
                        <span>Em breve</span>
                      </div>
                    ))}
                  </div>
                </section>
              )}
            </>
          ) : (
            <div className="challenge-track-empty">
              <span className="challenge-track-empty-number">
                {String(activeTrackIndex + 1).padStart(2, '0')}
              </span>

              <h3>Esta trilha está sendo preparada</h3>
              <p>
                Os desafios desta área ainda não estão disponíveis.
                Enquanto isso, estes são os tópicos que organizarão a trilha.
              </p>

              <div className="challenge-topic-roadmap">
                {activeTrack.topics.map(topic => (
                  <div
                    className="challenge-topic-roadmap-item"
                    key={topic.id}
                  >
                    <span className="challenge-topic-roadmap-dot" />
                    <div>
                      <strong>{topic.title}</strong>
                      <p>{topic.description}</p>
                    </div>
                    <span className="challenge-topic-roadmap-status">
                      Em breve
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}

function ChallengeCard({
  challenge,
  onOpen
}: {
  challenge: ChallengeCatalogItem;
  onOpen(): void;
}): React.ReactElement {
  return (
    <button
      type="button"
      className={`challenge-card difficulty-${challenge.difficulty}`}
      onClick={onOpen}
      aria-label={`Abrir desafio: ${challenge.title}`}
    >
      <header className="challenge-card-head">
        <h2>{challenge.title}</h2>

        {challenge.solved && (
          <span
            className="challenge-card-solved"
            aria-label="Resolvido"
            title="Resolvido"
          >
            <EyeCodeIcon name="successDialog" />
          </span>
        )}
      </header>

      <p className="challenge-card-summary">{challenge.summary}</p>

      <div className="challenge-card-tags">
        {challenge.tags.map(tag => (
          <span key={tag} className="challenge-tag">
            {tag}
          </span>
        ))}
      </div>

      <footer className="challenge-card-foot">
        <span
          className={`challenge-difficulty difficulty-${challenge.difficulty}`}
        >
          {CHALLENGE_DIFFICULTY_LABEL[challenge.difficulty]}
        </span>

        <span className="challenge-card-open" aria-hidden="true">
          Abrir →
        </span>
      </footer>
    </button>
  );
}
