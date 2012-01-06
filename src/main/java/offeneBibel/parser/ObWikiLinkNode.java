package offeneBibel.parser;

public class ObWikiLinkNode extends ObAstNode {
	private String m_linkText;
	
	public ObWikiLinkNode(String linkText) {
		super(NodeType.wikiLink);
		m_linkText = linkText;
	}
	
	public String getLinkText() {
		return m_linkText;
	}
}
