package offeneBibel.osisExporter;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObFassungNode;
import offeneBibel.parser.ObParallelPassageNode;
import offeneBibel.parser.ObTextNode;
import offeneBibel.parser.ObTreeNode;
import offeneBibel.parser.ObVerseNode;
import offeneBibel.parser.ObVerseStatus;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

public class ObAstVisitor extends DifferentiatingVisitor<ObTreeNode> implements IVisitor<ObTreeNode>
{
	private final int m_chapter;
	private final String m_book;
	private final String m_verseTagStart;
	
	private String m_studienFassung = null;
	private String m_leseFassung = null;
	private String m_currentFassung = "";
	
	private String m_verseTag = null;
	private int m_quoteCounter = 0;
	private boolean m_multiParallelPassage = false;
	
	/**
	 * Started by <poem> and stopped by </poem>.
	 * If in poem mode, all text nodes will replace \n with </l><l>.
	 * A <l> is added at the beginning of a <poem> area also, but only if
	 * there actually was some text. A </l> is added after the last text
	 * block too.
	 */
	private boolean m_poemMode;
	
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
	
	public ObAstVisitor(int chapter, String book, ObVerseStatus requiredTranslationStatus)
	{
		m_chapter = chapter;
		m_book = book;
		m_verseTagStart = m_book + "." + m_chapter + ".";
		m_noteIndexCounter = new NoteIndexCounter();
		m_requiredTranslationStatus = requiredTranslationStatus;
	}
	
	public void visitBeforeDefault(ObTreeNode node) throws Throwable
	{
		ObAstNode astNode = (ObAstNode)node;
		
		if(astNode.getNodeType() == ObAstNode.NodeType.fassung) {
			m_currentFassung = "";
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.quote) {
			if(m_skipVerse) return;
			if(m_quoteCounter>0)
			{
				m_quoteCounter++;
				m_currentFassung += "»<q level=\" + m_quoteCounter + \" marker=\"\">";
			}
			else
			{
				QuoteSearcher quoteSearcher = new QuoteSearcher();
				astNode.host(quoteSearcher, false);
				if(quoteSearcher.foundQuote == false)
					m_currentFassung += "„<q marker=\"\">";
				else {
					m_quoteCounter++;
					m_currentFassung += "„<q level=\" + m_quoteCounter + \" marker=\"\">";
				}
			}
		}

		else if(astNode.getNodeType() == ObAstNode.NodeType.alternative) {
			if(m_skipVerse) return;
			m_currentFassung += "(";
		}

		else if(astNode.getNodeType() == ObAstNode.NodeType.insertion) {
			if(m_skipVerse) return;
			m_currentFassung += "[";
		}

		else if(astNode.getNodeType() == ObAstNode.NodeType.omission) {
			if(m_skipVerse) return;
			m_currentFassung += "{";
		}

		else if(astNode.getNodeType() == ObAstNode.NodeType.heading) {
			m_currentFassung += "<title>";
		}

		else if(astNode.getNodeType() == ObAstNode.NodeType.hebrew) {
			if(m_skipVerse) return;
			m_currentFassung += "<foreign xml:lang=\"he\">";
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.note) {
			if(m_skipVerse) return;
			m_currentFassung += "<note type=\"x-footnote\" n=\"" + m_noteIndexCounter.getNextNoteString() + "\">";
		}
	}

