package offeneBibel.osisExporter;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObChapterNode;
import offeneBibel.parser.ObChapterTag;
import offeneBibel.parser.ObChapterTag.ChapterTagName;
import offeneBibel.parser.ObFassungNode;
import offeneBibel.parser.ObFassungNode.FassungType;
import offeneBibel.parser.ObNoteNode;
import offeneBibel.parser.ObParallelPassageNode;
import offeneBibel.parser.ObTextNode;
import offeneBibel.parser.ObVerseNode;
import offeneBibel.parser.ObVerseStatus;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

public class ObWebViewerVisitor extends DifferentiatingVisitor<ObAstNode> implements IVisitor<ObAstNode>
{
    private int m_studienFassungStatus = 1;
    private int m_leseFassungStatus = 1;
    private String m_studienFassung = null;
    private String m_leseFassung = null;
    private StringBuilder m_currentFassung = new StringBuilder();
    private boolean m_currentFassungContainsVerses = false;

    private int m_quoteCounter = 0;

    /**
     * Started by <poem> and stopped by </poem>.
     * If in poem mode, all text nodes will replace \n with </l><l>.
     * A <l> is added at the beginning of a <poem> area also, but only if
     * there actually was some text. A </l> is added after the last text
     * block too.
     */
    private boolean m_poemMode;

    /** Skip the current verse if true. */
    private boolean m_skipVerse = false;

    private ObVerseStatus m_requiredTranslationStatus;

    private NoteIndexCounter m_noteIndexCounter;

    public ObWebViewerVisitor(ObVerseStatus requiredTranslationStatus)
    {
        m_noteIndexCounter = new NoteIndexCounter();
        m_requiredTranslationStatus = requiredTranslationStatus;
    }

