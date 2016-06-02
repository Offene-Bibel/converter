/* Copyright (C) 2013-2015 Patrick Zimmermann, Michael Schierl, Stephan Kreutzer
 *
 * This file is part of converter.
 *
 * converter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * converter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License 3 for more details.
 *
 * You should have received a copy of the GNU General Public License 3
 * along with converter.  If not, see <http://www.gnu.org/licenses/>.
 */

package offeneBibel.osisExporter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import offeneBibel.parser.BookNameHelper;
import offeneBibel.parser.AstNode;
import offeneBibel.parser.AstNode.NodeType;
import offeneBibel.parser.FassungNode;
import offeneBibel.parser.NoteNode;
import offeneBibel.parser.ParallelPassageNode;
import offeneBibel.parser.TextNode;
import offeneBibel.parser.VerseNode;
import offeneBibel.parser.VerseStatus;
import offeneBibel.parser.WikiLinkNode;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

/**
 * This visitor constructs a Studienfassung and Lesefassung from an AST tree.
 * To use this class, first create an instance, then let it visit an ObAstNode,
 * then retrieve the results via getStudienfassung() and getLesefassung().
 */
public class OsisGeneratorVisitor extends DifferentiatingVisitor<AstNode> implements IVisitor<AstNode>
{

    private static final Pattern DIVINE_NAME_PATTERN = Pattern.compile("JHWH|(JAHWE|HERR|GOTT)[A-Z]*");

    private final int chapter;
    private final String book;

    private String studienFassung = null;
    private String leseFassung = null;
    private StringBuilder currentFassung = new StringBuilder();
    private boolean currentFassungContainsVerses = false;
    private VerseStatus currentVerseStatus = VerseStatus.none;

    private final String verseTagStart;
    private String verseTag = null;

    private String lTagStart;
    private String lgTagStart;
    private String qTagStart;
    private int lTagCounter = 1;
    private int lgTagCounter = 1;
    private int qTagCounter = 1;

    private int quoteCounter = 0;
    private boolean multiParallelPassage = false;

    /**
     * Started by <poem> and stopped by </poem>.
     * If in poem mode, all text nodes will replace \n with </l><l>.
     * A <l> is added at the beginning of a <poem> area also, but only if
     * there actually was some text. A </l> is added after the last text
     * block too.
     */
    private boolean poemMode = false;

    /**
     * If in poem mode and a textual line has started, this is true.
     * It is used to securely add the final </l> tag after the last line
     * in a poem, but only if there actually was some text.
     * It is also used to securely add the <l> before the first text after
     * the <poem> tag.
     */
    private boolean lineStarted = false;

    /** Skip the current verse if true. */
    private boolean skipVerse = false;

    private VerseStatus requiredTranslationStatus;

    private NoteIndexCounter noteIndexCounter;

    private boolean inlineVersStatus;

    private boolean unmilestonedLineGroup;

    /**
     * @param chapter The chapter number. Used in verse tags.
     * @param book The OSIS book abbreviation. Used in verse tags.
     * @param requiredTranslationStatus The minimum translation status verses need to meet to be included.
     * @param inlineVerseStatus Whether to include inline verse status in footnotes.
     * @param unmilestonedLineGroup Do not use milestones for line groups; instead milestone quote tags
     */
    public OsisGeneratorVisitor(int chapter, String book, VerseStatus requiredTranslationStatus, boolean inlineVerseStatus, boolean unmilestonedLineGroup)
    {
        this.chapter = chapter;
        this.book = book;
        this.verseTagStart = book + "." + chapter + ".";
        this.lTagStart = book + "." + chapter + "_l_tag_";
        this.lgTagStart = book + "." + chapter + "_lg_tag_";
        this.qTagStart = book + "." + chapter + "_q_tag_";
        this.noteIndexCounter = new NoteIndexCounter();
        this.requiredTranslationStatus = requiredTranslationStatus;
        this.inlineVersStatus = inlineVerseStatus;
        this.unmilestonedLineGroup = unmilestonedLineGroup;
    }

