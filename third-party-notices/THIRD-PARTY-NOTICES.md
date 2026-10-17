# Third-Party Notices — EyeCode Windows x64 distribution

This document lists every third-party component **actually shipped inside the
EyeCode Windows x64 Electron ZIP** (`EyeCode-win32-x64-<version>.zip`), the
license under which each component is used, and where the license text can be
found both in this repository and inside the packaged application.

Packaged location: `resources/third-party-notices/` (copied at build time from the
repository directory `third-party-notices/` via `forge.config.cjs`
`extraResource`). This file is `resources/third-party-notices/THIRD-PARTY-NOTICES.md`
inside the ZIP.

Component inventory is derived from the real packaged artifact and from the
`webshell-dist/target/webshell-dist/lib` jar list that the packaging pipeline
copies into the ZIP. If a component is not listed here, it was not found in
the distribution.

> **Status of this notice set**: prepared for the EyeCode beta ZIP. It is not
> a legal opinion. Components whose upstream license metadata could not be
> verified from first-party sources are marked `NEEDS_REVIEW` below.

---

## 1. Electron 33.2.1 (MIT)

- Shipped as: `EyeCode.exe`, `resources/default_app.asar`, Electron runtime
  DLLs under the app root (product of electron-forge `make --platform=win32`).
- License: **MIT** (Electron contributors).
- Evidence: the ZIP root contains Electron's own `LICENSE` file
  ("Copyright (c) Electron contributors").
- License text in package: `<zip root>/LICENSE` (kept by the maker; must not
  be deleted).
- License text in repo: `third-party-notices/licenses/MIT.txt` (generic MIT
  template; Electron's specific copyright line is in the ZIP root `LICENSE`).

## 2. Chromium (inside Electron) — BSD-3-Clause and others

- Shipped as: Chromium binaries inside `EyeCode.exe` / Electron runtime.
- License: Chromium's license set, primarily **BSD-3-Clause** (Google Inc. and
  others) plus additional notices for bundled third-party Chromium code.
- Evidence: the ZIP contains `LICENSES.chromium.html` at the app root
  (standard for Electron distributions).
- License text in package: `<zip root>/LICENSES.chromium.html` (the complete
  Chromium third-party notice file — must not be deleted).
- License text in repo: not duplicated here; the authoritative file ships
  inside the ZIP as `LICENSES.chromium.html`.

## 3. Eclipse Temurin JDK 21.0.12.1+1 x64 (GPLv2+CE)

- Shipped as: `resources/jdk/` (full JRE-equivalent layout including
  `legal/` directories with per-module license files).
- License: **GNU General Public License, version 2, with the Classpath
  Exception** (GPLv2+CE) — OpenJDK license.
- Evidence: JDK archive SHA-256
  `f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7d2d2d8b4e`
  (Eclipse Temurin 21.0.12.1+1 win_x64); `resources/jdk/legal/` contains 243
  license files (java.base, java.desktop, jdk.localedata, …) as recorded in
  the CP4 ZIP audit.
- License text in package: `resources/jdk/legal/**` (per-module `ASSEMBLY_EXCEPTION`,
  `LICENSE`, `ADDITIONAL_LICENSE_INFO`, module-specific third-party notices).
- License text in repo: not duplicated here; the JDK's own `legal/` tree is
  the authoritative source and ships in full.

## 4. OpenJFX / WebView-related natives — see bundled jar licenses

No separate OpenJFX distribution is shipped. JavaFX classes used by the
legacy Swing/JavaFX adapters are not part of the Electron ZIP's runtime path;
the packaged backend is the WebShell distribution (section 5).

## 5. Eyecode WebShell backend distribution (33 jars)

