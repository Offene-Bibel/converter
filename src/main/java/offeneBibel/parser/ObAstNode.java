package offeneBibel.parser;

public class ObAstNode extends ObTreeNode<ObAstNode> { // implements IVisitorHost<ObAstNode> {
	public enum NodeType {
		fassung,
		fassungNotes,
		chapter,
		verse,
		text,
		emphasis,
		italics,
		insertion,
		omission,
		alternative,
		poemStart,
		poemStop,
		textBreak,
		parallelPassage,
		note,
		noteLink,
		hebrew,
		wikiLink,
		superScript,
		heading,
		quote,
		chapterNotes,
		alternateReading
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
}
