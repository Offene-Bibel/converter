package offeneBibel.osisExporter;

import offeneBibel.parser.AstNode;
import offeneBibel.parser.ChapterNode;
import offeneBibel.parser.ChapterTag;
import offeneBibel.parser.ChapterTag.ChapterTagName;
import offeneBibel.parser.FassungNode;
import offeneBibel.parser.FassungNode.FassungType;
import offeneBibel.parser.NoteNode;
import offeneBibel.parser.ParallelPassageNode;
import offeneBibel.parser.TextNode;
import offeneBibel.parser.VerseNode;
import offeneBibel.parser.VerseStatus;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

public class WebViewerVisitor extends DifferentiatingVisitor<AstNode> implements IVisitor<AstNode>
{
    private VerseStatus studienFassungStatus = VerseStatus.none;
    private VerseStatus leseFassungStatus = VerseStatus.none;
    private String studienFassung = null;
    private String leseFassung = null;
    private StringBuilder currentFassung = new StringBuilder();
    private boolean currentFassungContainsVerses = false;

    private int quoteCounter = 0;

    /**
     * Started by <poem> and stopped by </poem>.
     * If in poem mode, all text nodes will replace \n with </l><l>.
     * A <l> is added at the beginning of a <poem> area also, but only if
     * there actually was some text. A </l> is added after the last text
     * block too.
     */
    private boolean poemMode;

    /** Skip the current verse if true. */
    private boolean skipVerse = false;

    private VerseStatus requiredTranslationStatus;

    private NoteIndexCounter noteIndexCounter;

    public WebViewerVisitor(VerseStatus requiredTranslationStatus)
    {
        this.noteIndexCounter = new NoteIndexCounter();
        this.requiredTranslationStatus = requiredTranslationStatus;
    }

    @Override
    public void visitBeforeDefault(AstNode node) throws Throwable
    {
        if(node.getNodeType() == AstNode.NodeType.fassung) {
            noteIndexCounter.reset();
            currentFassung = new StringBuilder();
            currentFassungContainsVerses = false;
        }

        else if(node.getNodeType() == AstNode.NodeType.quote) {
            if(skipVerse) return;
            if(quoteCounter>0)
            {
                quoteCounter++;
                currentFassung.append("»");
            }
            else
            {
                QuoteSearcher quoteSearcher = new QuoteSearcher();
                node.host(quoteSearcher, false);
                if(quoteSearcher.foundQuote == false)
                    currentFassung.append("„");
                else {
                    quoteCounter++;
                    currentFassung.append("„");
                }
            }
        }

        else if(node.getNodeType() == AstNode.NodeType.alternative) {
            if(skipVerse) return;
            currentFassung.append("<span class=\"alternative\">(");
        }

        else if(node.getNodeType() == AstNode.NodeType.insertion) {
            if(skipVerse) return;
            currentFassung.append("<span class=\"insertion-start\">[</span><span class=\"insertion\">");
        }

        else if(node.getNodeType() == AstNode.NodeType.omission) {
            if(skipVerse) return;
            currentFassung.append("<span class=\"omission\">{");
        }

        else if(node.getNodeType() == AstNode.NodeType.heading) {
            currentFassung.append("<h3>");
        }

        else if(node.getNodeType() == AstNode.NodeType.note) {
            if(skipVerse) return;
            currentFassung.append("<a href=\"#\" data-toggle=\"tooltip\" data-placement=\"auto bottom\" title=\"");
        }
    }

    @Override
    public void visitDefault(AstNode node) throws Throwable
    {
        if(node.getNodeType() == AstNode.NodeType.text) {
            if(skipVerse) return;
            TextNode text = (TextNode)node;
            String textString = text.getText();

            if(poemMode && ! node.isDescendantOf(NoteNode.class)) {
                textString.replaceAll("\n", "<br/>");
            }

            textString = textString.replaceAll("&", "&amp;");

            currentFassung.append(textString);
        }

        else if(node.getNodeType() == AstNode.NodeType.verse) {
            VerseNode verse = (VerseNode)node;
            if(verse.getStatus().ordinal() >= requiredTranslationStatus.ordinal()) {
                currentFassungContainsVerses = true;
                skipVerse = false;
                currentFassung.append("<span class=\"verse_num\">" + verse.getNumber() + "</span>");
            }
            else {
                // skip this verse
                skipVerse = true;
            }
        }

        else if(node.getNodeType() == AstNode.NodeType.parallelPassage) {
            if(skipVerse) return;
            ParallelPassageNode passage = (ParallelPassageNode)node;
            currentFassung.append("<a href=\"" + passage.getOsisBookId() + "_" + passage.getChapter() + "?verse=" + passage.getStartVerse() +
                                            "\" data-toggle=\"tooltip\" data-placement=\"auto bottom\" title=\"" +
                                            passage.getOsisBookId() + " " + passage.getChapter() + ", " + passage.getStartVerse() + "\">℘</a>");
        }

        else if(node.getNodeType() == AstNode.NodeType.poemStart) {
            poemMode = true;
        }

        else if(node.getNodeType() == AstNode.NodeType.poemStop) {
            poemMode = false;
        }

        else if(node.getNodeType() == AstNode.NodeType.chapter) {
            ChapterNode chapterNode = (ChapterNode)node;
            ChapterTagName tagName = null;
            for (ChapterTag tag  : chapterNode.getChapterTags()) {
                if(false == tag.isSpecific()) {
                    if(tagName == null || tagName.getPriority() < tag.getTag().getPriority()) {
                        tagName = tag.getTag();
                    }
                }
            }
            if(tagName != null) {
                leseFassungStatus = tagName.getVerseStatus(FassungType.lesefassung);
                studienFassungStatus = tagName.getVerseStatus(FassungType.studienfassung);
            }
        }
    }

