import { describe, expect, it } from 'vitest';
import { appendRunOutput, emptyRunOutput, MAX_OUTPUT_STATE_CHARS } from './runOutputModel';

describe('appendRunOutput', () => {
  it('acumula saídas pequenas por completo e na ordem', () => {
    let state = emptyRunOutput();
    state = appendRunOutput(state, { text: 'primeira\n', error: false });
    state = appendRunOutput(state, { text: 'segunda\n', error: true });
    expect(state.chunks).toEqual([{ text: 'primeira\n', error: false }, { text: 'segunda\n', error: true }]);
    expect(state.droppedChars).toBe(0);
  });

  it('descarta os chunks mais antigos mantendo o final e contabilizando o volume oculto', () => {
    let state = emptyRunOutput();
    state = appendRunOutput(state, { text: 'a'.repeat(150_000), error: false });
    state = appendRunOutput(state, { text: 'b'.repeat(50_000), error: false });
    state = appendRunOutput(state, { text: 'FINAL\n', error: false });
    const total = state.chunks.reduce((sum, chunk) => sum + chunk.text.length, 0);
    expect(total).toBeLessThanOrEqual(MAX_OUTPUT_STATE_CHARS);
    expect(state.chunks.at(-1)?.text).toBe('FINAL\n');
    expect(state.droppedChars).toBe(150_000 + 50_000 + 6 - total);
  });

  it('fatia um único chunk maior que o teto e preserva o flag de erro', () => {
    const state = appendRunOutput(emptyRunOutput(), { text: 'x'.repeat(1_000_000), error: true });
    expect(state.chunks).toHaveLength(1);
    expect(state.chunks[0].text).toBe('x'.repeat(MAX_OUTPUT_STATE_CHARS));
    expect(state.chunks[0].error).toBe(true);
    expect(state.droppedChars).toBe(1_000_000 - MAX_OUTPUT_STATE_CHARS);
  });

  it('o descarte acumula entre appends e o clear reinicia tudo', () => {
    let state = appendRunOutput(emptyRunOutput(), { text: 'x'.repeat(400_000), error: false });
    const droppedAfterFirst = state.droppedChars;
    expect(droppedAfterFirst).toBe(400_000 - MAX_OUTPUT_STATE_CHARS);
    state = appendRunOutput(state, { text: 'z'.repeat(100), error: false });
    expect(state.droppedChars).toBeGreaterThanOrEqual(droppedAfterFirst);
    const cleared = emptyRunOutput();
    expect(cleared).toEqual({ chunks: [], droppedChars: 0 });
  });
});
