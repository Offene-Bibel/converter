package offeneBibel.parser;

public class ObVerseNode extends ObAstNode {
	private int m_fromNumber;
	private int m_toNumber;
	
	public ObVerseNode(int number) {
		super(NodeType.verse);
		m_fromNumber = number;
		m_toNumber = number;
	}
	
	public ObVerseNode(int fromNumber, int toNumber) {
		super(NodeType.verse);
		m_fromNumber = fromNumber;
		m_toNumber = toNumber;
	}
	
	public boolean isSingleVerse() {
		return m_fromNumber == m_toNumber;
	}
	
	public int getNumber() {
		return m_fromNumber;
	}
	
	public int getFromNumber() {
		return m_fromNumber;
	}
	
	public int getToNumber() {
		return m_toNumber;
	}
}
