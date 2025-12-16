package de.gfn.coffee;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SmartCoffeeGUI extends JFrame {

    private final Kaffeeautomat automat;
    private final Muenzwechsler wechsler;

    // UI Komponenten
    private JLabel statusLabel;    // "Bereit" / "Zubereitung..."
    private JLabel creditLabel;    // "Guthaben: ..."
    private JLabel stockLabel;     // "Bohnen: ... | Milch: ... | Status: ..."
    private JProgressBar progressBar;
    private JTextArea logArea;
    private JPanel mainPanel;

    // Farben für das Design
    private final Color COLOR_BROWN = new Color(60, 40, 30);
    private final Color COLOR_CREAM = new Color(245, 235, 220);
    private final Color COLOR_ACCENT = new Color(210, 180, 140);
    private final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 22);

    public SmartCoffeeGUI() {
        // 1. Datenbank initialisieren
        DatabaseManager db = new DatabaseManager();

        // 2. Münzwechsler mit Datenbank verbinden (für Persistenz der Münzen)
        wechsler = new Muenzwechsler(db);

        // 3. Automat mit Datenbank und Wechsler verbinden
        automat = new Kaffeeautomat(db, wechsler);

        initUI();
    }

    private void initUI() {
        setTitle("Smart Coffee System v2.2 - Final");
        setSize(950, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Zentrieren

        mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(COLOR_BROWN);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        // --- HEADER (Titel + Status + Füllstände) ---
        createHeader();

        // --- CENTER (Produkte & Münzen) ---
        JPanel centerWrapper = new JPanel(new GridLayout(1, 2, 20, 0));
        centerWrapper.setOpaque(false);
        centerWrapper.add(createProductPanel());
        centerWrapper.add(createCoinPanel());
        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        // --- FOOTER (Log & Progress & Wartung) ---
        createFooter();

        // Initiale Anzeige aktualisieren
        updateDisplay();
    }

    private void createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("☕ Smart Coffee Lounge");
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(COLOR_CREAM);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Info Panel: Guthaben, Prozess-Status UND Maschinen-Werte
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setOpaque(false);

        creditLabel = new JLabel("Guthaben: 0 Cent");
        creditLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        creditLabel.setForeground(Color.ORANGE);
        creditLabel.setHorizontalAlignment(SwingConstants.CENTER);

        statusLabel = new JLabel("Bereit.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Füllstandsanzeige
        stockLabel = new JLabel("Lade Maschinendaten...");
        stockLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        stockLabel.setForeground(new Color(173, 216, 230));
        stockLabel.setHorizontalAlignment(SwingConstants.CENTER);

        infoPanel.add(creditLabel);
        infoPanel.add(statusLabel);
        infoPanel.add(stockLabel);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(infoPanel, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
    }

    private JPanel createProductPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_CREAM), " Getränkeauswahl ",
                0, 0, FONT_BOLD, COLOR_CREAM));

        JButton btnBlack = createStyledButton("Kaffee Schwarz (100ct)", "Ein Klassiker.");
        JButton btnMilk = createStyledButton("Kaffee mit Milch (120ct)", "Cremig & Lecker.");

        btnBlack.addActionListener(e -> starteBestellVorgang("Kaffee Schwarz", false, 100));
        btnMilk.addActionListener(e -> starteBestellVorgang("Kaffee Milch", true, 120));

        panel.add(btnBlack);
        panel.add(btnMilk);
        return panel;
    }

    private JPanel createCoinPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_CREAM), " Münzeinwurf ",
                0, 0, FONT_BOLD, COLOR_CREAM));

        int[] coins = {1, 2, 5, 10, 20, 50, 100, 200};
        for (int coin : coins) {
            JButton btn = new JButton((coin >= 100 ? (coin/100)+" €" : coin + " ct"));
            btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btn.setBackground(COLOR_ACCENT);
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                wechsler.muenzeAnnehmen(coin);
                statusLabel.setText("Münze erkannt: " + coin + " Cent");
                updateDisplay();
            });
            panel.add(btn);
        }
        return panel;
    }

    private void createFooter() {
        JPanel footerPanel = new JPanel(new BorderLayout(10, 10));
        footerPanel.setOpaque(false);

        // Ladebalken
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(50, 205, 50));
        progressBar.setBackground(Color.WHITE);
        progressBar.setVisible(false);

        // Log Bereich
        logArea = new JTextArea(6, 40); // Etwas größer für Münzanzeige
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(230, 230, 230));
        JScrollPane scroll = new JScrollPane(logArea);

        // Wartungsbutton
        JButton btnMaint = new JButton("<html><center>🔧<br>Wartung</center></html>");
        btnMaint.setBackground(Color.DARK_GRAY);
        btnMaint.setForeground(Color.WHITE);
        btnMaint.addActionListener(e -> {
            automat.auffuellen();
            log("Service: Bohnen/Milch aufgefüllt & Defekte repariert.");
            JOptionPane.showMessageDialog(this, "Wartung durchgeführt!\nAlles wieder aufgefüllt.", "Service", JOptionPane.INFORMATION_MESSAGE);
            updateDisplay();
        });

        footerPanel.add(progressBar, BorderLayout.NORTH);
        footerPanel.add(scroll, BorderLayout.CENTER);
        footerPanel.add(btnMaint, BorderLayout.EAST);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    // --- LOGIK & ANIMATION ---

    private void starteBestellVorgang(String typ, boolean milch, int preis) {
        // Check 1: Guthaben
        if (wechsler.getAktuellerEinwurf() < preis) {
            JOptionPane.showMessageDialog(this,
                    "Zu wenig Geld! Fehlen: " + (preis - wechsler.getAktuellerEinwurf()) + " Cent.",
                    "Guthaben", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check 2: Maschinenstatus VOR Animation prüfen
        String statusCheck = automat.getStatus();
        if (statusCheck.contains("Defekt: JA")) {
            JOptionPane.showMessageDialog(this, "Automat ist DEFEKT!\nBitte Wartung rufen (Button rechts unten).", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // GUI sperren und Animation starten
        setButtonsEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(0);
        progressBar.setString("Zubereitung läuft... ☕");
        statusLabel.setText("Verarbeite Bestellung...");

        Timer timer = new Timer(20, null);
        timer.addActionListener(e -> {
            int value = progressBar.getValue();
            if (value < 100) {
                progressBar.setValue(value + 1);
                if(value == 20) statusLabel.setText("Mahlwerk aktiv...");
                if(value == 50) statusLabel.setText("Brühvorgang...");
                if(value == 80 && milch) statusLabel.setText("Schäume Milch...");
            } else {
                timer.stop();
                bestellungAbschliessen(typ, milch, preis);
            }
        });
        timer.start();
    }

    private void bestellungAbschliessen(String typ, boolean milch, int preis) {
        String ergebnis = automat.getraenkZubereiten(typ, milch, preis);

        log("--- Transaktion ---");
        log(ergebnis);
        updateDisplay();

        setButtonsEnabled(true);
        progressBar.setVisible(false);
        statusLabel.setText("Bereit für nächste Bestellung.");

        if (ergebnis.contains("FEHLER") || ergebnis.contains("CRITICAL") || ergebnis.contains("nicht möglich")) {
            JOptionPane.showMessageDialog(this, ergebnis, "Problem aufgetreten", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Bitte entnehmen: " + typ + "\n\n" + ergebnis, "Fertig!", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateDisplay() {
        creditLabel.setText("Guthaben: " + wechsler.getAktuellerEinwurf() + " Cent");

        // Status String formatieren
        String rawStatus = automat.getStatus();
        String color = rawStatus.contains("Defekt: JA") ? "red" : "#ADD8E6";
        String icon = rawStatus.contains("Defekt: JA") ? "⚠️ " : "✅ ";
        String formatted = rawStatus.replace("|", "&nbsp;&nbsp;•&nbsp;&nbsp;");

        stockLabel.setText("<html><span style='color:" + color + "'>" + icon + formatted + "</span></html>");

        // --- NEU: Erfüllung der Anforderung "Anzeige der Münzschächte" ---
        // Wir zeigen das im Log an, damit man [4] [5] [2] ... sieht, ohne das Design zu sprengen
        logArea.append(wechsler.getBestandsAnzeige() + "\n");
        // Scroll automatisch nach unten
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private JButton createStyledButton(String text, String subtext) {
        JButton btn = new JButton("<html><center><span style='font-size:14px'>" + text + "</span><br><span style='font-size:10px; color:gray'>" + subtext + "</span></center></html>");
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BROWN, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return btn;
    }

    private void setButtonsEnabled(boolean enabled) {
        setComponentEnabled(mainPanel, enabled);
    }

    private void setComponentEnabled(Container container, boolean enabled) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton) {
                c.setEnabled(enabled);
            } else if (c instanceof Container) {
                setComponentEnabled((Container)c, enabled);
            }
        }
    }

    private void log(String text) {
        logArea.append(text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}