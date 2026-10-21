import path from 'node:path';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import {
  assertDistReady,
  buildBackendLaunch,
  isJdkHome,
  isMavenHome,
  parseReadinessUrl,
  resolveDistDir,
  resolveJdkHome,
  resolveMavenHome,
  resolveJavaExecutable,
  MAIN_CLASS,
  STARTUP_TIMEOUT_MS,
} from './backendLaunch.js';

const electronDir = path.dirname(fileURLToPath(import.meta.url));

const always = () => true;
const never = () => false;

function existsFor(set) {
  return (p) => set.has(p);
}

function distReadyJdkMissing(distDir) {
  return (p) =>
    p.startsWith(distDir) ||
    p.endsWith(path.join('bin', 'java.exe')) === false;
}

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

  it('prefers packaged resources webshell-dist when the jar exists there', () => {
    const resourcesPath = path.join('C:', 'install', 'resources');
    const packagedDist = path.join(resourcesPath, 'webshell-dist');
    const files = new Set([path.join(packagedDist, 'eyecode-webshell.jar')]);
    const distDir = resolveDistDir(
      path.join('C:', 'repo'),
      {},
      resourcesPath,
      existsFor(files)
    );
    expect(distDir).toBe(packagedDist);
  });

  it('falls back to the repo distribution when packaged resources are missing', () => {
    const distDir = resolveDistDir(
      path.join('C:', 'repo'),
      {},
      path.join('C:', 'install', 'resources'),
      never
    );
    expect(distDir).toBe(path.join('C:', 'repo', 'webshell-dist', 'target', 'webshell-dist'));
  });

  it('prefers EYECODE_WEBSHELL_DIST over packaged resources', () => {
    const custom = path.join('C:', 'custom dist');
    const files = new Set([path.join(path.join('C:', 'r'), 'webshell-dist', 'eyecode-webshell.jar')]);
    const distDir = resolveDistDir(
      path.join('C:', 'repo'),
      { EYECODE_WEBSHELL_DIST: custom },
      path.join('C:', 'r'),
      existsFor(files)
    );
    expect(distDir).toBe(path.resolve(custom));
  });
});

describe('isJdkHome', () => {
  it('accepts a home with java and javac', () => {
    const home = 'C:/jdk21';
    const files = new Set([
      path.join(home, 'bin', 'java.exe'),
      path.join(home, 'bin', 'javac.exe'),
    ]);
    expect(isJdkHome(home, 'win32', existsFor(files))).toBe(true);
  });

  it('rejects a home missing javac', () => {
    const home = 'C:/jre';
    const files = new Set([path.join(home, 'bin', 'java.exe')]);
    expect(isJdkHome(home, 'win32', existsFor(files))).toBe(false);
  });

  it('rejects null home', () => {
    expect(isJdkHome(null, 'win32', always)).toBe(false);
  });

  it('uses java/javac without .exe on posix', () => {
    const home = '/opt/jdk';
    const files = new Set([
      path.join(home, 'bin', 'java'),
      path.join(home, 'bin', 'javac'),
    ]);
    expect(isJdkHome(home, 'linux', existsFor(files))).toBe(true);
  });
});

