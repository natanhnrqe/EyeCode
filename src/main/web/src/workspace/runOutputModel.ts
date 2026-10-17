import type { RunOutputChunk } from './protocol';

export type RunOutputState = {
  chunks: RunOutputChunk[];
  droppedChars: number;
};

/**
 * Rolling state for the run console. A flooding program can publish far more
 * text than the console needs to keep, so the state retains at most
 * {@link MAX_OUTPUT_STATE_CHARS} of the most recent output and accounts every
 * hidden character in {@link RunOutputState.droppedChars} — the interface
 * reports the discarded volume instead of silently dropping it.
 */
export const MAX_OUTPUT_STATE_CHARS = 256_000;

export function emptyRunOutput(): RunOutputState {
  return { chunks: [], droppedChars: 0 };
}

export function appendRunOutput(state: RunOutputState, chunk: RunOutputChunk): RunOutputState {
  const chunks = state.chunks.concat([chunk]);
  let total = 0;
  for (const item of chunks) {
    total += item.text.length;
  }
  let dropped = state.droppedChars;
  let start = 0;
  while (start < chunks.length - 1 && total - chunks[start].text.length >= MAX_OUTPUT_STATE_CHARS) {
    total -= chunks[start].text.length;
    dropped += chunks[start].text.length;
    start++;
  }
  const kept = chunks.slice(start);
  if (total > MAX_OUTPUT_STATE_CHARS && kept.length === 1) {
    const excess = total - MAX_OUTPUT_STATE_CHARS;
    kept[0] = { text: kept[0].text.slice(excess), error: kept[0].error };
    dropped += excess;
    total = MAX_OUTPUT_STATE_CHARS;
  }
  return { chunks: kept, droppedChars: dropped };
}
