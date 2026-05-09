package com.eyecode.ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
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
public class FileExplorerPanel extends JPanel {

    private JPopupMenu popupMenu;

    private JTree jTree;
    private DefaultTreeModel treeModel;

    // Callback para comunicar ao MainWindow que um arquivo foi aberto
    private Consumer<File> fileOpenCallBack;

    // Diretorio raiz atual (estado do explorer)
    private File currentRoot;


    public FileExplorerPanel(File rootDirectory) {
        setLayout(new BorderLayout());

        // Guarda o estado atual da raiz
        this.currentRoot = rootDirectory;

        // Cria a arvore recursivamente a partir do diretorio raiz
        DefaultMutableTreeNode rootNode = createNode(rootDirectory);

        treeModel = new DefaultTreeModel(rootNode);
        this.jTree = new JTree(treeModel);

        jTree.setCellRenderer(new FileTreeCellRender());

        JScrollPane scrollPane = new JScrollPane(jTree);
        add(scrollPane, BorderLayout.CENTER);

        jTree.setBackground(new Color(43, 43, 43));
        jTree.setForeground(new Color(169, 183, 198));


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
                for (File child : files){

                    // Recursao: cria subarvores para cada filho
                    node.add(createNode(child));
                }
            }
        }
        return node;
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

            JMenuItem newFile = new JMenuItem("New File");
            newFile.addActionListener(e -> createNewFile(file));

            JMenuItem newFolder = new JMenuItem("New Folder");
            newFolder.addActionListener(e -> createNewFolder(file));

            popupMenu.add(newFile);
            popupMenu.add(newFolder);
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

    private void createNewFile(File directory){
        String name = JOptionPane.showInputDialog("File name:");

        if (name == null || name.isBlank()) return;

        try {
            File newFile = new File(directory, name);
            if (newFile.createNewFile()) {
                refresh();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void createNewFolder(File directory){
        String name = JOptionPane.showInputDialog("Folder name:");

        if (name == null || name.isBlank()) return;

        File folder = new File(directory, name);
        if (folder.mkdir()){
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
            if (file != null){
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
        if (confirm != JOptionPane.YES_NO_OPTION) return;

        deleteRecursively(file);
        refresh();
    }

    public File getCurrentRoot() {
        return currentRoot;
    }
}
