package ide.java.ui.editor;

import javax.swing.*;
import java.awt.*;

public class SuggestionRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus
        );

        Suggestion s = (Suggestion) value;

        String icon = switch (s.getType()){
            case "METHOD" -> "m";
            case "KEYWORD" -> "k";
            case "CLASS" -> "c";
            case "VARIABLE" -> "v";
            default -> "• ";
        };

        label.setText(icon + s.getText());

        label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        return label;

        }

    }

