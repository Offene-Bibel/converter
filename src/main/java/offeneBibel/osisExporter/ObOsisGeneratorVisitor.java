/* Copyright (C) 2013-2015 Patrick Zimmermann, Michael Schierl
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
import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObFassungNode;
import offeneBibel.parser.ObFassungNode.FassungType;
import offeneBibel.parser.ObNoteNode;
import offeneBibel.parser.ObParallelPassageNode;
import offeneBibel.parser.ObTextNode;
import offeneBibel.parser.ObVerseNode;
import offeneBibel.parser.ObVerseStatus;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This visitor constructs a Studienfassung and Lesefassung from an AST tree.
 * To use this class, first create an instance, then let it visit an ObAstNode,
 * then retrieve the results via getStudienfassung() and getLesefassung().
 */
public class ObOsisGeneratorVisitor extends DifferentiatingVisitor<ObAstNode> implements IVisitor<ObAstNode>
{

    private static final Pattern DIVINE_NAME_PATTERN = Pattern.compile("JHWH|(JAHWE|HERR|GOTT)[A-Z]*");

    private final int m_chapter;
    private final String m_book;

    private String m_studienFassung = null;
    private String m_leseFassung = null;
    private StringBuilder m_currentFassung = new StringBuilder();
    private boolean m_currentFassungContainsVerses = false;
    private String m_currentVerseStatus = "";

    private final String m_verseTagStart;
    private String m_verseTag = null;

    private String m_lTagStart;
    private String m_lgTagStart;
    private String m_qTagStart;
    private int m_lTagCounter = 1;
    private int m_lgTagCounter = 1;
    private int m_qTagCounter = 1;

    private int m_quoteCounter = 0;
    private boolean m_multiParallelPassage = false;

    /**
     * Started by <poem> and stopped by </poem>.
     * If in poem mode, all text nodes will replace \n with </l><l>.
     * A <l> is added at the beginning of a <poem> area also, but only if
     * there actually was some text. A </l> is added after the last text
     * block too.
     */
    private boolean m_poemMode = false;

    /**
     * If in poem mode and a textual line has started, this is true.
     * It is used to securely add the final </l> tag after the last line
     * in a poem, but only if there actually was some text.
     * It is also used to securely add the <l> before the first text after
     * the <poem> tag.
     */
    private boolean m_lineStarted = false;

    /** Skip the current verse if true. */
    private boolean m_skipVerse = false;

    private ObVerseStatus m_requiredTranslationStatus;

    private NoteIndexCounter m_noteIndexCounter;

    private boolean m_inlineVersStatus;

    private boolean m_unmilestonedLineGroup;

    /**
     * @param chapter The chapter number. Used in verse tags.
     * @param book The OSIS book abbreviation. Used in verse tags.
     * @param requiredTranslationStatus The minimum translation status verses need to meet to be included.
     * @param inlineVerseStatus Whether to include inline verse status in footnotes.
     * @param unmilestonedLineGroup Do not use milestones for line groups; instead milestone quote tags
     */
    public ObOsisGeneratorVisitor(int chapter, String book, ObVerseStatus requiredTranslationStatus, boolean inlineVerseStatus, boolean unmilestonedLineGroup)
    {
        m_chapter = chapter;
        m_book = book;
        m_verseTagStart = m_book + "." + m_chapter + ".";
        m_lTagStart = m_book + "." + m_chapter + "_l_tag_";
        m_lgTagStart = m_book + "." + m_chapter + "_lg_tag_";
        m_qTagStart = m_book + "." + m_chapter + "_q_tag_";
        m_noteIndexCounter = new NoteIndexCounter();
        m_requiredTranslationStatus = requiredTranslationStatus;
        m_inlineVersStatus = inlineVerseStatus;
        m_unmilestonedLineGroup = unmilestonedLineGroup;
    }