    @Override
    public void visitAfterDefault(AstNode node) throws Throwable
    {

        if(node.getNodeType() == AstNode.NodeType.fassung) {
            FassungNode fassung = (FassungNode)node;

            // prevent empty chapters
            if(currentFassungContainsVerses == false) {
                currentFassung = null;
            }

            if(fassung.getFassung() == FassungNode.FassungType.lesefassung) {
                leseFassung = currentFassung == null ? null : currentFassung.toString();
            }
            else {
                studienFassung = currentFassung == null ? null : currentFassung.toString();
            }
        }

        else if(node.getNodeType() == AstNode.NodeType.quote) {
            if(skipVerse) return;
            if(quoteCounter>0)
                quoteCounter--;
            if(quoteCounter>0)
                currentFassung.append("«");
            else
                currentFassung.append("“");

        }

        else if(node.getNodeType() == AstNode.NodeType.alternative) {
            if(skipVerse) return;
            currentFassung.append(")</span>");
        }

        else if(node.getNodeType() == AstNode.NodeType.insertion) {
            if(skipVerse) return;
            currentFassung.append("</span><span class=\"insertion-end\">]</span>");
        }

        else if(node.getNodeType() == AstNode.NodeType.omission) {
            if(skipVerse) return;
            currentFassung.append("}</span>");
        }

        else if(node.getNodeType() == AstNode.NodeType.heading) {
            currentFassung.append("</h3>");
        }

        else if(node.getNodeType() == AstNode.NodeType.note) {
            if(skipVerse) return;
            currentFassung.append("\">〈" + noteIndexCounter.getNextNoteString() + "〉</a>");
        }
    }

    /**
     * @return Studienfassung string or null if there was no Studienfassung in this text.
     */
    public String getStudienFassung() {
        return studienFassung;
    }

    /**
     * @return Lesefassung string or null if there was no Lesefassung in this text.
     */
    public String getLeseFassung() {
        return leseFassung;
    }

    public VerseStatus getStudienFassungStatus() {
        return studienFassungStatus;
    }

    public VerseStatus getLeseFassungStatus() {
        return leseFassungStatus;
    }

    class QuoteSearcher implements IVisitor<AstNode>
    {
        public boolean foundQuote = false;
        @Override
        public void visit(AstNode node) throws Throwable {
            if(node.getNodeType() == AstNode.NodeType.quote)
                foundQuote = true;
        }
        @Override
        public void visitBefore(AstNode hostNode) throws Throwable {}
        @Override
        public void visitAfter(AstNode hostNode) throws Throwable {}
    }

    class NoteIndexCounter {
        private String noteIndexCounter;

        public NoteIndexCounter()
        {
            noteIndexCounter = "a";
        }

        public void reset()
        {
            noteIndexCounter = "a";
        }

        public String getNextNoteString()
        {
            String result = noteIndexCounter;
            incrementNoteCounter();
            return result;
        }

        private void incrementNoteCounter()
        {
            int walker = noteIndexCounter.length() - 1;

            while(walker >=0) {
                String preWalkerString = walker>0 ? noteIndexCounter.substring(0, walker) : "";
                String postWalkerString = walker<noteIndexCounter.length()-1 ? noteIndexCounter.substring(walker+1, noteIndexCounter.length()) : "";
                if(noteIndexCounter.charAt(walker) < 'z') {
                    noteIndexCounter = preWalkerString + (char)(noteIndexCounter.charAt(walker)+1) + postWalkerString;
                    break;
                }
                else {
                    noteIndexCounter = preWalkerString + 'a' + postWalkerString;
                }
                --walker;
            }
            if(walker == -1) {
                noteIndexCounter = "a" + noteIndexCounter;
            }
        }
    }
}
