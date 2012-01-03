package offeneBibel.parser;

public class ObChapterTag {
	public enum ChapterTagName {
		lesefassunginArbeit,
		studienfassunginArbeit,
		lesefassungZuPruefen,
		studienfassungZuPruefen,
		studienfassungLiegtInRohuebersetzungVor,
		studienfassungErfuelltDieMeistenKriterien,
		studienfassungUndLesefassungErfuellenDieKriterien,
		ueberpruefungAngefordert,
		versUnvollstaendigUebersetzt,
	}
	
	private ChapterTagName m_tag;
	private int m_startVerse;
	private int m_stopVerse;
	public ObChapterTag(ChapterTagName tag)
	{
		m_tag = tag;
		m_startVerse = 0;
		m_stopVerse = 0;
	}
	public ObChapterTag(ChapterTagName tag, int startVerse, int stopVerse)
	{
		m_tag = tag;
		m_startVerse = startVerse;
		m_stopVerse = stopVerse;
	}
	public ChapterTagName getTag() {
		return m_tag;
	}
	public boolean setTag(ChapterTagName tag) {
		m_tag = tag;
		return true;
	}
	public int getStartVerse() {
		return m_startVerse;
	}
	public int getStopVerse() {
		return m_stopVerse;
	}
}
