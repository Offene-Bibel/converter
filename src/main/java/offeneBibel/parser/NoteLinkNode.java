package offeneBibel.parser;

/**
 * A note link object is a reference to another note.
 * It is constructed in two steps.
 * 1. During parsing only the note tag will be set.
 * 2. In a second run the target will be searched and set.
 */
public class NoteLinkNode extends AstNode {
    private static final long serialVersionUID = 1L;
    private String noteTag;
    private NoteNode linkTarget;

    public NoteLinkNode(String tag) {
        super(NodeType.noteLink);
        noteTag = tag;
        linkTarget = null;
    }

    public String getTag() {
        return noteTag;
    }

    public void setLinkTarget(NoteNode target) {
        linkTarget = target;
    }

    public NoteNode getLinkTarget() {
        return linkTarget;
    }
}
