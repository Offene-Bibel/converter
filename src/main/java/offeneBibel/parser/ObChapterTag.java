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

        /**
         * Returns a priority of the different stati.
         * The higher the priority the more dominant that status is.
         * So if you are unsure which status applies, pick the one
         * with the higher status.
         * 
         * The worse the status is, the higher the status.
         */
        public int getPriority() {
            switch (this) {
            case lesefassunginArbeit:
                return 4;
            case studienfassunginArbeit:
                return 8;
            case lesefassungZuPruefen:
                return 2;
            case studienfassungZuPruefen:
                return 5;
            case studienfassungLiegtInRohuebersetzungVor:
                return 7;
            case lesefassungErfuelltDieMeistenKriterien:
                return 3;
            case studienfassungErfuelltDieMeistenKriterien:
                return 6;
            case studienfassungUndLesefassungErfuellenDieKriterien:
                return 1;
            case ueberpruefungAngefordert:
                return 10;
            case versUnvollstaendigUebersetzt:
                return 9;
            default:
                return 0; // never reached
            }
        }

        public ObVerseStatus getVerseStatus(FassungType fassung) {
            switch (this) {
            case lesefassunginArbeit:
                if(fassung == FassungType.lesefassung) return ObVerseStatus.inArbeit;
                else return ObVerseStatus.erfuelltDieKriterien;
            case studienfassunginArbeit:
                if(fassung == FassungType.lesefassung) return ObVerseStatus.none;
                else return ObVerseStatus.inArbeit;
            case lesefassungZuPruefen:
                if(fassung == FassungType.lesefassung) return ObVerseStatus.zuPruefen;
                else return ObVerseStatus.erfuelltDieKriterien;
            case studienfassungZuPruefen:
                if(fassung == FassungType.lesefassung) return ObVerseStatus.none;
                else return ObVerseStatus.zuPruefen;
            case studienfassungLiegtInRohuebersetzungVor:
                if(fassung == FassungType.lesefassung) return ObVerseStatus.none;
                else return ObVerseStatus.liegtInRohuebersetzungVor;
            case lesefassungErfuelltDieMeistenKriterien:
                if(fassung == FassungType.lesefassung) return ObVerseStatus.erfuelltDieMeistenKriterien;
                else return ObVerseStatus.erfuelltDieKriterien;
            case studienfassungErfuelltDieMeistenKriterien:
                if(fassung == FassungType.lesefassung) return ObVerseStatus.none;
                else return ObVerseStatus.erfuelltDieMeistenKriterien;
            case studienfassungUndLesefassungErfuellenDieKriterien:
                if(fassung == FassungType.lesefassung) return ObVerseStatus.erfuelltDieKriterien;
                else return ObVerseStatus.erfuelltDieKriterien;
            case ueberpruefungAngefordert:
                if(fassung == FassungType.lesefassung) return ObVerseStatus.ueberpruefungAngefordert;
                else return ObVerseStatus.ueberpruefungAngefordert;
            case versUnvollstaendigUebersetzt:
                if(fassung == FassungType.lesefassung) return ObVerseStatus.versUnvollstaendigUebersetzt;
                else return ObVerseStatus.versUnvollstaendigUebersetzt;
            default:
                return ObVerseStatus.none; // never hits
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
