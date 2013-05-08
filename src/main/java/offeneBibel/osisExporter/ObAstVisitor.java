package offeneBibel.osisExporter;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObFassungNode;
import offeneBibel.parser.ObNoteNode;
import offeneBibel.parser.ObParallelPassageNode;
import offeneBibel.parser.ObTextNode;
import offeneBibel.parser.ObVerseNode;
import offeneBibel.parser.ObVerseStatus;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

public class ObAstVisitor extends DifferentiatingVisitor<ObAstNode> implements IVisitor<ObAstNode>
{
	private final int m_chapter;
	private final String m_book;
	
	private String m_studienFassung = null;
	private String m_leseFassung = null;
	private String m_currentFassung = "";
	private boolean m_currentFassungContainsVerses = false;
	
	private final String m_verseTagStart;
	private String m_verseTag = null;
	
	private String m_lTagStart;
	private String m_lTag = null;
	private int m_lTagCounter = 1;
	
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
		m_lTagStart = m_book + "." + m_chapter + "_l_tag_";
		m_noteIndexCounter = new NoteIndexCounter();
		m_requiredTranslationStatus = requiredTranslationStatus;
	}
	
	public void visitBeforeDefault(ObAstNode node) throws Throwable
	{
		if(node.getNodeType() == ObAstNode.NodeType.fassung) {
			m_noteIndexCounter.reset();
			m_currentFassung = "";
			m_currentFassungContainsVerses = false;
			m_lTag = null;
			m_lTagCounter = 1;
		}
		
		else if(node.getNodeType() == ObAstNode.NodeType.quote) {
			if(m_skipVerse) return;
			if(m_quoteCounter>0)
			{
				m_quoteCounter++;
				m_currentFassung += "»<q level=\" + m_quoteCounter + \" marker=\"\">";
			}
			else
			{
				QuoteSearcher quoteSearcher = new QuoteSearcher();
				node.host(quoteSearcher, false);
				if(quoteSearcher.foundQuote == false)
					m_currentFassung += "„<q marker=\"\">";
				else {
					m_quoteCounter++;
					m_currentFassung += "„<q level=\" + m_quoteCounter + \" marker=\"\">";
				}
			}
		}

		else if(node.getNodeType() == ObAstNode.NodeType.alternative) {
			if(m_skipVerse) return;
			m_currentFassung += "(";
		}

		else if(node.getNodeType() == ObAstNode.NodeType.insertion) {
			if(m_skipVerse) return;
			m_currentFassung += "[";
		}

		else if(node.getNodeType() == ObAstNode.NodeType.omission) {
			if(m_skipVerse) return;
			m_currentFassung += "{";
		}

		else if(node.getNodeType() == ObAstNode.NodeType.heading) {
			m_currentFassung += "<title>";
		}

		else if(node.getNodeType() == ObAstNode.NodeType.hebrew) {
			if(m_skipVerse) return;
			m_currentFassung += "<foreign xml:lang=\"he\">";
		}
		
		else if(node.getNodeType() == ObAstNode.NodeType.note) {
			if(m_skipVerse) return;
			m_currentFassung += "<note type=\"x-footnote\" n=\"" + m_noteIndexCounter.getNextNoteString() + "\">";
		}
	}

	public void visitDefault(ObAstNode node) throws Throwable
	{
		if(node.getNodeType() == ObAstNode.NodeType.text) {
			if(m_skipVerse) return;
			ObTextNode text = (ObTextNode)node;
			String textString = text.getText();
			
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
			
			textString = textString.replaceAll("&", "&amp;");
			
			m_currentFassung += textString;
		}
		
		else if(node.getNodeType() == ObAstNode.NodeType.verse) {
			ObVerseNode verse = (ObVerseNode)node;
			addStopTag();
			//System.out.println("Verse:" + m_verseTagStart + verse.getNumber() + " " + verse.getStatus().toString());
			if(verse.getStatus().ordinal() >= m_requiredTranslationStatus.ordinal()) {
				m_currentFassungContainsVerses = true;
				m_skipVerse = false;
				m_verseTag = m_verseTagStart + verse.getNumber();
				m_currentFassung += "<verse osisID=\"" + m_verseTag + "\" sID=\"" + m_verseTag + "\"/>";
				if(m_poemMode && m_lineStarted == false){
					m_currentFassung += getLTagStart();
					m_lineStarted = true;
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
		
		else if(node.getNodeType() == ObAstNode.NodeType.poemStart) {
			m_poemMode = true;
		}
		
		else if(node.getNodeType() == ObAstNode.NodeType.poemStop) {
			m_poemMode = false;
			if(m_lineStarted) {
				m_currentFassung += getLTagStop();
				m_lineStarted = false;
			}
		}
	}

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
				m_leseFassung = m_currentFassung;
			}
			else {
				m_studienFassung = m_currentFassung;
			}
		}
		
		else if(node.getNodeType() == ObAstNode.NodeType.quote) {
			if(m_skipVerse) return;
			if(m_quoteCounter>0)
				m_quoteCounter--;
			if(m_quoteCounter>0)
				m_currentFassung += "</q>«";
			else
				m_currentFassung += "</q>“";
				
		}

		else if(node.getNodeType() == ObAstNode.NodeType.alternative) {
			if(m_skipVerse) return;
			m_currentFassung += ")";
		}

		else if(node.getNodeType() == ObAstNode.NodeType.insertion) {
			if(m_skipVerse) return;
			m_currentFassung += "]";
		}

		else if(node.getNodeType() == ObAstNode.NodeType.omission) {
			if(m_skipVerse) return;
			m_currentFassung += "}";
		}
		
		else if(node.getNodeType() == ObAstNode.NodeType.heading) {
			m_currentFassung += "</title>";
		}

		else if(node.getNodeType() == ObAstNode.NodeType.hebrew) {
			if(m_skipVerse) return;
			m_currentFassung += "</foreign>";
		}

		else if(node.getNodeType() == ObAstNode.NodeType.note) {
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
				m_currentFassung += getLTagStop();
				m_lineStarted = false;
			}
			m_currentFassung += "<verse eID=\"" + m_verseTag + "\"/>\n";
			m_verseTag = null;
		}
	}
	
	private String getLTagStop() {
		if(m_lTag == null) {
			m_lTag = m_lTagStart + m_lTagCounter;
		}
		return "<l eID=\"" + m_lTag + "\"/>";
	}
	
	private String getLTagStart() {
		if(m_lTag != null) {
			++m_lTagCounter;
		}
		m_lTag = m_lTagStart + m_lTagCounter;
		return "<l sID=\"" + m_lTag + "\"/>";
	}
	
	class QuoteSearcher implements IVisitor<ObAstNode>
	{
		public boolean foundQuote = false; 
		public void visit(ObAstNode node) throws Throwable {
			if(node.getNodeType() == ObAstNode.NodeType.quote)
				foundQuote = true;
		}
		public void visitBefore(ObAstNode hostNode) throws Throwable {}
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
