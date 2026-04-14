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



public class FileExplorerPanel extends JPanel {

    private JTree jTree;
    private DefaultTreeModel treeModel;
    private Consumer<File> fileOpenCallBack;
    private File currentRoot;



    public FileExplorerPanel(File rootDirectory) {
        setLayout(new BorderLayout());

        this.currentRoot = rootDirectory;

        DefaultMutableTreeNode rootNode = createNode(rootDirectory);


        treeModel = new DefaultTreeModel(rootNode);
        this.jTree = new JTree(treeModel);

        JScrollPane scrollPane = new JScrollPane(jTree);
        scrollPane.setBackground(Color.DARK_GRAY);

        add(scrollPane, BorderLayout.CENTER);

        jTree.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2){
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                            jTree.getLastSelectedPathComponent();

                    if (node == null) return;

                    File file = (File) node.getUserObject();

                    if (file.isFile() && fileOpenCallBack != null){
                        fileOpenCallBack.accept(file);
                    }
                }
            }
        });
    }

    private DefaultMutableTreeNode createNode(File file){
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(file);


        if (file.isDirectory()){
            File[] files = file.listFiles();

            if (files != null){
                for (File child : files){
                    node.add(createNode(child));
                }
            }
        }
        return node;
    }

    public JTree getjTree() {
        return jTree;
    }

    public void setFileOpenCallBack(Consumer<File> fileOpenCallBack) {
        this.fileOpenCallBack = fileOpenCallBack;
    }

    public void setRootDirectory(File rootDirectory){
        this.currentRoot = rootDirectory;

        DefaultMutableTreeNode rootNode = createNode(rootDirectory);
        treeModel.setRoot(rootNode);
        treeModel.reload();
    }
    public void refresh(){
        if (currentRoot != null){
            setRootDirectory(currentRoot);
        }
    }
}
