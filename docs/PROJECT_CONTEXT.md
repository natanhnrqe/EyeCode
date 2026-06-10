# EyeCode IDE

## Visão Geral

EyeCode é uma IDE Desktop desenvolvida em Java Swing inspirada visualmente no IntelliJ IDEA.

O objetivo principal é aprender desenvolvimento de software em larga escala através da construção de uma IDE funcional, explorando arquitetura, UX, execução de código, sistemas de plugins, análise de projetos e ferramentas de produtividade para desenvolvedores.

O projeto prioriza aprendizado e qualidade arquitetural acima da velocidade de implementação.

---

# Stack Tecnológica

## Core

* Java 21+
* Swing

## UI

* FlatLaf
* JetBrains Mono

## Build System

* Maven

## Terminal

* JEditerm
* Pty4j

---

# Arquitetura Atual

## UI

* MainWindow
* TopBarPanel
* ToolWindowBar
* BottomToolWindowPanel
* FileExplorerPanel
* StatusBar
* ActivityBar

## Editor

* EditorPanel
* Document

## Execução

* RunManager
* ProjectRunner
* JavaRunner
* MavenRunner
* MainClassFinder

## Maven

* MavenProject
* MavenDependency
* PomParser

---

# Filosofia

## Regras

* Evitar acoplamento desnecessário.
* Separar UI de lógica de negócio.
* Refatorar quando necessário.
* Componentes reutilizáveis.
* Estruturas inspiradas no IntelliJ IDEA.

## Não Fazer

* Soluções temporárias que precisem ser descartadas.
* Classes gigantes com múltiplas responsabilidades.
* Lógica de negócio dentro da UI.

---

# Objetivos de Curto Prazo

* Refinar UX
* Melhorar aparência profissional
* Tornar execução Maven robusta
* Melhorar gerenciamento de abas

---

# Objetivos de Médio Prazo

* Busca global
* File Watcher
* Run Configurations
* Gradle Support

---

# Objetivos de Longo Prazo

* Auto Complete
* LSP
* Debugger
* Sistema de Plugins
* Marketplace