Shipped as: `resources/webshell-dist/` containing `eyecode-webshell.jar` and
`lib/*.jar` (33 jars, exact list in the package manifest
`target/eyecode-desktop/package-manifest.json` under
`webshell.runtimeJars`). Licenses below were verified from first-party
sources: embedded `pom.properties` / `META-INF` license files inside the
jars themselves, or the corresponding Maven Central `.pom` files (path
`.m2/repository/<groupPath>/<artifact>/<version>/<artifact>-<version>.pom`).

### 5.1 Apache-2.0 components

| Artifact | Version | Evidence |
|---|---|---|
| `org.jetbrains:annotations` | 24.0.1 | Maven Central pom |
| `com.google.errorprone:error_prone_annotations` | 2.48.0 | embedded jar pom |
| `com.google.code.gson:gson` | 2.14.0 | Maven Central pom |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.1.21 | Maven Central pom (no embedded license file) |

- License text in package: `resources/third-party-notices/licenses/Apache-2.0.txt`.
- License text in repo: `third-party-notices/licenses/Apache-2.0.txt`.

### 5.2 MIT components

| Artifact | Version | Evidence |
|---|---|---|
| `org.nibor.autolink:autolink` | 0.6.0 | embedded jar pom |
| `org.java-websocket:Java-WebSocket` | 1.5.7 | Maven Central pom |
| `org.slf4j:slf4j-api` | 2.0.13 | embedded `META-INF/LICENSE.txt` (QOS.ch Sarl) |

