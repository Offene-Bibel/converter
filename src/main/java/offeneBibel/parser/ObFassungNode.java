package offeneBibel.parser;

public class ObFassungNode extends ObAstNode {
    private static final long serialVersionUID = 1L;

    public enum FassungType { lesefassung, studienfassung };
    private final FassungType m_fassung;

    public ObFassungNode(FassungType fassung) {
        super(NodeType.fassung);
        m_fassung = fassung;
    }

    public FassungType getFassung() {
        return m_fassung;
    }
}
