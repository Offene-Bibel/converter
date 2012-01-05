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
			String verseTag = m_verseTagStart + verse.getNumber();
			m_currentFassung += "<verse osisID=\"" + verseTag + "\" sID=\"" + verseTag + "\"/>";
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
			if(fassung.getFassung() == ObFassungNode.FassungType.lesefassung) {
				m_leseFassung = m_currentFassung;
			}
			else {
				m_studienFassung = m_currentFassung;
			}
		}
		
		else if(astNode.getNodeType() == ObAstNode.NodeType.verse) {
			ObVerseNode verse = (ObVerseNode)node;
			String verseTag = m_verseTagStart + verse.getNumber();
			m_currentFassung += "<verse eID=\"" + verseTag + "\"/>\n";
		}
	}

	public String getStudienFassung() {
		return m_studienFassung;
	}

	public String getLeseFassung() {
		return m_leseFassung;
	}
}
