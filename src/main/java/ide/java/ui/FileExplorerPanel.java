package ide.java.ui;

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

        JScrollPane scrollPane = new JScrollPane(jTree);
        add(scrollPane, BorderLayout.CENTER);

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

    public JTree getjTree() {
        return jTree;
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
}