- License text in package: `resources/third-party-notices/licenses/MIT.txt`
  (generic MIT template — each project's specific copyright line is in the
  jar's own `META-INF` or in the upstream repository).
- License text in repo: `third-party-notices/licenses/MIT.txt`.

### 5.3 BSD 2-Clause components (flexmark-java family)

| Artifact | Version | Evidence |
|---|---|---|
| `com.vladsch.flexmark:flexmark` (+ 20 sibling modules: `flexmark-ext-*`, `flexmark-jira-converter`, `flexmark-util-*`) | 0.64.8 | parent pom `flexmark-java-0.64.8` (`<licenses>` = BSD 2-Clause); child poms have no license section |

- License text in package: `resources/third-party-notices/licenses/BSD-2-Clause.txt`.
- License text in repo: `third-party-notices/licenses/BSD-2-Clause.txt`.

### 5.4 JNA 5.14.0 — dual-licensed LGPL-2.1-or-later OR Apache-2.0; EyeCode elects Apache-2.0

| Artifact | Version | Evidence |
|---|---|---|
| `net.java.dev.jna:jna` | 5.14.0 | Maven Central pom + jar `META-INF/LICENSE` ("SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1 … you can freely decide") |
| `net.java.dev.jna:jna-platform` | 5.14.0 | same |

- Election: EyeCode distributes JNA under **Apache-2.0** (the dual-license
  allows free choice; electing the permissive option avoids LGPL obligations
  in this binary distribution).
- License text in package: `resources/third-party-notices/licenses/Apache-2.0.txt`.
- License text in repo: `third-party-notices/licenses/Apache-2.0.txt`
  (LGPL-2.1 text kept at
  `third-party-notices/licenses/LGPL-2.1.txt` for reference of the
  alternative option).

### 5.5 Eclipse Public License 2.0 components

| Artifact | Version | Evidence |
|---|---|---|
| `org.eclipse.lsp4j:org.eclipse.lsp4j` | 1.0.0 | Maven Central pom |
| `org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc` | 1.0.0 | Maven Central pom |

- License: **EPL-2.0**.
- Source availability: EPL-2.0 requires source code availability for the
  Program (the LSP4J jars). EyeCode ships the unmodified binary jars;
  upstream source is available from the Eclipse LSP4J project
  (`https://github.com/eclipse-lsp4j/lsp4j`). This notice records the
  obligation; it is not a substitute for complying with EPL-2.0 source
  availability if EyeCode itself redistributes modified LSP4J binaries
  (currently: unmodified).
- License text in package: `resources/third-party-notices/licenses/EPL-2.0.txt`.
- License text in repo: `third-party-notices/licenses/EPL-2.0.txt`.

### 5.6 Eclipse Public License 1.0 components

| Artifact | Version | Evidence |
|---|---|---|
| `org.jetbrains.pty4j:pty4j` | 0.13.12 | Maven Central pom (no embedded license file inside the jar) |

- License: **EPL-1.0**.
- Source availability: EPL-1.0 requires source availability from the
  Contributor for object-code distribution. Upstream pty4j source is
  available from `https://github.com/JetBrains/pty4j`. EyeCode ships the
  unmodified binary jar.
- License text in package: `resources/third-party-notices/licenses/EPL-1.0.txt`.
- License text in repo: `third-party-notices/licenses/EPL-1.0.txt`.

### 5.7 pty4j native libraries (winpty, conpty, win-helper)

- Shipped inside `pty4j-0.13.12.jar` as platform natives (winpty DLLs,
  conpty, `win-helper.dll`, libpty.*). These are components of the pty4j
  distribution and are covered by pty4j's EPL-1.0 (section 5.6) and any
  upstream notices inside the jar.
- `NEEDS_REVIEW` (low): winpty itself is MIT-licensed upstream; its notice
  is not separately extracted here because it is not separately shipped —
  it is part of the pty4j jar as published by JetBrains. If EyeCode later
  republishes winpty binaries standalone, a separate MIT notice is required.

### 5.8 JNA native library (jnidispatch)

- Shipped inside `jna-5.14.0.jar` as `com/sun/jna/win32-x86-64/jnidispatch.dll`
  (and other platforms). Covered by JNA's dual license; EyeCode elects
  Apache-2.0 (section 5.4).

### 5.9 Remaining webshell-dist jars — license inventory

All 33 jars accounted for above:

| # | Jar | License section |
|---|---|---|
| 1 | annotations-24.0.1.jar | 5.1 Apache-2.0 |
| 2 | autolink-0.6.0.jar | 5.2 MIT |
| 3 | error_prone_annotations-2.48.0.jar | 5.1 Apache-2.0 |
| 4–24 | flexmark* (21 jars, v0.64.8) | 5.3 BSD-2-Clause |
| 25 | gson-2.14.0.jar | 5.1 Apache-2.0 |
| 26 | Java-WebSocket-1.5.7.jar | 5.2 MIT |
| 27 | jna-5.14.0.jar | 5.4 Apache-2.0 (elected) |
| 28 | jna-platform-5.14.0.jar | 5.4 Apache-2.0 (elected) |
| 29 | kotlin-stdlib-2.1.21.jar | 5.1 Apache-2.0 |
| 30 | org.eclipse.lsp4j.jsonrpc-1.0.0.jar | 5.5 EPL-2.0 |
| 31 | org.eclipse.lsp4j-1.0.0.jar | 5.5 EPL-2.0 |
| 32 | pty4j-0.13.12.jar | 5.6 EPL-1.0 (+ 5.7 natives) |
| 33 | slf4j-api-2.0.13.jar | 5.2 MIT |

## 6. Monaco Editor 0.52.2 (MIT)

- Shipped as: `resources/webshell-dist/eyecode-webshell.jar` →
  `monaco/editor/**` (AMD loader, vs workers, CSS). Version marker:
  `src/main/resources/monaco/editor/vs/loader.js` header
  ("Monaco Editor 0.52.2").
- License: **MIT** (Microsoft Corporation).
- Evidence: Monaco upstream is MIT; the bundled copy inside the jar does
  **not** currently contain an embedded LICENSE file — this notice supplies
  the MIT text.
- License text in package: `resources/third-party-notices/licenses/MIT.txt`.
- License text in repo: `third-party-notices/licenses/MIT.txt`.

## 7. Frontend libraries bundled into the webshell assets

Shipped as compiled/minified assets under
`resources/webshell-dist/` (production `index.html` + hashed JS/CSS chunks
produced by Vite from `src/main/web/`).

| Library | Version | License | Evidence |
|---|---|---|---|
| React | 19.1.1 | MIT (Meta Platforms, Inc. and affiliates) | `node_modules/react/LICENSE` |
| React DOM | 19.1.1 | MIT (Meta Platforms, Inc. and affiliates) | same LICENSE file |
| @xterm/xterm | 5.5.0 | MIT | `node_modules/@xterm/xterm/LICENSE` |
| @xterm/addon-fit | 0.10.0 | MIT | same LICENSE family |

- License text in package: `resources/third-party-notices/licenses/MIT.txt`.
- License text in repo: `third-party-notices/licenses/MIT.txt`.

## 8. Build-time-only tools (NOT shipped — listed for completeness)

The following are used at build time and are **not** present in the ZIP
root, asar, or resources. They are listed only so the absence is explicit:

- Vite 7.1.3 (MIT), TypeScript 5.8.3 (Apache-2.0),
  @vitejs/plugin-react (MIT), Electron Forge 7.9.0 (MIT),
  Vitest (MIT), jsdom (MIT), @types/* (MIT), Electron packager/maker
  (MIT). None of these appear as loose files or jars in the distribution.

## 9. EyeCode first-party code — license status

- EyeCode's original project code is licensed under **GPL-3.0-or-later**
  (GNU General Public License v3.0 or any later version, at the recipient's
  option). The full license text is the repository root `LICENSE` file
  (canonical GPLv3 text from gnu.org). In the desktop ZIP it ships at
  `resources/LICENSE`, **separately** from this third-party notices file.
- This first-party license does **not** change the license of any component
  listed in sections 1–8; third-party components keep their own licenses.
- `src/main/java/LICENSE.txt` is the **JCEF BSD-3-Clause** license for the
  extracted JCEF Java bindings (Marshall Greenblatt / Google Inc.) — it is
  **not** a license for EyeCode itself.
- Copyright-holder line: not asserted in this document; the maintainer
  should confirm the preferred copyright notice (see
  `docs/OPEN_SOURCE_LICENSING_REPORT.md`).

## 10. Obligations checklist (what this distribution must keep)

| Obligation | How this ZIP satisfies it | Residual work |
|---|---|---|
| Electron MIT — keep copyright notice | `<zip root>/LICENSE` ships | none |
| Chromium — keep full third-party notice file | `<zip root>/LICENSES.chromium.html` ships | none |
| JDK GPLv2+CE — keep `legal/` tree | `resources/jdk/legal/**` ships (243 files) | none |
| EPL-2.0 (LSP4J) — source availability for shipped binaries | Unmodified jars; upstream source public | none while unmodified; document if jars are ever patched |
| EPL-1.0 (pty4j) — source availability for shipped binaries | Unmodified jar; upstream source public | none while unmodified |
| JNA dual license — license election | Apache-2.0 elected (permissive option) | keep Apache-2.0 text in package (done) |
| Apache-2.0 / MIT / BSD-2 — retain notices | Supplied via this document + license texts | none |
| First-party license | **GPL-3.0-or-later** (`LICENSE` at repo root + `resources/LICENSE` in ZIP) | Maintainer should confirm preferred copyright-holder line |

## 11. `NEEDS_REVIEW` items

1. **First-party copyright line**: the project license is now
   `GPL-3.0-or-later` (section 9), but no explicit copyright-holder notice
   has been asserted in source files or a NOTICE file. Maintainer decision
   remains on the preferred copyright line (e.g. author name + years).
   Does not block packaging; recommended before a public announcement.
2. **winpty upstream notice inside pty4j jar**: not separately extracted
   (section 5.7). Low risk — part of the pty4j EPL-1.0 distribution as
   published by JetBrains.

---

*Generated for EyeCode Electron packaging (CP5). Component versions are
pinned by `src/main/web/package.json` (product/Electron) and
`webshell-dist/pom.xml` (backend jars). Re-verify this inventory after any
dependency change.*
