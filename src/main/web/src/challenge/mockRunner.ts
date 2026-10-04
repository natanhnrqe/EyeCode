import type { ChallengeSummary, TestResult } from './types';

const FAILURE_MESSAGE = 'expected: <true> but was: <false>';

const FAILURE_STACK_TRACE = [
  `org.opentest4j.AssertionFailedError: ${FAILURE_MESSAGE}`,
  '\tat org.junit.jupiter.api.AssertionUtils.fail(AssertionUtils.java:55)',
  '\tat org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:40)',
  '\tat CnpjValidatorTest.deveRejeitarDigitoVerificadorInvalido(CnpjValidatorTest.java:24)'
].join('\n');

function delay(ms: number): Promise<void> {
  return new Promise(resolve => {
    setTimeout(resolve, ms);
  });
}

export async function runMockSubmission(
  tests: TestResult[],
  delayMs = 2000,
  onProgress?: (running: TestResult[]) => void
): Promise<TestResult[]> {
  const running = tests.map(test => ({
    ...test,
    status: 'running' as const,
    errorMessage: undefined,
    stackTrace: undefined
  }));

  if (onProgress) {
    onProgress(running);
  }

  await delay(delayMs);

  return tests.map((test, index) => {
    if (index === tests.length - 1) {
      return {
        ...test,
        status: 'failure' as const,
        errorMessage: FAILURE_MESSAGE,
        stackTrace: FAILURE_STACK_TRACE
      };
    }
    return {
      ...test,
      status: 'success' as const,
      errorMessage: undefined,
      stackTrace: undefined
    };
  });
}

export function summarize(tests: TestResult[]): ChallengeSummary {
  let passed = 0;
  let failed = 0;
  for (const test of tests) {
    if (test.status === 'success') {
      passed += 1;
    } else if (test.status === 'failure') {
      failed += 1;
    }
  }
  return { passed, failed, total: tests.length };
}
