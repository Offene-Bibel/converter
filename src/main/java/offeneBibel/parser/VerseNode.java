package offeneBibel.parser;

import offeneBibel.parser.FassungNode.FassungType;

public class VerseNode extends AstNode {
    private static final long serialVersionUID = 1L;
    private int fromNumber;
    private int toNumber;
    private VerseStatus statusOverride = null;

    public VerseNode(int number) {
        super(NodeType.verse);
        fromNumber = number;
        toNumber = number;
    }

    public VerseNode(int fromNumber, int toNumber) {
        super(NodeType.verse);
        this.fromNumber = fromNumber;
        this.toNumber = toNumber;
    }

    public boolean isSingleVerse() {
        return fromNumber == toNumber;
    }

    public int getNumber() {
        return fromNumber;
    }

    public int getFromNumber() {
        return fromNumber;
    }

    public int getToNumber() {
        return toNumber;
    }
    
    public void setStatusOverride(VerseStatus status) {
    	statusOverride = status;
    }

    public VerseStatus getStatus() {
        FassungNode fassungNode = getFassungNode();
        FassungType fassung = fassungNode.getFassung();
        return getStatus(fassung);
    }

    public VerseStatus getStatus(FassungType fassung) {
    	if(statusOverride != null) {
    		return statusOverride;
    	}
    	else {
	        ChapterNode chapterNode = getChapterNode();
	
	        VerseStatus status = VerseStatus.none;
	        // whether the last set status was a generic status or one with a range, specific ones have precedence
	        boolean isLastTagSpecific = false;
	
	        for (ChapterTag tag  : chapterNode.getChapterTags()) {
	            if(tag.getTag().doesMatchFassung(fassung)) {
	                if(tag.tagAppliesToVerse(fromNumber, toNumber)) {
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
