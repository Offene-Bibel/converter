package offeneBibel.parser;

import java.util.Vector;

import offeneBibel.parser.ObFassungNode.FassungType;

public class ObChapterTag {
	public enum ChapterTagName {
		lesefassunginArbeit,
		studienfassunginArbeit,
		lesefassungZuPruefen,
		studienfassungZuPruefen,
		studienfassungLiegtInRohuebersetzungVor,
		lesefassungErfuelltDieMeistenKriterien,
		studienfassungErfuelltDieMeistenKriterien,
		studienfassungUndLesefassungErfuellenDieKriterien,
		ueberpruefungAngefordert,
		versUnvollstaendigUebersetzt;
		
		public ObVerseStatus getVerseStatus(FassungType fassung) {
			if(fassung == FassungType.lesefassung) {
				if (this == lesefassunginArbeit)
					return ObVerseStatus.inArbeit;
				else if (this == lesefassungZuPruefen)
						return ObVerseStatus.zuPruefen;
				else if (this == studienfassungUndLesefassungErfuellenDieKriterien)
					return ObVerseStatus.erfuelltDieKriterien;
				else if (this == ueberpruefungAngefordert)
					return ObVerseStatus.ueberpruefungAngefordert;
				else if (this == versUnvollstaendigUebersetzt)
					return ObVerseStatus.versUnvollstaendigUebersetzt;
				else
					return ObVerseStatus.none; // should never hit
			}
			else { // studienfassung
				if (this == lesefassunginArbeit)
					return ObVerseStatus.erfuelltDieKriterien;
				else if (this == studienfassunginArbeit)
					return ObVerseStatus.inArbeit;
				else if (this == lesefassungZuPruefen)
						return ObVerseStatus.erfuelltDieKriterien;
				else if (this == studienfassungZuPruefen)
					return ObVerseStatus.zuPruefen;
				else if (this == studienfassungLiegtInRohuebersetzungVor)
					return ObVerseStatus.liegtInRohuebersetzungVor;
				else if (this == studienfassungErfuelltDieMeistenKriterien)
					return ObVerseStatus.erfuelltDieMeistenKriterien;
				else if (this == studienfassungUndLesefassungErfuellenDieKriterien)
					return ObVerseStatus.erfuelltDieKriterien;
				else if (this == ueberpruefungAngefordert)
					return ObVerseStatus.ueberpruefungAngefordert;
				else if (this == versUnvollstaendigUebersetzt)
					return ObVerseStatus.versUnvollstaendigUebersetzt;
				else
					return ObVerseStatus.none; // should never hit
			}
		}
		
		public boolean doesMatchFassung(FassungType fassung) {
			if(fassung == FassungType.lesefassung &&
					(this == ObChapterTag.ChapterTagName.lesefassunginArbeit ||
					this == ObChapterTag.ChapterTagName.lesefassungZuPruefen ||
					this == ObChapterTag.ChapterTagName.lesefassungErfuelltDieMeistenKriterien ||
					this == ObChapterTag.ChapterTagName.studienfassungUndLesefassungErfuellenDieKriterien ||
					this == ObChapterTag.ChapterTagName.ueberpruefungAngefordert))
				return true;
			
			if(fassung == FassungType.studienfassung &&
					(this == ObChapterTag.ChapterTagName.lesefassunginArbeit ||
					this == ObChapterTag.ChapterTagName.lesefassungErfuelltDieMeistenKriterien ||
					this == ObChapterTag.ChapterTagName.studienfassunginArbeit ||
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
	
	private class VerseRange {
    	private int m_startVerse;
    	private int m_stopVerse;
    	
	   public VerseRange(int startVerse, int stopVerse)
	    {
	        m_startVerse = startVerse;
	        m_stopVerse = stopVerse;
	    }
	    public VerseRange(int verse)
	    {
	        m_startVerse = verse;
	        m_stopVerse = verse;
	    }
    	
    	public int getStartVerse() {
    		return m_startVerse;
    	}
    	public int getStopVerse() {
    		return m_stopVerse;
    	}
    	
    	public boolean verseInRange(int verseStart, int verseStop) {
    	    if(verseStart >= m_startVerse && verseStop <=m_stopVerse) {
    	        return true;
    	    }
    	    return false;
    	}
	}
	
	private ChapterTagName m_tag;
	private Vector<VerseRange> m_verseRanges = new Vector<VerseRange>();
	public ObChapterTag(ChapterTagName tag)
	{
		m_tag = tag;
	}
	public ObChapterTag()
	{}
	public ObChapterTag(ChapterTagName tag, int verse)
    {
        m_tag = tag;
        m_verseRanges.add(new VerseRange(verse));
    }
	
	public boolean addVerse(int verse) {
        m_verseRanges.add(new VerseRange(verse));
        return true;
	}
    
    public boolean addVerseRange(int startVerse, int stopVerse) {
        m_verseRanges.add(new VerseRange(startVerse, stopVerse));
        return true;
    }
	
	public ChapterTagName getTag() {
		return m_tag;
	}
	public boolean setTag(ChapterTagName tag) {
		m_tag = tag;
		return true;
	}
	
	public boolean isSpecific() {
		return false == m_verseRanges.isEmpty();
	}
	
	public boolean tagAppliesToVerse(int verseStart, int verseStop) {
	    if(false == isSpecific()) {
	        return true;
	    }
	    else {
	        for(VerseRange range : m_verseRanges) {
	            if(range.verseInRange(verseStart, verseStop)) {
	                return true;
	            }
	        }
	        return false;
	    }
	}
}
