package com.eyecode.ui;

import com.eyecode.editor.Document;
import com.eyecode.filesystem.FileManager;
import com.eyecode.run.RunManager;
import com.eyecode.terminal.TerminalPanel;
import com.eyecode.ui.editor.EditorPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Classe principal da interface da aplicação.
 *
 * Atua como o "Controller" da arquitetura:
 * - gerencia a UI
 * - controla fluxo (open, save, run, close)
 * - conecta Editor ↔ Document ↔ Services
 *
 * É o ponto central de orquestração do IDE.
 */
public class MainWindow extends JFrame {

    // Gerencia multiplos editores (cada aba = 1 arquivo)
    private JTabbedPane tabbedPane;

    // Responsavel por operacoes de arquivo (ler/salvar)
    private FileManager fileManager;

    // Responsavel por compilar/executar o codigo
    private RunManager runManager;

    // Explorer de arquivos (lado esquerdo)
    private FileExplorerPanel explorerPanel;

    private ActivityBar activityBar;

    private BottomToolWindowPanel bottomTool;

    private TopBarPanel topBar;

    private StatusBar statusBar;


    public MainWindow() {

        // Titulo da janela
        setTitle("EyeCode");

        //Tamanho inicial
        setSize(1000, 700);


        // Encerra a aplicacao ao fechar
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Inicializa service (camada de logica)
        fileManager = new FileManager();
        runManager = new RunManager();

        // Cria componentes principais
        tabbedPane = new JTabbedPane();
        explorerPanel = new FileExplorerPanel(new File("."));

        activityBar = new ActivityBar();
        bottomTool = new BottomToolWindowPanel();

        topBar = new TopBarPanel();

        statusBar = new StatusBar();

        /**
         * Layout principal dividido:
         *
         * [ Explorer | Editor ]
         * [      Terminal      ]
         *
         * Isso simula layout de IDE real
         */
        JPanel leftArea = new JPanel(new BorderLayout());

        leftArea.add(activityBar, BorderLayout.WEST);
        leftArea.add(explorerPanel, BorderLayout.CENTER);

        JSplitPane centerSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftArea,
                tabbedPane
        );

