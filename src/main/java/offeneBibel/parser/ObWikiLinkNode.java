package offeneBibel.parser;

public class ObWikiLinkNode extends ObAstNode {
	private String m_text;
	private String m_linkText;
	
	public ObWikiLinkNode(String text, String linkText) {
		super(NodeType.wikiLink);
		m_text = text;
		m_linkText = linkText;
	}
	
	public String getText() {
		return m_text;
	}
	
	public String getLinkText() {
		return m_linkText;
	}
}
