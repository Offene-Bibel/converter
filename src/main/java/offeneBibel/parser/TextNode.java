package offeneBibel.parser;

public class TextNode extends AstNode { // implements IVisitorHost<ObAstNode> {
    private static final long serialVersionUID = 1L;
    private final StringBuilder text;

    public TextNode(String text)
    {
        super(NodeType.text);
        this.text = new StringBuilder(text);
    }

    public String getText() {
        return text.toString();
    }

    public boolean setText(String text) {
        this.text.setLength(0);
        this.text.append(text);
        return true;
    }

    public void appendText(String text) {
        this.text.append(text);
    }
}
