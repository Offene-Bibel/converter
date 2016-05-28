package offeneBibel.parser;

import java.io.Serializable;
import java.util.Vector;

import offeneBibel.parser.ObFassungNode.FassungType;

public class ObChapterTag implements Serializable {
    public enum ChapterTagName {
        studienfassungKannErstelltWerden,
        studienfassunginArbeit,
        ungepruefteStudienfassung,
        zuverlaessigeStudienfassung,
        sehrGuteStudienfassung,
        lesefassungFolgtSpaeter,
        lesefassungKannErstelltWerden,
        ungepruefteLesefassung,
        zuverlaessigeLesefassung,
        sehrGuteLesefassung,
        ueberpruefungAngefordert;
        
        /**
         * Returns a priority of the different statuses.
         * 
         * Usually no competing statuses should be specified. But in case there are
         * they take precedence according to the following priority.
         * The general rule of thumb is: Play safe. Thus the worse the status is,
         * the higher the priority.
         */
        public int getPriority() {
            switch (this) {
            case studienfassungKannErstelltWerden:
                return 5;
            case studienfassunginArbeit:
                return 4;
            case ungepruefteStudienfassung:
                return 3;
            case zuverlaessigeStudienfassung:
                return 2;
            case sehrGuteStudienfassung:
                return 1;
            
            case lesefassungFolgtSpaeter:
                return 5;
            case lesefassungKannErstelltWerden:
                return 4;
            case ungepruefteLesefassung:
                return 3;
            case zuverlaessigeLesefassung:
                return 2;
            case sehrGuteLesefassung:
                return 1;
            
            case ueberpruefungAngefordert:
            default:
                return 0; // never reached
            }
        }

        public ObVerseStatus getVerseStatus(FassungType fassung) {
            if(fassung == FassungType.studienfassung) {
                switch (this) {
                case ungepruefteStudienfassung:
                case studienfassunginArbeit:
                    return ObVerseStatus.ungeprueft;
                case zuverlaessigeStudienfassung:
                    return ObVerseStatus.zuverlaessig;
                case sehrGuteStudienfassung:
                    return ObVerseStatus.sehrGut;
                case studienfassungKannErstelltWerden:
                default:
                    return ObVerseStatus.none;
                }
            }
            else {
                switch (this) {
                case ungepruefteLesefassung:
                    return ObVerseStatus.ungeprueft;
                case zuverlaessigeLesefassung:
                    return ObVerseStatus.zuverlaessig;
                case sehrGuteLesefassung:
                    return ObVerseStatus.sehrGut;
                case lesefassungKannErstelltWerden:
                case lesefassungFolgtSpaeter:
                default:
                    return ObVerseStatus.none;
                }
            }
        }

        public boolean doesMatchFassung(FassungType fassung) {
            if(fassung == FassungType.lesefassung &&
                                           (this == ObChapterTag.ChapterTagName.lesefassungFolgtSpaeter ||
                                            this == ObChapterTag.ChapterTagName.lesefassungKannErstelltWerden ||
                                            this == ObChapterTag.ChapterTagName.ungepruefteLesefassung ||
                                            this == ObChapterTag.ChapterTagName.zuverlaessigeLesefassung ||
                                            this == ObChapterTag.ChapterTagName.sehrGuteLesefassung ||
                                            this == ObChapterTag.ChapterTagName.ueberpruefungAngefordert))
                return true;
            if(fassung == FassungType.studienfassung &&
                                           (this == ObChapterTag.ChapterTagName.studienfassungKannErstelltWerden ||
                                            this == ObChapterTag.ChapterTagName.studienfassunginArbeit ||
                                            this == ObChapterTag.ChapterTagName.ungepruefteStudienfassung ||
                                            this == ObChapterTag.ChapterTagName.zuverlaessigeStudienfassung ||
                                            this == ObChapterTag.ChapterTagName.sehrGuteStudienfassung ||
                                            this == ObChapterTag.ChapterTagName.ueberpruefungAngefordert))
                return true;

            return false;
        }
    }

    private class VerseRange implements Serializable {
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
