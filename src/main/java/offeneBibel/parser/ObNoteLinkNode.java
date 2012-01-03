package offeneBibel.parser;

/**
 * A note link object is a reference to another note.
 * It is constructed in two steps.
 * 1. During parsing only the note tag will be set.
 * 2. In a second run the target will be searched and set.
 */
public class ObNoteLinkNode extends ObAstNode {
	private String m_noteTag;
	private ObNoteNode m_linkTarget;
	
	public ObNoteLinkNode(String tag) {
		super(NodeType.note);
		m_noteTag = tag;
		m_linkTarget = null;
	}
	
	public String getTag() {
		return m_noteTag;
	}
	
	public void setLinkTarget(ObNoteNode target) {
		m_linkTarget = target;
	}
	
	public ObNoteNode getLinkTarget() {
		return m_linkTarget;
	}
}