    @Override
    public void visitBeforeDefault(AstNode node) throws Throwable
    {
        if(node.getNodeType() == AstNode.NodeType.fassung) {
            noteIndexCounter.reset();
            currentFassung = new StringBuilder("");
            currentFassungContainsVerses = false;
            lTagCounter = 1;
            currentVerseStatus = VerseStatus.none;
        }

        else if(node.getNodeType() == AstNode.NodeType.quote) {
            if(skipVerse) return;
            String end = ">";
            if (unmilestonedLineGroup) {
                end = " sID=\"" + qTagStart + qTagCounter + "\"/>";
            }
            if(quoteCounter>0)
            {
                quoteCounter++;
                
                currentFassung.append("»");
                
                if (node.getParent().isDescendantOf(AstNode.NodeType.italics))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    currentFassung.append("</hi>");          
                }
                if (node.getParent().isDescendantOf(AstNode.NodeType.fat))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    currentFassung.append("</hi>");
                }

                currentFassung.append("<q level=\"" + quoteCounter + "\" marker=\"\"" + end);
                
                if (node.getParent().isDescendantOf(AstNode.NodeType.italics))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    currentFassung.append("<hi type=\"italic\">");          
                }
                if (node.getParent().isDescendantOf(AstNode.NodeType.fat))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    currentFassung.append("<hi type=\"bold\">");
                }
            }
            else
            {
                QuoteSearcher quoteSearcher = new QuoteSearcher();
                node.host(quoteSearcher, false);

                currentFassung.append("„");

                if (node.getParent().isDescendantOf(AstNode.NodeType.italics))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    currentFassung.append("</hi>");          
                }
                if (node.getParent().isDescendantOf(AstNode.NodeType.fat))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    currentFassung.append("</hi>");
                }

                if(quoteSearcher.foundQuote == false)
                    currentFassung.append("<q marker=\"\"" + end);
                else {
                    quoteCounter++;
                    currentFassung.append("<q level=\"" + quoteCounter + "\" marker=\"\"" + end);
                }

                if (node.getParent().isDescendantOf(AstNode.NodeType.italics))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    currentFassung.append("<hi type=\"italic\">");          
                }
                if (node.getParent().isDescendantOf(AstNode.NodeType.fat))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    currentFassung.append("<hi type=\"bold\">");
                }
            }
        }

        else if(node.getNodeType() == AstNode.NodeType.alternative) {
            if(skipVerse) return;
            if (node.isDescendantOf(AstNode.NodeType.note))
                currentFassung.append("(");
            else
                currentFassung.append("<seg type=\"x-alternative\">(");
        }

        else if(node.getNodeType() == AstNode.NodeType.insertion) {
            if(skipVerse) return;
            if (node.getParent().isDescendantOf(AstNode.NodeType.insertion) || node.isDescendantOf(AstNode.NodeType.omission))
                currentFassung.append("[");
            else
                currentFassung.append("<transChange type=\"added\">[");
        }

        else if(node.getNodeType() == AstNode.NodeType.omission) {
            if(skipVerse) return;
            if (node.getParent().isDescendantOf(AstNode.NodeType.omission) || node.isDescendantOf(AstNode.NodeType.insertion))
                currentFassung.append("{");
            else
                currentFassung.append("<transChange type=\"deleted\">{");
        }

        else if(node.getNodeType() == AstNode.NodeType.heading) {
            currentFassung.append("<title>");
        }

        else if(node.getNodeType() == AstNode.NodeType.hebrew) {
            if(skipVerse) return;

            if (!node.getParent().isDescendantOf(AstNode.NodeType.wikiLink))
            {
                // foreign are not allowed inside of <a>, so skip them
                currentFassung.append("<foreign xml:lang=\"he\">");
            }
        }

        else if(node.getNodeType() == AstNode.NodeType.note) {
            if(skipVerse) return;
            currentFassung.append("<note type=\"x-footnote\" n=\"" + noteIndexCounter.getNextNoteString() + "\">");
        }

        else if(node.getNodeType() == AstNode.NodeType.italics) {
            if (skipVerse) return;
            currentFassung.append("<hi type=\"italic\">");
        }
        else if(node.getNodeType() == AstNode.NodeType.fat) {
            if (skipVerse) return;
            currentFassung.append("<hi type=\"bold\">");
        }
        else if (node.getNodeType() == AstNode.NodeType.superScript) {
            if (skipVerse) return;
            currentFassung.append("<hi type=\"super\">");
        }
        else if (node.getNodeType() == AstNode.NodeType.strikeThrough) {
            if (skipVerse) return;
            currentFassung.append("<hi type=\"line-through\">");
        }
        else if (node.getNodeType() == AstNode.NodeType.underline) {
            if (skipVerse) return;
            currentFassung.append("<hi type=\"underline\">");
        }
        else if (node.getNodeType() == AstNode.NodeType.wikiLink) {
            if (skipVerse) return;
            WikiLinkNode obWikiLinkNode = (WikiLinkNode)node;
            currentFassung.append("<a href=\"");
            if(obWikiLinkNode.isWikiLink())
                currentFassung.append("http://offene-bibel.de/wiki/");
            currentFassung.append(obWikiLinkNode.getLink().replace("&", "&amp;"));
            currentFassung.append("\">");
            
            if(obWikiLinkNode.childCount() == 0)
                currentFassung.append(obWikiLinkNode.getLink().replace("&", "&amp;"));
        }
    }

    @Override
    public void visitDefault(AstNode node) throws Throwable
    {
        if(node.getNodeType() == AstNode.NodeType.text) {
            if(skipVerse) return;
            TextNode text = (TextNode)node;
            String textString = text.getText();

            // Escaping &<> has to happen *before* inserting <l> tags.
            // Otherwise they would be replaced too.
            textString = textString.replaceAll("&", "&amp;");
            textString = textString.replaceAll(">", "&gt;");
            textString = textString.replaceAll("<", "&lt;");

            // Tag greek.
            if (node.isDescendantOf(AstNode.NodeType.note))
                textString = tagGreek(textString);

            // pretty print divine names
            if (!node.isDescendantOf(NoteNode.class)) {
                if (textString.contains("|")) {
                    textString = textString.replaceAll("\\|([^ |]+)\\|", "<divineName>$1</divineName>").replace("|", "");
                }
                Matcher m = DIVINE_NAME_PATTERN.matcher(textString);
                StringBuffer sb = null;
                if (m.find()) {
                    if (sb == null)
                        sb = new StringBuffer(textString.length());
                    String name = m.group();
                    if (!name.equals("JHWH"))
                        name = name.substring(0,1)+name.substring(1).toLowerCase();
                    m.appendReplacement(sb, "<divineName>"+name+"</divineName>");
                }
                if (sb != null) {
                    m.appendTail(sb);
                    textString = sb.toString();
                }
            }

            if(poemMode && ! node.isDescendantOf(NoteNode.class)) {
                if(textString.contains("\n")) {
                    if(lineStarted == false) {
                        textString = textString.replaceFirst("\n", getLTagStart());
                        lineStarted = true;
                    }
                    while(textString.contains("\n")) {
                        String stop = getLTagStop();
                        String start = getLTagStart();
                        textString = textString.replaceFirst("\n", stop + start);
                    }
                }
            }
            currentFassung.append(textString);
        }

        else if(node.getNodeType() == AstNode.NodeType.verse) {
            VerseNode verse = (VerseNode)node;
            addStopTag();
            //System.out.println("Verse:" + verseTagStart + verse.getNumber() + " " + verse.getStatus().toString());
            if(verse.getStatus().ordinal() >= requiredTranslationStatus.ordinal()) {
                currentFassungContainsVerses = true;
                skipVerse = false;
                verseTag = verseTagStart + verse.getNumber();
                currentFassung.append("<verse osisID=\"" + verseTag + "\" sID=\"" + verseTag + "\"/>");
                if(poemMode && lineStarted == false){
                    currentFassung.append(getLTagStart());
                    lineStarted = true;
                }
                if (inlineVersStatus) {
                    AstNode nextNode = verse.getNextSibling();
                    while(nextNode != null) {
                        if(nextNode instanceof TextNode) {
                            if (!((TextNode) nextNode).getText().trim().isEmpty())
                                break;
                        }
                        else if (nextNode.getNodeType() != NodeType.poemStart && nextNode.getNodeType() != NodeType.poemStop) {
                            break;
                        }
                        nextNode = nextNode.getNextSibling();
                    }
                    if (nextNode == null || nextNode instanceof VerseNode || nextNode.getNodeType() == NodeType.heading) {
                        // empty verses do not need any status
                        currentVerseStatus = VerseStatus.none;
                    } else {
                        VerseStatus status = verse.getStatus();
                        if (currentVerseStatus != status) {
                            currentVerseStatus = status;
                            currentFassung.append("<note type=\"x-footnote\" n=\"Status\">[Status: " + status.getExportStatusString() + "]</note> ");
                        }
                    }
                }
            }
            else {
                // skip this verse
                skipVerse = true;
            }
        }

        else if(node.getNodeType() == AstNode.NodeType.parallelPassage) {
            if(skipVerse) return;
            ParallelPassageNode passage = (ParallelPassageNode)node;

            if(multiParallelPassage == false) {
                currentFassung.append("<note type=\"crossReference\" osisID=\"" + verseTag + "!crossReference\" osisRef=\"" + verseTag + "\">");
            }

            currentFassung.append("<reference osisRef=\"" +
                                            passage.getOsisBookId() + "." + passage.getChapter() + "." + passage.getStartVerse() + "\">" +
                                            BookNameHelper.getInstance().getGermanBookNameForOsisId(passage.getOsisBookId()) + " " + passage.getChapter() + "," + passage.getStartVerse() + "</reference>");

            if(passage.getNextSibling() != null && passage.getNextSibling().getNodeType() == AstNode.NodeType.parallelPassage) {
                multiParallelPassage = true;
                currentFassung.append("; ");
            }
            else {
                multiParallelPassage = false;
                currentFassung.append("</note>");
            }
        }

        else if(node.getNodeType() == AstNode.NodeType.poemStart) {
            poemMode = true;
            currentFassung.append(getLgTagStart());
        }

        else if(node.getNodeType() == AstNode.NodeType.poemStop) {
            poemMode = false;
            if(lineStarted) {
                currentFassung.append(getLTagStop());
                lineStarted = false;
            }
            currentFassung.append(getLgTagStop());
        }
    }

    @Override
    public void visitAfterDefault(AstNode node) throws Throwable
    {

        if(node.getNodeType() == AstNode.NodeType.fassung) {
            FassungNode fassung = (FassungNode)node;
            addStopTag();

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

            if (node.getParent().isDescendantOf(AstNode.NodeType.italics))
            {
                // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                currentFassung.append("</hi>");          
            }
            if (node.getParent().isDescendantOf(AstNode.NodeType.fat))
            {
                // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                currentFassung.append("</hi>");
            }

            if (unmilestonedLineGroup) {
                currentFassung.append("<q marker=\"\" eID=\""+qTagStart+qTagCounter+"\"/>");
                ++qTagCounter;
            } else {
                currentFassung.append("</q>");
            }

            if (node.getParent().isDescendantOf(AstNode.NodeType.italics))
            {
                // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                currentFassung.append("<hi type=\"italic\">");          
            }
            if (node.getParent().isDescendantOf(AstNode.NodeType.fat))
            {
                // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                currentFassung.append("<hi type=\"bold\">");
            }

            if(quoteCounter>0)
                currentFassung.append("«");
            else
                currentFassung.append("“");

        }

        else if(node.getNodeType() == AstNode.NodeType.alternative) {
            if(skipVerse) return;
            if (node.isDescendantOf(AstNode.NodeType.note))
                currentFassung.append(")");
            else
                currentFassung.append(")</seg>");
        }

        else if(node.getNodeType() == AstNode.NodeType.insertion) {
            if(skipVerse) return;
            if (node.getParent().isDescendantOf(AstNode.NodeType.insertion) || node.isDescendantOf(AstNode.NodeType.omission))
                currentFassung.append("]");
            else
                currentFassung.append("]</transChange>");
        }

        else if(node.getNodeType() == AstNode.NodeType.omission) {
            if(skipVerse) return;
            if (node.getParent().isDescendantOf(AstNode.NodeType.omission) || node.isDescendantOf(AstNode.NodeType.insertion))
                currentFassung.append("}");
            else
                currentFassung.append("}</transChange>");
        }

        else if(node.getNodeType() == AstNode.NodeType.heading) {
            currentFassung.append("</title>");
        }

        else if(node.getNodeType() == AstNode.NodeType.hebrew) {
            if(skipVerse) return;
            if (!node.getParent().isDescendantOf(AstNode.NodeType.wikiLink))
            {
                // foreign are not allowed inside of <a>, so skip them
                currentFassung.append("</foreign>");
            }
        }

        else if(node.getNodeType() == AstNode.NodeType.note) {
            if(skipVerse) return;
            currentFassung.append("</note>");
        }

        else if(node.getNodeType() == AstNode.NodeType.italics) {
            if (skipVerse) return;
            currentFassung.append("</hi>");
        }
        else if(node.getNodeType() == AstNode.NodeType.fat) {
            if (skipVerse) return;
            currentFassung.append("</hi>");
        }
        else if(node.getNodeType() == AstNode.NodeType.superScript) {
            if (skipVerse) return;
            currentFassung.append("</hi>");
        }
        else if(node.getNodeType() == AstNode.NodeType.strikeThrough) {
            if (skipVerse) return;
            currentFassung.append("</hi>");
        }
        else if(node.getNodeType() == AstNode.NodeType.underline) {
            if (skipVerse) return;
            currentFassung.append("</hi>");
        }
        else if (node.getNodeType() == AstNode.NodeType.wikiLink) {
            if (skipVerse) return;
            currentFassung.append("</a>");
        }
    }

    public String getStudienFassung() {
        return studienFassung;
    }

    public String getLeseFassung() {
        return leseFassung;
    }

    private void addStopTag() {
        if(verseTag != null) {
            if(poemMode && lineStarted) {
                currentFassung.append(getLTagStop());
                lineStarted = false;
            }
            currentFassung.append("<verse eID=\"" + verseTag + "\"/>\n");
            verseTag = null;
        }
    }

    private String getLTagStop() {
        if (unmilestonedLineGroup)
            return "</l>";
        return "<l eID=\"" + lTagStart + lTagCounter + "\"/>";
    }

    private String getLTagStart() {
        if (unmilestonedLineGroup)
            return "<l>";
        String lTag = lTagStart + lTagCounter;
        ++lTagCounter;
        return "<l sID=\"" + lTag + "\"/>";
    }
    
    private String getLgTagStop() {
        if (unmilestonedLineGroup)
            return "</lg>";
        return "<lg eID=\"" + lgTagStart + lgTagCounter + "\"/>";
    }

    private String getLgTagStart() {
        if (unmilestonedLineGroup)
            return "<lg>";
        String lgTag = lgTagStart + lgTagCounter;
        ++lgTagCounter;
        return "<lg sID=\"" + lgTag + "\"/>";
    }

    private static final Pattern FIND_GREEK = Pattern.compile("[\\p{IsGreek}]+([\\p{IsCommon}]+[\\p{IsGreek}]+)*");

    private static String tagGreek(String str) {
        Matcher m = FIND_GREEK.matcher(str);
        StringBuffer result = new StringBuffer(str.length());
        while (m.find()) {
            m.appendReplacement(result, "<foreign xml:lang=\"grc\">$0</foreign>");
        }
        m.appendTail(result);
        return result.toString();
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

    public static class NoteIndexCounter {
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
