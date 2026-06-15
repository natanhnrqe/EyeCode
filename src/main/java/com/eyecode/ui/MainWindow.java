package com.eyecode.ui;

import com.eyecode.editor.Document;
import com.eyecode.filesystem.FileManager;
import com.eyecode.run.RunManager;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import com.eyecode.ui.designsystem.UIConstants;
import com.eyecode.ui.editor.EditorPanel;
import com.eyecode.ui.editor.ToolWindowBar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;

public class MainWindow extends JFrame {

    private static final String WELCOME_VIEW = "WELCOME";
    private static final String EDITOR_VIEW = "EDITOR";

    private final JTabbedPane tabbedPane;
    private final FileManager fileManager;
    private final RunManager runManager;
    private final FileExplorerPanel explorerPanel;
    private final BottomToolWindowPanel bottomTool;
    private final TopBarPanel topBar;
    private final StatusBar statusBar;
    private final ToolWindowBar toolWindowBar;
    private final BreadcrumpPanel breadcrumpPanel;
    private final JPanel editorStack;
    private final CardLayout editorCards;

    private JSplitPane rootSplit;
    private int lastBottomHeight = 300;
    private boolean bottomVisible = true;
    private Point dragStart;

    public MainWindow() {

        setTitle("EyeCode");
        setUndecorated(true);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(ColorManager.WINDOW_BG);

        fileManager = new FileManager();
        runManager = new RunManager();

        tabbedPane = new JTabbedPane();
        explorerPanel = new FileExplorerPanel(new File("."));
        bottomTool = new BottomToolWindowPanel();
        topBar = new TopBarPanel();
        statusBar = new StatusBar();
        toolWindowBar = new ToolWindowBar();
        breadcrumpPanel = new BreadcrumpPanel();

        editorCards = new CardLayout();
        editorStack = new JPanel(editorCards);
        editorStack.setOpaque(false);
        editorStack.add(new WelcomePanel(this::newFile, this::openFolder, this::runCode), WELCOME_VIEW);
        editorStack.add(tabbedPane, EDITOR_VIEW);

        configureActions();
        configureLayout();
        configureTabs();
        configureExplorer();
        configureWindowChrome();

        updateProjectUi(new File("."));
        showWelcomeIfNeeded();

        setMaximizedBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(UIConstants.WINDOW_MIN_WIDTH, UIConstants.WINDOW_MIN_HEIGHT));
        setVisible(true);

