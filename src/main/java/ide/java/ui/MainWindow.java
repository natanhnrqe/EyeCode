package ide.java.ui;

import ide.java.editor.Document;
import ide.java.filesystem.FileManager;
import ide.java.run.RunManager;

import javax.print.Doc;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MainWindow extends JFrame {

    private JTabbedPane tabbedPane;
    private ConsolePanel consolePanel;
    private FileManager fileManager;
    private RunManager runManager;
    private FileExplorerPanel explorerPanel;

    public MainWindow(){

        setTitle("Mini IDE");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        consolePanel = new ConsolePanel();

        fileManager = new FileManager();

        runManager = new RunManager();

        explorerPanel = new FileExplorerPanel(new File("."));

        explorerPanel.setFileOpenCallBack(file -> {
            String content = fileManager.openFile(file);
            Document doc = new Document(file, content);
            addNewTab(doc, file.getName());
        });

        JSplitPane horizontalSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                explorerPanel,
                tabbedPane
        );

        JSplitPane verticalSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                horizontalSplit,
                consolePanel
        );

        verticalSplit.setDividerLocation(450);

        add(verticalSplit, BorderLayout.CENTER);

        createMenu();

        setVisible(true);
    }

    private void createMenu(){
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

    private void runCode(){
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

        if (doc == null) return;

        if (doc.getFile() == null){
            saveFileAs();
            doc = editor.getDocument();
        }

        if (doc.getModified()){
            saveFile();
        }
        consolePanel.print("Running...\n");

        String output = runManager.runJavaFile(doc.getFile());

        consolePanel.print(output);

    }

    private void openFile(){
        JFileChooser chooser = new JFileChooser();

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION){
            File file = chooser.getSelectedFile();

            String content = fileManager.openFile(file);

            Document document = new Document(file, content);

           addNewTab(document, file.getName());

           consolePanel.print("Opened" + file.getName());


        }
    }
    private void saveFile(){
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

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

    //Save file for new docs
    private void saveFileAs(){
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

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
            editor.setDocument(doc);

            tabbedPane.setTitleAt(
                    tabbedPane.getSelectedIndex(),
                    file.getName()
            );

            consolePanel.print("File Saved As" + file.getName());
        }
    }
    private void newFile(){
        Document doc = new Document(null,"");

        addNewTab(doc,"Untitled");

        consolePanel.print("New File Created");
    }
    private void addNewTab(Document document, String title){
        EditorPanel editor = new EditorPanel();
        editor.setDocument(document);

        editor.setOnChangeCallback(() -> updateTabTitle(editor));

        tabbedPane.addTab(title, editor);
        tabbedPane.setSelectedComponent(editor);
    }
    private EditorPanel getCurrentEditor(){
        return (EditorPanel) tabbedPane.getSelectedComponent();
    }
    private void updateTabTitle(EditorPanel editor){
        Document doc = editor.getDocument();

        int index = tabbedPane.indexOfComponent(editor);

        if (index == -1) return;

        String title;

        if (doc.getFile() != null){
            title = doc.getFile().getName();
        }else {
            title = "Untitled";
        }
        if (doc.getModified()){
            title += " *";
        }

        tabbedPane.setTitleAt(index, title);
    }
    private void closeCurrentTab(){
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

        if (doc != null && doc.getModified()){
            int opt = JOptionPane.showConfirmDialog(
                    this,
                    "File has unsaved changes. Save before closing?",
                    "Warning",
                    JOptionPane.YES_NO_CANCEL_OPTION
            );
            if (opt == JOptionPane.CANCEL_OPTION) return;

            if (opt == JOptionPane.YES_OPTION) saveFile();
        }
        tabbedPane.remove(editor);
    }

    private void openFolder(){
        JFileChooser chooser = new JFileChooser();

        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION){
            File folder = chooser.getSelectedFile();

            explorerPanel.setRootDirectory(folder);

            consolePanel.print("Opened Folder: " + folder.getAbsolutePath());
        }
    }

    private void refreshExplorer(){
        explorerPanel.refresh();
        consolePanel.print("Explorer Refreshed");
    }
}
