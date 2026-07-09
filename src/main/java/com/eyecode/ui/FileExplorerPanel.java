package com.eyecode.ui;

import com.eyecode.filesystem.FileSystemService;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import com.eyecode.ui.designsystem.UIConstants;
import com.eyecode.ui.scroll.ModernScrollBarUI;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
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
        if (dialog.isConfirmed()) {
            refresh();
        }
    }

    private void renameFile(File file){
        String name = JOptionPane.showInputDialog("New name:", file.getName());

        if (name == null || name.isBlank()) return;

        File newfile = new File(file.getParent(), name);

        if (file.renameTo(newfile)){
            refresh();
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
        refresh();
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

}
