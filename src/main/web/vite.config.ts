import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const webRoot = path.dirname(fileURLToPath(import.meta.url));
const monacoRoot = path.resolve(webRoot, '../../main/resources/monaco/editor');
const completionIconsRoot = path.resolve(webRoot, '../../main/resources/icons/completion');
const iconsRoot = path.resolve(webRoot, '../../main/resources/icons');

function existingMonacoAssets() {
  return {
    name: 'eyecode-existing-monaco-assets',
    configureServer(server: { middlewares: { use: (path: string, handler: (request: any, response: any, next: () => void) => void) => void } }) {
      server.middlewares.use('/monaco/editor', (request, response, next) => {
        const relative = decodeURIComponent((request.url ?? '/').split('?')[0]).replace(/^[/\\]+/, '');
        const file = path.resolve(monacoRoot, relative);
        if (!file.startsWith(monacoRoot + path.sep)) {
          next();
          return;
        }
        try {
          if (!statSync(file).isFile()) {
            next();
            return;
          }
          const contentTypes: Record<string, string> = {
            '.css': 'text/css',
            '.js': 'text/javascript',
            '.json': 'application/json',
            '.svg': 'image/svg+xml',
            '.woff': 'font/woff',
            '.ttf': 'font/ttf'
          };
          response.setHeader('Content-Type', contentTypes[path.extname(file)] ?? 'application/octet-stream');
          response.end(readFileSync(file));
        } catch {
          next();
        }
      });
    }
  };
}

function existingCompletionIcons() {
  return {
    name: 'eyecode-existing-completion-icons',
    configureServer(server: { middlewares: { use: (path: string, handler: (request: any, response: any, next: () => void) => void) => void } }) {
      server.middlewares.use('/icons/completion', (request, response, next) => {
        const relative = decodeURIComponent((request.url ?? '/').split('?')[0]).replace(/^[/\\]+/, '');
        const file = path.resolve(completionIconsRoot, relative);
        if (!file.startsWith(completionIconsRoot + path.sep)) { next(); return; }
        try {
          if (!statSync(file).isFile()) { next(); return; }
          response.setHeader('Content-Type', 'image/svg+xml');
          response.end(readFileSync(file));
        } catch { next(); }
      });
    },
    generateBundle() {
      for (const name of readdirSync(completionIconsRoot)) {
        if (name.endsWith('.svg')) {
          this.emitFile({
            type: 'asset',
            fileName: `icons/completion/${name}`,
            source: readFileSync(path.join(completionIconsRoot, name))
          });
        }
      }
    }
  };
}

function existingEyeCodeIcons() {
  return {
    name: 'eyecode-existing-icons',
    configureServer(server: { middlewares: { use: (path: string, handler: (request: any, response: any, next: () => void) => void) => void } }) {
      server.middlewares.use('/icons', (request, response, next) => {
        const relative = decodeURIComponent((request.url ?? '/').split('?')[0]).replace(/^[/\\]+/, '');
        const file = path.resolve(iconsRoot, relative);
        if (!file.startsWith(iconsRoot + path.sep)) { next(); return; }
        try {
          if (!statSync(file).isFile()) { next(); return; }
          response.setHeader('Content-Type', path.extname(file) === '.png' ? 'image/png' : 'image/svg+xml');
          response.end(readFileSync(file));
        } catch { next(); }
      });
    },
    generateBundle() {
      for (const name of readdirSync(iconsRoot)) {
        const source = path.join(iconsRoot, name);
        if (statSync(source).isFile() && (name.endsWith('.svg') || name.endsWith('.png'))) {
          this.emitFile({ type: 'asset', fileName: `icons/${name}`, source: readFileSync(source) });
        }
      }
    }
  };
}

export default defineConfig({
  base: './',
  plugins: [react(), existingMonacoAssets(), existingCompletionIcons(), existingEyeCodeIcons()],
  build: {
    outDir: '../../main/resources/webshell',
    emptyOutDir: true
  }
});