    @Override
    public void visitBeforeDefault(ObAstNode node) throws Throwable
    {
        if(node.getNodeType() == ObAstNode.NodeType.fassung) {
            m_noteIndexCounter.reset();
            m_currentFassung = new StringBuilder();
            m_currentFassungContainsVerses = false;
        }

        else if(node.getNodeType() == ObAstNode.NodeType.quote) {
            if(m_skipVerse) return;
            if(m_quoteCounter>0)
            {
                m_quoteCounter++;
                m_currentFassung.append("»");
            }
            else
            {
                QuoteSearcher quoteSearcher = new QuoteSearcher();
                node.host(quoteSearcher, false);
                if(quoteSearcher.foundQuote == false)
                    m_currentFassung.append("„");
                else {
                    m_quoteCounter++;
                    m_currentFassung.append("„");
                }
            }
        }

        else if(node.getNodeType() == ObAstNode.NodeType.alternative) {
            if(m_skipVerse) return;
            m_currentFassung.append("<span class=\"alternative\">(");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.insertion) {
            if(m_skipVerse) return;
            m_currentFassung.append("<span class=\"insertion-start\">[</span><span class=\"insertion\">");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.omission) {
            if(m_skipVerse) return;
            m_currentFassung.append("<span class=\"omission\">{");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.heading) {
            m_currentFassung.append("<h3>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.note) {
            if(m_skipVerse) return;
            m_currentFassung.append("<a href=\"#\" data-toggle=\"tooltip\" data-placement=\"auto bottom\" title=\"");
        }
    }

    @Override
    public void visitDefault(ObAstNode node) throws Throwable
    {
        if(node.getNodeType() == ObAstNode.NodeType.text) {
            if(m_skipVerse) return;
            ObTextNode text = (ObTextNode)node;
            String textString = text.getText();

            if(m_poemMode && ! node.isDescendantOf(ObNoteNode.class)) {
                textString.replaceAll("\n", "<br/>");
            }

            textString = textString.replaceAll("&", "&amp;");

            m_currentFassung.append(textString);
        }

        else if(node.getNodeType() == ObAstNode.NodeType.verse) {
            ObVerseNode verse = (ObVerseNode)node;
            if(verse.getStatus().ordinal() >= m_requiredTranslationStatus.ordinal()) {
                m_currentFassungContainsVerses = true;
                m_skipVerse = false;
                m_currentFassung.append("<span class=\"verse_num\">" + verse.getNumber() + "</span>");
            }
            else {
                // skip this verse
                m_skipVerse = true;
            }
        }

        else if(node.getNodeType() == ObAstNode.NodeType.parallelPassage) {
            if(m_skipVerse) return;
            ObParallelPassageNode passage = (ObParallelPassageNode)node;
            m_currentFassung.append("<a href=\"" + passage.getOsisBookId() + "_" + passage.getChapter() + "?verse=" + passage.getStartVerse() +
                                            "\" data-toggle=\"tooltip\" data-placement=\"auto bottom\" title=\"" +
                                            passage.getOsisBookId() + " " + passage.getChapter() + ", " + passage.getStartVerse() + "\">℘</a>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.poemStart) {
            m_poemMode = true;
        }

        else if(node.getNodeType() == ObAstNode.NodeType.poemStop) {
            m_poemMode = false;
        }

        else if(node.getNodeType() == ObAstNode.NodeType.chapter) {
            ObChapterNode chapterNode = (ObChapterNode)node;
            ChapterTagName tagName = null;
            for (ObChapterTag tag  : chapterNode.getChapterTags()) {
                if(false == tag.isSpecific()) {
                    if(tagName == null || tagName.getPriority() < tag.getTag().getPriority()) {
                        tagName = tag.getTag();
                    }
                }
            }
            if(tagName != null) {
                m_leseFassungStatus = tagName.getVerseStatus(FassungType.lesefassung).quality();
                m_studienFassungStatus = tagName.getVerseStatus(FassungType.studienfassung).quality();
            }
        }
    }

    @Override
    public void visitAfterDefault(ObAstNode node) throws Throwable
    {

        if(node.getNodeType() == ObAstNode.NodeType.fassung) {
            ObFassungNode fassung = (ObFassungNode)node;

            // prevent empty chapters
            if(m_currentFassungContainsVerses == false) {
                m_currentFassung = null;
            }

            if(fassung.getFassung() == ObFassungNode.FassungType.lesefassung) {
                m_leseFassung = m_currentFassung == null ? null : m_currentFassung.toString();
            }
            else {
                m_studienFassung = m_currentFassung == null ? null : m_currentFassung.toString();
            }
        }

        else if(node.getNodeType() == ObAstNode.NodeType.quote) {
            if(m_skipVerse) return;
            if(m_quoteCounter>0)
                m_quoteCounter--;
            if(m_quoteCounter>0)
                m_currentFassung.append("«");
            else
                m_currentFassung.append("“");

        }

        else if(node.getNodeType() == ObAstNode.NodeType.alternative) {
            if(m_skipVerse) return;
            m_currentFassung.append(")</span>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.insertion) {
            if(m_skipVerse) return;
            m_currentFassung.append("</span><span class=\"insertion-end\">]</span>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.omission) {
            if(m_skipVerse) return;
            m_currentFassung.append("}</span>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.heading) {
            m_currentFassung.append("</h3>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.note) {
            if(m_skipVerse) return;
            m_currentFassung.append("\">〈" + m_noteIndexCounter.getNextNoteString() + "〉</a>");
        }
    }

    /**
     * @return Studienfassung string or null if there was no Studienfassung in this text.
     */
    public String getStudienFassung() {
        return m_studienFassung;
    }

    /**
     * @return Lesefassung string or null if there was no Lesefassung in this text.
     */
    public String getLeseFassung() {
        return m_leseFassung;
    }

    /**
     * @return Status of the Studienfassung according to ObVerseStatus.quality().
     */
    public int getStudienFassungQuality() {
        return m_studienFassungStatus;
    }

    /**
     * @return Status of the Lesefassung according to ObVerseStatus.quality().
     */
    public int getLeseFassungQuality() {
        return m_leseFassungStatus;
    }

    class QuoteSearcher implements IVisitor<ObAstNode>
    {
        public boolean foundQuote = false;
        @Override
        public void visit(ObAstNode node) throws Throwable {
            if(node.getNodeType() == ObAstNode.NodeType.quote)
                foundQuote = true;
        }
        @Override
        public void visitBefore(ObAstNode hostNode) throws Throwable {}
        @Override
        public void visitAfter(ObAstNode hostNode) throws Throwable {}
    }

    class NoteIndexCounter {
        private String m_noteIndexCounter;

        public NoteIndexCounter()
        {
            m_noteIndexCounter = "a";
        }

        public void reset()
        {
            m_noteIndexCounter = "a";
        }

        public String getNextNoteString()
        {
            String result = m_noteIndexCounter;
            incrementNoteCounter();
            return result;
        }

        private void incrementNoteCounter()
        {
            int walker = m_noteIndexCounter.length() - 1;

            while(walker >=0) {
                String preWalkerString = walker>0 ? m_noteIndexCounter.substring(0, walker) : "";
                String postWalkerString = walker<m_noteIndexCounter.length()-1 ? m_noteIndexCounter.substring(walker+1, m_noteIndexCounter.length()) : "";
                if(m_noteIndexCounter.charAt(walker) < 'z') {
                    m_noteIndexCounter = preWalkerString + (char)(m_noteIndexCounter.charAt(walker)+1) + postWalkerString;
                    break;
                }
                else {
                    m_noteIndexCounter = preWalkerString + 'a' + postWalkerString;
                }
                --walker;
            }
            if(walker == -1) {
                m_noteIndexCounter = "a" + m_noteIndexCounter;
            }
        }
    }
}