	public void visitDefault(ObTreeNode node) throws Throwable
	{
		ObAstNode astNode = (ObAstNode)node;
		
		if(astNode.getNodeType() == ObAstNode.NodeType.text) {
			if(m_skipVerse) return;
			ObTextNode text = (ObTextNode)node;
			String textString = text.getText();
			if(m_poemMode) {
				if(m_lineStarted == false) {
					textString = textString.replaceFirst("\n", "<l>");
					m_lineStarted = true;
				}
				textString = textString.replaceAll("\n", "</l><l>");
			}
			m_currentFassung += textString;
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.verse) {
			ObVerseNode verse = (ObVerseNode)node;
			addStopTag();
			if(verse.getStatus().ordinal() >= m_requiredTranslationStatus.ordinal()) {
				m_skipVerse = false;
				m_verseTag = m_verseTagStart + verse.getNumber();
				m_currentFassung += "<verse osisID=\"" + m_verseTag + "\" sID=\"" + m_verseTag + "\"/>";
				if(m_poemMode){
					m_currentFassung += "<l>";
					m_lineStarted = true;
				}
			}
			else {
				// skip this verse
				m_skipVerse = true;
			}
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.parallelPassage) {
			if(m_skipVerse) return;
			ObParallelPassageNode passage = (ObParallelPassageNode)node;
			
			if(m_multiParallelPassage == false) {
				m_currentFassung += "<note type=\"crossReference\" osisID=\"" + m_verseTag + "!crossReference\" osisRef=\"" + m_verseTag + "\">";
			}
			
			m_currentFassung += "<reference osisRef=\"" +
			passage.getBook() + "." + passage.getChapter() + "." + passage.getStartVerse() + "\">" +
			passage.getBook() + " " + passage.getChapter() + ", " + passage.getStartVerse() + "</reference>";
			
			if(((ObAstNode)passage.getNextSibling()).getNodeType() == ObAstNode.NodeType.parallelPassage) {
				m_multiParallelPassage = true;
				m_currentFassung += "|"; 
			}
			else {
				m_multiParallelPassage = false;
				m_currentFassung += "</note>";
			}
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.poemStart) {
			m_poemMode = true;
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.poemStop) {
			m_poemMode = false;
			if(m_lineStarted) {
				m_currentFassung += "</l>";
				m_lineStarted = false;
			}
		}
	}

	public void visitAfterDefault(ObTreeNode node) throws Throwable
	{
		ObAstNode astNode = (ObAstNode)node;
		
		if(astNode.getNodeType() == ObAstNode.NodeType.fassung) {
			ObFassungNode fassung = (ObFassungNode)node;
			addStopTag();
			if(fassung.getFassung() == ObFassungNode.FassungType.lesefassung) {
				m_leseFassung = m_currentFassung;
			}
			else {
				m_studienFassung = m_currentFassung;
			}
			m_noteIndexCounter.reset();
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.quote) {
			if(m_skipVerse) return;
			if(m_quoteCounter>0)
				m_quoteCounter--;
			if(m_quoteCounter>0)
				m_currentFassung += "</q>«";
			else
				m_currentFassung += "</q>“";
				
		}

		else if(astNode.getNodeType() == ObAstNode.NodeType.alternative) {
			if(m_skipVerse) return;
			m_currentFassung += ")";
		}

		else if(astNode.getNodeType() == ObAstNode.NodeType.insertion) {
			if(m_skipVerse) return;
			m_currentFassung += "]";
		}

		else if(astNode.getNodeType() == ObAstNode.NodeType.omission) {
			if(m_skipVerse) return;
			m_currentFassung += "}";
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.heading) {
			m_currentFassung += "</title>";
		}

		else if(astNode.getNodeType() == ObAstNode.NodeType.hebrew) {
			if(m_skipVerse) return;
			m_currentFassung += "</foreign>";
		}

		else if(astNode.getNodeType() == ObAstNode.NodeType.note) {
			if(m_skipVerse) return;
			m_currentFassung += "</note>";
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
				m_currentFassung += "</l>";
				m_lineStarted = false;
			}
			m_currentFassung += "<verse eID=\"" + m_verseTag + "\"/>\n";
			m_verseTag = null;
		}
	}
	
	class QuoteSearcher implements IVisitor<ObTreeNode>
	{
		public boolean foundQuote = false; 
		public void visit(ObTreeNode node) throws Throwable {
			if(((ObAstNode)node).getNodeType() == ObAstNode.NodeType.quote)
				foundQuote = true;
		}
		public void visitBefore(ObTreeNode hostNode) throws Throwable {}
		public void visitAfter(ObTreeNode hostNode) throws Throwable {}
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
