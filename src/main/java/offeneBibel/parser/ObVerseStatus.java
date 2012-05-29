package offeneBibel.parser;

public enum ObVerseStatus {
	none, // ordinal == 0
	versUnvollstaendigUebersetzt, // ordinal == 1
	inArbeit, // ordinal == 2
	zuPruefen, // ordinal == 3
	liegtInRohuebersetzungVor, // ordinal == 4
	ueberpruefungAngefordert, // ordinal == 5
	erfuelltDieMeistenKriterien, // ordinal == 6
	erfuellenDieKriterien; // ordinal == 7
}
