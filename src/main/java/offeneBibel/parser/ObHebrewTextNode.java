package offeneBibel.parser;

public class ObHebrewTextNode extends ObAstNode {
	private String m_text;
	
	public ObHebrewTextNode(String text) {
		super(NodeType.hebrew);
		m_text = text;
	}
	
	public String getText() {
		return m_text;
	}
}