    @Override
    public void visitBeforeDefault(ObAstNode node) throws Throwable
    {
        if(node.getNodeType() == ObAstNode.NodeType.fassung) {
            m_noteIndexCounter.reset();
            m_currentFassung = new StringBuilder("");
            m_currentFassungContainsVerses = false;
            m_lTagCounter = 1;
            m_currentVerseStatus = "";
        }

        else if(node.getNodeType() == ObAstNode.NodeType.quote) {
            if(m_skipVerse) return;
            String end = ">";
            if (m_unmilestonedLineGroup) {
                end = " sID=\"" + m_qTagStart + m_qTagCounter + "\"/>";
            }
            if(m_quoteCounter>0)
            {
                m_quoteCounter++;
                
                m_currentFassung.append("»");
                
                if (node.getParent().isDescendantOf(ObAstNode.NodeType.italics))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    m_currentFassung.append("</hi>");          
                }

                m_currentFassung.append("<q level=\"" + m_quoteCounter + "\" marker=\"\"" + end);
                
                if (node.getParent().isDescendantOf(ObAstNode.NodeType.italics))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    m_currentFassung.append("<hi type=\"italic\">");          
                }
            }
            else
            {
                QuoteSearcher quoteSearcher = new QuoteSearcher();
                node.host(quoteSearcher, false);

                m_currentFassung.append("„");

                if (node.getParent().isDescendantOf(ObAstNode.NodeType.italics))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    m_currentFassung.append("</hi>");          
                }

                if(quoteSearcher.foundQuote == false)
                    m_currentFassung.append("<q marker=\"\"" + end);
                else {
                    m_quoteCounter++;
                    m_currentFassung.append("<q level=\"" + m_quoteCounter + "\" marker=\"\"" + end);
                }

