package offeneBibel.parser;

public class WikiLinkNode extends AstNode {
    private static final long serialVersionUID = 1L;
    private String link;
    private boolean wikiLink;

    public WikiLinkNode(String link, boolean wikiLink) {
        super(NodeType.wikiLink);
        this.link = link;
        this.wikiLink = wikiLink;
    }

    public String getLink() {
        return link;
    }
    
    public boolean isWikiLink() {
        return wikiLink;
    }
}
