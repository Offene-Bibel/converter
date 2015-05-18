package offeneBibel.parser;

public class ObAstNode extends ObTreeNode<ObAstNode> { // implements IVisitorHost<ObAstNode> {
    public enum NodeType {
        fassung,
        fassungNotes,
        chapter,
        verse,
        text,
        fat,
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
        alternateReading,
        secondaryContent,
        secondVoice,
        strikeThrough,
        underline,
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

    public boolean isDescendantOf(NodeType nodeType) {
        ObAstNode runner =  this;
        while(runner != null && runner.getNodeType() != nodeType)
            runner = runner.getParent();

        if (runner == null)
            return false;
        else
            return true;
    }
}
