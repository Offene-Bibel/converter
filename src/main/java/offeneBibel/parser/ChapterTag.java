package offeneBibel.parser;

import java.io.Serializable;
import java.util.ArrayList;

import offeneBibel.parser.FassungNode.FassungType;

public class ChapterTag implements Serializable {
    private static final long serialVersionUID = 1L;

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

        public VerseStatus getVerseStatus(FassungType fassung) {
            if(fassung == FassungType.studienfassung) {
                switch (this) {
                case ungepruefteStudienfassung:
                case studienfassunginArbeit:
                    return VerseStatus.ungeprueft;
                case zuverlaessigeStudienfassung:
                    return VerseStatus.zuverlaessig;
                case sehrGuteStudienfassung:
                    return VerseStatus.sehrGut;
                case studienfassungKannErstelltWerden:
                default:
                    return VerseStatus.none;
                }
            }
            else {
                switch (this) {
                case ungepruefteLesefassung:
                    return VerseStatus.ungeprueft;
                case zuverlaessigeLesefassung:
                    return VerseStatus.zuverlaessig;
                case sehrGuteLesefassung:
                    return VerseStatus.sehrGut;
                case lesefassungKannErstelltWerden:
                case lesefassungFolgtSpaeter:
                default:
                    return VerseStatus.none;
                }
            }
        }

        public boolean doesMatchFassung(FassungType fassung) {
            if(fassung == FassungType.lesefassung &&
                                           (this == ChapterTag.ChapterTagName.lesefassungFolgtSpaeter ||
                                            this == ChapterTag.ChapterTagName.lesefassungKannErstelltWerden ||
                                            this == ChapterTag.ChapterTagName.ungepruefteLesefassung ||
                                            this == ChapterTag.ChapterTagName.zuverlaessigeLesefassung ||
                                            this == ChapterTag.ChapterTagName.sehrGuteLesefassung ||
                                            this == ChapterTag.ChapterTagName.ueberpruefungAngefordert))
                return true;
            if(fassung == FassungType.studienfassung &&
                                           (this == ChapterTag.ChapterTagName.studienfassungKannErstelltWerden ||
                                            this == ChapterTag.ChapterTagName.studienfassunginArbeit ||
                                            this == ChapterTag.ChapterTagName.ungepruefteStudienfassung ||
                                            this == ChapterTag.ChapterTagName.zuverlaessigeStudienfassung ||
                                            this == ChapterTag.ChapterTagName.sehrGuteStudienfassung ||
                                            this == ChapterTag.ChapterTagName.ueberpruefungAngefordert))
                return true;

            return false;
        }
    }

    private class VerseRange implements Serializable {
        private static final long serialVersionUID = 1L;
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

        public boolean verseInRange(int verseStart, int verseStop) {
            if(verseStart >= m_startVerse && verseStop <=m_stopVerse) {
                return true;
            }
            return false;
        }
    }

    private ChapterTagName m_tag;
    private ArrayList<VerseRange> m_verseRanges = new ArrayList<>();
    public ChapterTag(ChapterTagName tag)
    {
        m_tag = tag;
    }
    public ChapterTag()
    {}
    public ChapterTag(ChapterTagName tag, int verse)
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
