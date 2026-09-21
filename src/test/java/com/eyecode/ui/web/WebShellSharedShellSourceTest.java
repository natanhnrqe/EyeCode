package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellSharedShellSourceTest {
    @Test
    void queuesEphemeralLessonModelsUntilMonacoIsMounted() throws IOException {
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));

        assertTrue(monaco.contains("private readonly pendingEphemeralModels = new Map<string, PendingEphemeralModel>();"));
        assertTrue(monaco.contains("this.pendingEphemeralModels.set(uri, { content, language, readOnly });"));
        assertTrue(monaco.contains("this.pendingEphemeralActiveUri = uri;"));
        assertTrue(monaco.contains("const pendingEphemeral = [...this.pendingEphemeralModels.entries()];"));
        assertTrue(monaco.contains("uri === activeEphemeralUri"));
        assertTrue(monaco.contains("pending.content = content;"));
        assertTrue(monaco.contains("this.pendingEphemeralModels.delete(uri);"));
    }

    @Test
    void workspaceOwnsOnePersistentMonacoHostAcrossProjectAndLearnModes() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String learnWorkspace = Files.readString(Path.of("src/main/web/src/lessons/LearnWorkspace.tsx"));
        String lessonPanel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        assertTrue(workspace.contains("type AppMode = 'WELCOME' | 'PROJECT' | 'LEARN'"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(workspace.contains("<div ref={shellWorkspace} className={`shell-workspace"));
        assertTrue(workspace.contains("learnNavigationVisible && <LearnWorkspace"));
        assertFalse(workspace.contains("LessonsPanel"));
        assertFalse(learnWorkspace.contains("MonacoHost"));
        assertTrue(workspace.contains("onClick={() => startLesson(selectedLearnLesson)}"));
        assertTrue(styles.contains(".workspace-empty > div:last-child, .workspace-empty > button"));
        assertTrue(lessonPanel.contains("highlightLearningJavaSource"));
        assertTrue(monaco.contains("animateEphemeralEdit"));
        assertTrue(monaco.contains("model.applyEdits([edit])"));
        assertFalse(monaco.contains("executeEdits('eyecode.lesson.typing'"));
        assertTrue(monaco.contains("cancelLessonTyping()"));
        assertTrue(monaco.contains("this.ephemeralModels.get(uri)"));
        assertTrue(monaco.contains("uri.startsWith('lesson://')"));
        assertTrue(monaco.contains("this.editor?.getModel() !== model || model.uri.toString() !== uri"));
        assertTrue(monaco.contains("model.getValue() !== finalCode"));
        assertTrue(monaco.contains("resolve(false);"));
        assertTrue(monaco.contains("character === '\\n' && !establishIndentation()"));
        assertTrue(monaco.contains("replacementText.charAt(end) === ' ' || replacementText.charAt(end) === '\\t'"));
        assertEquals(1, occurrences(monaco, "establishIndentation()"));
        assertFalse(monaco.contains("typeEphemeralCode"));
        assertFalse(monaco.contains("findIndex(command => command.type === 'ANIMATE_EDIT')"));
        assertFalse(monaco.contains("publishLessonAnimationDiagnostic"));
        assertFalse(workspace.contains("LessonAnimationTrace"));
        assertFalse(styles.contains("lesson-animation-trace"));
        assertTrue(workspace.contains("lessonPresentationReady"));
        assertTrue(workspace.contains("lessonSession?.kind === 'PRACTICE' && lessonPresentationReady && <LessonAnnotation"));
        assertTrue(styles.contains(".lesson-reading-article"));
    }

    @Test
    void compiledLineInsertionPreparesStructureBeforeTypingTheStatement() throws IOException {
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));

        assertTrue(monaco.contains("const firstCharacter = replacementText.charAt(0);"));
        assertTrue(monaco.contains("text: `${operation.prefix}${firstCharacter}${operation.suffix}`"));
        assertTrue(monaco.contains("replacementText = replacementText.slice(firstCharacter.length);"));
        assertTrue(monaco.contains("operation.startOffset + operation.prefix.length + firstCharacter.length"));
        assertFalse(monaco.contains("text: `${operation.prefix}${operation.suffix}`"));
        assertTrue(monaco.contains("index === program.operations.length - 1"));
        assertTrue(monaco.contains("}, replacementText, program.targetCode, LESSON_TYPING_CADENCE_MS, index === program.operations.length - 1);"));
        assertFalse(monaco.contains("mainClosingBraceOffset"));
        assertFalse(monaco.contains("APPEND_TO_MAIN"));
    }

    @Test
    void independentPresentationBasesAreNotMistakenForCumulativeTransitions() throws IOException {
        String controller = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));

        assertTrue(controller.contains("const currentCode = this.service.ephemeralModelValue(this.activeUri ?? '');"));
        assertTrue(controller.contains("currentCode === program.sourceCode"));
        assertTrue(controller.contains("session.navigationDirection !== 'NONE'"));
        assertTrue(controller.contains("if (!sequential || !canonicalCode)"));
        assertTrue(monaco.contains("for (let index = 0; index < program.operations.length; index++)"));
        assertTrue(monaco.contains("await this.animateEphemeralEdit(uri,"));
    }

    @Test
    void reverseNavigationCancelsTheCurrentPlayerBeforeStartingTheNextProgram() throws IOException {
        String controller = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));

        assertTrue(controller.contains("this.cancelAnimation();"));
        assertTrue(monaco.contains("async playPresentationProgram(uri: string, program: PresentationProgram)"));
        assertTrue(monaco.contains("this.cancelLessonTyping();"));
        assertTrue(monaco.contains("if (this.ephemeralModels.has(active.uri)) this.setEphemeralModelValue(active.uri, active.finalCode);"));
    }

    @Test
    void persistentEditorSurfaceIsTheRoundedMonacoClipBoundary() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        String surfaceRule = cssRule(styles, ".persistent-editor-surface");
        assertTrue(workspace.contains("className={`persistent-editor-surface"));
        assertTrue(workspace.contains("<MonacoHost service={service} />"));
        assertTrue(surfaceRule.contains("border-radius: var(--eyecode-radius-md);"));
        assertTrue(surfaceRule.contains("overflow: hidden;"));
        assertEquals(1, occurrences(styles, ".persistent-editor-surface {"));
        assertFalse(styles.contains(".monaco-editor {\n  border-radius"));
        assertFalse(styles.contains(".overflow-guard {\n  border-radius"));
    }

    @Test
    void practiceUsesTheExistingLessonModelWithoutStartingProfessorCommands() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String controller = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));

        assertTrue(monaco.contains("if ([...this.ephemeralModels.values()].includes(model)) return;"));
        assertTrue(monaco.indexOf("if ([...this.ephemeralModels.values()].includes(model)) return;")
                < monaco.indexOf("bridge.request<{ document: DocumentSnapshot }>('document', 'change'"));
        assertTrue(controller.contains("if (session.phase === 'PRACTICE') this.enterPractice();\n    else this.applyPresentation(session);"));
        assertTrue(workspace.contains("else if (!lessonEditor.lessonUri() && session.workspace)"));
        assertTrue(workspace.contains("lessonEditor.openWorkspace(session).forEach(updateDocument);"));
        assertTrue(controller.contains("private readonly documentsByUri = new Map<string, LessonDocument>();"));
        assertTrue(controller.contains("if (!this.practiceStarted || this.service.ephemeralModelValue(this.activeUri) !== active.file.starterCode)"));
        assertTrue(controller.contains("this.service.updateLessonFile("));
        assertTrue(controller.contains("this.practiceStarted = true;"));
        assertTrue(controller.contains("this.service.setEphemeralReadOnly(document.uri, true);"));
        assertTrue(monaco.contains("focus(): void { this.editor?.focus(); }"));
        assertTrue(workspace.contains("const lessonDocuments = documents.filter(document => document.kind === 'lesson');"));
        assertTrue(workspace.contains("<EditorTabs documents={lessonDocuments} activeUri={activeUri}"));
        assertFalse(workspace.contains("documents={[lessonTab("));
        assertTrue(controller.contains("for (const file of workspace.files)"));
        assertTrue(controller.contains("this.service.registerLessonFile(\n        uri,\n        file.starterCode,"));
        assertTrue(workspace.contains("function activateLessonFile(uri: string): boolean"));
        assertTrue(controller.contains("return this.service.activateLessonFile(uri);"));
    }

    @Test
    void practiceShowsItsInstructionAndEnablesOnlyEphemeralIntelligence() throws IOException {
        String panel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String taskCard = Files.readString(Path.of("src/main/web/src/lessons/LessonTaskCard.tsx"));
        String controller = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));
        String completion = Files.readString(Path.of("src/main/java/com/eyecode/ui/web/WebShellCompletionController.java"));
        String learning = Files.readString(Path.of("src/main/java/com/eyecode/ui/web/WebShellLearningController.java"));

        assertTrue(taskCard.contains("const practice = session.practice;"));
        assertTrue(panel.contains("lesson-practice-header"));
        assertTrue(taskCard.contains("lesson-task-instruction\"><InlineContent content={practice.instruction} /></p>"));
        assertTrue(controller.contains("this.service.setLessonPracticeIntelligence(document.uri, false);"));
        assertTrue(controller.contains("this.service.setLessonPracticeIntelligence(\n        document.uri,\n        true"));
        assertTrue(monaco.contains("private readonly lessonPracticeUris = new Set<string>();"));
        assertTrue(monaco.contains("uri.startsWith('lesson://') && !lessonPractice"));
        assertTrue(monaco.contains("lessonPractice,"));
        assertTrue(monaco.contains("lessonPractice: this.lessonPracticeUris.has(target.uri),"));
        assertTrue(monaco.contains("if ([...this.ephemeralModels.values()].includes(model)) return;"));
        assertTrue(completion.contains("boolean lessonPractice = isLessonPracticeRequest(message.payload(), modelId);"));
        assertTrue(completion.contains("new LanguageDocument(modelId, session == null ? null : session.getFile(),"));
        assertTrue(completion.contains("new CompletionRequest(document,"));
        assertTrue(completion.contains("uri.startsWith(\"lesson://\") && Boolean.TRUE.equals(payload.get(\"lessonPractice\"))"));
        assertTrue(learning.contains("boolean lessonPractice = isLessonPracticeRequest(message.payload(), uri);"));
        assertTrue(learning.contains("new EditorDocument(session == null ? null : session.getFile(), content)"));
        assertTrue(learning.contains("uri.startsWith(\"lesson://\") && Boolean.TRUE.equals(payload.get(\"lessonPractice\"))"));
    }

    @Test
    void practiceVerificationUsesCurrentEphemeralSourceAndAuthoritativeSessionState() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String controller = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String taskCard = Files.readString(Path.of("src/main/web/src/lessons/LessonTaskCard.tsx"));
        String lessons = Files.readString(Path.of("src/main/java/com/eyecode/ui/web/WebShellLessonsController.java"));

        assertTrue(controller.contains("practiceSource(): string | null"));
        assertTrue(controller.contains("this.service.ephemeralModelValue(this.activeUri)"));
        assertTrue(workspace.contains("lessonEditor.practiceSource()"));
        assertTrue(workspace.contains("'lessons', 'session/verify'"));
        assertTrue(workspace.contains("sessionId, practiceId, source"));
        assertTrue(workspace.contains("lessonEditor.practiceSource() !== source"));
        assertTrue(workspace.contains("response.session.phase !== 'PRACTICE'"));
        assertTrue(taskCard.contains("onClick={onVerify}"));
        assertTrue(taskCard.contains("{verifying ? 'Verificando...' : 'Verificar'}"));
        assertTrue(taskCard.contains("{verification.message}"));
        assertTrue(lessons.contains("surface.registerHandler(\"lessons\", \"session/verify\", this::verify)"));
        assertTrue(lessons.contains("sessionService.verifyPractice(sessionId, practiceId, source, validator)"));
        assertTrue(lessons.contains("\"verification\", Map.of(\"status\""));
    }

    @Test
    void dockPaneRegistryPreservesThePersistentMonacoSurfaceAndCurrentPaneBoundaries() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String pane = Files.readString(Path.of("src/main/web/src/workspace/WorkspacePane.ts"));
        String dockPane = Files.readString(Path.of("src/main/web/src/workspace/DockPane.tsx"));
        String bottomPanel = Files.readString(Path.of("src/main/web/src/workspace/BottomPanel.tsx"));
        String execution = Files.readString(Path.of("src/main/java/com/eyecode/ui/web/WebShellExecutionController.java"));
        String lessonPanel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String projectExplorer = Files.readString(Path.of("src/main/web/src/workspace/ProjectExplorer.tsx"));
        String learnWorkspace = Files.readString(Path.of("src/main/web/src/lessons/LearnWorkspace.tsx"));

        assertTrue(pane.contains("'editor' | 'explorer' | 'bottom' | 'lesson'"));
        assertTrue(pane.contains("id: 'editor'"));
        assertTrue(pane.contains("id: 'bottom'"));
        assertTrue(dockPane.contains("data-pane-id={paneId}"));
        assertTrue(dockPane.contains("data-dock-handle"));
        assertFalse(dockPane.contains("onPointer"));
        assertTrue(dockPane.contains("dragHandleOnly"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(workspace.contains("data-editor-region-slot"));
        assertTrue(workspace.contains("persistent-editor-surface"));
        assertFalse(workspace.contains("<DockPane paneId=\"editor\""));
        assertTrue(bottomPanel.contains("<DockPane paneId=\"bottom\""));
        assertTrue(bottomPanel.contains("<TerminalPanel state={terminalState} />"));
        assertTrue(bottomPanel.contains("runState: RunState"));
        assertTrue(bottomPanel.contains("chunk.text"));
        assertFalse(bottomPanel.contains("label: 'Output'"));
        assertFalse(bottomPanel.contains("active === 'output'"));
        assertFalse(bottomPanel.contains("output.join('\\n')"));
        assertTrue(execution.contains("\"text\", text"));
        assertTrue(execution.contains("\"error\", error"));
        assertTrue(execution.contains("payload.put(\"finished\", runService.hasCompletion())"));
        assertTrue(execution.contains("payload.put(\"exitCode\", runService.lastExitCode())"));
        assertTrue(execution.contains("payload.put(\"stopped\", runService.lastStopped())"));
        assertFalse(execution.contains("\"line\", line"));
        String learningPanel = Files.readString(Path.of("src/main/web/src/lessons/LearningPanel.tsx"));
        assertTrue(learningPanel.contains("<DockPane paneId=\"lesson\""));
        assertTrue(lessonPanel.contains("const practiceLesson = session.kind === 'PRACTICE';"));
        assertFalse(lessonPanel.contains("bottom-panel lesson-panel"));
        assertTrue(projectExplorer.contains("<DockPane paneId=\"explorer\""));
        assertTrue(projectExplorer.contains("headerClassName=\"panel-heading\""));
        assertTrue(bottomPanel.contains("headerClassName=\"bottom-tabs\""));
        assertTrue(learningPanel.contains("headerClassName=\"learning-panel-tabs\""));
        assertTrue(learnWorkspace.contains("learn-navigation-page"));
        assertFalse(learnWorkspace.contains("DockPane"));
    }

    @Test
    void staticDockTreesKeepTheEditorAndOverlaysOutsidePaneContent() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String pane = Files.readString(Path.of("src/main/web/src/workspace/WorkspacePane.ts"));
        String layout = Files.readString(Path.of("src/main/web/src/workspace/DockLayout.tsx"));

        assertTrue(pane.contains("export type DockNode = DockPaneNode | DockSplitNode"));
        assertTrue(pane.contains("export const projectDockTree"));
        assertTrue(pane.contains("export const learnPracticeDockTree"));
        assertTrue(pane.contains("export const theoryDockTree"));
        assertEquals(3, occurrences(pane, "paneId: 'explorer'"));
        assertEquals(3, occurrences(pane, "paneId: 'editor'"));
        assertEquals(2, occurrences(pane, "paneId: 'bottom'"));
        assertEquals(2, occurrences(pane, "paneId: 'lesson'"));
        assertTrue(layout.contains("dock-split-${node.orientation}"));
        assertTrue(layout.contains("const draggablePaneIds = new Set(dockPaneIds(tree).filter"));
        assertTrue(layout.contains("const rootBounds = active.element.getBoundingClientRect()"));
        assertFalse(layout.contains("dock-drag-indicator"));
        assertFalse(layout.contains("onDrag"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(workspace.contains("const [projectDockLayout, setProjectDockLayout]"));
        assertTrue(workspace.contains("const [learnPracticeDockLayout, setLearnPracticeDockLayout]"));
        assertTrue(workspace.contains("layoutKind === 'LEARN' ? learnPracticeDockLayout : projectDockLayout"));
        assertTrue(workspace.indexOf("<DockLayout") < workspace.indexOf("<div className=\"overlay-root\">"));
        assertFalse(workspace.contains("<DockPane paneId=\"editor\""));
    }

    @Test
    void dockSplitResizeUsesPointerCaptureMinimumsAndIndependentModeState() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String pane = Files.readString(Path.of("src/main/web/src/workspace/WorkspacePane.ts"));
        String layout = Files.readString(Path.of("src/main/web/src/workspace/DockLayout.tsx"));
        String dockPane = Files.readString(Path.of("src/main/web/src/workspace/DockPane.tsx"));

        assertTrue(pane.contains("editor: { id: 'editor'"));
        assertTrue(pane.contains("minWidth: 320, minHeight: 180"));
        assertTrue(pane.contains("export const explorerCollapsedRailWidth = 64"));
        assertTrue(pane.contains("minWidth: explorerCollapsedRailWidth, minHeight: 0"));
        assertTrue(pane.contains("minWidth: 0, minHeight: 140"));
        assertTrue(pane.contains("lesson: { id: 'lesson'"));
        assertTrue(pane.contains("export function dockNodeMinimum"));
        assertTrue(pane.contains("export function clampDockSplitRatio"));
        assertTrue(pane.contains("export function clampDockSplitPosition"));
        assertTrue(pane.contains("first.minWidth + dockSeparatorSize + second.minWidth"));
        assertTrue(pane.contains("first.minHeight + dockSeparatorSize + second.minHeight"));
        assertTrue(layout.contains("role=\"separator\""));
        assertTrue(layout.contains("aria-orientation={separatorOrientation}"));
        assertTrue(layout.contains("setPointerCapture(event.pointerId)"));
        assertTrue(layout.contains("releasePointerCapture(event.pointerId)"));
        assertTrue(layout.contains("requestAnimationFrame"));
        assertTrue(layout.contains("onPointerDown"));
        assertTrue(layout.contains("onPointerMove"));
        assertTrue(layout.contains("onPointerUp"));
        assertTrue(layout.contains("onPointerCancel"));
        assertTrue(layout.contains("onEditorGeometryChange()"));
        assertTrue(layout.contains("scheduleRatio(active, clampDockSplitPosition"));
        assertTrue(layout.contains("dock-split-grip"));
        assertTrue(pane.contains("export function updateDockSplitRatio"));
        assertTrue(workspace.contains("setLearnPracticeDockLayout(current => updateDockSplitRatio"));
        assertTrue(workspace.contains("setProjectDockLayout(current => updateDockSplitRatio"));
        assertTrue(workspace.contains("onEditorGeometryChange={() => service.layout()}"));
        assertFalse(dockPane.contains("onPointer"));
        assertTrue(layout.contains("const draggablePaneIds = new Set(dockPaneIds(tree).filter"));
        assertFalse(layout.contains("onDrag"));
    }

    @Test
    void learnLessonUsesTheResizableEditorLessonSplitWithoutChangingPracticeWiring() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String pane = Files.readString(Path.of("src/main/web/src/workspace/WorkspacePane.ts"));
        String panel = Files.readString(Path.of("src/main/web/src/lessons/LearningPanel.tsx"));
        String controller = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        String projectTree = pane.substring(pane.indexOf("projectDockTree"), pane.indexOf("learnPracticeDockTree"));
        String learnTree = pane.substring(pane.indexOf("learnPracticeDockTree"), pane.indexOf("theoryDockTree"));
        assertTrue(projectTree.contains("orientation: 'vertical', ratio: 0.7"));
        assertTrue(projectTree.contains("paneId: 'bottom'"));
        assertTrue(learnTree.contains("orientation: 'vertical', ratio: 0.68"));
        assertTrue(learnTree.contains("paneId: 'lesson'"));
        assertTrue(learnTree.contains("paneId: 'bottom'"));
        assertEquals(1, occurrences(learnTree, "paneId: 'editor'"));
        assertEquals(1, occurrences(learnTree, "paneId: 'lesson'"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(panel.contains("LearningPanelTab"));
        assertTrue(panel.contains("label: 'Exercício'"));
        assertTrue(panel.contains("<section className=\"exercise-task\"><h2>Sua tarefa</h2>"));
        assertTrue(panel.contains("onClick={onVerify}"));
        assertTrue(panel.contains("{verification.message}"));
        assertTrue(controller.contains("this.service.setEphemeralReadOnly(\n        document.uri,\n        document.file.readOnly"));
        assertTrue(controller.contains("this.service.setLessonPracticeIntelligence(\n        document.uri,\n        true"));
        assertTrue(styles.contains(".learning-panel-content {"));
        assertFalse(styles.contains(".lesson-panel { grid-column: 2 / 4"));
        assertTrue(workspace.indexOf("<DockLayout") < workspace.indexOf("<div className=\"overlay-root\">"));
    }

    @Test
    void theoryKeepsTheSingleMonacoHostMountedWhileMakingLessonContentPrimary() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String pane = Files.readString(Path.of("src/main/web/src/workspace/WorkspacePane.ts"));
        String layout = Files.readString(Path.of("src/main/web/src/workspace/DockLayout.tsx"));
        String panel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        assertTrue(pane.contains("export const theoryDockTree"));
        assertTrue(pane.contains("ratio: 0"));
        assertTrue(workspace.contains("if (session.kind === 'THEORY') lessonEditor.exit();"));
        assertFalse(workspace.contains("if (session.kind === 'THEORY') lessonEditor.enter(session);"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(layout.contains("layoutKind?: 'PROJECT' | 'LEARN' | 'THEORY'"));
        assertTrue(layout.contains("gridTemplateColumns: '0 0 minmax(0, 1fr)"));
        assertTrue(styles.contains(".dock-leaf.is-theory-hidden"));
        assertTrue(panel.contains("lesson-reading-article"));
        assertTrue(panel.contains("session.kind === 'THEORY'"));
    }

    @Test
    void enteringLearnPreservesTheActiveProjectModelForLessonRestoration() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String controller = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String openLessons = workspace.substring(workspace.indexOf("function openLessons()"), workspace.indexOf("async function leaveProject()"));

        assertFalse(openLessons.contains("clearActiveModel"));
        assertTrue(controller.contains("this.previousUri = this.service.activeModelUri();"));
        assertTrue(controller.contains("this.service.disposeLessonWorkspace(uris);"));
        assertTrue(controller.contains("if (previousUri) this.service.activate(previousUri);"));
    }

    @Test
    void learnNavigationUsesCatalogPagesWithoutChangingProjectExplorerOrMonacoOwnership() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String learnWorkspace = Files.readString(Path.of("src/main/web/src/lessons/LearnWorkspace.tsx"));
        String projectExplorer = Files.readString(Path.of("src/main/web/src/workspace/ProjectExplorer.tsx"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        assertTrue(learnWorkspace.contains("screen: 'HOME'"));
        assertTrue(learnWorkspace.contains("screen: 'ROADMAP'"));
        assertTrue(learnWorkspace.contains("screen: 'TOPIC'"));
        assertTrue(learnWorkspace.contains("screen: 'LESSON'"));
        assertTrue(learnWorkspace.contains("bridge.request<LessonsCatalog>('lessons', 'catalog', {})"));
        assertTrue(learnWorkspace.contains("lesson.executable"));
        assertTrue(learnWorkspace.contains("Teoria"));
        assertTrue(learnWorkspace.contains("Prática"));
        assertTrue(learnWorkspace.contains("Em breve"));
        assertTrue(workspace.contains("const [learnNavigation, setLearnNavigation]"));
        assertTrue(workspace.contains("const learnNavigationVisible = learnMode && learnNavigation.screen !== 'LESSON'"));
        assertTrue(workspace.contains("is-learn-navigation"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(styles.contains(".shell-workspace.is-learn-navigation > .dock-layout"));
        assertTrue(projectExplorer.contains("<DockPane paneId=\"explorer\""));
    }

    @Test
    void learnExplorerUsesTheActiveCatalogTrackAndSharesTheShellPageBase() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String learnWorkspace = Files.readString(Path.of("src/main/web/src/lessons/LearnWorkspace.tsx"));
        String welcome = Files.readString(Path.of("src/main/web/src/workspace/WelcomeScreen.tsx"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        assertTrue(workspace.contains("const [activeLearnTrackId, setActiveLearnTrackId]"));
        assertTrue(workspace.contains("setActiveLearnTrackId(lesson.categoryId);"));
        assertTrue(workspace.contains("<LearnExplorer activeTrackId={activeLearnTrackId}"));
        assertTrue(learnWorkspace.contains("export function topicsForTrack"));
        assertTrue(learnWorkspace.contains("category.id === activeTrackId"));
        assertFalse(learnWorkspace.contains("categories.flatMap(category => category.topics"));
        assertFalse(learnWorkspace.contains("slice(0,"));
        assertTrue(welcome.contains("shell-page-content"));
        assertTrue(learnWorkspace.contains("shell-page-content"));
        assertTrue(styles.contains(".shell-page-content"));
        assertTrue(styles.contains(".learn-navigation-page") && styles.contains("background: transparent"));
    }

    @Test
    void learnUsesTheGlobalToolbarAndBottomBreadcrumbsWithoutAddingAnEditorHeader() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String toolbar = Files.readString(Path.of("src/main/web/src/workspace/TopToolbar.tsx"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        assertFalse(workspace.contains("collapsed-lesson-header"));
        assertFalse(styles.contains("collapsed-lesson-header"));
        assertTrue(workspace.contains("breadcrumbs={learnStatusBreadcrumbs}"));
        assertTrue(workspace.contains("<TopToolbar learnMode={learnMode}"));
        assertTrue(toolbar.contains("learnMode ? 'EyeCode Learn'"));
        assertTrue(toolbar.contains("learnMode && <span className=\"toolbar-mode-indicator\""));
        assertTrue(toolbar.contains("Aprender <span aria-hidden=\"true\">⌄</span>"));
        assertTrue(toolbar.contains("learnMode ? 'Executar' : 'Run'"));
        assertTrue(toolbar.contains("{!learnMode && <select"));
        assertTrue(workspace.contains("onClick={() => setLearnExplorerCollapsed(value => !value)}"));
        assertTrue(workspace.contains("const editorSurfaceKey = `${mode}:${learnNavigation.screen}:${layoutKind}`;"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(styles.contains(".toolbar-mode-indicator"));
        assertTrue(styles.contains(".dock-layout.is-learn-explorer-collapsed"));
        assertTrue(styles.contains("@media (prefers-reduced-motion: reduce)"));
    }

    @Test
    void dockDragUsesStableSlotsAndKeepsTheEditorMounted() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String pane = Files.readString(Path.of("src/main/web/src/workspace/WorkspacePane.ts"));
        String layout = Files.readString(Path.of("src/main/web/src/workspace/DockLayout.tsx"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));
        String learnWorkspace = Files.readString(Path.of("src/main/web/src/lessons/LearnWorkspace.tsx"));

        String projectTree = pane.substring(pane.indexOf("projectDockTree"), pane.indexOf("learnPracticeDockTree"));
        assertTrue(projectTree.contains("orientation: 'vertical', ratio: 0.7"));
        assertTrue(projectTree.contains("orientation: 'horizontal', ratio: 0.28"));
        assertTrue(projectTree.contains("second: { type: 'pane', paneId: 'editor' }"));
        assertTrue(projectTree.contains("second: { type: 'pane', paneId: 'bottom' }"));
        assertTrue(pane.contains("export const learnPracticeDockRules"));
        assertTrue(pane.contains("export function removeDockPane"));
        assertTrue(pane.contains("export function insertDockPaneRelative"));
        assertTrue(pane.contains("export function validateDockTree"));
        assertTrue(pane.contains("export function moveDockPane"));
        assertTrue(pane.contains("export function layoutDockTree"));
        assertTrue(pane.contains("export function dockInsertionRatio"));
        assertTrue(pane.contains("allowedDockSides: ['TOP', 'BOTTOM']"));
        assertTrue(pane.contains("requiredPaneIds"));
        assertTrue(pane.contains("dockablePaneIds"));
        assertTrue(pane.contains("export const dockSeparatorSize = 11"));
        assertTrue(layout.contains("const draggablePaneIds = new Set(dockPaneIds(tree).filter"));
        assertTrue(layout.contains("dockSides.some(side => canDockDrop?.(paneId, targetId, side))"));
        assertTrue(layout.contains("const dockDragThreshold = 5"));
        assertTrue(layout.contains("draggablePaneIds.has(pane)"));
        assertTrue(layout.contains("closest<HTMLElement>('[data-dock-handle]')"));
        assertTrue(layout.contains("closest('button,a,input,select,textarea,[role=\"button\"],[role=\"tab\"],[contenteditable=\"true\"]')"));
        assertTrue(layout.contains("event.preventDefault()"));
        assertTrue(layout.contains("setPointerCapture(event.pointerId)"));
        assertTrue(layout.contains("Math.hypot(event.clientX - active.startX, event.clientY - active.startY) < dockDragThreshold"));
        assertTrue(layout.contains("document.body.classList.add('is-dock-dragging')"));
        assertTrue(layout.contains("document.body.classList.remove('is-dock-dragging')"));
        assertTrue(layout.contains("onPointerUp={finishResize} onPointerCancel={finishResize}"));
        assertTrue(layout.contains("onLostPointerCapture"));
        assertTrue(layout.contains("onLostPointerCapture={cleanupDrag}"));
        assertTrue(learnWorkspace.contains("data-pane-id=\"explorer\""));
        assertTrue(learnWorkspace.contains("data-dock-handle"));
        assertTrue(workspace.contains("data-pane-id=\"editor\""));
        assertTrue(workspace.contains("data-dock-handle>{learnPath.join(' / ')}"));
        assertTrue(layout.contains("const leaf = dockLeafAt(active.element, x, y)"));
        assertTrue(layout.contains("function dockLeafAt"));
        assertTrue(layout.contains("canDockDrop(active.paneId, paneId, side)"));
        assertTrue(layout.contains("function dockInsertionAt"));
        assertTrue(layout.contains("export function dockInsertionSide"));
        assertTrue(layout.contains("Math.abs(pointer - midpoint) <= 10"));
        assertTrue(layout.contains("layoutDockTree(candidate, root)[active.paneId]"));
        assertTrue(layout.contains("dockInsertionRatio(active.source, targetBounds, side)"));
        assertTrue(layout.contains("onDockDrop?.(active.paneId, target.paneId, target.side, target.ratio)"));
        assertTrue(layout.contains("dock-drag-ghost"));
        assertTrue(layout.contains("dock-drop-preview"));
        assertTrue(layout.contains("event.key === 'Escape'"));
        assertTrue(workspace.contains("canDockDrop={dockRules ? canDockDrop : undefined}"));
        assertTrue(workspace.contains("resolveDockPreview={dockRules ? resolveDockPreview : undefined}"));
        assertTrue(workspace.contains("onDockDrop={handleDockDrop}"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(layout.contains("key={`pane-${node.paneId}`}"));
        assertFalse(layout.contains("service.dispose"));
        assertTrue(styles.contains(".dock-drop-preview"));
        assertTrue(styles.contains(".dock-drag-ghost"));
        assertFalse(styles.contains(".dock-drag-indicator"));
        assertTrue(styles.contains(".dock-leaf.is-dragging"));
        assertTrue(styles.contains(".dock-leaf.is-draggable [data-dock-handle] { cursor: grab; user-select: none; -webkit-user-select: none; }"));
        assertTrue(styles.contains(".is-dock-dragging, .is-dock-dragging * { cursor: grabbing !important;"));
        assertTrue(styles.contains("@container explorer (max-width: 92px)"));
    }

    @Test
    void snippetAcceptanceUsesTheBundledSnippetControllerAndKeepsNormalEditsDirect() throws IOException {
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));

        assertFalse(monaco.contains("editor.action.insertSnippet"));
        assertTrue(monaco.contains("editor.getContribution('snippetController2')"));
        assertTrue(monaco.contains("editor.setSelection(range);"));
        assertTrue(monaco.contains("snippetController.insert(item.insertText);"));
        assertTrue(monaco.contains("eyecode.completion.snippet-fallback"));
        assertTrue(monaco.contains("function snippetFallbackText"));
        assertTrue(monaco.contains("editor.executeEdits('eyecode.completion', [{ range, text: item.insertText, forceMoveMarkers: true }]);"));
    }

    @Test
    void learningCardOpeningIsDelayedAndPendingRequestsAreCancelled() throws IOException {
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));

        assertTrue(monaco.contains("const LEARNING_CARD_OPEN_DELAY_MS = 200"));
        assertTrue(monaco.contains("const LEARNING_CARD_CLOSE_DELAY_MS = 10"));
        assertTrue(monaco.contains("private learningOpenTimer: number | null = null"));
        assertTrue(monaco.contains("private scheduleLearningOpen"));
        assertTrue(monaco.contains("window.setTimeout(() =>"));
        assertTrue(monaco.contains("}, LEARNING_CARD_OPEN_DELAY_MS)"));
        assertTrue(monaco.contains("target.key !== this.hoverKey"));
        assertTrue(monaco.contains("private cancelLearningOpen"));
        assertTrue(monaco.contains("this.cancelLearningOpen();"));
        assertTrue(monaco.contains("}, LEARNING_CARD_CLOSE_DELAY_MS)"));
    }

    @Test
    void runFeedbackConsumesStructuredPhaseWithoutParsingOutput() throws IOException {
        String toolbar = Files.readString(Path.of("src/main/web/src/workspace/TopToolbar.tsx"));
        String statusBar = Files.readString(Path.of("src/main/web/src/workspace/StatusBar.tsx"));
        String protocol = Files.readString(Path.of("src/main/web/src/workspace/protocol.ts"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));
        String execution = Files.readString(Path.of("src/main/java/com/eyecode/ui/web/WebShellExecutionController.java"));

        assertTrue(protocol.contains("export type RunPhase = 'IDLE' | 'PREPARING' | 'COMPILING' | 'RUNNING';"));
        assertTrue(protocol.contains("phase: RunPhase;"));
        assertTrue(toolbar.contains("runState.running ? <span className=\"toolbar-run-spinner\""));
        assertTrue(statusBar.contains("runState.phase === 'COMPILING'"));
        assertTrue(statusBar.contains("status-progress-track"));
        assertTrue(styles.contains("@media (prefers-reduced-motion: reduce)"));
        assertTrue(styles.contains(".status-progress-track span"));
        assertTrue(execution.contains("payload.put(\"phase\", runService.phase().name())"));
        assertTrue(execution.contains("onPhase(com.eyecode.runtime.RunPhase phase)"));
        assertFalse(statusBar.contains("output"));
    }

    @Test
    void lessonPracticeRunUsesTheSharedRunTransportAndCurrentMonacoSources() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String lessonEditor = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String toolbar = Files.readString(Path.of("src/main/web/src/workspace/TopToolbar.tsx"));
        String execution = Files.readString(Path.of("src/main/java/com/eyecode/ui/web/WebShellExecutionController.java"));

        assertTrue(workspace.contains("context: 'lesson'"));
        assertTrue(workspace.contains("lessonSession?.phase === 'PRACTICE'"));
        assertTrue(workspace.contains("lessonEditor.executionFiles()"));
        assertTrue(lessonEditor.contains("executionFiles(): { name: string; source: string }[]"));
        assertTrue(toolbar.contains("runAvailable"));
        assertTrue(execution.contains("runService.runLesson(lessonRunRequest(message))"));
    }

    @Test
    void practiceFeedbackIsResetAtLessonAndStepBoundaries() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String taskCard = Files.readString(Path.of("src/main/web/src/lessons/LessonTaskCard.tsx"));

        assertTrue(occurrences(workspace, "setPracticeVerification(null);") >= 4);
        assertTrue(workspace.substring(workspace.indexOf("async function startLesson"), workspace.indexOf("async function changeLessonStep"))
                .contains("setPracticeVerification(null);"));
        assertTrue(workspace.substring(workspace.indexOf("async function changeLessonStep"), workspace.indexOf("async function verifyPractice"))
                .contains("setPracticeVerification(null);"));
        assertFalse(taskCard.contains("useState<PracticeVerificationResult"));
        assertTrue(taskCard.contains("verification && <section className=\"lesson-task-feedback\""));
    }

    private static int occurrences(String text, String target) {
        return text.split(java.util.regex.Pattern.quote(target), -1).length - 1;
    }

    private static String cssRule(String stylesheet, String selector) {
        String marker = selector + " {";
        int start = stylesheet.indexOf(marker);
        assertTrue(start >= 0, "Missing CSS rule: " + selector);
        int bodyStart = stylesheet.indexOf('{', start);
        int end = stylesheet.indexOf('}', bodyStart);
        assertTrue(end >= 0, "Unclosed CSS rule: " + selector);
        return stylesheet.substring(start, end + 1);
    }
}
