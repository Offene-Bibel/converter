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
     * 1 = worst
     * 4 = best
     */
    public int quality() {
        switch(this) {
        case none:
        case versUnvollstaendigUebersetzt:
        case inArbeit:
            return 1;
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
}
