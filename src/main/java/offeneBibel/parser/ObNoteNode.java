package offeneBibel.parser;

public class ObNoteNode extends ObAstNode {
    private String m_noteTag;

    public ObNoteNode() {
        super(NodeType.note);
        m_noteTag = null;
    }

    public ObNoteNode(String tag) {
        super(NodeType.note);
        m_noteTag = tag;
    }

    public String getTag() {
        return m_noteTag;
    }
}