        JSplitPane rootSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                centerSplit,
                bottomTool
        );

        centerSplit.setResizeWeight(0.20);
        rootSplit.setResizeWeight(0.78);

        centerSplit.setDividerSize(4);
        rootSplit.setDividerSize(4);

        centerSplit.setBorder(null);
        rootSplit.setBorder(null);

        tabbedPane.putClientProperty("JTabbedPane.tabHeight", 34);
        tabbedPane.setBackground(new Color(30, 31, 34));
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());
        tabbedPane.putClientProperty("JTabbedPane.showTabsSeparators", true);
        tabbedPane.putClientProperty("JTabbedPane.tabInsets", new Insets(8, 16, 8, 16));
        tabbedPane.setBorder(
                BorderFactory.createEmptyBorder(
                        6,
                        6,
                        6,
                        6
                )
        );

        tabbedPane.addChangeListener(e -> {

            for (int i = 0; i < tabbedPane.getTabCount(); i++) {

                Component component = tabbedPane.getTabComponentAt(i);

                if (component instanceof TabComponent tab) {

                    tab.setSelected(i == tabbedPane.getSelectedIndex());
                }
            }
        });

        add(statusBar, BorderLayout.SOUTH);

        setExtendedState(JFrame.MAXIMIZED_BOTH);

        setMinimumSize(new Dimension(1200, 700));

        add(rootSplit, BorderLayout.CENTER);

        add(topBar, BorderLayout.NORTH);

        /**
         * Conecta o explorer ao editor:
         * Quando o usuário abre um arquivo na árvore,
         * criamos uma nova aba com o conteúdo.
         */
        explorerPanel.setFileOpenCallBack(file -> {
            String content = fileManager.openFile(file);
            Document doc = new Document(file, content);
            addNewTab(doc, file.getName());
        });

        setVisible(true);
        SwingUtilities.invokeLater(this::openDefaultFile);
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

    /**
     * Executa o código Java do documento atual.
     * <p>
     * Pipeline:
     * salvar → compilar → executar → mostrar saída
     */
    private void runCode() {
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

        if (doc == null) return;

        /**
         * Garante que o arquivo exista antes de rodar.
         * (não dá pra compilar algo que não está no disco)
         */
        if (doc.getFile() == null) {
            saveFileAs();
            doc = editor.getDocument();
        }

        /**
         * Garante que o código atual está salvo
         * antes de compilar.
         */
        if (doc.getModified()) {
            saveFile();
        }
        bottomTool.printRunOutput("Running...\n");

        File projectRoot = explorerPanel.getCurrentRoot();

        // Executa via RunManager
        new Thread(() -> {
            String output = runManager.runProject(projectRoot);

            SwingUtilities.invokeLater(() -> {
                bottomTool.printRunOutput(output);
        });
        }).start();

    }

    /**
     * Abre um arquivo do sistema usando JFileChooser.
     * <p>
     * Fluxo:
     * arquivo → conteúdo → Document → nova aba
     */
    private void openFile() {
        JFileChooser chooser = new JFileChooser();

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            // Lê conteúdo do arquivo
            String content = fileManager.openFile(file);

            // Cria model em memória
            Document document = new Document(file, content);

            // Abre no editor
            addNewTab(document, file.getName());

            bottomTool.printRunOutput("Opened" + file.getName());


        }
    }

    /**
     * Salva o arquivo atual.
     * <p>
     * Se não existir arquivo (novo documento),
     * redireciona para Save As.
     */
    private void saveFile() {
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

        if (doc == null) {
            return;
        }

        File file = doc.getFile();

        // Documento novo → precisa escolher onde salvar
        if (file == null) {
            saveFileAs();
            return;
        }

        // Salva no disco
        fileManager.saveFile(file, doc.getContent());

        // Marca como sincronizado
        doc.setModified(false);

        bottomTool.printRunOutput("File Saved: " + file.getName());
    }

    //Save file for new docs
    private void saveFileAs() {
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

        if (doc == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser();

        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            fileManager.saveFile(file, doc.getContent());

            doc.setModified(false);

            doc = new Document(file, doc.getContent());
            editor.setDocument(doc);

            tabbedPane.setTitleAt(
                    tabbedPane.getSelectedIndex(),
                    file.getName()
            );

            bottomTool.printRunOutput("File Saved As" + file.getName());
        }
    }

    private void newFile() {
        Document doc = new Document(null, "");

        addNewTab(doc, "Untitled");

        bottomTool.printRunOutput("New File Created");
    }

    /**
     * Cria uma nova aba com um editor independente.
     * <p>
     * Cada aba possui:
     * - seu próprio EditorPanel
     * - seu próprio Document
     * <p>
     * Isso permite múltiplos arquivos abertos simultaneamente.
     */
    private void addNewTab(Document document, String title) {
        EditorPanel editor = new EditorPanel();

        // Conecta o documento ao editor
        editor.setDocument(document);

        editor.setCaretPositionListener((line, col) -> statusBar.updateCaretPosition(line, col));

        /**
         * Callback disparado quando o usuário digita.
         * Usado para atualizar o título da aba (*)
         */
        editor.setOnChangeCallback(() -> updateTabTitle(editor));

        // Adiciona aba
        tabbedPane.addTab(title, editor);

        int index = tabbedPane.indexOfTabComponent(editor);

        tabbedPane.setTabComponentAt(index, new TabComponent(tabbedPane, title) {
        });

        // Foca na aba recém criada
        tabbedPane.setSelectedComponent(editor);
    }

    /**
     * Retorna o editor atualmente ativo.
     * <p>
     * Isso é essencial porque:
     * - todas as ações (save, run, etc.)
     * atuam apenas na aba atual
     */
    private EditorPanel getCurrentEditor() {
        return (EditorPanel) tabbedPane.getSelectedComponent();
    }


    private void updateTabTitle(EditorPanel editor) {
        Document doc = editor.getDocument();

        int index = tabbedPane.indexOfComponent(editor);

        if (index == -1) return;

        String title;

        if (doc.getFile() != null) {
            title = doc.getFile().getName();
        } else {
            title = "Untitled";
        }
        if (doc.getModified()) {
            title += " *";
        }

        tabbedPane.setTitleAt(index, title);
    }

    /**
     * Fecha a aba atual com segurança.
     * <p>
     * Se houver alterações não salvas:
     * - pergunta ao usuário
     * - evita perda de dados
     */
    private void closeCurrentTab() {
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

        if (doc != null && doc.getModified()) {
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

    private void openFolder() {
        JFileChooser chooser = new JFileChooser();

        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();

            explorerPanel.setRootDirectory(folder);

            bottomTool.printRunOutput("Opened Folder: " + folder.getAbsolutePath());
        }
    }

    private void refreshExplorer() {
        explorerPanel.refresh();
        bottomTool.printRunOutput("Explorer Refreshed");
    }

    private void openDefaultFile() {
        File mainFile = new File("src/main/java/com/eyecode/Main.java");

        if (mainFile.exists()) {
            String content = fileManager.openFile(mainFile);
            Document doc = new Document(mainFile, content);
            addNewTab(doc, mainFile.getName());
        }
    }
}


