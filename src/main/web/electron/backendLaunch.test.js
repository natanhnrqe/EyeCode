import path from 'node:path';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import {
  assertDistReady,
  buildBackendLaunch,
  parseReadinessUrl,
  resolveDistDir,
  resolveJavaExecutable,
  MAIN_CLASS,
  STARTUP_TIMEOUT_MS,
} from './backendLaunch.js';

const electronDir = path.dirname(fileURLToPath(import.meta.url));

const always = () => true;
const never = () => false;

describe('resolveDistDir', () => {
  it('uses default path under repoRoot when no override is set', () => {
    const distDir = resolveDistDir('/repo', {});
    expect(distDir).toBe(path.join('/repo', 'webshell-dist', 'target', 'webshell-dist'));
  });

  it('honors EYECODE_WEBSHELL_DIST override', () => {
    const distDir = resolveDistDir('/repo', { EYECODE_WEBSHELL_DIST: 'D:/custom dist' });
    expect(distDir).toBe(path.resolve('D:/custom dist'));
  });

  it('ignores blank override', () => {
    const distDir = resolveDistDir('/repo', { EYECODE_WEBSHELL_DIST: '   ' });
    expect(distDir).toBe(path.join('/repo', 'webshell-dist', 'target', 'webshell-dist'));
  });
});

describe('resolveJavaExecutable', () => {
  it('prefers EYECODE_JAVA over JAVA_HOME and PATH', () => {
    expect(
      resolveJavaExecutable(
        { EYECODE_JAVA: 'D:/JBR/bin/java.exe', JAVA_HOME: 'C:/other' },
        'win32',
        always
      )
    ).toBe('D:/JBR/bin/java.exe');
  });

  it('uses JAVA_HOME binary when it exists', () => {
    expect(
      resolveJavaExecutable({ JAVA_HOME: 'C:/jbr' }, 'win32', always)
    ).toBe(path.join('C:/jbr', 'bin', 'java.exe'));
  });

  it('uses JAVA_HOME binary on posix', () => {
    expect(
      resolveJavaExecutable({ JAVA_HOME: '/opt/jbr' }, 'linux', always)
    ).toBe(path.join('/opt/jbr', 'bin', 'java'));
  });

  it('falls back to PATH java when JAVA_HOME binary is missing', () => {
    expect(resolveJavaExecutable({ JAVA_HOME: 'C:/missing' }, 'win32', never)).toBe('java');
  });

  it('falls back to PATH java without JAVA_HOME', () => {
    expect(resolveJavaExecutable({}, 'win32', never)).toBe('java');
  });
});

describe('assertDistReady', () => {
  it('throws actionable error when jar is missing', () => {
    const distDir = path.join('C:', 'no dist');
    let error;
    try {
      assertDistReady(distDir, never);
    } catch (thrown) {
      error = thrown;
    }
    expect(error).toBeInstanceOf(Error);
    expect(error.message).toContain(distDir);
    expect(error.message).toContain('mvn -f webshell-dist/pom.xml package');
    expect(error.message).toContain('npm run electron:dev');
  });

  it('throws when lib directory is missing', () => {
    const exists = (p) => p.endsWith('eyecode-webshell.jar');
    expect(() => assertDistReady(path.join('C:', 'dist'), exists)).toThrow(/distribution not found/i);
  });

  it('returns jar and lib paths when ready', () => {
    const distDir = path.join('C:', 'dist');
    const result = assertDistReady(distDir, always);
    expect(result.jar).toBe(path.join(distDir, 'eyecode-webshell.jar'));
    expect(result.libDir).toBe(path.join(distDir, 'lib'));
  });
});

