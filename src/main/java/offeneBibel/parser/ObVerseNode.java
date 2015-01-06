package offeneBibel.parser;

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
        ObFassungNode fassungNode = getFassungNode();
        FassungType fassung = fassungNode.getFassung();
        return getStatus(fassung);
    }

    public ObVerseStatus getStatus(FassungType fassung) {
        ObChapterNode chapterNode = getChapterNode();

        ObVerseStatus status = ObVerseStatus.none;
        // whether the last set status was a generic status or one with a range, specific ones have precedence
        boolean isLastTagSpecific = false;

        for (ObChapterTag tag  : chapterNode.getChapterTags()) {
            if(tag.getTag().doesMatchFassung(fassung)) {
                if(tag.tagAppliesToVerse(m_fromNumber, m_toNumber)) {
                    // tag applies to this verse
                    if(status == ObVerseStatus.none) {
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

    private ObChapterNode getChapterNode() {
        ObAstNode runner = this;
        while(runner != null && ! ObChapterNode.class.isInstance(runner)) {
            runner = runner.getParent();
        }

        if (runner == null)
            throw new RuntimeException("ObVerseNode.getChapterNode() called, but was unable to retrieve chapter node.");

        return (ObChapterNode) runner;
    }

    private ObFassungNode getFassungNode() {
        ObAstNode runner = this;
        while(runner != null && ! ObFassungNode.class.isInstance(runner)) {
            runner = runner.getParent();
        }

        if (runner == null)
            throw new RuntimeException("ObVerseNode.getFassungNode() called, but was unable to retrieve fassung node.");

        return (ObFassungNode) runner;
    }
}
