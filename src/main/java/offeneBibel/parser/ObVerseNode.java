package offeneBibel.parser;

import offeneBibel.parser.ObChapterTag.ChapterTagName;
import offeneBibel.parser.ObFassungNode.FassungType;

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
	
	public ObVerseStatus getStatus() {
		ObChapterNode chapterNode = getChapterNode();
		ObFassungNode fassungNode = getFassungNode();
		FassungType fassung = fassungNode.getFassung();
		
		ObVerseStatus status = ObVerseStatus.none;
		// whether the last set status was a generic status or one with a range, specific ones have precedence
		boolean isLastTagSpecific = false;
		
		for (ObChapterTag tag  : chapterNode.getChapterTags()) {
			if(tag.getTag().doesMatchFassung(fassung)) {
				if(tag.getStartVerse() == 0 && tag.getStopVerse() == 0 ||
						tag.getStartVerse() <= this.getToNumber() && tag.getStopVerse() >= this.getFromNumber()) {
					// tag applies to this verse
					if(status == ObVerseStatus.none) {
						status = tag.getTag().getVerseStatus();
						isLastTagSpecific = tag.isSpecific();
					}
					else if(isLastTagSpecific == false) {
						if(tag.isSpecific()) { // specific has precedence
							isLastTagSpecific = true;
							status = tag.getTag().getVerseStatus();
						}
						else {
							if(tag.getTag().getVerseStatus().ordinal() < status.ordinal()) { // lower status has precedence
								status = tag.getTag().getVerseStatus();
							}
						}
					}
					else { //isLastTagSpecific == true
						if(tag.isSpecific() && tag.getTag().getVerseStatus().ordinal() < status.ordinal())
							status = tag.getTag().getVerseStatus();
					}
				}
					
			}
		}
		
		return status;
	}
	
	private ObChapterNode getChapterNode() {
		ObTreeNode runner = this;
		while(runner != null && ! ObChapterNode.class.isInstance(runner)) {
			runner = runner.getParent();
		}
		
		if (runner == null)
			throw new RuntimeException("ObVerseNode.getChapterNode() called, but was unable to retrieve chapter node.");
		
		return (ObChapterNode) runner;
	}
	
	private ObFassungNode getFassungNode() {
		ObTreeNode runner = this;
		while(runner != null && ! ObFassungNode.class.isInstance(runner)) {
			runner = runner.getParent();
		}
		
		if (runner == null)
			throw new RuntimeException("ObVerseNode.getFassungNode() called, but was unable to retrieve fassung node.");
		
		return (ObFassungNode) runner;
	}
}
