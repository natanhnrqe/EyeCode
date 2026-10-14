import type { RunOutputChunk } from '../workspace/protocol';

export type OutputWindow = {
  chunks: RunOutputChunk[];
  droppedChars: number;
};

/**
 * Rendering window for the lesson output console. The backend keeps the full
 * (capped) run output in memory, but rendering every accumulated chunk on each
 * update makes the interface crawl during heavy output. Only the tail — the
 * most recent {@link OUTPUT_WINDOW_CHARS} — is rendered, and the amount of
 * hidden text is reported honestly instead of being silently dropped.
 */
export const OUTPUT_WINDOW_CHARS = 32_000;

export function windowRunOutput(chunks: readonly RunOutputChunk[], maxChars = OUTPUT_WINDOW_CHARS): OutputWindow {
  const window: RunOutputChunk[] = [];
  let kept = 0;
  let dropped = 0;
  for (let index = chunks.length - 1; index >= 0; index--) {
    const chunk = chunks[index];
    const remaining = maxChars - kept;
    if (remaining <= 0) {
      dropped += chunk.text.length;
      continue;
    }
    if (chunk.text.length <= remaining) {
      window.unshift(chunk);
      kept += chunk.text.length;
    } else {
      window.unshift({ text: chunk.text.slice(chunk.text.length - remaining), error: chunk.error });
      dropped += chunk.text.length - remaining;
      kept += remaining;
    }
  }
  return { chunks: window, droppedChars: dropped };
}
