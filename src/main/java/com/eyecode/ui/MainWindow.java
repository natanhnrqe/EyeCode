package com.eyecode.ui;

import com.eyecode.command.CommandContext;
import com.eyecode.autosave.AutoSaveManager;
import com.eyecode.editor.Document;
import com.eyecode.editor.EditorContext;
import com.eyecode.editor.v2.integration.EditorHostPanel;
import com.eyecode.editor.v2.integration.EditorSession;
import com.eyecode.editor.v2.ui.RichEditorView;
import com.eyecode.eventbus.EventBus;
import com.eyecode.explorer.integration.ExplorerIntegrationController;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.filesystem.FileManager;
import com.eyecode.filesystem.FileSystemService;
import com.eyecode.project.ProjectDetector;
import com.eyecode.project.ProjectInfo;
import com.eyecode.project.ProjectService;
import com.eyecode.project.ProjectType;
import com.eyecode.project.template.ProjectTemplateService;
import com.eyecode.run.RunManager;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import com.eyecode.ui.designsystem.UIConstants;
import com.eyecode.ui.editor.EditorPanel;
import com.eyecode.ui.editor.ToolWindowBar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class MainWindow extends JFrame {

    private static final String WELCOME_VIEW = "WELCOME";
    private static final String EDITOR_VIEW = "EDITOR";

    private final EditorHostPanel tabbedPane;
    private final FileManager fileManager;
    private final FileSystemService fileSystemService;
    private final AutoSaveManager autoSaveManager;
    private final RunManager runManager;
    private final FileExplorerPanel explorerPanel;
    private final BottomToolWindowPanel bottomTool;
    private final TopBarPanel topBar;
    private final StatusBar statusBar;
    private final ToolWindowBar toolWindowBar;
    private final JPanel editorStack;
    private final CardLayout editorCards;
    private final WelcomePanel welcomePanel;

    private final ProjectService projectService;
    private final ProjectTemplateService templateService;
    private final EventBus eventBus;
    private final EditorContext editorContext;
    private final CommandContext commandContext;
    private final ExplorerIntegrationController explorerController;
    private JSplitPane rootSplit;
    private int lastBottomHeight = 300;
    private boolean bottomVisible = true;
    private Point dragStart;
    private final List<File> recentFiles = new ArrayList<>();
    private JMenu recentFilesMenu;
    private volatile boolean processRunning;

    public MainWindow() {

        setTitle("EyeCode");
        setUndecorated(true);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(ColorManager.WINDOW_BG);

        fileManager = new FileManager();
        fileSystemService = new DefaultFileSystemService();
        autoSaveManager = new AutoSaveManager(fileSystemService);
        runManager = new RunManager();
        projectService = new ProjectService();
        templateService = new ProjectTemplateService();
        eventBus = new EventBus();
        editorContext = new EditorContext();
        commandContext = new CommandContext(eventBus, editorContext, null);
        explorerController = new ExplorerIntegrationController(eventBus, editorContext, commandContext);

        UIManager.put("TabbedPane.cardTabArc", 14);
        tabbedPane = new EditorHostPanel(fileSystemService, autoSaveManager);
        explorerPanel = new FileExplorerPanel(new File("."), fileSystemService);
        bottomTool = new BottomToolWindowPanel();
        topBar = new TopBarPanel();
        statusBar = new StatusBar();
        toolWindowBar = new ToolWindowBar();

        editorCards = new CardLayout();
        editorStack = new JPanel(editorCards);
        editorStack.setOpaque(false);
        welcomePanel = new WelcomePanel(this::openProject, this::newProject, this::openRecentProject);
        editorStack.add(welcomePanel, WELCOME_VIEW);
        editorStack.add(tabbedPane, EDITOR_VIEW);

        configureActions();
        configureLayout();
        configureTabs();
        configureExplorer();
        configureWindowChrome();
        configureLifecycle();

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), "quickFileSearch");
        getRootPane().getActionMap().put("quickFileSearch", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                showQuickFileSearch();
            }
        });

        updateWelcomeRecent();
        showWelcomeIfNeeded();

        setMaximizedBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(UIConstants.WINDOW_MIN_WIDTH, UIConstants.WINDOW_MIN_HEIGHT));
        setVisible(true);
    }

    private void configureActions() {
        topBar.setOnRun(this::runCode);
        topBar.setOnStop(this::stopCode);
        topBar.setOnDebug(this::debugCode);
        topBar.setOnSave(this::saveFile);
        topBar.setOnOpenProject(this::openProject);
        topBar.setOnSearch(this::showSearchDialog);
        topBar.setOnNewFile(this::newFileOrType);
        topBar.setOnSettings(() -> {
            Font currentFont = TypographyManager.UI_TITLE();
            RichEditorView view = tabbedPane.getActiveView();
            if (view != null) {
                currentFont = view.getTextPane().getFont();
            }
            new SettingsDialog(MainWindow.this, currentFont, newFont -> {
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    Component c = tabbedPane.getComponentAt(i);
                    if (c instanceof RichEditorView rev) {
                        rev.getTextPane().setFont(newFont);
                    }
                }
            });
        });
        topBar.setOnMinimize(() -> setState(Frame.ICONIFIED));
        topBar.setOnMaximize(this::toggleMaximize);
        topBar.setOnClose(() -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

        JPopupMenu hamburger = buildHamburgerMenu();
        topBar.setHamburgerPopup(hamburger);

        toolWindowBar.setActionListener(action -> {
            switch (action) {
                case "PROJECT" -> explorerPanel.setVisible(!explorerPanel.isVisible());
                case "TERMINAL" -> toggleBottomTool(true);
                case "RUN" -> toggleBottomTool(false);
            }

            revalidate();
            repaint();
        });
    }

    private JPopupMenu buildHamburgerMenu() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(ColorManager.BORDER));

        JMenu fileMenu = new JMenu("File");
        addItem(fileMenu, "New...", e -> newFileOrType());
        addItem(fileMenu, "Save", e -> saveFile());
        fileMenu.addSeparator();
        addItem(fileMenu, "Open Project", e -> openProject());
        fileMenu.addSeparator();
            recentFilesMenu = new JMenu("Recent Files");
            updateRecentMenu(recentFilesMenu);
            fileMenu.add(recentFilesMenu);
        fileMenu.addSeparator();
        addItem(fileMenu, "Exit", e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        popup.add(fileMenu);

        JMenu runMenu = new JMenu("Run");
        addItem(runMenu, "Run Project", e -> runCode());
        addItem(runMenu, "Debug", e -> debugCode());
        runMenu.addSeparator();
        addItem(runMenu, "Stop", e -> stopCode());
        JMenuItem buildItem = new JMenuItem("Build");
        buildItem.setEnabled(false);
        runMenu.add(buildItem);
        popup.add(runMenu);

        JMenu gitMenu = new JMenu("Git");
        JMenuItem commitItem = new JMenuItem("Commit");
        commitItem.setEnabled(false);
        gitMenu.add(commitItem);
        JMenuItem pushItem = new JMenuItem("Push");
        pushItem.setEnabled(false);
        gitMenu.add(pushItem);
        JMenuItem pullItem = new JMenuItem("Pull");
        pullItem.setEnabled(false);
        gitMenu.add(pullItem);
        popup.add(gitMenu);

        JMenu toolsMenu = new JMenu("Tools");
        addItem(toolsMenu, "Terminal", e -> { showBottomPanel(); bottomTool.showTerminal(); });
        addItem(toolsMenu, "Settings", e -> {
            topBar.getOnSettings().run();
        });
        JMenuItem extItem = new JMenuItem("Extensions");
        extItem.setEnabled(false);
        toolsMenu.add(extItem);
        popup.add(toolsMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem docItem = new JMenuItem("Documentation");
        docItem.setEnabled(false);
        helpMenu.add(docItem);
        JMenuItem aboutItem = new JMenuItem("About EyeCode");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "EyeCode - A modern Java IDE\nVersion 1.0",
                "About EyeCode", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        popup.add(helpMenu);

        return popup;
    }

    private void addItem(JMenu menu, String text, java.awt.event.ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(action);
        menu.add(item);
    }

    private void configureWindowChrome() {
        getRootPane().setBorder(BorderFactory.createLineBorder(ColorManager.BORDER));
        topBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    toggleMaximize();
                }
            }
        });

        topBar.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart == null || (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                    return;
                }

                Point screenPoint = e.getLocationOnScreen();
                setLocation(screenPoint.x - dragStart.x, screenPoint.y - dragStart.y);
            }
        });
    }

    private void configureLifecycle() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                autoSaveManager.saveAll();
                autoSaveManager.shutdown();
            }
        });
    }

    private void toggleMaximize() {
        int state = getExtendedState();

        if ((state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            setExtendedState(Frame.NORMAL);
            return;
        }

        setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    private void configureLayout() {
        JPanel leftArea = new JPanel(new BorderLayout());
        leftArea.setOpaque(false);
        leftArea.add(explorerPanel, BorderLayout.CENTER);

        RoundedPanel editorArea = new RoundedPanel(
                new BorderLayout(),
                ColorManager.EDITOR_BG,
                ColorManager.BORDER_EDITOR,
                UIConstants.BORDER_RADIUS_PANEL
        );
        editorArea.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XS, SpacingSystem.XS, SpacingSystem.XS, SpacingSystem.XS));
        editorArea.add(editorStack, BorderLayout.CENTER);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftArea, editorArea);
        rootSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerSplit, bottomTool);

        centerSplit.setResizeWeight(UIConstants.SPLIT_EXPLORER_WEIGHT);
        rootSplit.setResizeWeight(UIConstants.SPLIT_VERTICAL_WEIGHT);
        centerSplit.setDividerSize(UIConstants.SPLIT_DIVIDER_SIZE);
        rootSplit.setDividerSize(UIConstants.SPLIT_DIVIDER_SIZE);
        centerSplit.setBorder(null);
        rootSplit.setBorder(null);
        centerSplit.setOpaque(false);
        rootSplit.setOpaque(false);
        centerSplit.setBackground(ColorManager.WINDOW_BG);
        rootSplit.setBackground(ColorManager.WINDOW_BG);

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setOpaque(false);
        workspace.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.MD, 0, 6, 0));
        workspace.add(toolWindowBar, BorderLayout.WEST);
        workspace.add(rootSplit, BorderLayout.CENTER);

        add(statusBar, BorderLayout.SOUTH);
        add(workspace, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);
    }

    private void configureTabs() {
        tabbedPane.putClientProperty("JTabbedPane.tabType", "card");
        tabbedPane.putClientProperty("JTabbedPane.tabHeight", UIConstants.TAB_HEIGHT);
        tabbedPane.putClientProperty("JTabbedPane.showTabsSeparators", true);
        tabbedPane.putClientProperty("JTabbedPane.tabInsets", new Insets(SpacingSystem.MD, SpacingSystem.XXL, SpacingSystem.MD, SpacingSystem.XXL));
        tabbedPane.putClientProperty("JTabbedPane.tabClosable", true);
        tabbedPane.putClientProperty("JTabbedPane.tabCloseCallback", (IntConsumer) index -> {
            tabbedPane.setSelectedIndex(index);
            closeCurrentTab();
        });
        tabbedPane.setBackground(ColorManager.EDITOR_BG);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XXS, SpacingSystem.XXS, SpacingSystem.XXS, SpacingSystem.XXS));

        tabbedPane.addChangeListener(e -> {
            updateBreadcrump();
            updateSelectedFileUi();
            File file = tabbedPane.getActiveFile();
            if (file != null) {
                explorerPanel.selectFile(file);
            }
        });
    }

    private void configureExplorer() {
        explorerPanel.setFileOpenCallBack(file -> {
            openFile(file);
        });
    }

    private void toggleBottomTool(boolean terminal) {
        boolean selected = terminal ? bottomTool.isTerminalSelected() : bottomTool.isRunSelected();

        if (bottomVisible && selected) {
            hideBottomPanel();
            return;
        }

        showBottomPanel();

        if (terminal) {
            bottomTool.showTerminal();
        } else {
            bottomTool.showRun();
        }
    }

    private void updateRecentMenu(JMenu recentMenu) {
        recentMenu.removeAll();
        if (recentFiles.isEmpty()) {
            JMenuItem empty = new JMenuItem("(No recent files)");
            empty.setEnabled(false);
            recentMenu.add(empty);
            return;
        }
        for (File f : recentFiles) {
            JMenuItem item = new JMenuItem(f.getName());
            item.setToolTipText(f.getAbsolutePath());
            item.addActionListener(e -> openFile(f));
            recentMenu.add(item);
        }
    }

    private void addRecentFile(File file) {
        if (file == null) return;
        recentFiles.remove(file);
        recentFiles.add(0, file);
        if (recentFiles.size() > 10) {
            recentFiles.remove(recentFiles.size() - 1);
        }
        if (recentFilesMenu != null) {
            updateRecentMenu(recentFilesMenu);
        }
    }

    private void runCode() {
        File activeFile = tabbedPane.getActiveFile();
        if (activeFile == null) {
            saveFileAs();
            activeFile = tabbedPane.getActiveFile();
        }
        autoSaveManager.saveAll();

        bottomTool.showRun();
        bottomTool.clearRunOutput();
        bottomTool.setRunStatus("Running");
        bottomTool.printRunOutput("Running...");
        topBar.setProjectRunning(true);
        statusBar.setRunning(true);
        showBottomPanel();
        processRunning = true;

        File projectRoot = explorerPanel.getCurrentRoot();

        new Thread(() -> {
            String output = runManager.runProject(projectRoot);

            SwingUtilities.invokeLater(() -> {
                boolean wasStopped = !processRunning;
                processRunning = false;
                bottomTool.printRunOutput(output);
                if (wasStopped) {
                    bottomTool.setRunStatus("Stopped");
                    bottomTool.printRunOutput("\n--- Process terminated ---");
                } else {
                    bottomTool.setRunStatus("Finished");
                }
                topBar.setProjectRunning(false);
                statusBar.setRunning(false);
                statusBar.updateStatus(wasStopped ? "Stopped" : "Run finished");
            });
        }).start();
    }

    private void stopCode() {
        if (!processRunning) return;
        processRunning = false;
        runManager.stop();
    }

    private void debugCode() {
        JOptionPane.showMessageDialog(this,
                "Debug mode not yet implemented.",
                "Debug", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveFile() {
        File file = tabbedPane.getActiveFile();

        if (file == null) {
            saveFileAs();
            return;
        }

        tabbedPane.saveActive(file);
        bottomTool.printRunOutput("File Saved: " + file.getName());
        statusBar.updateStatus("Saved " + file.getName());
        updateTabTitle();
    }

    private void saveFileAs() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            tabbedPane.saveActive(file);
            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), file.getName());
            tabbedPane.setIconAt(tabbedPane.getSelectedIndex(), IconManager.forFile(file.getName()));

            bottomTool.printRunOutput("File Saved As " + file.getName());
            statusBar.updateStatus("Saved " + file.getName());
            updateTabTitle();
            updateSelectedFileUi();
        }
    }

    private void newFile() {
        tabbedPane.newUntitled();
        int index = tabbedPane.getSelectedIndex();
        tabbedPane.setIconAt(index, IconManager.forFile("Untitled"));
        editorCards.show(editorStack, EDITOR_VIEW);
        bottomTool.printRunOutput("New File Created");
        statusBar.updateStatus("New file created");
        updateBreadcrump();
        updateSelectedFileUi();
    }

    private void newFileOrType() {
        File root = explorerPanel.getCurrentRoot();
        if (root == null) {
            newFile();
            return;
        }
        NewFileDialog dialog = new NewFileDialog(this, root, fileSystemService);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            refreshExplorer();
            bottomTool.printRunOutput("Created: " + dialog.getResultName());
            statusBar.updateStatus("Created " + dialog.getResultName());
        }
    }

    private void updateTabTitle() {
        int index = tabbedPane.getSelectedIndex();
        if (index == -1) return;

        File file = tabbedPane.getActiveFile();
        String name = file != null ? file.getName() : "Untitled";
        String title = tabbedPane.isDirty() ? name + " *" : name;
        tabbedPane.setTitleAt(index, title);
        if (file != null) {
            tabbedPane.setIconAt(index, IconManager.forFile(file.getName()));
        }
        updateSelectedFileUi();
    }

    private void closeCurrentTab() {
        EditorSession activeSession = tabbedPane.getActiveSession();
        if (activeSession != null) {
            autoSaveManager.saveNow(activeSession.getBuffer().getDocument());
        }

        tabbedPane.closeActive();
        showWelcomeIfNeeded();
        statusBar.updateStatus("Tab closed");
    }

    private void openProject() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Open Project");

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            ProjectType type = ProjectDetector.detect(folder);

            if (type == ProjectType.UNKNOWN) {
                int choice = JOptionPane.showOptionDialog(this,
                        "This folder does not appear to contain a supported project.",
                        "Unknown Project",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[]{"Open Anyway", "Cancel"},
                        "Cancel");
                if (choice != JOptionPane.YES_OPTION) return;
            }

            openProjectDir(folder);
        }
    }

    private void openProjectDir(File folder) {
        explorerPanel.setRootDirectory(folder);
        updateProjectUi(folder);

        ProjectType type = ProjectDetector.detect(folder);
        ProjectInfo info = new ProjectInfo(folder.getName(), folder.getAbsolutePath(), type);
        projectService.addRecent(info);
        updateWelcomeRecent();

        editorCards.show(editorStack, EDITOR_VIEW);
        bottomTool.printRunOutput("Opened Project: " + folder.getAbsolutePath());
        statusBar.updateStatus("Opened project " + folder.getName());

        tryOpenDefaultFile(folder);
    }

    private void openRecentProject(ProjectInfo info) {
        File folder = new File(info.getPath());
        if (folder.exists() && folder.isDirectory()) {
            openProjectDir(folder);
        } else {
            int choice = JOptionPane.showOptionDialog(this,
                    "Project \"" + info.getName() + "\" no longer exists.\nRemove it from recent projects?",
                    "Project Not Found",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new Object[]{"Remove", "Keep"},
                    "Remove");
            if (choice == JOptionPane.YES_OPTION) {
                projectService.removeRecent(info.getPath());
                updateWelcomeRecent();
            }
        }
    }

    private void newProject() {
        new NewProjectWizard(this, templateService, this::openProjectDir).setVisible(true);
    }

    private void tryOpenDefaultFile(File projectRoot) {
        File srcDir = new File(projectRoot, "src/main/java");
        if (!srcDir.exists()) return;
        java.util.List<File> javaFiles = new java.util.ArrayList<>();
        collectJavaFiles(srcDir, javaFiles);
        if (!javaFiles.isEmpty()) {
            openFile(javaFiles.get(0));
        }
    }

    private void collectJavaFiles(File dir, java.util.List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectJavaFiles(f, result);
            } else if (f.getName().endsWith(".java")) {
                result.add(f);
            }
        }
    }

    private void updateWelcomeRecent() {
        welcomePanel.setRecentProjects(projectService.getRecentProjects());
    }

    private void refreshExplorer() {
        explorerPanel.refresh();
        bottomTool.printRunOutput("Explorer Refreshed");
        statusBar.updateStatus("Explorer refreshed");
    }

    private void showSearchDialog() {
        String query = JOptionPane.showInputDialog(this, "Search in project:", "Search", JOptionPane.PLAIN_MESSAGE);
        if (query == null || query.isBlank()) return;

        bottomTool.showRun();
        bottomTool.clearRunOutput();
        bottomTool.setRunStatus("Search results");
        bottomTool.printRunOutput("Searching for: " + query + "\n");
        showBottomPanel();

        File root = explorerPanel.getCurrentRoot();
        new Thread(() -> {
            StringBuilder results = new StringBuilder();
            searchInDirectory(root, query, results);
            SwingUtilities.invokeLater(() -> {
                if (results.isEmpty()) {
                    bottomTool.printRunOutput("No results found.");
                } else {
                    bottomTool.printRunOutput(results.toString());
                }
                statusBar.updateStatus("Search complete");
            });
        }).start();
    }

    private void searchInDirectory(File dir, String query, StringBuilder results) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                searchInDirectory(f, query, results);
            } else if (f.getName().endsWith(".java") || f.getName().endsWith(".txt") || f.getName().endsWith(".xml")) {
                try {
                    String content = fileManager.openFile(f);
                    if (content.toLowerCase().contains(query.toLowerCase())) {
                        results.append(f.getPath()).append("\n");
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void hideBottomPanel() {
        int totalHeight = rootSplit.getHeight();
        rootSplit.setDividerLocation(totalHeight - 5);
        bottomVisible = false;
    }

    private void showBottomPanel() {
        int totalHeight = rootSplit.getHeight();
        rootSplit.setDividerLocation(totalHeight - lastBottomHeight);
        bottomVisible = true;
    }

    private void updateBreadcrump() {
        File file = tabbedPane.getActiveFile();
        statusBar.updatePath(file);
    }

    private void showWelcomeIfNeeded() {
        if (tabbedPane.getTabCount() == 0) {
            editorCards.show(editorStack, WELCOME_VIEW);
            statusBar.updateFile(null);
            statusBar.updatePath(null);
            return;
        }

        editorCards.show(editorStack, EDITOR_VIEW);
    }

    private void updateSelectedFileUi() {
        File file = tabbedPane.getActiveFile();
        if (file == null && tabbedPane.getTabCount() > 0) {
            statusBar.updateFile("Untitled");
        } else if (file == null) {
            statusBar.updateFile(null);
        } else {
            statusBar.updateFile(file.getName());
        }
    }

    private void updateProjectUi(File root) {
        String name = root.getAbsoluteFile().getName();

        if (name == null || name.isBlank() || name.equals(".")) {
            name = "No project";
        }

        topBar.setProjectName(name);
        statusBar.setProjectRoot(root);
        statusBar.updateProject(name);
    }

    private void showQuickFileSearch() {
        File root = explorerPanel.getCurrentRoot();
        if (root == null) return;

        java.util.List<File> allFiles = new ArrayList<>();
        collectFiles(root, allFiles);

        JDialog dialog = new JDialog(this, "Quick File Search", false);
        dialog.setUndecorated(true);
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(ColorManager.BORDER));

        JTextField searchField = new JTextField();
        searchField.setFont(TypographyManager.UI_TAB());
        searchField.setBackground(ColorManager.INPUT_BG);
        searchField.setForeground(ColorManager.TEXT_PRIMARY);
        searchField.setCaretColor(ColorManager.EDITOR_CARET);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorManager.BORDER_DIVIDER),
                BorderFactory.createEmptyBorder(SpacingSystem.SM, SpacingSystem.MD, SpacingSystem.SM, SpacingSystem.MD)));

        DefaultListModel<File> listModel = new DefaultListModel<>();
        JList<File> fileList = new JList<>(listModel);
        fileList.setFont(TypographyManager.UI_STATUS());
        fileList.setBackground(ColorManager.WINDOW_BG);
        fileList.setForeground(ColorManager.TEXT_PRIMARY);
        fileList.setSelectionBackground(ColorManager.ACCENT_SELECTION);
        fileList.setSelectionForeground(ColorManager.TEXT_PRIMARY);
        fileList.setFixedCellHeight(24);
        fileList.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XS, 0, SpacingSystem.XS, 0));

        fileList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    File selected = fileList.getSelectedValue();
                    if (selected != null) {
                        dialog.dispose();
                        openFile(selected);
                    }
                }
            }
        });

        searchField.addActionListener(e -> {
            File selected = fileList.getSelectedValue();
            if (selected != null) {
                dialog.dispose();
                openFile(selected);
            }
        });

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                String query = searchField.getText().trim().toLowerCase();
                listModel.clear();
                if (query.isEmpty()) return;
                for (File f : allFiles) {
                    if (f.getName().toLowerCase().contains(query)) {
                        listModel.addElement(f);
                    }
                }
                if (listModel.size() > 0) fileList.setSelectedIndex(0);
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        fileList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    File selected = fileList.getSelectedValue();
                    if (selected != null) {
                        dialog.dispose();
                        openFile(selected);
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dialog.dispose();
                }
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchField, new JScrollPane(fileList));
        split.setBorder(null);
        split.setDividerSize(0);
        dialog.add(split);

        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        searchField.requestFocusInWindow();
    }

    private void collectFiles(File dir, java.util.List<File> files) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".")) collectFiles(f, files);
            } else {
                files.add(f);
            }
        }
    }

    private void openFile(File file) {
        tabbedPane.openFile(file);
        int index = tabbedPane.getSelectedIndex();
        tabbedPane.setIconAt(index, IconManager.forFile(file.getName()));
        editorCards.show(editorStack, EDITOR_VIEW);
        addRecentFile(file);
        explorerPanel.selectFile(file);
        statusBar.updateStatus("Opened " + file.getName());
    }
}
