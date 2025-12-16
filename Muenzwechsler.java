package de.gfn.coffee;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Muenzwechsler {
    // Werte in Cent, aber sortiert für die Verarbeitung (Groß nach Klein für Algorithmus)
    // Index-Mapping wird über Logic gelöst.
    private final int[] muenzWerte = {200, 100, 50, 20, 10, 5, 2, 1};

    // Bestand pro Münzart (Index korrespondiert mit muenzWerte)
    private int[] muenzBestand;

    private int aktuellerEinwurf = 0;
    private Map<Integer, Integer> eingeworfeneMuenzenMap = new HashMap<>();

    private DatabaseManager dbManager;

    public Muenzwechsler(DatabaseManager db) {
        this.dbManager = db;
        this.muenzBestand = new int[muenzWerte.length];
        ladeBestandAusDB();
    }

    // Methode: datenAbrufen / ladeBestandAusDB
    private void ladeBestandAusDB() {
        Map<Integer, Integer> dbBestand = dbManager.ladeMuenzBestand();
        for (int i = 0; i < muenzWerte.length; i++) {
            muenzBestand[i] = dbBestand.getOrDefault(muenzWerte[i], 0);
        }
    }

    // Methode: muenzeAnnehmen
    public void muenzeAnnehmen(int wertInCent) {
        aktuellerEinwurf += wertInCent;
        eingeworfeneMuenzenMap.put(wertInCent, eingeworfeneMuenzenMap.getOrDefault(wertInCent, 0) + 1);

        // Münze direkt in den Bestand aufnehmen (bestandAktualisieren)
        bestandAktualisieren(wertInCent, 1);
    }

    // Methode: wechselgeldBerechnen
    public String wechselgeldBerechnen(int preis) {
        int rueckgabeBetrag = aktuellerEinwurf - preis;

        // Fall 1: Zu wenig Geld (Implizit, wird oft von GUI schon gefangen, aber hier zur Sicherheit)
        if (rueckgabeBetrag < 0) return "Zu wenig Geld eingeworfen.";

        // Fall 2: Passend gezahlt [Anforderung 99, 102]
        if (rueckgabeBetrag == 0) {
            resetEinwurf();
            return "Zahlvorgang erfolgreich.";
        }

        // Fall 3: Wechselgeld nötig - Prüfen ob vorhanden [Anforderung 100]
        Map<Integer, Integer> wechselgeld = new HashMap<>();
        int rest = rueckgabeBetrag;
        int[] tempBestand = muenzBestand.clone();

        // Greedy Algorithmus (Große Münzen zuerst für optimale Ausgabe)
        for (int i = 0; i < muenzWerte.length; i++) {
            int wert = muenzWerte[i];
            while (rest >= wert && tempBestand[i] > 0) {
                rest -= wert;
                tempBestand[i]--;
                wechselgeld.put(wert, wechselgeld.getOrDefault(wert, 0) + 1);
            }
        }

        if (rest > 0) {
            // Fall 4: Wechseln nicht möglich [Anforderung 101, 104]
            // Eingeworfene Münzen zurückbuchen (da Transaktion abgebrochen)
            rueckbuchenEingeworfen();
            resetEinwurf();
            return "Wechseln ist nicht möglich. Entnehmen Sie Ihre Münzen.";
        } else {
            // Fall 5: Erfolgreich mit Wechselgeld [Anforderung 103]
            // Bestand wirklich reduzieren (bestandAktualisieren)
            for (Map.Entry<Integer, Integer> entry : wechselgeld.entrySet()) {
                bestandAktualisieren(entry.getKey(), -entry.getValue());
            }
            resetEinwurf();
            return "Zahlvorgang erfolgreich entnehmen Sie das Wechselgeld.\n(" + formatWechselgeld(wechselgeld) + ")";
        }
    }

    // Hilfsmethode: bestandAktualisieren
    private void bestandAktualisieren(int wert, int anzahlAenderung) {
        for(int i=0; i<muenzWerte.length; i++) {
            if(muenzWerte[i] == wert) {
                muenzBestand[i] += anzahlAenderung;
                // Sofort in DB speichern
                dbManager.updateMuenzBestand(wert, muenzBestand[i]);
                break;
            }
        }
    }

    private void rueckbuchenEingeworfen() {
        for (Map.Entry<Integer, Integer> entry : eingeworfeneMuenzenMap.entrySet()) {
            bestandAktualisieren(entry.getKey(), -entry.getValue());
        }
    }

    private String formatWechselgeld(Map<Integer, Integer> wechselgeld) {
        // Sortierte Ausgabe für schöneren Text
        TreeMap<Integer, Integer> sorted = new TreeMap<>(wechselgeld);
        StringBuilder sb = new StringBuilder();
        sorted.descendingMap().forEach((wert, anzahl) -> sb.append(anzahl).append("x ").append(wert).append("ct, "));
        String res = sb.toString();
        return res.isEmpty() ? "" : res.substring(0, res.length() - 2);
    }

    // ANFORDERUNG: Anzeige beginnend beim kleinsten Münzschacht (1 Ct ... 2 €) [96, 97]
    public String getBestandsAnzeige() {
        StringBuilder sb = new StringBuilder("Münzbestand: ");
        // Wir iterieren Rückwärts durch muenzWerte (da das Array 200...1 ist),
        // um 1...200 auszugeben.
        for (int i = muenzWerte.length - 1; i >= 0; i--) {
            sb.append("[").append(muenzBestand[i]).append("] ");
        }
        return sb.toString();
    }

    public int getAktuellerEinwurf() {
        return aktuellerEinwurf;
    }

    public Map<Integer, Integer> getEingeworfeneMuenzenMap() {
        return eingeworfeneMuenzenMap;
    }

    private void resetEinwurf() {
        aktuellerEinwurf = 0;
        eingeworfeneMuenzenMap.clear();
    }
}