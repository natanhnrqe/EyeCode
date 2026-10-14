import path from 'node:path';
import fs from 'node:fs';

export const MAIN_CLASS = 'com.eyecode.ui.web.LocalWebShellLauncher';
export const DIST_JAR_NAME = 'eyecode-webshell.jar';
export const DIST_LIB_DIR_NAME = 'lib';
export const READINESS_URL_PATTERN = /\[EyeCode\] URL: (http:\/\/127\.0\.0\.1:\d+\/webshell\/index\.html)/;
export const STARTUP_TIMEOUT_MS = 60_000;

export function resolveDistDir(repoRoot, env = process.env) {
  const override = (env.EYECODE_WEBSHELL_DIST || '').trim();
  if (override) return path.resolve(override);
  return path.join(repoRoot, 'webshell-dist', 'target', 'webshell-dist');
}

export function resolveJavaExecutable(env = process.env, platform = process.platform, existsSync = fs.existsSync) {
  const override = (env.EYECODE_JAVA || '').trim();
  if (override) return override;
  const javaHome = (env.JAVA_HOME || '').trim();
  if (javaHome) {
    const candidate = path.join(javaHome, 'bin', platform === 'win32' ? 'java.exe' : 'java');
    if (existsSync(candidate)) return candidate;
  }
  return 'java';
}

export function assertDistReady(distDir, existsSync = fs.existsSync) {
  const jar = path.join(distDir, DIST_JAR_NAME);
  const libDir = path.join(distDir, DIST_LIB_DIR_NAME);
  if (!existsSync(jar) || !existsSync(libDir)) {
    throw new Error(
      `WebShell distribution not found at:\n  ${distDir}\n\n` +
      'Build it from the repository root:\n' +
      '  mvn -f webshell-dist/pom.xml package\n\n' +
      'Or run "npm run electron:dev" from src/main/web, which builds the frontend and the distribution before starting Electron.'
    );
  }
  return { jar, libDir };
}

export function buildBackendLaunch({ repoRoot, env = process.env, platform = process.platform, existsSync = fs.existsSync }) {
  const distDir = resolveDistDir(repoRoot, env);
  const { jar, libDir } = assertDistReady(distDir, existsSync);
  const java = resolveJavaExecutable(env, platform, existsSync);
  const classpath = `${jar}${path.delimiter}${path.join(libDir, '*')}`;
  const args = ['-Deyecode.web.openBrowser=false'];
  const jdtlsHome = (env.EYECODE_JDTLS_HOME || '').trim();
  if (jdtlsHome) {
    args.push(`-Deyecode.jdtls.home=${jdtlsHome}`);
  }
  args.push('-cp', classpath, MAIN_CLASS);
  return { command: java, args, cwd: distDir, distDir };
}

export function parseReadinessUrl(buffer) {
  const match = buffer.match(READINESS_URL_PATTERN);
  return match ? match[1] : null;
}
