import type React from 'react';
import { catalogItem, CHALLENGE_DIFFICULTY_LABEL, CHALLENGE_TRACKS } from './catalog';
import { parseInline } from './ChallengeMarkdown';
import { legacyStatementFor, validDocumentationReference } from './statement';
import type { Challenge } from './types';

export function ChallengeStatement({ challenge, onOpenDocumentation }: { challenge: Challenge; onOpenDocumentation(id: string): void }): React.ReactElement {
  const legacy = legacyStatementFor(challenge);
  const content = challenge.statement;
  const metadata = challenge.metadata;
  const objectiveValue = content?.objective ?? catalogItem(challenge.id)?.summary;
  const instructionValue = content?.instructions ?? legacy.instructions;
  const objective = objectiveValue?.trim() || undefined;
  const instructions = instructionValue.trim() || undefined;
  const rules = (content?.businessRules ?? legacy.businessRules).filter(item => item.trim());
  const examples = (content?.examples ?? legacy.examples).filter(example => example.input.trim() && example.output.trim());
  const constraints = (content?.constraints ?? legacy.constraints).filter(item => item.trim());
  const hints = (content?.hints ?? legacy.hints).filter(item => item.trim());
  const catalog = catalogItem(challenge.id);
  const track = catalog && CHALLENGE_TRACKS.find(item => item.id === catalog.trackId);
  const topic = track?.topics.find(item => item.id === catalog?.topicId);
  const difficulty = catalog ? CHALLENGE_DIFFICULTY_LABEL[catalog.difficulty] : undefined;
  const badges = [metadata?.track ?? track?.title, metadata?.topic ?? topic?.title, metadata?.language, difficulty,
    metadata?.estimatedMinutes ? `${metadata.estimatedMinutes} min` : undefined]
    .filter((badge): badge is string => !!badge);
  const references = (metadata?.documentationReferences ?? catalog?.documentationReferences ?? []).map(validDocumentationReference)
    .filter((reference): reference is NonNullable<typeof reference> => !!reference);

  return <>
    {(badges.length > 0 || objective || references.length > 0) && <div className="challenge-statement-heading">
      {badges.length > 0 && <div className="challenge-statement-badges" aria-label="Metadados do desafio">
        {badges.map(badge => <span key={badge}>{badge}</span>)}
      </div>}
      {objective && <section className="challenge-statement-section">
        <h2>Objetivo</h2>
        <p>{objective}</p>
      </section>}
      {references.length === 1 && (
        <div className="challenge-documentation-card">
          <div className="challenge-documentation-card-header">
            <span
              className="challenge-documentation-card-icon"
              aria-hidden="true"
            >
              ↗
            </span>

            <div className="challenge-documentation-card-copy">
              <h2 className="challenge-documentation-card-title">
                Documentação relacionada
              </h2>
              <p>
                {references[0].relevance || 'Consulte este artigo da documentação do EyeCode.'}
              </p>
            </div>
          </div>

          <button
            type="button"
            className="challenge-documentation-link"
            onClick={() => onOpenDocumentation(references[0].id)}
          >
            <span>Abrir documentação no editor</span>
            <span aria-hidden="true">↗</span>
          </button>
        </div>
      )}

      {references.length > 1 && (
        <details className="challenge-documentation-card challenge-documentation-list">
          <summary className="challenge-documentation-card-summary">
            <span
              className="challenge-documentation-card-icon"
              aria-hidden="true"
            >
              ↗
            </span>

            <span className="challenge-documentation-card-copy">
              <strong>Documentação relacionada</strong>
              <small>{references.length} referências disponíveis</small>
            </span>
          </summary>

          <ul>
            {references.map(reference => (
              <li key={reference.id}>
                <button type="button" onClick={() => onOpenDocumentation(reference.id)}>
                  {reference.title}
                </button>
                {reference.relevance && <p>{reference.relevance}</p>}
              </li>
            ))}
          </ul>
        </details>
      )}
    </div>}
    {instructions && <section className="challenge-statement-section">
      <h2>O que implementar</h2>
      <p>{instructions}</p>
    </section>}
    {rules.length > 0 && <section className="challenge-statement-section">
      <h2>Regras de negócio</h2>
      <ul>{rules.map((rule, index) => <li key={`${index}-${rule}`}>{parseInline(rule)}</li>)}</ul>
    </section>}
    {examples.length > 0 && <section className="challenge-statement-section">
      <h2>{examples.length === 1 ? 'Exemplo' : 'Exemplos'}</h2>
      {examples.map((example, index) => <div className="challenge-example" key={`${index}-${example.input}`}>
        <div><span>Entrada</span><pre className="challenge-code"><code className={example.language ? `language-${example.language}` : undefined}>{example.input}</code></pre></div>
        <div><span>Saída esperada</span><pre className="challenge-code"><code>{example.output}</code></pre></div>
      </div>)}
    </section>}
    {constraints.length > 0 && <section className="challenge-statement-section">
      <h2>Restrições e casos-limite</h2><ul>{constraints.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}</ul>
    </section>}
    {hints.length > 0 && <section className="challenge-statement-section">
      <h2>Dicas</h2><ul>{hints.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}</ul>
    </section>}
  </>;
}
