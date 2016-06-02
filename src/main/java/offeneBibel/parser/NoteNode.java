package offeneBibel.parser;

public class NoteNode extends AstNode {
    private static final long serialVersionUID = 1L;
    private String m_noteTag;

    public NoteNode() {
        super(NodeType.note);
        m_noteTag = null;
    }

    public NoteNode(String tag) {
        super(NodeType.note);
        m_noteTag = tag;
    }

    public String getTag() {
        return m_noteTag;
    }
}
