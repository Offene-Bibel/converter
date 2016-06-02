package offeneBibel.parser;

public class NoteNode extends AstNode {
    private static final long serialVersionUID = 1L;
    private String noteTag;

    public NoteNode() {
        super(NodeType.note);
        noteTag = null;
    }

    public NoteNode(String tag) {
        super(NodeType.note);
        noteTag = tag;
    }

    public String getTag() {
        return noteTag;
    }
}
