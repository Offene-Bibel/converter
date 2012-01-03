package offeneBibel.parser;

public class ObVerseNode extends ObAstNode {
	private int m_number;
	
	public ObVerseNode(int number) {
		super(NodeType.verse);
		m_number = number;
	}
	
	public int getNumber() {
		return m_number;
	}
}
