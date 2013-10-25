package offeneBibel.parser;

public class ObTextNode extends ObAstNode { // implements IVisitorHost<ObAstNode> {
    private String m_text;

    public ObTextNode(String text)
    {
        super(NodeType.text);
        m_text = text;
    }

    public String getText() {
        return m_text;
    }

    public boolean setText(String text) {
        m_text = text;
        return true;
    }

    public void appendText(String text) {
        m_text += text;
    }
}
