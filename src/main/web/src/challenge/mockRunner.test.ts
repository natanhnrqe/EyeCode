import { afterEach, describe, expect, it, vi } from 'vitest';
import { HIDDEN_TESTS } from './challengeData';
import { runMockSubmission, summarize } from './mockRunner';
import type { TestResult } from './types';

function pending(): TestResult[] {
  return HIDDEN_TESTS.map(test => ({ ...test, status: 'pending' }));
}

afterEach(() => {
  vi.useRealTimers();
});

describe('summarize', () => {
  it('counts passed, failed and total', () => {
    const tests: TestResult[] = [
      { id: 'a', name: 'a', status: 'success' },
      { id: 'b', name: 'b', status: 'success' },
      { id: 'c', name: 'c', status: 'failure' },
      { id: 'd', name: 'd', status: 'pending' },
      { id: 'e', name: 'e', status: 'running' }
    ];
    expect(summarize(tests)).toEqual({ passed: 2, failed: 1, total: 5 });
  });

  it('returns zeroes for an empty list', () => {
    expect(summarize([])).toEqual({ passed: 0, failed: 0, total: 0 });
  });
});

describe('runMockSubmission', () => {
  it('marks tests running through onProgress before completing', async () => {
    vi.useFakeTimers();
    const onProgress = vi.fn<(running: TestResult[]) => void>();
    const promise = runMockSubmission(pending(), 2000, onProgress);

    expect(onProgress).toHaveBeenCalledTimes(1);
    const progress = onProgress.mock.calls[0]?.[0] ?? [];
    expect(progress).toHaveLength(3);
    expect(progress.every(test => test.status === 'running')).toBe(true);

    await vi.advanceTimersByTimeAsync(2000);
    const results = await promise;
    expect(results).toHaveLength(3);
  });

  it('produces exactly two success and one failure preserving ids, names and order', async () => {
    vi.useFakeTimers();
    const promise = runMockSubmission(pending(), 500);
    await vi.advanceTimersByTimeAsync(500);
    const results = await promise;

    expect(results.map(test => test.status)).toEqual(['success', 'success', 'failure']);
    expect(results.map(test => test.id)).toEqual(HIDDEN_TESTS.map(test => test.id));
    expect(results.map(test => test.name)).toEqual(HIDDEN_TESTS.map(test => test.name));
  });

  it('attaches a JUnit assertion failure message and stack trace', async () => {
    vi.useFakeTimers();
    const promise = runMockSubmission(pending(), 500);
    await vi.advanceTimersByTimeAsync(500);
    const results = await promise;

    const failure = results.find(test => test.status === 'failure');
    expect(failure).toBeDefined();
    expect(failure?.errorMessage).toBe('expected: <true> but was: <false>');
    expect(failure?.stackTrace).toContain('org.opentest4j.AssertionFailedError');
    expect(failure?.stackTrace).toContain('expected: <true> but was: <false>');
  });
});