describe('buildBackendLaunch', () => {
  const repoRoot = path.join('C:', 'repo root with spaces');
  const distDir = path.join(repoRoot, 'webshell-dist', 'target', 'webshell-dist');

  it('never invokes Maven or exec:java', () => {
    const launch = buildBackendLaunch({
      repoRoot,
      env: {},
      platform: 'win32',
      existsSync: always,
    });
    const joined = [launch.command, ...launch.args].join(' ');
    expect(joined).not.toMatch(/\bmvn\b/);
    expect(joined).not.toMatch(/exec:java/);
    expect(launch.command).not.toBe('mvn');
  });

  it('builds direct java -cp launch of LocalWebShellLauncher', () => {
    const launch = buildBackendLaunch({
      repoRoot,
      env: {},
      platform: 'win32',
      existsSync: always,
    });
    expect(launch.command).toBe('java');
    expect(launch.args).toContain(MAIN_CLASS);
    expect(launch.args).toContain('-cp');
    expect(launch.args).toContain('-Deyecode.web.openBrowser=false');
    const cpIndex = launch.args.indexOf('-cp');
    const classpath = launch.args[cpIndex + 1];
    expect(classpath).toContain(path.join(distDir, 'eyecode-webshell.jar'));
    expect(classpath).toContain(path.join(distDir, 'lib', '*'));
    expect(classpath).toContain(path.delimiter);
    expect(launch.cwd).toBe(distDir);
  });

  it('keeps spaces in the distribution path inside a single spawn argument', () => {
    const launch = buildBackendLaunch({
      repoRoot,
      env: {},
      platform: 'win32',
      existsSync: always,
    });
    const cpIndex = launch.args.indexOf('-cp');
    const classpath = launch.args[cpIndex + 1];
    expect(classpath).toContain('repo root with spaces');
    expect(classpath).toContain(path.join('webshell-dist', 'target', 'webshell-dist'));
    expect(launch.args.filter((arg) => arg.includes('repo root with spaces'))).toHaveLength(1);
  });

  it('adds -Deyecode.jdtls.home only when EYECODE_JDTLS_HOME is set', () => {
    const without = buildBackendLaunch({
      repoRoot,
      env: {},
      platform: 'win32',
      existsSync: always,
    });
    expect(without.args.some((a) => a.includes('eyecode.jdtls.home'))).toBe(false);

    const withHome = buildBackendLaunch({
      repoRoot,
      env: { EYECODE_JDTLS_HOME: 'C:/jdt ls home' },
      platform: 'win32',
      existsSync: always,
    });
    expect(withHome.args).toContain('-Deyecode.jdtls.home=C:/jdt ls home');
  });

  it('respects EYECODE_WEBSHELL_DIST and EYECODE_JAVA', () => {
    const customDist = path.join('C:', 'custom dist with spaces');
    const launch = buildBackendLaunch({
      repoRoot,
      env: {
        EYECODE_WEBSHELL_DIST: customDist,
        EYECODE_JAVA: path.join('C:', 'JBR', 'bin', 'java.exe'),
      },
      platform: 'win32',
      existsSync: always,
    });
    expect(launch.distDir).toBe(path.resolve(customDist));
    expect(launch.command).toBe(path.join('C:', 'JBR', 'bin', 'java.exe'));
    expect(launch.cwd).toBe(path.resolve(customDist));
  });

  it('throws with build instructions when the distribution is missing', () => {
    expect(() =>
      buildBackendLaunch({ repoRoot, env: {}, platform: 'win32', existsSync: never })
    ).toThrow(/mvn -f webshell-dist\/pom\.xml package/);
  });
});

describe('parseReadinessUrl', () => {
  it('parses the LocalWebShellLauncher readiness line', () => {
    const line = '[EyeCode] URL: http://127.0.0.1:5174/webshell/index.html\n';
    expect(parseReadinessUrl(line)).toBe('http://127.0.0.1:5174/webshell/index.html');
  });

  it('finds the URL inside a multi-chunk buffer', () => {
    const buffer = 'noise\nmore noise\n[EyeCode] URL: http://127.0.0.1:8765/webshell/index.html\n';
    expect(parseReadinessUrl(buffer)).toBe('http://127.0.0.1:8765/webshell/index.html');
  });

  it('returns null when readiness is not present', () => {
    expect(parseReadinessUrl('dev mode\n')).toBeNull();
    expect(parseReadinessUrl('')).toBeNull();
  });
});

describe('startup constants', () => {
  it('keeps the 60 second startup timeout', () => {
    expect(STARTUP_TIMEOUT_MS).toBe(60_000);
  });
});

describe('main.js launch path', () => {
  const mainSource = fs.readFileSync(path.join(electronDir, 'main.js'), 'utf8');

  it('does not spawn Maven or call exec:java', () => {
    expect(mainSource).not.toMatch(/\bmvn\b/);
    expect(mainSource).not.toMatch(/exec:java/);
    expect(mainSource).not.toMatch(/shell:\s*true/);
  });

  it('uses buildBackendLaunch from backendLaunch.js', () => {
    expect(mainSource).toContain('buildBackendLaunch');
    expect(mainSource).toContain('parseReadinessUrl');
  });
});