describe('resolveJdkHome', () => {
  const repoRoot = path.join('C:', 'repo');

  it('prefers EYECODE_JDK_HOME over staged and host', () => {
    const override = path.join('C:', 'override-jdk');
    const staged = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'jdk');
    const files = new Set([
      path.join(override, 'bin', 'java.exe'),
      path.join(override, 'bin', 'javac.exe'),
      path.join(staged, 'bin', 'java.exe'),
      path.join(staged, 'bin', 'javac.exe'),
    ]);
    expect(
      resolveJdkHome({
        repoRoot,
        env: { EYECODE_JDK_HOME: override, JAVA_HOME: 'C:/host' },
        platform: 'win32',
        existsSync: existsFor(files),
      })
    ).toBe(override);
  });

  it('prefers staged JDK over packaged resources and JAVA_HOME', () => {
    const staged = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'jdk');
    const packaged = path.join('C:', 'resources', 'jdk');
    const files = new Set([
      path.join(staged, 'bin', 'java.exe'),
      path.join(staged, 'bin', 'javac.exe'),
      path.join(packaged, 'bin', 'java.exe'),
      path.join(packaged, 'bin', 'javac.exe'),
      path.join('C:', 'host', 'bin', 'java.exe'),
      path.join('C:', 'host', 'bin', 'javac.exe'),
    ]);
    expect(
      resolveJdkHome({
        repoRoot,
        env: { JAVA_HOME: path.join('C:', 'host') },
        platform: 'win32',
        existsSync: existsFor(files),
        resourcesPath: path.join('C:', 'resources'),
      })
    ).toBe(staged);
  });

  it('uses packaged resources JDK when staged is missing', () => {
    const packaged = path.join('C:', 'resources', 'jdk');
    const files = new Set([
      path.join(packaged, 'bin', 'java.exe'),
      path.join(packaged, 'bin', 'javac.exe'),
    ]);
    expect(
      resolveJdkHome({
        repoRoot,
        env: {},
        platform: 'win32',
        existsSync: existsFor(files),
        resourcesPath: path.join('C:', 'resources'),
      })
    ).toBe(packaged);
  });

  it('falls back to JAVA_HOME when staged and packaged are missing', () => {
    const host = path.join('C:', 'host');
    const files = new Set([
      path.join(host, 'bin', 'java.exe'),
      path.join(host, 'bin', 'javac.exe'),
    ]);
    expect(
      resolveJdkHome({
        repoRoot,
        env: { JAVA_HOME: host },
        platform: 'win32',
        existsSync: existsFor(files),
      })
    ).toBe(host);
  });

  it('returns null when nothing is available', () => {
    expect(
      resolveJdkHome({
        repoRoot,
        env: {},
        platform: 'win32',
        existsSync: never,
      })
    ).toBeNull();
  });

  it('ignores EYECODE_JDK_HOME without java and javac', () => {
    expect(
      resolveJdkHome({
        repoRoot,
        env: { EYECODE_JDK_HOME: path.join('C:', 'broken') },
        platform: 'win32',
        existsSync: never,
      })
    ).toBeNull();
  });
});