                if (node.getParent().isDescendantOf(ObAstNode.NodeType.italics))
                {
                    // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                    m_currentFassung.append("<hi type=\"italic\">");          
                }
            }
        }

        else if(node.getNodeType() == ObAstNode.NodeType.alternative) {
            if(m_skipVerse) return;
            if (node.isDescendantOf(ObAstNode.NodeType.note))
                m_currentFassung.append("(");
            else
                m_currentFassung.append("<seg type=\"x-alternative\">(");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.insertion) {
            if(m_skipVerse) return;
            if (node.getParent().isDescendantOf(ObAstNode.NodeType.insertion) || node.isDescendantOf(ObAstNode.NodeType.omission))
                m_currentFassung.append("[");
            else
                m_currentFassung.append("<transChange type=\"added\">[");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.omission) {
            if(m_skipVerse) return;
            if (node.getParent().isDescendantOf(ObAstNode.NodeType.omission) || node.isDescendantOf(ObAstNode.NodeType.insertion))
                m_currentFassung.append("{");
            else
                m_currentFassung.append("<transChange type=\"deleted\">{");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.heading) {
            m_currentFassung.append("<title>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.hebrew) {
            if(m_skipVerse) return;
            m_currentFassung.append("<foreign xml:lang=\"he\">");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.note) {
            if(m_skipVerse) return;
            m_currentFassung.append("<note type=\"x-footnote\" n=\"" + m_noteIndexCounter.getNextNoteString() + "\">");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.italics) {
            if (m_skipVerse) return;
            m_currentFassung.append("<hi type=\"italic\">");
        }
        else if (node.getNodeType() == ObAstNode.NodeType.superScript) {
            if (m_skipVerse) return;
            m_currentFassung.append("<hi type=\"super\">");
        }
        else if (node.getNodeType() == ObAstNode.NodeType.strikeThrough) {
            if (m_skipVerse) return;
            m_currentFassung.append("<hi type=\"line-through\">");
        }
        else if (node.getNodeType() == ObAstNode.NodeType.underline) {
            if (m_skipVerse) return;
            m_currentFassung.append("<hi type=\"underline\">");
        }
    }

    @Override
    public void visitDefault(ObAstNode node) throws Throwable
    {
        if(node.getNodeType() == ObAstNode.NodeType.text) {
            if(m_skipVerse) return;
            ObTextNode text = (ObTextNode)node;
            String textString = text.getText();

            // Escaping &<> has to happen *before* inserting <l> tags.
            // Otherwise they would be replaced too.
            textString = textString.replaceAll("&", "&amp;");
            textString = textString.replaceAll(">", "&gt;");
            textString = textString.replaceAll("<", "&lt;");

            // Tag greek.
            if (node.isDescendantOf(ObAstNode.NodeType.note))
                textString = tagGreek(textString);

            // pretty print divine names
            if (!node.isDescendantOf(ObNoteNode.class)) {
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

            if(m_poemMode && ! node.isDescendantOf(ObNoteNode.class)) {
                if(textString.contains("\n")) {
                    if(m_lineStarted == false) {
                        textString = textString.replaceFirst("\n", getLTagStart());
                        m_lineStarted = true;
                    }
                    while(textString.contains("\n")) {
                        String stop = getLTagStop();
                        String start = getLTagStart();
                        textString = textString.replaceFirst("\n", stop + start);
                    }
                }
            }
            m_currentFassung.append(textString);
        }

        else if(node.getNodeType() == ObAstNode.NodeType.verse) {
            ObVerseNode verse = (ObVerseNode)node;
            addStopTag();
            //System.out.println("Verse:" + m_verseTagStart + verse.getNumber() + " " + verse.getStatus().toString());
            if(verse.getStatus().ordinal() >= m_requiredTranslationStatus.ordinal()) {
                m_currentFassungContainsVerses = true;
                m_skipVerse = false;
                m_verseTag = m_verseTagStart + verse.getNumber();
                m_currentFassung.append("<verse osisID=\"" + m_verseTag + "\" sID=\"" + m_verseTag + "\"/>");
                if(m_poemMode && m_lineStarted == false){
                    m_currentFassung.append(getLTagStart());
                    m_lineStarted = true;
                }
                if (m_inlineVersStatus) {
                    ObVerseStatus status = verse.getStatus();
                    ObVerseStatus leseStatus = verse.getStatus(FassungType.lesefassung);
                    ObVerseStatus studienStatus = verse.getStatus(FassungType.studienfassung);
                    String verseStatus;
                    if (leseStatus == studienStatus || status == studienStatus) {
                        // all statuses the same or this is actually the
                        // Studienfassung
                        verseStatus = "[Status: "+studienStatus.toHumanReadableString()+"]";
                    } else {
                        verseStatus = "[Studienfassung: " + studienStatus.toHumanReadableString() + "; Lesefassung: " + leseStatus.toHumanReadableString()+"]";
                    }
                    if (!m_currentVerseStatus.equals(verseStatus)) {
                        m_currentVerseStatus = verseStatus;
                        m_currentFassung.append("<note type=\"x-footnote\" n=\"Status\">" + verseStatus + "</note> ");
                    }
                }
            }
            else {
                // skip this verse
                m_skipVerse = true;
            }
        }

        else if(node.getNodeType() == ObAstNode.NodeType.parallelPassage) {
            if(m_skipVerse) return;
            ObParallelPassageNode passage = (ObParallelPassageNode)node;

            if(m_multiParallelPassage == false) {
                m_currentFassung.append("<note type=\"crossReference\" osisID=\"" + m_verseTag + "!crossReference\" osisRef=\"" + m_verseTag + "\">");
            }

            m_currentFassung.append("<reference osisRef=\"" +
                                            passage.getOsisBookId() + "." + passage.getChapter() + "." + passage.getStartVerse() + "\">" +
                                            BookNameHelper.getInstance().getGermanBookNameForOsisId(passage.getOsisBookId()) + " " + passage.getChapter() + "," + passage.getStartVerse() + "</reference>");

            if(passage.getNextSibling() != null && passage.getNextSibling().getNodeType() == ObAstNode.NodeType.parallelPassage) {
                m_multiParallelPassage = true;
                m_currentFassung.append("; ");
            }
            else {
                m_multiParallelPassage = false;
                m_currentFassung.append("</note>");
            }
        }

        else if(node.getNodeType() == ObAstNode.NodeType.poemStart) {
            m_poemMode = true;
            m_currentFassung.append(getLgTagStart());
        }

        else if(node.getNodeType() == ObAstNode.NodeType.poemStop) {
            m_poemMode = false;
            if(m_lineStarted) {
                m_currentFassung.append(getLTagStop());
                m_lineStarted = false;
            }
            m_currentFassung.append(getLgTagStop());
        }
    }

    @Override
    public void visitAfterDefault(ObAstNode node) throws Throwable
    {

        if(node.getNodeType() == ObAstNode.NodeType.fassung) {
            ObFassungNode fassung = (ObFassungNode)node;
            addStopTag();

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

            if (node.getParent().isDescendantOf(ObAstNode.NodeType.italics))
            {
                // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                m_currentFassung.append("</hi>");          
            }

            if (m_unmilestonedLineGroup) {
                m_currentFassung.append("<q marker=\"\" eID=\""+m_qTagStart+m_qTagCounter+"\"/>");
                ++m_qTagCounter;
            } else {
                m_currentFassung.append("</q>");
            }

            if (node.getParent().isDescendantOf(ObAstNode.NodeType.italics))
            {
                // Quotations are not allowed inside of <hi/>, so wrap <hi/> around them.
                m_currentFassung.append("<hi type=\"italic\">");          
            }

            if(m_quoteCounter>0)
                m_currentFassung.append("«");
            else
                m_currentFassung.append("“");

        }

        else if(node.getNodeType() == ObAstNode.NodeType.alternative) {
            if(m_skipVerse) return;
            if (node.isDescendantOf(ObAstNode.NodeType.note))
                m_currentFassung.append(")");
            else
                m_currentFassung.append(")</seg>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.insertion) {
            if(m_skipVerse) return;
            if (node.getParent().isDescendantOf(ObAstNode.NodeType.insertion) || node.isDescendantOf(ObAstNode.NodeType.omission))
                m_currentFassung.append("]");
            else
                m_currentFassung.append("]</transChange>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.omission) {
            if(m_skipVerse) return;
            if (node.getParent().isDescendantOf(ObAstNode.NodeType.omission) || node.isDescendantOf(ObAstNode.NodeType.insertion))
                m_currentFassung.append("}");
            else
                m_currentFassung.append("}</transChange>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.heading) {
            m_currentFassung.append("</title>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.hebrew) {
            if(m_skipVerse) return;
            m_currentFassung.append("</foreign>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.note) {
            if(m_skipVerse) return;
            m_currentFassung.append("</note>");
        }

        else if(node.getNodeType() == ObAstNode.NodeType.italics) {
            if (m_skipVerse) return;
            m_currentFassung.append("</hi>");
        }
        else if(node.getNodeType() == ObAstNode.NodeType.superScript) {
            if (m_skipVerse) return;
            m_currentFassung.append("</hi>");
        }
        else if(node.getNodeType() == ObAstNode.NodeType.strikeThrough) {
            if (m_skipVerse) return;
            m_currentFassung.append("</hi>");
        }
        else if(node.getNodeType() == ObAstNode.NodeType.underline) {
            if (m_skipVerse) return;
            m_currentFassung.append("</hi>");
        }
    }

    public String getStudienFassung() {
        return m_studienFassung;
    }

    public String getLeseFassung() {
        return m_leseFassung;
    }

    private void addStopTag() {
        if(m_verseTag != null) {
            if(m_poemMode && m_lineStarted) {
                m_currentFassung.append(getLTagStop());
                m_lineStarted = false;
            }
            m_currentFassung.append("<verse eID=\"" + m_verseTag + "\"/>\n");
            m_verseTag = null;
        }
    }

    private String getLTagStop() {
        if (m_unmilestonedLineGroup)
            return "</l>";
        return "<l eID=\"" + m_lTagStart + m_lTagCounter + "\"/>";
    }

    private String getLTagStart() {
        if (m_unmilestonedLineGroup)
            return "<l>";
        String m_lTag = m_lTagStart + m_lTagCounter;
        ++m_lTagCounter;
        return "<l sID=\"" + m_lTag + "\"/>";
    }
    
    private String getLgTagStop() {
        if (m_unmilestonedLineGroup)
            return "</lg>";
        return "<lg eID=\"" + m_lgTagStart + m_lgTagCounter + "\"/>";
    }

    private String getLgTagStart() {
        if (m_unmilestonedLineGroup)
            return "<lg>";
        String m_lgTag = m_lgTagStart + m_lgTagCounter;
        ++m_lgTagCounter;
        return "<lg sID=\"" + m_lgTag + "\"/>";
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

    public static class NoteIndexCounter {
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
