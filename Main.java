package de.gfn.coffee;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SmartCoffeeGUI gui = new SmartCoffeeGUI();
            gui.setVisible(true);
        });
    }
}