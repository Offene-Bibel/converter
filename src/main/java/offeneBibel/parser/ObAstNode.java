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
	
	public ObAstNode(NodeType type)
	{
		setNodeType(type);
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
