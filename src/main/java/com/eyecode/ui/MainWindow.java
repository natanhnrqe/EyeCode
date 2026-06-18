package com.eyecode.ui;

import com.eyecode.editor.Document;
import com.eyecode.filesystem.FileManager;
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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

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
    private final JPanel editorStack;
    private final CardLayout editorCards;

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
        runManager = new RunManager();

        UIManager.put("TabbedPane.cardTabArc", 14);
        tabbedPane = new JTabbedPane();
        explorerPanel = new FileExplorerPanel(new File("."));
        bottomTool = new BottomToolWindowPanel();
        topBar = new TopBarPanel();
        statusBar = new StatusBar();
        toolWindowBar = new ToolWindowBar();

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

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), "quickFileSearch");
        getRootPane().getActionMap().put("quickFileSearch", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                showQuickFileSearch();
            }
        });

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
        topBar.setOnStop(this::stopCode);
        topBar.setOnDebug(this::debugCode);
        topBar.setOnSave(this::saveFile);
        topBar.setOnOpenProject(this::openFolder);
        topBar.setOnOpenFile(this::openFile);
        topBar.setOnSearch(this::showSearchDialog);
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
        addItem(fileMenu, "New File", e -> newFile());
        addItem(fileMenu, "Save", e -> saveFile());
        fileMenu.addSeparator();
        addItem(fileMenu, "Open Project", e -> openFolder());
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
            Component comp = tabbedPane.getComponentAt(index);
            if (comp instanceof EditorPanel editor) {
                closeTabAt(editor);
            }
        });
        tabbedPane.setBackground(ColorManager.EDITOR_BG);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XXS, SpacingSystem.XXS, SpacingSystem.XXS, SpacingSystem.XXS));

        tabbedPane.addChangeListener(e -> {
            updateBreadcrump();
            updateSelectedFileUi();
            EditorPanel editor = getCurrentEditor();
            if (editor != null && editor.getDocument() != null && editor.getDocument().getFile() != null) {
                explorerPanel.selectFile(editor.getDocument().getFile());
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

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            openFile(file);
            bottomTool.printRunOutput("Opened " + file.getName());
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
        tabbedPane.setIconAt(index, IconManager.forFile(filename));

        if (document.getFile() != null) {
            addRecentFile(document.getFile());
        }

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

        String name = doc.getFile() != null ? doc.getFile().getName() : "Untitled";
        String title = doc.getModified() ? name + " *" : name;
        tabbedPane.setTitleAt(index, title);
        if (doc.getFile() != null) {
            tabbedPane.setIconAt(index, IconManager.forFile(doc.getFile().getName()));
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
            openFile(mainFile);
        }
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
        EditorPanel editor = getCurrentEditor();

        if (editor == null || editor.getDocument() == null) {
            statusBar.updatePath(null);
            return;
        }

        File file = editor.getDocument().getFile();
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
        String content = fileManager.openFile(file);
        Document doc = new Document(file, content);
        addNewTab(doc, file.getName());
        explorerPanel.selectFile(file);
        statusBar.updateStatus("Opened " + file.getName());
    }
}
