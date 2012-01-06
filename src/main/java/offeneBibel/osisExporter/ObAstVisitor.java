package offeneBibel.osisExporter;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObFassungNode;
import offeneBibel.parser.ObTextNode;
import offeneBibel.parser.ObTreeNode;
import offeneBibel.parser.ObVerseNode;
import offeneBibel.visitorPattern.IVisitor;

public class ObAstVisitor implements IVisitor<ObTreeNode>
{
	private final int m_chapter;
	private final String m_book;
	private final String m_verseTagStart;
	
	private String m_studienFassung = null;
	private String m_leseFassung = null;
	private String m_currentFassung = "";
	
	private String m_verseStopTag = null;
	private String m_noteIndexCounter = "a";
	
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
	
	public ObAstVisitor(int chapter, String book)
	{
		m_chapter = chapter;
		m_book = book;
		m_verseTagStart = m_book + "." + m_chapter + ".";
	}
	
	public void visitBefore(ObTreeNode node) throws Throwable
	{
		ObAstNode astNode = (ObAstNode)node;
		
		if(astNode.getNodeType() == ObAstNode.NodeType.fassung) {
			m_currentFassung = "";
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.verse) {
			ObVerseNode verse = (ObVerseNode)node;
			addStopTag();
			String verseTag = m_verseTagStart + verse.getNumber();
			m_currentFassung += "<verse osisID=\"" + verseTag + "\" sID=\"" + verseTag + "\"/>";
			m_verseStopTag = "<verse eID=\"" + verseTag + "\"/>\n";
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.note) {
			m_currentFassung += "<note type=\"x-footnote\" n=\"" + m_noteIndexCounter + "\">";
			incrementNoteCounter();
		}
	}

	private void addStopTag() {
		if(m_verseStopTag != null) {
			m_currentFassung += m_verseStopTag;
			m_verseStopTag = null;
		}
	}

	public void visit(ObTreeNode node) throws Throwable
	{
		ObAstNode astNode = (ObAstNode)node;
		
		if(astNode.getNodeType() == ObAstNode.NodeType.text) {
			ObTextNode text = (ObTextNode)node;
			m_currentFassung += text.getText();
		}
	}

	public void visitAfter(ObTreeNode node) throws Throwable
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
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.note) {
			m_currentFassung += "</note>";
		}
	}

	public String getStudienFassung() {
		return m_studienFassung;
	}

	public String getLeseFassung() {
		return m_leseFassung;
	}
}
