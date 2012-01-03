package offeneBibel.parser;

public class ObAstNode extends ObTreeNode { // implements IVisitorHost<ObAstNode> {
	public enum NodeType {
		fassung,
		chapter,
		verse,
		text,
		emphasis,
		insertion,
		omission,
		alternative,
		poemStart,
		poemStop,
		textBreak,
		parallelPassage,
		note,
		hebrew,
		wikiLink,
		superScript
	}
	private NodeType m_nodeType;
	private String m_text = null;
	
	public ObAstNode(NodeType type)
	{
		setNodeType(type);
	}

	public ObAstNode(NodeType type, String text)
	{
		setNodeType(type);
		m_text = text;
	}

	public String getText() {
		return m_text;
	}

	public boolean setText(String text) {
		m_text = text;
		return true;
	}

	public NodeType getNodeType() {
		return m_nodeType;
	}

	public boolean setNodeType(NodeType nodeType) {
		m_nodeType = nodeType;
		return true;
	}
	
	/*public void host(IVisitor<ObAstNode> visitor) throws Throwable {
		visitor.visitBefore(this);
		visitor.visit(this);
		for(ObAstNode child : m_children) {
			child.host(visitor);
		}
		visitor.visitAfter(this);
	}*/
}
