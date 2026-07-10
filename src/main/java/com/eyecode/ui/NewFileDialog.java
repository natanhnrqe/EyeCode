package com.eyecode.ui;

import com.eyecode.filesystem.FileSystemService;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;

public class NewFileDialog extends JDialog {

    public enum ItemType {
        JAVA_CLASS, JAVA_INTERFACE, JAVA_ENUM, JAVA_RECORD, JAVA_ANNOTATION,
        GENERAL_FILE, GENERAL_DIRECTORY, GENERAL_PACKAGE
    }

    private ItemType selectedType = ItemType.JAVA_CLASS;
    private String resultName;
    private boolean confirmed;
    private Path createdPath;
    private boolean createdDirectory;

    private final JTextField nameField;
    private final JList<ListItem> itemList;
    private final DefaultListModel<ListItem> itemModel;

    public NewFileDialog(Frame owner, File targetDirectory, FileSystemService fileSystemService) {
        super(owner, "New", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        setSize(520, 400);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(ColorManager.EDITOR_BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 20, 8, 20));
        JLabel titleLabel = new JLabel("Create New");
        titleLabel.setFont(TypographyManager.UI_TITLE());
        titleLabel.setForeground(ColorManager.TEXT_PRIMARY);
        header.add(titleLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        itemModel = new DefaultListModel<>();
        itemModel.addElement(new ListItem("Java", "Class", ItemType.JAVA_CLASS));
        itemModel.addElement(new ListItem("Java", "Interface", ItemType.JAVA_INTERFACE));
        itemModel.addElement(new ListItem("Java", "Enum", ItemType.JAVA_ENUM));
        itemModel.addElement(new ListItem("Java", "Record", ItemType.JAVA_RECORD));
        itemModel.addElement(new ListItem("Java", "Annotation", ItemType.JAVA_ANNOTATION));
        itemModel.addElement(new ListItem("General", "File", ItemType.GENERAL_FILE));
        itemModel.addElement(new ListItem("General", "Directory", ItemType.GENERAL_DIRECTORY));
        itemModel.addElement(new ListItem("General", "Package", ItemType.GENERAL_PACKAGE));

        itemList = new JList<>(itemModel);
        itemList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                ListItem item = (ListItem) value;
                JPanel panel = new JPanel(new BorderLayout());
                panel.setOpaque(true);
                panel.setPreferredSize(new Dimension(200, 32));

                if (isSelected) {
                    panel.setBackground(ColorManager.ACCENT_SELECTION);
                } else {
                    panel.setBackground(ColorManager.EDITOR_BG);
                }

                JLabel nameLabel = new JLabel(item.displayName);
                nameLabel.setFont(TypographyManager.UI_BODY());
                nameLabel.setForeground(isSelected ? ColorManager.TEXT_PRIMARY : ColorManager.TEXT_SECONDARY);
                nameLabel.setBorder(new EmptyBorder(0, 12, 0, 0));
                panel.add(nameLabel, BorderLayout.CENTER);

                JLabel catLabel = new JLabel(item.category);
                catLabel.setFont(TypographyManager.UI_SMALL());
                catLabel.setForeground(ColorManager.TEXT_MUTED);
                catLabel.setBorder(new EmptyBorder(0, 12, 0, 12));
                panel.add(catLabel, BorderLayout.EAST);

                return panel;
            }
        });
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setSelectedIndex(0);
        itemList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedType = itemList.getSelectedValue().type;
            }
        });

        JScrollPane listScroll = new JScrollPane(itemList);
        listScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ColorManager.BORDER));
        listScroll.setPreferredSize(new Dimension(220, 0));
        add(listScroll, BorderLayout.WEST);

        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(TypographyManager.UI_BODY());
        nameLabel.setForeground(ColorManager.TEXT_SECONDARY);
        nameLabel.setBorder(new EmptyBorder(0, 0, 8, 0));

        nameField = new JTextField();
        nameField.setFont(TypographyManager.UI_BODY());
        nameField.setForeground(ColorManager.TEXT_PRIMARY);
        nameField.setBackground(ColorManager.SURFACE_BG);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER_CARD, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        nameField.setPreferredSize(new Dimension(0, 36));

        JPanel fieldHolder = new JPanel(new BorderLayout());
        fieldHolder.setOpaque(false);
        fieldHolder.add(nameLabel, BorderLayout.NORTH);
        fieldHolder.add(nameField, BorderLayout.CENTER);
        formPanel.add(fieldHolder, BorderLayout.NORTH);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        buttonBar.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(TypographyManager.UI_BUTTON());
        cancelButton.setForeground(ColorManager.TEXT_SECONDARY);
        cancelButton.setBackground(ColorManager.SURFACE_BG);
        cancelButton.setBorder(BorderFactory.createLineBorder(ColorManager.BORDER_CARD));
        cancelButton.setPreferredSize(new Dimension(90, 32));
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton createButton = new JButton("Create");
        createButton.setFont(TypographyManager.UI_BUTTON());
        createButton.setForeground(Color.WHITE);
        createButton.setBackground(new Color(42, 88, 188));
        createButton.setBorder(BorderFactory.createEmptyBorder());
        createButton.setPreferredSize(new Dimension(90, 32));
        createButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) return;
            try {
                create(targetDirectory, name, fileSystemService);
                confirmed = true;
                resultName = name;
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonBar.add(cancelButton);
        buttonBar.add(createButton);
        add(buttonBar, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(createButton);
    }

    private void create(File targetDirectory, String name, FileSystemService fs) throws Exception {
        Path basePath = targetDirectory.toPath();
        switch (selectedType) {
            case JAVA_CLASS -> {
                Path p = basePath.resolve(name + ".java");
                fs.writeFile(p, "public class " + name + " {\n}\n");
                recordCreated(p, false);
            }
            case JAVA_INTERFACE -> {
                Path p = basePath.resolve(name + ".java");
                fs.writeFile(p, "public interface " + name + " {\n}\n");
                recordCreated(p, false);
            }
            case JAVA_ENUM -> {
                Path p = basePath.resolve(name + ".java");
                fs.writeFile(p, "public enum " + name + " {\n}\n");
                recordCreated(p, false);
            }
            case JAVA_RECORD -> {
                Path p = basePath.resolve(name + ".java");
                fs.writeFile(p, "public record " + name + "() {\n}\n");
                recordCreated(p, false);
            }
            case JAVA_ANNOTATION -> {
                Path p = basePath.resolve(name + ".java");
                fs.writeFile(p, "public @interface " + name + " {\n}\n");
                recordCreated(p, false);
            }
            case GENERAL_FILE -> {
                Path p = basePath.resolve(name);
                fs.writeFile(p, "");
                recordCreated(p, false);
            }
            case GENERAL_DIRECTORY -> {
                Path p = basePath.resolve(name);
                fs.createDirectories(p);
                recordCreated(p, true);
            }
            case GENERAL_PACKAGE -> {
                String[] parts = name.split("\\.");
                Path pkgPath = basePath;
                for (String part : parts) {
                    pkgPath = pkgPath.resolve(part);
                }
                fs.createDirectories(pkgPath);
                recordCreated(pkgPath, true);
            }
        }
    }

    private void recordCreated(Path path, boolean directory) {
        this.createdPath = path;
        this.createdDirectory = directory;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getResultName() {
        return resultName;
    }

    public Path getCreatedPath() {
        return createdPath;
    }

    public boolean isCreatedDirectory() {
        return createdDirectory;
    }

    private static class ListItem {
        final String category;
        final String displayName;
        final ItemType type;

        ListItem(String category, String displayName, ItemType type) {
            this.category = category;
            this.displayName = displayName;
            this.type = type;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