describe('resolveJavaExecutable', () => {
  it('prefers EYECODE_JAVA over staged JDK and PATH', () => {
    expect(
      resolveJavaExecutable({
        env: { EYECODE_JAVA: 'D:/JBR/bin/java.exe', JAVA_HOME: 'C:/other' },
        platform: 'win32',
        existsSync: always,
        repoRoot: path.join('C:', 'repo'),
      })
    ).toBe('D:/JBR/bin/java.exe');
  });

  it('uses staged JDK java when present', () => {
    const repoRoot = path.join('C:', 'repo');
    const staged = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'jdk');
    const files = new Set([
      path.join(staged, 'bin', 'java.exe'),
      path.join(staged, 'bin', 'javac.exe'),
    ]);
    expect(
      resolveJavaExecutable({
        env: {},
        platform: 'win32',
        existsSync: existsFor(files),
        repoRoot,
      })
    ).toBe(path.join(staged, 'bin', 'java.exe'));
  });

  it('uses JAVA_HOME binary when it exists', () => {
    expect(
      resolveJavaExecutable({ env: { JAVA_HOME: 'C:/jbr' }, platform: 'win32', existsSync: always })
    ).toBe(path.join('C:/jbr', 'bin', 'java.exe'));
  });

  it('uses JAVA_HOME binary on posix', () => {
    expect(
      resolveJavaExecutable({ env: { JAVA_HOME: '/opt/jbr' }, platform: 'linux', existsSync: always })
    ).toBe(path.join('/opt/jbr', 'bin', 'java'));
  });

  it('falls back to PATH java when JAVA_HOME binary is missing', () => {
    expect(
      resolveJavaExecutable({ env: { JAVA_HOME: 'C:/missing' }, platform: 'win32', existsSync: never })
    ).toBe('java');
  });

  it('falls back to PATH java without JAVA_HOME', () => {
    expect(resolveJavaExecutable({ env: {}, platform: 'win32', existsSync: never })).toBe('java');
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
  const distReadyNoJdk = (p) => p.startsWith(distDir);

  it('never invokes Maven or exec:java', () => {
    const launch = buildBackendLaunch({
      repoRoot,
      env: {},
      platform: 'win32',
      existsSync: distReadyNoJdk,
    });
    const joined = [launch.command, ...launch.args].join(' ');
    expect(joined).not.toMatch(/\bmvn\b/);
    expect(joined).not.toMatch(/exec:java/);
    expect(launch.command).not.toBe('mvn');
  });

  it('builds direct java -cp launch of LocalWebShellLauncher without a toolchain', () => {
    const launch = buildBackendLaunch({
      repoRoot,
      env: {},
      platform: 'win32',
      existsSync: distReadyNoJdk,
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

  it('uses staged JDK absolute java path and passes -Deyecode.jdk.home', () => {
    const staged = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'jdk');
    const files = new Set([
      path.join(distDir, 'eyecode-webshell.jar'),
      path.join(distDir, 'lib'),
      path.join(staged, 'bin', 'java.exe'),
      path.join(staged, 'bin', 'javac.exe'),
    ]);
    const launch = buildBackendLaunch({
      repoRoot,
      env: {},
      platform: 'win32',
      existsSync: existsFor(files),
    });
    expect(launch.command).toBe(path.join(staged, 'bin', 'java.exe'));
    expect(launch.args).toContain(`-Deyecode.jdk.home=${staged}`);
    expect(launch.jdkHome).toBe(staged);
    expect(launch.command).not.toMatch(/\bmvn\b/);
  });

  it('keeps spaces in the distribution path inside a single spawn argument', () => {
    const launch = buildBackendLaunch({
      repoRoot,
      env: {},
      platform: 'win32',
      existsSync: distReadyNoJdk,
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
      existsSync: distReadyNoJdk,
    });
    expect(without.args.some((a) => a.includes('eyecode.jdtls.home'))).toBe(false);

    const withHome = buildBackendLaunch({
      repoRoot,
      env: { EYECODE_JDTLS_HOME: 'C:/jdt ls home' },
      platform: 'win32',
      existsSync: distReadyNoJdk,
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
      existsSync: (p) => p.startsWith(customDist),
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

  it('resolves packaged resources for both dist and JDK', () => {
    const resourcesPath = path.join('C:', 'install', 'resources');
    const packagedDist = path.join(resourcesPath, 'webshell-dist');
    const packagedJdk = path.join(resourcesPath, 'jdk');
    const files = new Set([
      path.join(packagedDist, 'eyecode-webshell.jar'),
      path.join(packagedDist, 'lib'),
      path.join(packagedJdk, 'bin', 'java.exe'),
      path.join(packagedJdk, 'bin', 'javac.exe'),
    ]);
    const launch = buildBackendLaunch({
      repoRoot: path.join('C:', 'nonexistent repo'),
      env: {},
      platform: 'win32',
      existsSync: existsFor(files),
      resourcesPath,
    });
    expect(launch.distDir).toBe(packagedDist);
    expect(launch.command).toBe(path.join(packagedJdk, 'bin', 'java.exe'));
    expect(launch.args).toContain(`-Deyecode.jdk.home=${packagedJdk}`);
    expect(launch.jdkHome).toBe(packagedJdk);
    const cpIndex = launch.args.indexOf('-cp');
    expect(launch.args[cpIndex + 1]).toContain(path.join(packagedDist, 'lib', '*'));
  });

  it('passes -Deyecode.maven.home when a packaged Maven home exists', () => {
    const resourcesPath = path.join('C:', 'install', 'resources');
    const packagedDist = path.join(resourcesPath, 'webshell-dist');
    const packagedMaven = path.join(resourcesPath, 'maven');
    const files = new Set([
      path.join(packagedDist, 'eyecode-webshell.jar'),
      path.join(packagedDist, 'lib'),
      path.join(packagedMaven, 'bin', 'mvn.cmd'),
    ]);
    const launch = buildBackendLaunch({
      repoRoot: path.join('C:', 'nonexistent repo'),
      env: {},
      platform: 'win32',
      existsSync: existsFor(files),
      resourcesPath,
    });
    expect(launch.args).toContain(`-Deyecode.maven.home=${packagedMaven}`);
    expect(launch.mavenHome).toBe(packagedMaven);
    expect(launch.args.filter((a) => a.includes('eyecode.maven.home'))).toHaveLength(1);
  });

  it('omits -Deyecode.maven.home when no Maven home is found', () => {
    const launch = buildBackendLaunch({
      repoRoot,
      env: {},
      platform: 'win32',
      existsSync: distReadyNoJdk,
    });
    expect(launch.args.some((a) => a.includes('eyecode.maven.home'))).toBe(false);
    expect(launch.mavenHome).toBeNull();
  });
});

describe('isMavenHome', () => {
  it('accepts a home with mvn.cmd on win32', () => {
    const home = 'C:/maven';
    const files = new Set([path.join(home, 'bin', 'mvn.cmd')]);
    expect(isMavenHome(home, 'win32', existsFor(files))).toBe(true);
  });

  it('rejects a home missing mvn.cmd', () => {
    expect(isMavenHome('C:/empty', 'win32', never)).toBe(false);
  });

  it('rejects null home', () => {
    expect(isMavenHome(null, 'win32', always)).toBe(false);
  });

  it('uses mvn without .exe on posix', () => {
    const home = '/opt/maven';
    const files = new Set([path.join(home, 'bin', 'mvn')]);
    expect(isMavenHome(home, 'linux', existsFor(files))).toBe(true);
  });
});

describe('resolveMavenHome', () => {
  const repoRoot = path.join('C:', 'repo');

  it('prefers EYECODE_MAVEN_HOME over staged and packaged', () => {
    const override = path.join('C:', 'override-maven');
    const staged = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'maven');
    const files = new Set([
      path.join(override, 'bin', 'mvn.cmd'),
      path.join(staged, 'bin', 'mvn.cmd'),
    ]);
    expect(
      resolveMavenHome({
        repoRoot,
        env: { EYECODE_MAVEN_HOME: override },
        platform: 'win32',
        existsSync: existsFor(files),
      })
    ).toBe(override);
  });

  it('uses the staged maven home when no override is set', () => {
    const staged = path.join(repoRoot, 'target', 'eyecode-desktop', 'runtime', 'maven');
    const files = new Set([path.join(staged, 'bin', 'mvn.cmd')]);
    expect(
      resolveMavenHome({
        repoRoot,
        env: {},
        platform: 'win32',
        existsSync: existsFor(files),
      })
    ).toBe(staged);
  });

  it('falls back to packaged resources/maven', () => {
    const resourcesPath = path.join('C:', 'install', 'resources');
    const packaged = path.join(resourcesPath, 'maven');
    const files = new Set([path.join(packaged, 'bin', 'mvn.cmd')]);
    expect(
      resolveMavenHome({
        repoRoot: path.join('C:', 'nonexistent repo'),
        env: {},
        platform: 'win32',
        existsSync: existsFor(files),
        resourcesPath,
      })
    ).toBe(packaged);
  });

  it('returns null when no maven home exists anywhere', () => {
    expect(
      resolveMavenHome({
        repoRoot,
        env: {},
        platform: 'win32',
        existsSync: never,
      })
    ).toBeNull();
  });

  it('ignores a blank EYECODE_MAVEN_HOME override', () => {
    expect(
      resolveMavenHome({
        repoRoot,
        env: { EYECODE_MAVEN_HOME: '   ' },
        platform: 'win32',
        existsSync: never,
      })
    ).toBeNull();
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
