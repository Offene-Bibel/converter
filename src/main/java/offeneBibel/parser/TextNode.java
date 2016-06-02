package offeneBibel.parser;

public class TextNode extends AstNode { // implements IVisitorHost<ObAstNode> {
    private static final long serialVersionUID = 1L;
    private final StringBuilder m_text;

    public TextNode(String text)
    {
        super(NodeType.text);
        m_text = new StringBuilder(text);
    }

    public String getText() {
        return m_text.toString();
    }

    public boolean setText(String text) {
        m_text.setLength(0);
        m_text.append(text);
        return true;
    }

    public void appendText(String text) {
        m_text.append(text);
    }
}
