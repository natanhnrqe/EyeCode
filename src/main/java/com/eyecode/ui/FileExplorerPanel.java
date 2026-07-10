package com.eyecode.ui;

import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.ProjectRefreshEvent;
import com.eyecode.filesystem.FileSystemService;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import com.eyecode.ui.designsystem.UIConstants;
import com.eyecode.ui.scroll.ModernScrollBarUI;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.function.Consumer;
import javax.swing.JTree;


/**
 * Responsável por exibir o sistema de arquivos em formato de árvore (JTree).
 *
 * Atua como a "visão" do File Explorer, permitindo:
 * - Navegar por diretórios
 * - Visualizar arquivos
 * - Abrir arquivos via callback (duplo clique)
 *
 * Não abre arquivos diretamente → delega essa responsabilidade via callback.
 */
public class FileExplorerPanel extends RoundedPanel {

    private JPopupMenu popupMenu;

    private JTree jTree;
    private DefaultTreeModel treeModel;

    // Callback para comunicar ao MainWindow que um arquivo foi aberto
    private Consumer<File> fileOpenCallBack;

    // Diretorio raiz atual (estado do explorer)
    private File currentRoot;

    private final FileSystemService fileSystemService;
    private EventBus eventBus;

    public FileExplorerPanel(File rootDirectory, FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
        setLayout(new BorderLayout());
        setBackground(ColorManager.EDITOR_BG);

        // Guarda o estado atual da raiz
        this.currentRoot = rootDirectory;

        // Cria a arvore recursivamente a partir do diretorio raiz
        DefaultMutableTreeNode rootNode = createNode(rootDirectory);

        treeModel = new DefaultTreeModel(rootNode);
        this.jTree = new JTree(treeModel);

        jTree.setRowHeight(UIConstants.EXPLORER_ROW_HEIGHT);
        jTree.setShowsRootHandles(true);

        jTree.setToggleClickCount(1);
        jTree.setOpaque(false);

        jTree.setBackground(ColorManager.EDITOR_BG);
        jTree.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XS, SpacingSystem.MD, SpacingSystem.XS, SpacingSystem.MD));



        jTree.setCellRenderer(new FileTreeCellRender());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.MD, SpacingSystem.MD, SpacingSystem.XS, SpacingSystem.MD));

        JLabel title = new JLabel("Project");
        title.setForeground(ColorManager.TEXT_SECONDARY);
        title.setFont(TypographyManager.UI_TITLE());
        header.add(title, BorderLayout.WEST);

        JScrollPane scrollPane = new JScrollPane(jTree);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());

        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());

        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        jTree.setOpaque(false);
        jTree.setForeground(ColorManager.TEXT_FILE_TREE);


        // Evento de duplo clique para abrir arquivos
        jTree.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {

                // So reage a duplo clique
                if (e.getClickCount() == 2){
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                            jTree.getLastSelectedPathComponent();

                    if (node == null) return;

                    // Recupera o File armazenado no nó
                    File file = (File) node.getUserObject();

                    // So abre se for arquivo (nao diretorio)
                    if (file.isFile() && fileOpenCallBack != null){
                        fileOpenCallBack.accept(file);
                    }
                }
            }
        });

        jTree.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)){

                    int row = jTree.getClosestRowForLocation(e.getX(), e.getY());
                    jTree.setSelectionRow(row);

                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();

                    if (node == null) return;

                    File file = (File) node.getUserObject();

                    createPopMenu(file);

                    popupMenu.show(jTree, e.getX(), e.getY());
                }
            }
        });
    }

    /**
     * Método recursivo que constrói a árvore de arquivos.
     *
     * Para cada diretório:
     * - cria um nó
     * - percorre os filhos
     * - adiciona subnós
     */
    private DefaultMutableTreeNode createNode(File file){
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(file);

        // Se for diretorio, percorre os filhos
        if (file.isDirectory()){
            File[] files = file.listFiles();

            if (files != null){
                Arrays.sort(files, Comparator
                        .comparing(File::isFile)
                        .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));

                for (File child : files){
                    if (shouldHide(child)) {
                        continue;
                    }

                    // Recursao: cria subarvores para cada filho
                    node.add(createNode(child));
                }
            }
        }
        return node;
    }

    private boolean shouldHide(File file) {
        String name = file.getName();
        return name.equals(".git")
                || name.equals(".idea")
                || name.equals("target")
                || name.equals("out");
    }

    private void createPopMenu(File file){
        popupMenu = new JPopupMenu();

        if (file.isFile()){

            JMenuItem openItem = new JMenuItem("Open");
            openItem.addActionListener(e -> {
                if (fileOpenCallBack != null){
                    fileOpenCallBack.accept(file);
                }
            });

            popupMenu.add(openItem);
        }

        if (file.isDirectory()){

            JMenuItem newItem = new JMenuItem("New...");
            newItem.addActionListener(e -> showNewFileDialog(file));

            popupMenu.add(newItem);
        }

        JMenuItem rename = new JMenuItem("Rename");
        rename.addActionListener(e -> renameFile(file));

        JMenuItem delete = new JMenuItem("Delete");
        delete.addActionListener(e -> deleteFile(file));

        popupMenu.addSeparator();
        popupMenu.add(rename);
        popupMenu.add(delete);
    }

    /**
     * Define o comportamento ao abrir um arquivo (callback).
     * Mantém baixo acoplamento com MainWindow.
     */
    public void setFileOpenCallBack(Consumer<File> fileOpenCallBack) {
        this.fileOpenCallBack = fileOpenCallBack;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    private void emit(ProjectRefreshEvent event) {
        if (eventBus != null && event != null) {
            eventBus.publish(event);
        }
    }

    /**
     * Permite trocar a pasta raiz dinamicamente (Open Folder).
     */
    public void setRootDirectory(File rootDirectory){
        this.currentRoot = rootDirectory;

        System.out.println(
                "NEW ROOT = " +
                        rootDirectory.getAbsolutePath()
        );

        DefaultMutableTreeNode rootNode = createNode(rootDirectory);
        treeModel.setRoot(rootNode);
        treeModel.reload();
    }

    /**
     * Recarrega a árvore mantendo a mesma raiz.
     * Usado para atualizar mudanças no sistema de arquivos.
     */
    public void refresh(){
        if (currentRoot != null){
            setRootDirectory(currentRoot);
        }
    }

    private void showNewFileDialog(File directory){
        NewFileDialog dialog = new NewFileDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                directory, fileSystemService);
        dialog.setVisible(true);
        if (dialog.isConfirmed() && dialog.getCreatedPath() != null) {
            ProjectRefreshEvent.Kind kind = dialog.isCreatedDirectory()
                    ? ProjectRefreshEvent.Kind.DIRECTORY_CREATED
                    : ProjectRefreshEvent.Kind.FILE_CREATED;
            emit(new ProjectRefreshEvent(kind, dialog.getCreatedPath()));
        }
    }

    private void renameFile(File file){
        String name = JOptionPane.showInputDialog("New name:", file.getName());

        if (name == null || name.isBlank()) return;

        File newfile = new File(file.getParent(), name);

        if (file.renameTo(newfile)){
            emit(new ProjectRefreshEvent(
                    ProjectRefreshEvent.Kind.FILE_RENAMED,
                    newfile.toPath(),
                    file.toPath()));
        }
    }

    private void deleteRecursively(File file){
        if (file.isDirectory()){
            File[] files = file.listFiles();
            if (files != null){
                for (File f : files){
                    deleteRecursively(f);
                }
            }
        }
        file.delete();
    }

    private void deleteFile(File file){
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete " + file.getName() + "?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        deleteRecursively(file);
        ProjectRefreshEvent.Kind kind = file.isDirectory()
                ? ProjectRefreshEvent.Kind.DIRECTORY_DELETED
                : ProjectRefreshEvent.Kind.FILE_DELETED;
        emit(new ProjectRefreshEvent(kind, file.toPath()));
    }

    public File getCurrentRoot() {
        return currentRoot;
    }

    public void selectFile(File file) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        DefaultMutableTreeNode node = findNode(root, file);
        if (node != null) {
            jTree.setSelectionPath(new javax.swing.tree.TreePath(node.getPath()));
            jTree.scrollPathToVisible(new javax.swing.tree.TreePath(node.getPath()));
        }
    }

    private DefaultMutableTreeNode findNode(DefaultMutableTreeNode parent, File target) {
        File parentFile = (File) parent.getUserObject();
        if (parentFile.equals(target)) return parent;
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            DefaultMutableTreeNode found = findNode(child, target);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Incremental refresh driven by {@link ProjectRefreshEvent}.
     * <p>
     * Patches only the affected node: never rebuilds the tree, never loses
     * folder expansion and never changes the current selection.
     */
    public void incrementalRefresh(ProjectRefreshEvent event) {
        if (event == null) return;

        TreePath savedSelection = jTree.getSelectionPath();
        java.util.List<TreePath> expandedPaths = captureExpandedPaths();

        switch (event.getKind()) {
            case FILE_CREATED, DIRECTORY_CREATED -> insertPath(event.getPath());
            case FILE_DELETED, DIRECTORY_DELETED -> removePath(event.getPath());
            case FILE_RENAMED -> {
                if (event.hasOldPath()) removePath(event.getOldPath());
                insertPath(event.getPath());
            }
            case FILE_MODIFIED -> { /* no structural change */ }
        }

        restoreExpandedPaths(expandedPaths);
        if (savedSelection != null) {
            jTree.setSelectionPath(savedSelection);
        }
    }

    private java.util.List<TreePath> captureExpandedPaths() {
        java.util.List<TreePath> paths = new java.util.ArrayList<>();
        Enumeration<TreePath> expanded = jTree.getExpandedDescendants(
                new TreePath(treeModel.getRoot()));
        if (expanded != null) {
            while (expanded.hasMoreElements()) {
                paths.add(expanded.nextElement());
            }
        }
        return paths;
    }

    private void restoreExpandedPaths(java.util.List<TreePath> paths) {
        for (TreePath path : paths) {
            jTree.expandPath(path);
        }
    }

    private void insertPath(Path path) {
        if (path == null) return;
        File file = path.toFile();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        File rootFile = (File) root.getUserObject();
        if (!isDescendant(rootFile, file)) return;

        DefaultMutableTreeNode parent = findParentNode(root, file);
        if (parent == null) return;
        if (findDirectChild(parent, file) != null) return;

        DefaultMutableTreeNode newNode = createNode(file);
        int index = insertionIndex(parent, file);
        parent.insert(newNode, index);
        treeModel.nodesWereInserted(parent, new int[]{index});
    }

    private void removePath(Path path) {
        if (path == null) return;
        File file = path.toFile();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        DefaultMutableTreeNode node = findNode(root, file);
        if (node == null || node.getParent() == null) return;
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        int index = parent.getIndex(node);
        parent.remove(index);
        treeModel.nodesWereRemoved(parent, new int[]{index}, new Object[]{node});
    }

    private DefaultMutableTreeNode findParentNode(DefaultMutableTreeNode root, File file) {
        File parentFile = file.getParentFile();
        if (parentFile == null) return null;
        DefaultMutableTreeNode parentNode = findNode(root, parentFile);
        return parentNode;
    }

    private DefaultMutableTreeNode findDirectChild(DefaultMutableTreeNode parent, File target) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            File childFile = (File) child.getUserObject();
            if (childFile.equals(target)) return child;
        }
        return null;
    }

    private int insertionIndex(DefaultMutableTreeNode parent, File newFile) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            File childFile = (File) child.getUserObject();
            if (compareChildren(newFile, childFile) < 0) {
                return i;
            }
        }
        return parent.getChildCount();
    }

    private int compareChildren(File a, File b) {
        return Comparator
                .comparing(File::isFile)
                .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER)
                .compare(a, b);
    }

    private boolean isDescendant(File root, File candidate) {
        if (root == null || candidate == null) return false;
        Path rootPath = root.toPath().toAbsolutePath().normalize();
        Path candidatePath = candidate.toPath().toAbsolutePath().normalize();
        return candidatePath.startsWith(rootPath);
    }

}

