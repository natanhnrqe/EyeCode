package ide.java.ui;

import ide.java.editor.Document;
import ide.java.filesystem.FileManager;

import javax.print.Doc;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MainWindow extends JFrame {

    private JTabbedPane tabbedPane;
    private ConsolePanel consolePanel;
    private FileManager fileManager;

    public MainWindow(){

        setTitle("Mini IDE");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        consolePanel = new ConsolePanel();

        fileManager = new FileManager();

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                tabbedPane,
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

        fileMenu.add(closeItem);

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
}
