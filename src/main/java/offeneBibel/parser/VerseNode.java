package offeneBibel.parser;

import offeneBibel.parser.FassungNode.FassungType;

public class VerseNode extends AstNode {
    private static final long serialVersionUID = 1L;
    private int m_fromNumber;
    private int m_toNumber;
    private VerseStatus m_statusOverride = null;

    public VerseNode(int number) {
        super(NodeType.verse);
        m_fromNumber = number;
        m_toNumber = number;
    }

    public VerseNode(int fromNumber, int toNumber) {
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
    
    public void setStatusOverride(VerseStatus status) {
    	m_statusOverride = status;
    }

    public VerseStatus getStatus() {
        FassungNode fassungNode = getFassungNode();
        FassungType fassung = fassungNode.getFassung();
        return getStatus(fassung);
    }

    public VerseStatus getStatus(FassungType fassung) {
    	if(m_statusOverride != null) {
    		return m_statusOverride;
    	}
    	else {
	        ChapterNode chapterNode = getChapterNode();
	
	        VerseStatus status = VerseStatus.none;
	        // whether the last set status was a generic status or one with a range, specific ones have precedence
	        boolean isLastTagSpecific = false;
	
	        for (ChapterTag tag  : chapterNode.getChapterTags()) {
	            if(tag.getTag().doesMatchFassung(fassung)) {
	                if(tag.tagAppliesToVerse(m_fromNumber, m_toNumber)) {
	                    // tag applies to this verse
	                    if(status == VerseStatus.none) {
	                        status = tag.getTag().getVerseStatus(fassung);
	                        isLastTagSpecific = tag.isSpecific();
	                    }
	                    else if(isLastTagSpecific == false) {
	                        if(tag.isSpecific()) { // specific has precedence
	                            isLastTagSpecific = true;
	                            status = tag.getTag().getVerseStatus(fassung);
	                        }
	                        else {
	                            if(tag.getTag().getVerseStatus(fassung).ordinal() < status.ordinal()) { // lower status has precedence
	                                status = tag.getTag().getVerseStatus(fassung);
	                            }
	                        }
	                    }
	                    else { //isLastTagSpecific == true
	                        if(tag.isSpecific() && tag.getTag().getVerseStatus(fassung).ordinal() < status.ordinal())
	                            status = tag.getTag().getVerseStatus(fassung);
	                    }
	                }
	
	            }
	        }
	        return status;
    	}
    }

    private ChapterNode getChapterNode() {
        AstNode runner = this;
        while(runner != null && ! ChapterNode.class.isInstance(runner)) {
            runner = runner.getParent();
        }

        if (runner == null)
            throw new RuntimeException("ObVerseNode.getChapterNode() called, but was unable to retrieve chapter node.");

        return (ChapterNode) runner;
    }

    private FassungNode getFassungNode() {
        AstNode runner = this;
        while(runner != null && ! FassungNode.class.isInstance(runner)) {
            runner = runner.getParent();
        }

        if (runner == null)
            throw new RuntimeException("ObVerseNode.getFassungNode() called, but was unable to retrieve fassung node.");

        return (FassungNode) runner;
    }
}
