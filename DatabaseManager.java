package de.gfn.coffee;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:smartcoffee.db";

    public DatabaseManager() {
        initialisiereDatenbank();
    }

    private void initialisiereDatenbank() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            // 1. Tabelle Bestellungen
            String sqlBestellungen = "CREATE TABLE IF NOT EXISTS Bestellungen (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "Kaffeeart TEXT," +
                    "Mit_Milch INTEGER," +
                    "Preis INTEGER," +
                    "Zeitstempel DATETIME DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(sqlBestellungen);

            // 2. Tabelle Zahlungen
            String sqlZahlungen = "CREATE TABLE IF NOT EXISTS Zahlungen (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "Bestellung_ID INTEGER," +
                    "Muenztyp INTEGER," +
                    "Anzahl INTEGER," +
                    "FOREIGN KEY(Bestellung_ID) REFERENCES Bestellungen(ID))";
            stmt.execute(sqlZahlungen);

            // 3. Tabelle Münzbestand (NEU) - Pflichtenheft Punkt 3.3 / [122]
            String sqlBestand = "CREATE TABLE IF NOT EXISTS Muenzbestand (" +
                    "Muenztyp INTEGER PRIMARY KEY," +
                    "Anzahl INTEGER)";
            stmt.execute(sqlBestand);

            // Initialisierung der Münz-Tabelle falls leer
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Muenzbestand");
            if (rs.next() && rs.getInt(1) == 0) {
                int[] werte = {200, 100, 50, 20, 10, 5, 2, 1};
                PreparedStatement ps = conn.prepareStatement("INSERT INTO Muenzbestand (Muenztyp, Anzahl) VALUES (?, ?)");
                for (int wert : werte) {
                    ps.setInt(1, wert);
                    ps.setInt(2, 10); // Startbestand: 10 Stück pro Münze
                    ps.executeUpdate();
                }
            }

        } catch (SQLException e) {
            System.err.println("DB Init Fehler: " + e.getMessage());
        }
    }

    // --- BESTELLUNGEN ---

    public int bestellungSpeichern(String kaffeeArt, boolean mitMilch, int preis) {
        String sql = "INSERT INTO Bestellungen(Kaffeeart, Mit_Milch, Preis) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, kaffeeArt);
            pstmt.setInt(2, mitMilch ? 1 : 0);
            pstmt.setInt(3, preis);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void zahlungSpeichern(int bestellungId, Map<Integer, Integer> eingeworfeneMuenzen) {
        String sql = "INSERT INTO Zahlungen(Bestellung_ID, Muenztyp, Anzahl) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map.Entry<Integer, Integer> entry : eingeworfeneMuenzen.entrySet()) {
                if (entry.getValue() > 0) {
                    pstmt.setInt(1, bestellungId);
                    pstmt.setInt(2, entry.getKey());
                    pstmt.setInt(3, entry.getValue());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- NEU: PERSISTENZ METHODEN ---

    // Liest die Gesamtanzahl aller jemals verkauften Tassen aus der DB [71]
    public int getGesamtTassenAnzahl() {
        String sql = "SELECT COUNT(*) FROM Bestellungen";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Lädt den aktuellen Münzbestand aus der DB
    public Map<Integer, Integer> ladeMuenzBestand() {
        Map<Integer, Integer> bestand = new HashMap<>();
        String sql = "SELECT Muenztyp, Anzahl FROM Muenzbestand";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                bestand.put(rs.getInt("Muenztyp"), rs.getInt("Anzahl"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bestand;
    }

    // Aktualisiert den Bestand einer bestimmten Münze
    public void updateMuenzBestand(int muenzTyp, int neueAnzahl) {
        String sql = "UPDATE Muenzbestand SET Anzahl = ? WHERE Muenztyp = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, neueAnzahl);
            pstmt.setInt(2, muenzTyp);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}