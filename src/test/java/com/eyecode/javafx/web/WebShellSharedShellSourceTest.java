package com.eyecode.javafx.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellSharedShellSourceTest {
    @Test
    void workspaceOwnsOnePersistentMonacoHostAcrossProjectAndLearnModes() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String learnWorkspace = Files.readString(Path.of("src/main/web/src/lessons/LearnWorkspace.tsx"));
        String lessonPanel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        assertTrue(workspace.contains("type AppMode = 'WELCOME' | 'PROJECT' | 'LEARN'"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(workspace.contains("<div className={`shell-workspace"));
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
        assertTrue(styles.contains(".lesson-panel-content.learning-body pre code { font-size: 12px; }"));
    }

    @Test
    void practiceUsesTheExistingLessonModelWithoutStartingProfessorCommands() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String controller = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));

        assertTrue(monaco.contains("if ([...this.ephemeralModels.values()].includes(model)) return;"));
        assertTrue(monaco.indexOf("if ([...this.ephemeralModels.values()].includes(model)) return;")
                < monaco.indexOf("bridge.request<{ document: DocumentSnapshot }>('document', 'change'"));
        assertTrue(controller.contains("if (session.phase === 'PRACTICE') this.enterPractice(session.practice!.starterCode);\n    else this.apply(session.commands);"));
        assertFalse(workspace.contains("lessonEditor.enter(session);\n      if (session.phase === 'PRACTICE')"));
        assertTrue(workspace.contains("if (session.phase === 'PRACTICE') lessonEditor.enterPractice(session.practice!.starterCode);\n      else lessonEditor.apply(session.commands);"));
        assertTrue(controller.contains("this.cancelAnimation();\n    this.service.clearEphemeralDecorations(this.activeUri);\n    this.service.setEphemeralModelValue(this.activeUri, starterCode);\n    this.service.setEphemeralReadOnly(this.activeUri, false);\n    this.service.setLessonPracticeIntelligence(this.activeUri, true);\n    this.service.focus();"));
        assertTrue(controller.contains("this.service.setEphemeralReadOnly(uri, true);"));
        assertTrue(monaco.contains("focus(): void { this.editor?.focus(); }"));
    }

    @Test
    void practiceShowsItsInstructionAndEnablesOnlyEphemeralIntelligence() throws IOException {
        String panel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String controller = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String monaco = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));
        String completion = Files.readString(Path.of("src/main/java/com/eyecode/javafx/web/WebShellCompletionController.java"));
        String learning = Files.readString(Path.of("src/main/java/com/eyecode/javafx/web/WebShellLearningController.java"));

        assertTrue(panel.contains("const practice = session.phase === 'PRACTICE' ? session.practice : undefined;"));
        assertTrue(panel.contains("<h2>Sua vez</h2><p>{practice.instruction}</p>"));
        assertTrue(controller.contains("this.service.setLessonPracticeIntelligence(uri, false);"));
        assertTrue(controller.contains("this.service.setLessonPracticeIntelligence(this.activeUri, true);"));
        assertTrue(monaco.contains("private readonly lessonPracticeUris = new Set<string>();"));
        assertTrue(monaco.contains("uri.startsWith('lesson://') && !lessonPractice"));
        assertTrue(monaco.contains("lessonPractice,"));
        assertTrue(monaco.contains("lessonPractice: this.lessonPracticeUris.has(target.uri),"));
        assertTrue(monaco.contains("if ([...this.ephemeralModels.values()].includes(model)) return;"));
        assertTrue(completion.contains("boolean lessonPractice = isLessonPracticeRequest(message.payload(), modelId);"));
        assertTrue(completion.contains("new EditorDocument(session == null ? null : session.getFile(), content)"));
        assertTrue(completion.contains("uri.startsWith(\"lesson://\") && Boolean.TRUE.equals(payload.get(\"lessonPractice\"))"));
        assertTrue(learning.contains("boolean lessonPractice = isLessonPracticeRequest(message.payload(), uri);"));
        assertTrue(learning.contains("new EditorDocument(session == null ? null : session.getFile(), content)"));
        assertTrue(learning.contains("uri.startsWith(\"lesson://\") && Boolean.TRUE.equals(payload.get(\"lessonPractice\"))"));
    }

    @Test
    void practiceVerificationUsesCurrentEphemeralSourceAndAuthoritativeSessionState() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String controller = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String panel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String lessons = Files.readString(Path.of("src/main/java/com/eyecode/javafx/web/WebShellLessonsController.java"));

        assertTrue(controller.contains("practiceSource(): string | null"));
        assertTrue(controller.contains("this.service.ephemeralModelValue(this.activeUri)"));
        assertTrue(workspace.contains("lessonEditor.practiceSource()"));
        assertTrue(workspace.contains("'lessons', 'session/verify'"));
        assertTrue(workspace.contains("sessionId, practiceId, source"));
        assertTrue(workspace.contains("lessonEditor.practiceSource() !== source"));
        assertTrue(workspace.contains("response.session.phase !== 'PRACTICE'"));
        assertTrue(panel.contains("onClick={onVerify}"));
        assertTrue(panel.contains("{verifying ? 'Verificando...' : 'Verificar'}"));
        assertTrue(panel.contains("{verification.message}"));
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
        String lessonPanel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String projectExplorer = Files.readString(Path.of("src/main/web/src/workspace/ProjectExplorer.tsx"));
        String learnWorkspace = Files.readString(Path.of("src/main/web/src/lessons/LearnWorkspace.tsx"));

        assertTrue(pane.contains("'editor' | 'explorer' | 'bottom' | 'lesson'"));
        assertTrue(pane.contains("id: 'editor'"));
        assertTrue(pane.contains("id: 'bottom'"));
        assertTrue(dockPane.contains("data-pane-id={paneId}"));
        assertTrue(dockPane.contains("data-dock-handle"));
        assertFalse(dockPane.contains("onPointer"));
        assertFalse(dockPane.contains("drag"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(workspace.contains("<section className=\"editor-region\">\n"));
        assertFalse(workspace.contains("<DockPane paneId=\"editor\""));
        assertTrue(bottomPanel.contains("<DockPane paneId=\"bottom\""));
        assertTrue(bottomPanel.contains("<TerminalPanel state={terminalState} />"));
        assertTrue(lessonPanel.contains("<DockPane paneId=\"lesson\""));
        assertTrue(lessonPanel.contains("className={`lesson-panel${session.kind === 'THEORY' ? ' is-theory' : ''}`"));
        assertFalse(lessonPanel.contains("bottom-panel lesson-panel"));
        assertTrue(projectExplorer.contains("<DockPane paneId=\"explorer\""));
        assertTrue(projectExplorer.contains("headerClassName=\"panel-heading\""));
        assertTrue(bottomPanel.contains("headerClassName=\"bottom-tabs\""));
        assertTrue(lessonPanel.contains("headerClassName=\"bottom-tabs lesson-pane-header\""));
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
        assertTrue(pane.contains("export const learnDockTree"));
        assertTrue(pane.contains("export const theoryDockTree"));
        assertEquals(4, occurrences(pane, "paneId: 'explorer'"));
        assertEquals(4, occurrences(pane, "paneId: 'editor'"));
        assertEquals(1, occurrences(pane, "paneId: 'bottom'"));
        assertEquals(3, occurrences(pane, "paneId: 'lesson'"));
        assertTrue(layout.contains("dock-split-${node.orientation}"));
        assertTrue(layout.contains("const draggablePaneIds = new Set(dockPaneIds(tree).filter"));
        assertFalse(layout.contains("onDrag"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(workspace.contains("const [learnDockArrangement, setLearnDockArrangement]"));
        assertTrue(workspace.contains("learnDockArrangement === 'LESSON_LEFT' ? learnLessonLeftDockTree : learnDockTree"));
        assertTrue(workspace.indexOf("<DockLayout") < workspace.indexOf("<div className=\"overlay-root\">"));
        assertFalse(workspace.contains("<DockPane paneId=\"editor\""));
    }

    @Test
    void dockSplitResizeUsesPointerCaptureMinimumsAndIndependentModeState() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String pane = Files.readString(Path.of("src/main/web/src/workspace/WorkspacePane.ts"));
        String layout = Files.readString(Path.of("src/main/web/src/workspace/DockLayout.tsx"));
        String dockPane = Files.readString(Path.of("src/main/web/src/workspace/DockPane.tsx"));

        assertTrue(pane.contains("editor: { minWidth: 320, minHeight: 180 }"));
        assertTrue(pane.contains("export const explorerCollapsedRailWidth = 64"));
        assertTrue(pane.contains("explorer: { minWidth: explorerCollapsedRailWidth, minHeight: 0 }"));
        assertTrue(pane.contains("bottom: { minWidth: 0, minHeight: 140 }"));
        assertTrue(pane.contains("lesson: { minWidth: 0, minHeight: 160 }"));
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
        assertTrue(layout.contains("const clampedPosition = clampDockSplitPosition"));
        assertTrue(layout.contains("dock-split-grip"));
        assertTrue(workspace.contains("Record<DockMode, Record<string, number>>"));
        assertTrue(workspace.contains("PROJECT: {}, LEARN: {}"));
        assertTrue(workspace.contains("[dockMode]: { ...current[dockMode], [splitId]: ratio }"));
        assertTrue(workspace.contains("onEditorGeometryChange={() => service.layout()}"));
        assertFalse(dockPane.contains("onPointer"));
        assertTrue(layout.contains("const draggablePaneIds = new Set(dockPaneIds(tree).filter"));
        assertFalse(layout.contains("onDrag"));
    }

    @Test
    void learnLessonUsesTheResizableEditorLessonSplitWithoutChangingPracticeWiring() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String pane = Files.readString(Path.of("src/main/web/src/workspace/WorkspacePane.ts"));
        String panel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String controller = Files.readString(Path.of("src/main/web/src/lessons/LessonEditorController.ts"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        String projectTree = pane.substring(pane.indexOf("projectDockTree"), pane.indexOf("learnDockTree"));
        String learnTree = pane.substring(pane.indexOf("learnDockTree"), pane.indexOf("learnLessonLeftDockTree"));
        assertTrue(projectTree.contains("orientation: 'vertical', ratio: 0.7"));
        assertTrue(projectTree.contains("paneId: 'bottom'"));
        assertTrue(learnTree.contains("orientation: 'horizontal', ratio: 0.55"));
        assertTrue(learnTree.contains("paneId: 'lesson'"));
        assertEquals(1, occurrences(learnTree, "paneId: 'editor'"));
        assertEquals(1, occurrences(learnTree, "paneId: 'lesson'"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(panel.contains("className={`lesson-panel${session.kind === 'THEORY' ? ' is-theory' : ''}`"));
        assertTrue(panel.contains("bodyClassName=\"lesson-pane-content lesson-panel-content learning-body\""));
        assertTrue(panel.contains("{session.contentBlocks.map"));
        assertTrue(panel.contains("{practice && <section className=\"lesson-practice\""));
        assertTrue(panel.contains("onClick={onVerify}"));
        assertTrue(panel.contains("{verification.message}"));
        assertTrue(controller.contains("this.service.setEphemeralReadOnly(this.activeUri, false);"));
        assertTrue(controller.contains("this.service.setLessonPracticeIntelligence(this.activeUri, true);"));
        assertTrue(styles.contains(".lesson-pane-content { min-height: 0; overflow: auto;"));
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
    void dockDragUsesStableSlotsAndKeepsTheEditorMounted() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String pane = Files.readString(Path.of("src/main/web/src/workspace/WorkspacePane.ts"));
        String layout = Files.readString(Path.of("src/main/web/src/workspace/DockLayout.tsx"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        String projectTree = pane.substring(pane.indexOf("projectDockTree"), pane.indexOf("learnDockTree"));
        assertTrue(projectTree.contains("orientation: 'vertical', ratio: 0.7"));
        assertTrue(projectTree.contains("orientation: 'horizontal', ratio: 0.28"));
        assertTrue(projectTree.contains("second: { type: 'pane', paneId: 'editor' }"));
        assertTrue(projectTree.contains("second: { type: 'pane', paneId: 'bottom' }"));
        assertTrue(pane.contains("export const learnLessonLeftDockTree"));
        assertTrue(pane.contains("export function learnDockArrangementForDrop"));
        assertFalse(pane.contains("export function moveDockPane"));
        assertTrue(pane.contains("export const dockSeparatorSize = 11"));
        assertTrue(layout.contains("const draggablePaneIds = new Set(dockPaneIds(tree).filter"));
        assertTrue(layout.contains("dockSides.some(side => canDockDrop?.(paneId, targetId, side))"));
        assertTrue(layout.contains("const dockDragThreshold = 5"));
        assertTrue(layout.contains("const dockInputDebug = true"));
        assertTrue(layout.contains("draggablePaneIds.has(pane)"));
        assertTrue(layout.contains("closest<HTMLElement>('[data-dock-handle]')"));
        assertTrue(layout.contains("closest('button,a,input,select,textarea,[role=\"button\"],[role=\"tab\"],[contenteditable=\"true\"]')"));
        assertTrue(layout.contains("event.preventDefault()"));
        assertTrue(layout.contains("setPointerCapture(event.pointerId)"));
        assertTrue(layout.contains("Math.hypot(event.clientX - active.startX, event.clientY - active.startY) < dockDragThreshold"));
        assertTrue(layout.contains("document.body.classList.add('is-dock-dragging')"));
        assertTrue(layout.contains("document.body.classList.remove('is-dock-dragging')"));
        assertTrue(layout.contains("logDockInput('header:pointerdown'"));
        assertTrue(layout.contains("logDockInput('separator:pointerdown'"));
        assertTrue(layout.contains("logDockInput(`separator:${event.type}`"));
        assertTrue(layout.contains("onPointerUp={finishResize} onPointerCancel={finishResize}"));
        assertTrue(layout.contains("onLostPointerCapture"));
        assertTrue(layout.contains("cleanupDrag('lostpointercapture')"));
        assertTrue(layout.contains("cleanupDrag('layout-change')"));
        assertTrue(layout.contains("document.elementFromPoint"));
        assertTrue(layout.contains("canDockDrop?.(active.paneId, targetId, side)"));
        assertTrue(layout.contains("onDockDrop?.(active.paneId, target.paneId, target.side)"));
        assertTrue(layout.contains("event.key === 'Escape'"));
        assertTrue(layout.contains("dock-preview"));
        assertTrue(workspace.contains("canDockDrop={layoutKind === 'LEARN' ? canDockDrop : undefined}"));
        assertTrue(workspace.contains("onDockDrop={handleDockDrop}"));
        assertEquals(1, occurrences(workspace, "<MonacoHost"));
        assertTrue(layout.contains("key={`pane-${node.paneId}`}"));
        assertFalse(layout.contains("service.dispose"));
        assertTrue(styles.contains(".dock-preview"));
        assertTrue(styles.contains(".dock-drag-indicator"));
        assertTrue(styles.contains(".dock-leaf.is-dragging"));
        assertTrue(styles.contains(".dock-leaf.is-draggable [data-dock-handle] { cursor: grab; user-select: none; -webkit-user-select: none; }"));
        assertTrue(styles.contains(".is-dock-dragging, .is-dock-dragging * { user-select: none; -webkit-user-select: none; }"));
        assertTrue(styles.contains("@container explorer (max-width: 92px)"));
    }

    private static int occurrences(String text, String target) {
        return text.split(java.util.regex.Pattern.quote(target), -1).length - 1;
    }
}
