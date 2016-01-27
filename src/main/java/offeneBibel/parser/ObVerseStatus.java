package offeneBibel.parser;

public enum ObVerseStatus {
    none, // ordinal == 0
    versUnvollstaendigUebersetzt, // ordinal == 1
    inArbeit, // ordinal == 2
    zuPruefen, // ordinal == 3
    liegtInRohuebersetzungVor, // ordinal == 4
    ueberpruefungAngefordert, // ordinal == 5
    erfuelltDieMeistenKriterien, // ordinal == 6
    erfuelltDieKriterien; // ordinal == 7

    /**
     * Returns the quality of this tag.
     * 1 = non existant or not complete
     * 2 = raw
     * 3 = most criteria met
     * 4 = all criteria met
     */
    public int quality() {
        switch(this) {
        case none:
        case versUnvollstaendigUebersetzt:
            return 1;
        case inArbeit:
        case zuPruefen:
        case liegtInRohuebersetzungVor:
        case ueberpruefungAngefordert:
            return 2;
        case erfuelltDieMeistenKriterien:
            return 3;
        case erfuelltDieKriterien:
            return 4;
        default:
            return 0; //never reached
        }
    }

    public String toHumanReadableString() {
        switch(this) {
        case none:
            return "Unbekannt";
        case versUnvollstaendigUebersetzt:
            return "Unvollständig übersetzt";
        case inArbeit:
            return "in Arbeit";
        case zuPruefen:
            return "zu prüfen";
        case liegtInRohuebersetzungVor:
            return "liegt in Rohübersetzung vor";
        case ueberpruefungAngefordert:
            return "Überprüfung angefordert";
        case erfuelltDieMeistenKriterien:
            return "erfüllt die meisten Kriterien";
        case erfuelltDieKriterien:
            return "erfüllt die Kriterien";
        default: throw new IllegalStateException("No translation for "+this);
        }
    }

    public String getExportStatusString() {
        switch(this) {
        case none:
        case inArbeit:
        case liegtInRohuebersetzungVor:
            return "Ungeprüft";
        case erfuelltDieMeistenKriterien:
            return "Zuverlässig";
        case erfuelltDieKriterien:
            return "Sehr gut";
        default: throw new IllegalStateException("Unhandled status "+this);
        }
    }
}
