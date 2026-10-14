import { describe, expect, it } from 'vitest';
import { OUTPUT_WINDOW_CHARS, windowRunOutput } from './learnOutputModel';

function chunk(text: string, error = false) {
  return { text, error };
}

describe('windowRunOutput', () => {
  it('preserva pequenas saídas por completo, na ordem e com os flags de erro', () => {
    const chunks = [chunk('Hello '), chunk('World!\n'), chunk('erro\n', true)];
    const result = windowRunOutput(chunks);
    expect(result.droppedChars).toBe(0);
    expect(result.chunks).toEqual(chunks);
  });

  it('renderiza apenas o final de saídas grandes e informa o volume oculto', () => {
    const chunks = Array.from({ length: 100 }, (_, index) => chunk(`linha-${index};`));
    const total = chunks.reduce((sum, item) => sum + item.text.length, 0);
    const result = windowRunOutput(chunks, 400);
    const kept = result.chunks.reduce((sum, item) => sum + item.text.length, 0);
    expect(kept).toBeLessThanOrEqual(400);
    expect(result.droppedChars).toBe(total - kept);
    expect(result.chunks.at(-1)?.text).toContain('linha-99');
  });

  it('fatia o início de um chunk maior que o orçamento restante', () => {
    const chunks = [chunk('a'.repeat(1_000)), chunk('b'.repeat(10_000))];
    const result = windowRunOutput(chunks, 250);
    expect(result.chunks).toHaveLength(1);
    expect(result.chunks[0].text).toBe('b'.repeat(250));
    expect(result.droppedChars).toBe(1_000 + 10_000 - 250);
  });

  it('mantém o flag de erro do chunk fatiado', () => {
    const result = windowRunOutput([chunk('a'.repeat(600), true)], 100);
    expect(result.chunks[0].error).toBe(true);
    expect(result.chunks[0].text).toBe('a'.repeat(100));
  });

  it('saída vazia continua vazia', () => {
    expect(windowRunOutput([])).toEqual({ chunks: [], droppedChars: 0 });
  });

  it('o tamanho padrão mantém a renderização limitada sob flood de 1 MB', () => {
    const flood = Array.from({ length: 256 }, () => chunk('x'.repeat(4_096)));
    const result = windowRunOutput(flood);
    const kept = result.chunks.reduce((sum, item) => sum + item.text.length, 0);
    expect(kept).toBeLessThanOrEqual(OUTPUT_WINDOW_CHARS);
    expect(result.droppedChars).toBeGreaterThan(900_000);
  });
});
