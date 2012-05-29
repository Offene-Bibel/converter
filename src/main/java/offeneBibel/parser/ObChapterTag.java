package offeneBibel.parser;

import offeneBibel.parser.ObFassungNode.FassungType;

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
		versUnvollstaendigUebersetzt;
		
		public ObVerseStatus getVerseStatus() {
			if (this == lesefassunginArbeit || this == studienfassunginArbeit)
				return ObVerseStatus.inArbeit;
			else if (this == lesefassungZuPruefen || this == studienfassungZuPruefen)
					return ObVerseStatus.inArbeit;
			else if (this == studienfassungLiegtInRohuebersetzungVor)
				return ObVerseStatus.liegtInRohuebersetzungVor;
			else if (this == studienfassungErfuelltDieMeistenKriterien)
				return ObVerseStatus.erfuelltDieMeistenKriterien;
			else if (this == studienfassungUndLesefassungErfuellenDieKriterien)
				return ObVerseStatus.erfuellenDieKriterien;
			else if (this == ueberpruefungAngefordert)
				return ObVerseStatus.ueberpruefungAngefordert;
			else if (this == versUnvollstaendigUebersetzt)
				return ObVerseStatus.versUnvollstaendigUebersetzt;
			else
				return ObVerseStatus.none; // should never hit
		}
		
		public boolean doesMatchFassung(FassungType fassung) {
			if(fassung == FassungType.lesefassung &&
					(this == ObChapterTag.ChapterTagName.lesefassunginArbeit ||
					this == ObChapterTag.ChapterTagName.lesefassungZuPruefen ||
					this == ObChapterTag.ChapterTagName.studienfassungUndLesefassungErfuellenDieKriterien ||
					this == ObChapterTag.ChapterTagName.ueberpruefungAngefordert))
				return true;
			
			if(fassung == FassungType.studienfassung &&
					(this == ObChapterTag.ChapterTagName.studienfassunginArbeit ||
					this == ObChapterTag.ChapterTagName.studienfassungZuPruefen ||
					this == ObChapterTag.ChapterTagName.studienfassungLiegtInRohuebersetzungVor ||
					this == ObChapterTag.ChapterTagName.studienfassungErfuelltDieMeistenKriterien ||
					this == ObChapterTag.ChapterTagName.studienfassungUndLesefassungErfuellenDieKriterien ||
					this == ObChapterTag.ChapterTagName.ueberpruefungAngefordert ||
					this == ObChapterTag.ChapterTagName.versUnvollstaendigUebersetzt))
				return true;
			
			return false;
		}
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
	
	public boolean isSpecific() {
		return getStartVerse() != 0 && getStopVerse() != 0;
	}

}
