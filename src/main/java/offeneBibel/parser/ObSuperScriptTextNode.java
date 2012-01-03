package offeneBibel.parser;

public class ObSuperScriptTextNode extends ObAstNode {
	private String m_text;
	
	public ObSuperScriptTextNode(String text) {
		super(NodeType.superScript);
		m_text = text;
	}
	
	public String getText() {
		return m_text;
	}
}
