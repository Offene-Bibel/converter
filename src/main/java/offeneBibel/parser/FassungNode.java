package offeneBibel.parser;

public class FassungNode extends AstNode {
    private static final long serialVersionUID = 1L;

    public enum FassungType { lesefassung, studienfassung };
    private final FassungType m_fassung;

    public FassungNode(FassungType fassung) {
        super(NodeType.fassung);
        m_fassung = fassung;
    }

    public FassungType getFassung() {
        return m_fassung;
    }
}
