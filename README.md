<div align="center">

<!-- BANNER -->
<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0f2027,50:2c5364,100:00c6ff&height=260&section=header&text=EyeCode&fontSize=70&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=Software%20Architecture%20Exploration%20Through%20Code&descAlignY=58&descSize=18" width="100%"/>

<!-- TYPING SVG -->
<a href="#-overview">
  <img src="https://readme-typing-svg.demolab.com/?font=Fira+Code&size=20&duration=3000&pause=1000&color=00C6FF&center=true&vCenter=true&width=650&lines=javac+EyeCode.java;java+-jar+eyecode.jar;%3E+Explorando+arquitetura+de+software+na+pr%C3%A1tica" />
</a>

<br><br>

<!-- BADGES -->
<img src="https://img.shields.io/badge/Java-21%2B-00C6FF?style=for-the-badge&logo=openjdk&labelColor=0f2027" />
<img src="https://img.shields.io/badge/Architecture-Modular-2c5364?style=for-the-badge&labelColor=0f2027" />
<img src="https://img.shields.io/badge/IDE-Desktop%20Application-00c6ff?style=for-the-badge&labelColor=0f2027" />
<img src="https://img.shields.io/badge/Status-In%20Development-2c5364?style=for-the-badge&labelColor=0f2027" />

<br><br>

<!-- NAVEGAÇÃO -->
<p>
  <a href="#-overview">Overview</a> •
  <a href="#-engineering-philosophy">Philosophy</a> •
  <a href="#-system-model">System Model</a> •
  <a href="#-architecture-decision-records-adr">ADRs</a> •
  <a href="#-roadmap">Roadmap</a>
</p>

</div>

<div align="center">
  <img alt="Image" src="https://github.com/user-attachments/assets/128893d4-dc2f-4404-afda-7b95dc4c1ea4" width="100%"/>
</div>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:00c6ff,100:0f2027&height=3&section=header" width="100%"/>

<br>

## 👁️ Overview

> *"Software architecture is not learned by reading — it is learned by building systems that evolve."*

**EyeCode** is a Java-based desktop IDE project designed not as a product, but as an **engineering exploration platform**.

The objective is to understand how professional IDEs are structured internally by building one from scratch and evolving its architecture over time.

Instead of focusing only on features, EyeCode focuses on:

<table align="center">
<tr>
<td align="center" width="16%">🧱<br><sub><b>System Design</b></sub></td>
<td align="center" width="16%">🧩<br><sub><b>Decomposition</b></sub></td>
<td align="center" width="16%">📝<br><sub><b>State Mgmt</b></sub></td>
<td align="center" width="16%">⚡<br><sub><b>Completion</b></sub></td>
<td align="center" width="16%">🎨<br><sub><b>Rendering</b></sub></td>
<td align="center" width="16%">🏗<br><sub><b>Orchestration</b></sub></td>
</tr>
</table>

<br>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:00c6ff,100:0f2027&height=3&section=header" width="100%"/>

<br>

## 🧠 Engineering Philosophy

EyeCode is built under a single principle:

> Architecture must justify itself through complexity.

This means:

- No feature exists without a structural reason
- Every module solves a system-level problem
- Complexity is introduced only when necessary
- Refactoring is part of the design, not a failure

<br>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:00c6ff,100:0f2027&height=3&section=header" width="100%"/>

<br>

## ⚙️ System Model

The system is structured around four core concerns:

<table align="center">
<tr>
<td align="center" width="25%">
<h3>🧩</h3>
<b>Editor System</b><br>
<sub>Document lifecycle, editing state, and interaction model</sub>
</td>
<td align="center" width="25%">
<h3>⚡</h3>
<b>Completion Engine</b><br>
<sub>Independent system for suggestion ranking and contextual awareness</sub>
</td>
<td align="center" width="25%">
<h3>🏗</h3>
<b>Workspace Layer</b><br>
<sub>Manages multiple editor sessions and application-level state</sub>
</td>
<td align="center" width="25%">
<h3>🎨</h3>
<b>Rendering Pipeline</b><br>
<sub>Decoupled UI rendering responsible for performance and composition</sub>
</td>
</tr>
</table>

<br>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:00c6ff,100:0f2027&height=3&section=header" width="100%"/>

<br>

## 🔁 Architectural Evolution

The system evolves continuously:

| Phase | Milestone |
|---|---|
| **Phase 1** | Monolithic prototype |
| **Phase 2** | Modular decomposition |
| **Phase 3** | System separation (editor/workspace/rendering) |
| **Phase 4** | Engine-level abstraction |
| **Phase 5** | AI-assisted architecture *(future)* |

<br>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:00c6ff,100:0f2027&height=3&section=header" width="100%"/>

<br>

## 📘 Architecture Decision Records (ADR)

<details open>
<summary><b>ADR-001 — Editor Isolation Model</b></summary>
<br>

Each editor instance operates independently with its own document lifecycle.

**Why:**
- Prevents global state coupling
- Enables multi-document architecture
- Improves scalability of editor system

</details>

<details>
<summary><b>ADR-002 — Completion Engine Separation</b></summary>
<br>

Completion is treated as an independent subsystem.

**Why:**
- Allows ranking algorithm evolution
- Decouples UI from suggestion logic
- Enables future AI integration

</details>

<details>
<summary><b>ADR-003 — Rendering Decoupling</b></summary>
<br>

Rendering is fully separated from business logic.

**Why:**
- Prevents UI from dictating architecture
- Enables performance optimization
- Improves long-term maintainability

</details>

<br>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:00c6ff,100:0f2027&height=3&section=header" width="100%"/>

<br>

## 🏗 System Architecture

```
Application Layer
 ├── Workspace Manager
 │    ├── Editor Sessions
 │    │    ├── Document Model
 │    │    └── Editor View
 │    └── Completion Engine
 │
 ├── Rendering Pipeline
 │
 └── UI Layer
```

### 📁 Repository Structure

```
src/
 ├── core/
 ├── editor/
 ├── workspace/
 ├── rendering/
 ├── completion/
 ├── ai/
 └── ui/
```

<br>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:00c6ff,100:0f2027&height=3&section=header" width="100%"/>

<br>

## 🗺 Roadmap

- [x] **Phase 1 — Core Editor** — Text editing system, basic UI foundation
- [ ] **Phase 2 — Architecture Refactor** — Modular separation, workspace system
- [ ] **Phase 3 — Completion Engine** — Ranking system, context-aware suggestions
- [ ] **Phase 4 — Advanced Tooling** — Plugin system, external integrations
- [ ] **Phase 5 — Intelligent Layer** — AI-assisted features, workflow automation

<br>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:00c6ff,100:0f2027&height=3&section=header" width="100%"/>

<div align="center">

### 📌 Final Statement

> EyeCode is not a project about features.
> It is a project about how software systems are designed, structured, and evolved.

<br>

⭐ If this project reflects your interests in software architecture, consider following its development.

</div>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0f2027,50:2c5364,100:00c6ff&height=150&section=footer" width="100%"/>



