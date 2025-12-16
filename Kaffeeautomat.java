package de.gfn.coffee;

public class Kaffeeautomat {
    private boolean defekt = false;
    private int kaffeeBestand = 2000;
    private int milchBestand = 200;
    private int tassenGesamt; // Wird jetzt aus DB geladen

    private final DatabaseManager dbManager;
    private final Muenzwechsler muenzWechsler;

    public Kaffeeautomat(DatabaseManager db, Muenzwechsler mw) {
        this.dbManager = db;
        this.muenzWechsler = mw;

        // LADE HISTORIE: Anforderung [71] - Gesamte Betriebszeit
        this.tassenGesamt = dbManager.getGesamtTassenAnzahl();
    }

    public String getraenkZubereiten(String typ, boolean mitMilch, int preis) {
        if (defekt) return "FEHLER: Automat ist defekt (Mahlwerk). Bitte Service rufen.";

        if (kaffeeBestand < 25) return "FEHLER: Nicht genügend Kaffeebohnen.";
        if (mitMilch && milchBestand < 10) return "FEHLER: Nicht genügend Milchpulver.";

        String bezahlStatus = muenzWechsler.wechselgeldBerechnen(preis);
        if (bezahlStatus.startsWith("Wechseln ist nicht möglich") || bezahlStatus.startsWith("Zu wenig")) {
            return bezahlStatus;
        }

        kaffeeBestand -= 25;
        if (mitMilch) milchBestand -= 10;
        tassenGesamt++;

        // Simulierter Defekt (2%) [90]
        if (Math.random() < 0.02) {
            defekt = true;
            return "CRITICAL: Mahlwerk ist gerade kaputt gegangen! Kaffee wurde nicht fertig.";
        }

        int orderId = dbManager.bestellungSpeichern(typ, mitMilch, preis);
        dbManager.zahlungSpeichern(orderId, muenzWechsler.getEingeworfeneMuenzenMap());

        String msg = "Ihr " + typ + " wird zubereitet. \n" + bezahlStatus;

        // Meldung jede 20. Tasse [88]
        if (tassenGesamt % 20 == 0) {
            msg += "\n(INFO: Dies ist die " + tassenGesamt + ". ausgegebene Tasse!)";
        }

        return msg;
    }

    public String getStatus() {
        return String.format("Bohnen: %dg | Milch: %dg | Tassen: %d | Defekt: %s",
                kaffeeBestand, milchBestand, tassenGesamt, defekt ? "JA" : "NEIN");
    }

    public void auffuellen() {
        this.kaffeeBestand = 2000;
        this.milchBestand = 200;
        this.defekt = false;
    }
}