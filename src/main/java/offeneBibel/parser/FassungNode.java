package offeneBibel.parser;

public class FassungNode extends AstNode {
    private static final long serialVersionUID = 1L;

    public enum FassungType { lesefassung, studienfassung };
    private final FassungType fassung;

    public FassungNode(FassungType fassung) {
        super(NodeType.fassung);
        this.fassung = fassung;
    }

    public FassungType getFassung() {
        return fassung;
    }
}
