package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeModuleSourceTest {
    @Test
    void challengeUiSourcesExistAndArePopulated() throws IOException {
        String[] sources = {
                "src/main/web/src/challenge/ChallengeRightPanel.tsx",
                "src/main/web/src/challenge/ChallengeMarkdown.tsx",
                "src/main/web/src/challenge/ChallengeLobby.tsx",
                "src/main/web/src/challenge/ChallengeArena.tsx",
                "src/main/web/src/challenge/useChallenge.tsx",
                "src/main/web/src/challenge/catalog.ts"
        };
        for (String source : sources) {
            Path path = Path.of(source);
            assertTrue(Files.exists(path), () -> "missing " + source);
            assertFalse(Files.readString(path).isBlank(), () -> "blank " + source);
        }
        assertFalse(Files.exists(Path.of("src/main/web/src/challenge/ChallengeExplorer.tsx")),
                "the simplified challenge explorer must stay discarded");
        assertFalse(Files.exists(Path.of("src/main/web/src/challenge/ChallengePanelContainer.tsx")),
                "the challenge container must stay replaced by the fixed right panel");
        assertFalse(Files.exists(Path.of("src/main/web/src/challenge/ChallengeStatementPane.tsx")),
                "statement must live inside the fixed right panel, not as a loose dock pane");
        assertFalse(Files.exists(Path.of("src/main/web/src/challenge/ChallengeTestsPane.tsx")),
                "tests must live inside the fixed right panel, not as a loose dock pane");
    }

    @Test
    void appRouterOwnsTheChallengeLobbyAndArenaRoutes() throws IOException {
        String app = Files.readString(Path.of("src/main/web/src/App.tsx"));

        assertTrue(app.contains("ChallengeLobby"));
        assertTrue(app.contains("ChallengeArena"));
        assertTrue(app.contains("challenge-lobby"));
        assertTrue(app.contains("challenge-arena"));
        assertTrue(app.contains("onOpenChallenges"));
    }

    @Test
    void arenaDelegatesRenderingToTheStandardWorkspace() throws IOException {
        String arena = Files.readString(Path.of("src/main/web/src/challenge/ChallengeArena.tsx"));

        assertTrue(arena.contains("<Workspace key={challengeId} challenge={context} />"),
                "ChallengeArena must delegate the layout to the standard Workspace with a fresh instance per challenge");
        assertTrue(arena.contains("mainFilePath"),
                "the arena must carry the main file path for auto-open");
        assertTrue(arena.contains("challenge-spinner"),
                "the arena loading gate must render an isolated spinner");
        assertFalse(arena.contains("ChallengeExplorer"));
        assertFalse(arena.contains("BottomPanel"));
    }

    @Test
    void challengePanelLivesInTheLessonDockSlotOnlyInChallengeMode() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String pane = Files.readString(Path.of("src/main/web/src/workspace/WorkspacePane.ts"));
        String panel = Files.readString(Path.of("src/main/web/src/challenge/ChallengeRightPanel.tsx"));
        String welcome = Files.readString(Path.of("src/main/web/src/workspace/WelcomeScreen.tsx"));

        assertTrue(workspace.contains("if (paneId === 'lesson')"),
                "the challenge panel must plug into the lesson slot reused from Learn mode");
        assertTrue(workspace.contains("challenge && layoutKind === 'CHALLENGE'"),
                "challenge content must only render when the challenge layout is explicitly active");
        assertTrue(workspace.contains("challengePanelCollapsed"));
        assertTrue(workspace.contains("is-challenge-panel-collapsed"),
                "collapse must drive the dock layout class, not global CSS columns");
        assertTrue(workspace.contains("ChallengeRightPanel"));
        assertTrue(workspace.contains("ChallengeProvider challengeId={challenge.id}"));
        assertTrue(workspace.contains("learnMode ? 'LEARN' : challenge ? 'CHALLENGE'"),
                "Learn mode must take precedence over the challenge layout");
        assertTrue(workspace.contains("if (challenge && (!workspace.project || mode === 'WELCOME'))"),
                "the challenge gate must hold the shell until the project loads and the mode leaves WELCOME");
        assertTrue(workspace.contains("challenge.onExit() : void leaveProject()"),
                "the toolbar welcome action must exit the arena instead of flashing the Welcome mode");
        assertTrue(workspace.contains("void openFile(challenge.mainFilePath)"),
                "the Workspace must auto-open the challenge main file");
        assertFalse(workspace.contains("' is-challenge'"),
                "the global shell grid must stay untouched by challenge styling");
        assertFalse(workspace.contains("ChallengeStatementPane"));
        assertFalse(workspace.contains("ChallengeTestsPane"));
        assertFalse(pane.contains("challenge-statement"));
        assertFalse(pane.contains("challenge-tests"));
        assertTrue(pane.contains("challengeDockRules"));
        assertTrue(pane.contains("challengeDockTree"));
        assertTrue(panel.contains("challenge-collapse-button"),
                "the panel must offer a collapse action anchored in the tab bar");
        assertTrue(panel.contains("challenge-tabs"),
                "the panel must own the statement/tests tabs");
        assertTrue(panel.contains("challenge-statement-meta"),
                "the title and action buttons must scroll with the statement content");
        assertTrue(welcome.contains("onChallenges"));
    }

    @Test
    void editorTabsSupportSplitViewGroups() throws IOException {
        String workspace = Files.readString(Path.of("src/main/web/src/workspace/Workspace.tsx"));
        String tabs = Files.readString(Path.of("src/main/web/src/workspace/EditorTabs.tsx"));
        String splitGroup = Files.readString(Path.of("src/main/web/src/workspace/EditorSplitGroup.tsx"));
        String service = Files.readString(Path.of("src/main/web/src/monaco/MonacoWorkspaceService.ts"));

        assertTrue(tabs.contains("onSplitRight"));
        assertTrue(tabs.contains("onSplitDown"));
        assertTrue(tabs.contains("onContextMenu"));
        assertTrue(tabs.contains("Split Right"));
        assertTrue(tabs.contains("Split Down"));
        assertTrue(tabs.contains("editor-context-menu"));
        assertTrue(workspace.contains("splitEditorTab"));
        assertTrue(workspace.contains("editor-split-root"));
        assertTrue(workspace.contains("EditorSplitGroup"));
        assertTrue(splitGroup.contains("EditorSplitMirror"));
        assertTrue(service.contains("createSplitEditor"),
                "the Monaco service must expose read-only split editor mirrors");
    }

    @Test
    void challengePersistenceIsBackedByTheChallengeWorkspaceContract() throws IOException {
        String hook = Files.readString(Path.of("src/main/web/src/challenge/useChallenge.tsx"));
        String controller = Files.readString(Path.of("src/main/java/com/eyecode/ui/web/WebShellChallengesController.java"));
        String projectService = Files.readString(Path.of("src/main/java/com/eyecode/project/ProjectService.java"));

        assertTrue(hook.contains("'challenges', 'state'"));
        assertTrue(hook.contains("'challenges', 'reset'"));
        assertTrue(hook.contains("window.location.reload()"));
        assertTrue(controller.contains("mainFilePath"));
        assertTrue(controller.contains("register(\"ensure\", this::ensure)"));
        assertTrue(controller.contains("register(\"state\", this::state)"));
        assertTrue(controller.contains("register(\"reset\", this::reset)"));
        assertTrue(projectService.contains("isChallengeWorkspace"),
                "recent projects must filter challenge workspaces out");
    }

    @Test
    void challengeStylesArePresent() throws IOException {
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        assertTrue(styles.contains(".challenge-panel {"));
        assertTrue(styles.contains(".dock-layout.is-challenge-panel-collapsed"),
                "collapse must be driven by the dock layout manager pattern");
        assertTrue(styles.contains(".challenge-rail"));
        assertTrue(styles.contains(".editor-split-root"));
        assertTrue(styles.contains(".editor-split-group"));
        assertTrue(styles.contains(".editor-context-menu"));
        assertTrue(styles.contains(".challenge-lobby"));
        assertTrue(styles.contains(".challenge-arena-gate"));
        assertTrue(styles.contains(".challenge-spinner"));
        assertTrue(styles.contains(".challenge-statement-meta"),
                "statement meta must live inside the scrollable statement flow");
        assertFalse(styles.contains(".challenge-header {"),
                "the fixed statement header must not exist; the header scrolls away with the content");
        assertFalse(styles.contains(".shell-workspace.is-challenge"),
                "the global shell grid must not carry challenge columns");
        assertFalse(styles.contains(".challenge-statement-pane"),
                "the loose dock pane styles must not linger");
        assertFalse(styles.contains(".challenge-tests-pane"));
        assertFalse(styles.contains(".challenge-pane-heading"));
        assertFalse(styles.contains(".challenge-explorer"));
    }
}
