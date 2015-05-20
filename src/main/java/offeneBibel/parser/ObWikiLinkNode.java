package offeneBibel.parser;

public class ObWikiLinkNode extends ObAstNode {
    private String m_link;
    private boolean m_wikiLink;

    public ObWikiLinkNode(String link, boolean wikiLink) {
        super(NodeType.wikiLink);
        m_link = link;
        m_wikiLink = wikiLink;
    }

    public String getLink() {
        return m_link;
    }
    
    public boolean isWikiLink() {
    	return m_wikiLink;
    }
}
