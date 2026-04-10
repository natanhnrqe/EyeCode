package ide.java.ui;

import ide.java.editor.Document;
import ide.java.filesystem.FileManager;

import javax.print.Doc;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MainWindow extends JFrame {

    private EditorPanel editorPanel;
    private ConsolePanel consolePanel;
    private FileManager fileManager;

    public MainWindow(){

        setTitle("Mini IDE");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        editorPanel = new EditorPanel();
        consolePanel = new ConsolePanel();

        fileManager = new FileManager();

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                editorPanel,
                consolePanel
        );

        splitPane.setDividerLocation(450);

        add(splitPane, BorderLayout.CENTER);

        createMenu();

        setVisible(true);
    }

    private void createMenu(){
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> saveFile());
        fileMenu.add(saveItem);


        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> openFile());

        fileMenu.add(openItem);
        menuBar.add(fileMenu);

        setJMenuBar(menuBar);
    }

    private void openFile(){
        JFileChooser chooser = new JFileChooser();

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION){
            File file = chooser.getSelectedFile();

            String content = fileManager.openFile(file);

            Document document = new Document(file, content);

            editorPanel.setDocument(document);

            setTitle("Mini-IDE - " + file.getName());
        }
    }
    private void saveFile(){
        Document doc = editorPanel.getDocument();

        if (doc == null){
            return;
        }

        File file = doc.getFile();

        if (file == null){
            saveFileAs();
            return;
        }

        fileManager.saveFile(file, doc.getContent());

        doc.setModified(false);

        consolePanel.print("File Saved: " + file.getName());
    }
    private void saveFileAs(){
        Document doc = editorPanel.getDocument();

        if (doc == null){
            return;
        }

        JFileChooser chooser = new JFileChooser();

        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION){
            File file = chooser.getSelectedFile();

            fileManager.saveFile(file, doc.getContent());

            doc.setModified(false);

            doc = new Document(file, doc.getContent());
            editorPanel.setDocument(doc);

            setTitle("Mine-IDE" + file.getName());

            consolePanel.print("File Saved As" + file.getName());
        }
    }
}
