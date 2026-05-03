package com.eyecode.ui;

import com.eyecode.editor.Document;
import com.eyecode.filesystem.FileManager;
import com.eyecode.run.RunManager;
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

    // Exibe logs e saida de execucao
    private ConsolePanel consolePanel;

    // Responsavel por operacoes de arquivo (ler/salvar)
    private FileManager fileManager;

    // Responsavel por compilar/executar o codigo
    private RunManager runManager;

    // Explorer de arquivos (lado esquerdo)
    private FileExplorerPanel explorerPanel;

    public MainWindow(){

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
        consolePanel = new ConsolePanel();
        explorerPanel = new FileExplorerPanel(new File("."));

        /**
         * Layout principal dividido:
         *
         * [ Explorer | Editor ]
         * [      Console      ]
         *
         * Isso simula layout de IDE real
         */
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

        // Cria o menu superior
        createMenu();

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

    /**
     * Executa o código Java do documento atual.
     *
     * Pipeline:
     * salvar → compilar → executar → mostrar saída
     */
    private void runCode(){
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

        if (doc == null) return;

        /**
         * Garante que o arquivo exista antes de rodar.
         * (não dá pra compilar algo que não está no disco)
         */
        if (doc.getFile() == null){
            saveFileAs();
            doc = editor.getDocument();
        }

        /**
         * Garante que o código atual está salvo
         * antes de compilar.
         */
        if (doc.getModified()){
            saveFile();
        }
        consolePanel.print("Running...\n");

        // Executa via RunManager
        String output = runManager.runJavaFile(doc.getFile());

        consolePanel.print(output);

    }
    /**
     * Abre um arquivo do sistema usando JFileChooser.
     *
     * Fluxo:
     * arquivo → conteúdo → Document → nova aba
     */
    private void openFile(){
        JFileChooser chooser = new JFileChooser();

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION){
            File file = chooser.getSelectedFile();

            // Lê conteúdo do arquivo
            String content = fileManager.openFile(file);

            // Cria model em memória
            Document document = new Document(file, content);

            // Abre no editor
            addNewTab(document, file.getName());

           consolePanel.print("Opened" + file.getName());


        }
    }

    /**
     * Salva o arquivo atual.
     *
     * Se não existir arquivo (novo documento),
     * redireciona para Save As.
     */
    private void saveFile(){
        EditorPanel editor = getCurrentEditor();

        if (editor == null) return;

        Document doc = editor.getDocument();

        if (doc == null){
            return;
        }

        File file = doc.getFile();

        // Documento novo → precisa escolher onde salvar
        if (file == null){
            saveFileAs();
            return;
        }

        // Salva no disco
        fileManager.saveFile(file, doc.getContent());

        // Marca como sincronizado
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

    /**
     * Cria uma nova aba com um editor independente.
     *
     * Cada aba possui:
     * - seu próprio EditorPanel
     * - seu próprio Document
     *
     * Isso permite múltiplos arquivos abertos simultaneamente.
     */
    private void addNewTab(Document document, String title){
        EditorPanel editor = new EditorPanel();

        // Conecta o documento ao editor
        editor.setDocument(document);

        /**
         * Callback disparado quando o usuário digita.
         * Usado para atualizar o título da aba (*)
         */
        editor.setOnChangeCallback(() -> updateTabTitle(editor));

        // Adiciona aba
        tabbedPane.addTab(title, editor);

        // Foca na aba recém criada
        tabbedPane.setSelectedComponent(editor);
    }

    /**
     * Retorna o editor atualmente ativo.
     *
     * Isso é essencial porque:
     * - todas as ações (save, run, etc.)
     *   atuam apenas na aba atual
     */
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

    /**
     * Fecha a aba atual com segurança.
     *
     * Se houver alterações não salvas:
     * - pergunta ao usuário
     * - evita perda de dados
     */
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

    private void openDefaultFile() {
        File mainFile = new File("src/main/java/com/eyecode/Main.java");

        if (mainFile.exists()) {
            String content = fileManager.openFile(mainFile);
            Document doc = new Document(mainFile, content);
            addNewTab(doc, mainFile.getName());
        }
    }
}