        SwingUtilities.invokeLater(this::openDefaultFile);
    }

    private void configureActions() {
        topBar.setOnRun(this::runCode);
        topBar.setOnSave(this::saveFile);
        topBar.setOnOpenFolder(this::openFolder);
        topBar.setOnNewFile(this::newFile);
        topBar.setOnSettings(() -> {
            Font currentFont = TypographyManager.UI_TITLE();
            EditorPanel editor = getCurrentEditor();
            if (editor != null && editor.getDocument() != null) {
                currentFont = editor.getEditorFont();
            }
            new SettingsDialog(MainWindow.this, currentFont, newFont -> {
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    Component c = tabbedPane.getComponentAt(i);
                    if (c instanceof EditorPanel ep) {
                        ep.setEditorFont(newFont);
                    }
                }
            });
        });
        topBar.setOnMinimize(() -> setState(Frame.ICONIFIED));
        topBar.setOnMaximize(this::toggleMaximize);
        topBar.setOnClose(() -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

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
        tabbedPane.putClientProperty("JTabbedPane.tabHeight", UIConstants.TAB_HEIGHT);
        tabbedPane.putClientProperty("JTabbedPane.showTabsSeparators", true);
        tabbedPane.putClientProperty("JTabbedPane.tabInsets", new Insets(SpacingSystem.MD, SpacingSystem.XXL, SpacingSystem.MD, SpacingSystem.XXL));
        tabbedPane.setBackground(ColorManager.EDITOR_BG);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XXS, SpacingSystem.XXS, SpacingSystem.XXS, SpacingSystem.XXS));

        tabbedPane.addChangeListener(e -> {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                Component component = tabbedPane.getTabComponentAt(i);

                if (component instanceof TabComponent tab) {
                    tab.setSelected(i == tabbedPane.getSelectedIndex());
                }
            }

            updateBreadcrump();
            updateSelectedFileUi();
        });
    }

    private void configureExplorer() {
        explorerPanel.setFileOpenCallBack(file -> {
            String content = fileManager.openFile(file);
            Document doc = new Document(file, content);
            addNewTab(doc, file.getName());
            statusBar.updateStatus("Opened " + file.getName());
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

    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu runMenu = new JMenu("Run");
        JMenuItem runItem = new JMenuItem("Run");
        runItem.addActionListener(e -> runCode());
        runMenu.add(runItem);
        menuBar.add(runMenu);

        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(e -> newFile());
        fileMenu.add(newItem);

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> saveFile());
        fileMenu.add(saveItem);

        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> openFile());

        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(e -> closeCurrentTab());

        JMenuItem openFolder = new JMenuItem("Open Folder");
        openFolder.addActionListener(e -> openFolder());
        fileMenu.add(openFolder);

        JMenuItem refresh = new JMenuItem("Refresh");
        refresh.addActionListener(e -> refreshExplorer());
        fileMenu.add(refresh);

        fileMenu.add(closeItem);
        fileMenu.add(openItem);
        menuBar.add(fileMenu);

        setJMenuBar(menuBar);
    }

    private void runCode() {
        EditorPanel editor = getCurrentEditor();

        if (editor != null) {
            Document doc = editor.getDocument();

            if (doc == null) return;

            if (doc.getFile() == null) {
                saveFileAs();
                doc = editor.getDocument();
            }

            if (doc.getModified()) {
                saveFile();
            }
        }

        bottomTool.showRun();
        bottomTool.clearRunOutput();
        bottomTool.setRunStatus("Running");
        bottomTool.printRunOutput("Running...");
        topBar.setRunActive(true);
        statusBar.setRunning(true);
        showBottomPanel();

        File projectRoot = explorerPanel.getCurrentRoot();

        new Thread(() -> {
            String output = runManager.runProject(projectRoot);

            SwingUtilities.invokeLater(() -> {
                bottomTool.printRunOutput(output);
                bottomTool.setRunStatus("Finished");
                topBar.setRunActive(false);
                statusBar.setRunning(false);
                statusBar.updateStatus("Run finished");
            });
        }).start();
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String content = fileManager.openFile(file);
            Document document = new Document(file, content);

            addNewTab(document, file.getName());
            bottomTool.printRunOutput("Opened " + file.getName());
            statusBar.updateStatus("Opened " + file.getName());
        }
    }

    private void saveFile() {
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

        if (doc == null) return;

        File file = doc.getFile();

        if (file == null) {
            saveFileAs();
            return;
        }

        fileManager.saveFile(file, doc.getContent());
        doc.setModified(false);

        bottomTool.printRunOutput("File Saved: " + file.getName());
        statusBar.updateStatus("Saved " + file.getName());
        updateTabTitle(editor);
    }

    private void saveFileAs() {
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

        if (doc == null) return;

        JFileChooser chooser = new JFileChooser();
        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            fileManager.saveFile(file, doc.getContent());

            doc.setModified(false);
            doc = new Document(file, doc.getContent());
            editor.setDocument(doc);

            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), file.getName());

            bottomTool.printRunOutput("File Saved As " + file.getName());
            statusBar.updateStatus("Saved " + file.getName());
            updateTabTitle(editor);
            updateSelectedFileUi();
        }
    }

    private void newFile() {
        Document doc = new Document(null, "");
        addNewTab(doc, "Untitled");
        bottomTool.printRunOutput("New File Created");
        statusBar.updateStatus("New file created");
    }

    private void addNewTab(Document document, String title) {
        EditorPanel editor = new EditorPanel();
        editor.setDocument(document);
        editor.setCaretPositionListener(statusBar::updateCaretPosition);
        editor.setOnChangeCallback(() -> updateTabTitle(editor));

        tabbedPane.addTab(title, editor);

        int index = tabbedPane.indexOfComponent(editor);
        String filename = document.getFile() != null ? document.getFile().getName() : title;
        TabComponent tab = new TabComponent(title, filename, () -> closeTabAt(editor));
        tabbedPane.setTabComponentAt(index, tab);

        tabbedPane.setSelectedComponent(editor);
        editorCards.show(editorStack, EDITOR_VIEW);
        updateBreadcrump();
        updateSelectedFileUi();
    }

    private EditorPanel getCurrentEditor() {
        return (EditorPanel) tabbedPane.getSelectedComponent();
    }

    private void updateTabTitle(EditorPanel editor) {
        Document doc = editor.getDocument();
        int index = tabbedPane.indexOfComponent(editor);

        if (index == -1) return;

        String title = doc.getFile() != null ? doc.getFile().getName() : "Untitled";

        tabbedPane.setTitleAt(index, title);

        Component component = tabbedPane.getTabComponentAt(index);
        if (component instanceof TabComponent tab) {
            tab.setTitle(title);
            tab.setModified(doc.getModified());
        }

        updateSelectedFileUi();
    }

    private void closeCurrentTab() {
        EditorPanel editor = getCurrentEditor();

        if (editor != null) {
            closeTabAt(editor);
        }
    }

    private boolean closeTabAt(EditorPanel editor) {
        Document doc = editor.getDocument();

        if (doc != null && doc.getModified()) {
            tabbedPane.setSelectedComponent(editor);

            int opt = JOptionPane.showConfirmDialog(
                    this,
                    "File has unsaved changes. Save before closing?",
                    "Warning",
                    JOptionPane.YES_NO_CANCEL_OPTION
            );

            if (opt == JOptionPane.CANCEL_OPTION) return false;

            if (opt == JOptionPane.YES_OPTION) {
                saveFile();

                if (doc.getModified()) {
                    return false;
                }
            }
        }

        tabbedPane.remove(editor);
        showWelcomeIfNeeded();
        statusBar.updateStatus("Tab closed");
        return true;
    }

    private void openFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            explorerPanel.setRootDirectory(folder);
            updateProjectUi(folder);
            bottomTool.printRunOutput("Opened Folder: " + folder.getAbsolutePath());
            statusBar.updateStatus("Opened folder " + folder.getName());
        }
    }

    private void refreshExplorer() {
        explorerPanel.refresh();
        bottomTool.printRunOutput("Explorer Refreshed");
        statusBar.updateStatus("Explorer refreshed");
    }

    private void openDefaultFile() {
        File mainFile = new File("src/main/java/com/eyecode/Main.java");

        if (mainFile.exists()) {
            String content = fileManager.openFile(mainFile);
            Document doc = new Document(mainFile, content);
            addNewTab(doc, mainFile.getName());
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
        EditorPanel editor = getCurrentEditor();

        if (editor == null || editor.getDocument() == null) {
            breadcrumpPanel.updatePath(null);
            statusBar.updatePath(null);
            return;
        }

        File file = editor.getDocument().getFile();
        breadcrumpPanel.updatePath(file);
        statusBar.updatePath(file);
    }

    private void showWelcomeIfNeeded() {
        if (tabbedPane.getTabCount() == 0) {
            editorCards.show(editorStack, WELCOME_VIEW);
            breadcrumpPanel.updatePath(null);
            statusBar.updateFile(null);
            statusBar.updatePath(null);
            return;
        }

        editorCards.show(editorStack, EDITOR_VIEW);
    }

    private void updateSelectedFileUi() {
        EditorPanel editor = getCurrentEditor();

        if (editor == null || editor.getDocument() == null) {
            statusBar.updateFile(null);
            return;
        }

        File file = editor.getDocument().getFile();
        statusBar.updateFile(file == null ? "Untitled" : file.getName());
    }

    private void updateProjectUi(File root) {
        String name = root.getAbsoluteFile().getName();

        if (name == null || name.isBlank() || name.equals(".")) {
            name = "No project";
        }

        topBar.setProjectName(name);
        statusBar.updateProject(name);
    }
}
