package ide.java.ui.editor;

import javax.swing.*;
import java.awt.*;


public class SuggestionRenderer extends JPanel implements ListCellRenderer<Suggestion> {

    private JLabel left = new JLabel();
    private JLabel right = new JLabel();

    public SuggestionRenderer() {
        setLayout(new BorderLayout(10, 0));

        left.setOpaque(false);
        right.setOpaque(false);

        right.setHorizontalAlignment(SwingConstants.RIGHT);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);

        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends Suggestion> list,
            Suggestion s,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        String icon = switch (s.getType()) {
            case "METHOD" -> "ƒ";
            case "KEYWORD" -> "K";
            case "VARIABLE" -> "V";
            case "CLASS" -> "C";
            default -> "•";
        };

        left.setText(icon + "  " + s.getText());
        right.setText(s.getDetail());

        if (isSelected) {
            setBackground(new Color(75,110,175));
        } else {
            setBackground(new Color(43,43,43));
        }

        left.setForeground(Color.WHITE);
        right.setForeground(new Color(140,140,140));

        setOpaque(true);

        return this;
    }
}