package offeneBibel.parser;

public enum VerseStatus {
    none(0, "Unbekannt"),
    ungeprueft(1, "Ungeprüft"),
    zuverlaessig(2, "Zuverlässig"),
    sehrGut(3, "Sehr gut");
    
    /**
     * To allow comparison of stati.
     */
    private final int quality;
    private final String stringRepresentation;
    
    private VerseStatus(int quality, String humanReadableString) {
        this.quality = quality;
        this.stringRepresentation = humanReadableString;
    }
    
    public int quality() {
        return quality;
    }

    public String getExportStatusString() {
        return stringRepresentation;
    }
    
    public String getHumanReadableString() {
        return stringRepresentation;
    }
}
